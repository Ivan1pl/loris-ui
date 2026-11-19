package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.imgui.backend.impl.DecodedImage;
import com.ivan1pl.lorisui.imgui.vulkan.VulkanTextures;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Vulkan implementation of {@link ImGuiBackend}, which loads textures from class-path resources into Vulkan images
 * on an application's device and releases them when they are unloaded. Hosts that render with
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlVulkanRenderer} use this backend.
 *
 * <p>Image textures are created in the {@code R8G8B8A8_SRGB} format with linear sampling, which is why the renderer
 * needs an sRGB color attachment format. Canvas textures are created in the {@code R8G8B8A8_UNORM} format, which is
 * not sRGB, with nearest-neighbor sampling, so canvas pixels look like the same colors in widgets and are drawn as
 * sharp squares when scaled.
 *
 * <p><b>Queue.</b> The queue follows the queue rule of
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlVulkanRenderer ImGuiSdlVulkanRenderer}. Each texture is uploaded with a
 * command buffer of its own, which is submitted to that queue and waited for before loading returns. Likewise, each
 * edited canvas costs one submission and one wait, together with a staging allocation, in every frame in which it is
 * drawn with changed pixels. That wait also waits for everything submitted to the queue before it, including the
 * frames still in flight, so frames with canvas edits run with one frame in flight. Unloading a texture, or
 * releasing the texture of a canvas that is no longer drawn, waits for the queue to go idle before the texture is
 * destroyed, once per texture. Submissions to the queue from other threads must not overlap LorisUI calls, as
 * described for the renderer.
 *
 * <p><b>Lifetime.</b> The backend does not own the device or the queue. The device must stay alive until every
 * texture the backend has loaded is released with {@link #unloadTextures()}, which the renderer does when it is
 * disposed. A texture must not be unloaded while a command buffer that was recorded with it has not been submitted
 * yet.
 *
 * <p>The backend is used on the host's UI thread only.
 */
public final class ImGuiVulkanBackend extends ImGuiBackend {
    private final VkDevice device;
    private final int queueFamilyIndex;
    private final VkQueue queue;
    private final Map<Long, VulkanTextures.Texture> textures = new HashMap<>();
    private final Set<Long> canvasTextureIds = new HashSet<>();
    private long linearSampler = VK10.VK_NULL_HANDLE;
    private long nearestSampler = VK10.VK_NULL_HANDLE;
    private long descriptorSetLayout = VK10.VK_NULL_HANDLE;

    /**
     * Creates a Vulkan backend for the given device and queue, with the built-in renderers and style appliers. Makes no
     * native calls.
     *
     * <p>{@code queue} must follow the queue rule of
     * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlVulkanRenderer ImGuiSdlVulkanRenderer}, which is not checked.
     *
     * @param device           the application's Vulkan device; must not be null
     * @param queueFamilyIndex index of the queue family of {@code queue}; must not be negative
     * @param queue            the application's graphics queue, created from {@code device}; must not be null
     * @throws IllegalArgumentException if {@code queueFamilyIndex} is negative (checked first), or if {@code queue}
     *                                  does not belong to {@code device}
     * @throws NullPointerException     if {@code device} or {@code queue} is null
     */
    public ImGuiVulkanBackend(VkDevice device, int queueFamilyIndex, VkQueue queue) {
        if (queueFamilyIndex < 0) {
            throw new IllegalArgumentException("queueFamilyIndex must not be negative: " + queueFamilyIndex);
        }
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(queue, "queue");
        if (queue.getDevice() != device) {
            throw new IllegalArgumentException("queue must belong to device");
        }
        this.device = device;
        this.queueFamilyIndex = queueFamilyIndex;
        this.queue = queue;
    }

    /**
     * Returns the Vulkan device that this backend creates its textures on.
     *
     * @return the device passed to the constructor, never {@code null}
     */
    public VkDevice getDevice() {
        return device;
    }

    /**
     * Returns the index of the queue family of the queue that this backend submits its uploads to.
     *
     * @return the queue family index passed to the constructor, never negative
     */
    public int getQueueFamilyIndex() {
        return queueFamilyIndex;
    }

    /**
     * Returns the queue that this backend submits its uploads to.
     *
     * @return the queue passed to the constructor, never {@code null}
     */
    public VkQueue getQueue() {
        return queue;
    }

    /**
     * Loads an image from a class-path resource into a new Vulkan texture, optionally recoloring it with channel
     * masks. Common image formats such as PNG and JPEG are supported.
     *
     * <p>When any mask is non-null, every pixel is recolored with {@link Color#mask(Color, Color, Color)
     * Color.mask(maskedR, maskedG, maskedB)}; when all masks are {@code null}, the image is loaded unchanged.
     *
     * <p>Called on the host's UI thread. The pixels are uploaded with a command buffer of their own, which is
     * submitted to the backend's queue and waited for before this method returns.
     *
     * @param resourcePath class-path resource name of the image, without a leading {@code /}; never {@code null}
     * @param maskedR      color replacing the red channel, or {@code null} to keep it
     * @param maskedG      color replacing the green channel, or {@code null} to keep it
     * @param maskedB      color replacing the blue channel, or {@code null} to keep it
     * @return the new texture, whose ID is its {@code VkDescriptorSet} handle; never {@code null}
     * @throws IOException           if the resource does not exist or cannot be decoded as an image (the message names
     *                               {@code resourcePath}), or if it cannot be read
     * @throws IllegalStateException if Vulkan fails to create or upload the texture; the message names the Vulkan
     *                               function, its {@code VkResult} and {@code resourcePath}, and nothing created for
     *                               the texture is left behind
     */
    @Override
    protected TextureData createTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        try (DecodedImage image = DecodedImage.load(resourcePath, maskedR, maskedG, maskedB)) {
            long id = upload(image.getPixels(), image.getWidth(), image.getHeight(), VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_FILTER_LINEAR, "texture " + resourcePath);
            return new TextureData(id, image.getWidth(), image.getHeight());
        }
    }

    /**
     * Creates a new Vulkan texture for a canvas, as described for {@link ImGuiBackend#createCanvasTexture}. The
     * texture has the {@code R8G8B8A8_UNORM} format, which is not sRGB, and is sampled with nearest-neighbor filtering
     * clamped to the edge.
     *
     * <p>Called on the host's UI thread. The pixels are uploaded with a command buffer of their own, which is
     * submitted to the backend's queue and waited for before this method returns.
     *
     * @param width      width of the texture in pixels
     * @param height     height of the texture in pixels
     * @param rgbaPixels tightly packed RGBA pixels of the whole texture; never {@code null}
     * @return the new texture, whose ID is its {@code VkDescriptorSet} handle; never {@code null}
     * @throws IllegalStateException if Vulkan fails to create or upload the texture; the message names the Vulkan
     *                               function, its {@code VkResult} and the canvas texture with its size, and nothing
     *                               created for the texture is left behind
     */
    @Override
    protected TextureData createCanvasTexture(int width, int height, ByteBuffer rgbaPixels) {
        long id = upload(rgbaPixels, width, height, VK10.VK_FORMAT_R8G8B8A8_UNORM, VK10.VK_FILTER_NEAREST,
                canvasLabel(width, height));
        canvasTextureIds.add(id);
        return new TextureData(id, width, height);
    }

    /**
     * Replaces a rectangle of a canvas texture, as described for {@link ImGuiBackend#updateCanvasTexture}.
     *
     * <p>Called on the host's UI thread. The pixels are copied with a command buffer of their own, which is submitted
     * to the backend's queue and waited for before this method returns. The copy waits for earlier submissions that
     * sample the texture.
     *
     * @param texture    texture to update, created by {@link #createCanvasTexture}; never {@code null}
     * @param x          left edge of the rectangle in texture pixels
     * @param y          top edge of the rectangle in texture pixels
     * @param width      width of the rectangle in pixels
     * @param height     height of the rectangle in pixels
     * @param rgbaPixels tightly packed RGBA pixels of the rectangle; never {@code null}
     * @throws IllegalStateException if Vulkan fails to upload the pixels; the message names the Vulkan function, its
     *                               {@code VkResult} and the canvas texture with its size
     */
    @Override
    protected void updateCanvasTexture(TextureData texture, int x, int y, int width, int height,
                                       ByteBuffer rgbaPixels) {
        VulkanTextures.Texture vulkanTexture = textures.get(texture.id());
        VulkanTextures.update(device, queueFamilyIndex, queue, rgbaPixels, x, y, width, height, vulkanTexture.image(),
                canvasLabel(texture.width(), texture.height()));
    }

    /**
     * Waits for the backend's queue to go idle, then destroys the Vulkan texture. Called on the host's UI thread.
     *
     * @param texture texture to release; never {@code null}
     * @throws IllegalStateException if waiting for the queue fails; the texture is destroyed anyway
     */
    @Override
    protected void deleteTexture(TextureData texture) {
        VulkanTextures.Texture vulkanTexture = textures.remove(texture.id());
        String what = canvasTextureIds.remove(texture.id())
                ? "releasing " + canvasLabel(texture.width(), texture.height())
                : "unloading textures";
        DeferredRelease.runAll(
                () -> VulkanTextures.check(VK10.vkQueueWaitIdle(queue), "vkQueueWaitIdle", what),
                () -> VulkanTextures.destroy(device, vulkanTexture),
                () -> {
                    if (textures.isEmpty()) {
                        destroySharedObjects();
                    }
                });
    }

    private long upload(ByteBuffer rgba, int width, int height, int format, int filter, String what) {
        VulkanTextures.Texture texture;
        try {
            long sampler = sampler(filter, what);
            if (descriptorSetLayout == VK10.VK_NULL_HANDLE) {
                descriptorSetLayout = VulkanTextures.createDescriptorSetLayout(device, what);
            }
            texture = VulkanTextures.upload(device, queueFamilyIndex, queue, rgba, width, height, format, sampler,
                    descriptorSetLayout, what);
        } catch (Throwable failure) {
            if (textures.isEmpty()) {
                DeferredRelease.runAfterFailure(failure, this::destroySharedObjects);
            }
            throw failure;
        }
        textures.put(texture.descriptorSet(), texture);
        return texture.descriptorSet();
    }

    private long sampler(int filter, String what) {
        if (filter == VK10.VK_FILTER_NEAREST) {
            if (nearestSampler == VK10.VK_NULL_HANDLE) {
                nearestSampler = VulkanTextures.createSampler(device, filter, what);
            }
            return nearestSampler;
        }
        if (linearSampler == VK10.VK_NULL_HANDLE) {
            linearSampler = VulkanTextures.createSampler(device, filter, what);
        }
        return linearSampler;
    }

    private void destroySharedObjects() {
        long oldLinearSampler = linearSampler;
        long oldNearestSampler = nearestSampler;
        long oldDescriptorSetLayout = descriptorSetLayout;
        linearSampler = VK10.VK_NULL_HANDLE;
        nearestSampler = VK10.VK_NULL_HANDLE;
        descriptorSetLayout = VK10.VK_NULL_HANDLE;
        // Destroying VK_NULL_HANDLE is a no-op, so an object that was never created is skipped.
        DeferredRelease.runAll(
                () -> VK10.vkDestroySampler(device, oldLinearSampler, null),
                () -> VK10.vkDestroySampler(device, oldNearestSampler, null),
                () -> VK10.vkDestroyDescriptorSetLayout(device, oldDescriptorSetLayout, null));
    }

    private static String canvasLabel(int width, int height) {
        return "canvas texture " + width + "x" + height;
    }
}
