package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.imgui.backend.impl.DecodedImage;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLGPU;
import org.lwjgl.sdl.SDL_GPUTextureCreateInfo;
import org.lwjgl.sdl.SDL_GPUTextureRegion;
import org.lwjgl.sdl.SDL_GPUTextureTransferInfo;
import org.lwjgl.sdl.SDL_GPUTransferBufferCreateInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The SDL_GPU implementation of {@link ImGuiBackend}, which loads textures from class-path resources into SDL_GPU
 * textures on an application's GPU device and releases them when they are unloaded. Hosts that render with
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGpuRenderer} use this backend.
 *
 * <p>Image textures are created in the {@code R8G8B8A8_UNORM_SRGB} format, so images look the same as on the OpenGL
 * backend when the window's swapchain uses the {@code SDR_LINEAR} composition. Canvas textures are created in the
 * {@code R8G8B8A8_UNORM} format, which is not sRGB, so canvas pixels look like the same colors in widgets. Canvas
 * textures are sampled with linear filtering, like every texture of this backend, because the renderer binds one
 * linear sampler for all textures, so scaled canvas pixels are smoothed instead of drawn as sharp squares. Each
 * texture, and each canvas update, is uploaded with its own command buffer, which is submitted at once.
 *
 * <p>The backend does not own the GPU device. The device must stay alive until every texture the backend has loaded
 * is released with {@link #unloadTextures()}, which the renderer does when it is disposed.
 *
 * <p>The backend is used on the host's UI thread only.
 */
public final class ImGuiSdlGpuBackend extends ImGuiBackend {
    private final long gpuDevice;

    /**
     * Creates an SDL_GPU backend for the given GPU device, with the built-in renderers and style appliers. Makes no
     * native calls.
     *
     * @param gpuDevice handle of the application's {@code SDL_GPUDevice}; must not be {@code 0}
     * @throws IllegalArgumentException if {@code gpuDevice} is {@code 0}
     */
    public ImGuiSdlGpuBackend(long gpuDevice) {
        if (gpuDevice == 0L) {
            throw new IllegalArgumentException("gpuDevice must not be 0");
        }
        this.gpuDevice = gpuDevice;
    }

    /**
     * Returns the handle of the GPU device that this backend creates its textures on.
     *
     * @return the {@code SDL_GPUDevice} handle passed to the constructor, never {@code 0}
     */
    public long getGpuDevice() {
        return gpuDevice;
    }

    /**
     * Loads an image from a class-path resource into a new SDL_GPU texture, optionally recoloring it with channel
     * masks. Common image formats such as PNG and JPEG are supported.
     *
     * <p>When any mask is non-null, every pixel is recolored with {@link Color#mask(Color, Color, Color)
     * Color.mask(maskedR, maskedG, maskedB)}; when all masks are {@code null}, the image is loaded unchanged.
     *
     * <p>Called on the host's UI thread. The pixels are uploaded with a command buffer of their own, which is
     * submitted before this method returns.
     *
     * @param resourcePath class-path resource name of the image, without a leading {@code /}; never {@code null}
     * @param maskedR      color replacing the red channel, or {@code null} to keep it
     * @param maskedG      color replacing the green channel, or {@code null} to keep it
     * @param maskedB      color replacing the blue channel, or {@code null} to keep it
     * @return the new texture, never {@code null}
     * @throws IOException           if the resource does not exist or cannot be decoded as an image (the message names
     *                               {@code resourcePath}), or if it cannot be read
     * @throws IllegalStateException if SDL fails to create or upload the texture; the message names
     *                               {@code resourcePath} and the SDL error, and nothing created for the texture is
     *                               left behind
     */
    @Override
    protected TextureData createTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        try (DecodedImage image = DecodedImage.load(resourcePath, maskedR, maskedG, maskedB)) {
            return createAndUpload(SDLGPU.SDL_GPU_TEXTUREFORMAT_R8G8B8A8_UNORM_SRGB, "texture " + resourcePath,
                    image.getPixels(), image.getWidth(), image.getHeight());
        }
    }

    /**
     * Releases the SDL_GPU texture. Called on the host's UI thread.
     *
     * @param texture texture to release; never {@code null}
     */
    @Override
    protected void deleteTexture(TextureData texture) {
        SDLGPU.SDL_ReleaseGPUTexture(gpuDevice, texture.id());
    }

    /**
     * Creates a new SDL_GPU texture for a canvas, as described for {@link ImGuiBackend#createCanvasTexture}. The
     * texture has the {@code R8G8B8A8_UNORM} format, which is not sRGB. It is sampled with linear filtering, because
     * the renderer binds one linear sampler for all textures.
     *
     * <p>Called on the host's UI thread. The pixels are uploaded with a command buffer of their own, which is
     * submitted before this method returns.
     *
     * @param width      width of the texture in pixels
     * @param height     height of the texture in pixels
     * @param rgbaPixels tightly packed RGBA pixels of the whole texture; never {@code null}
     * @return the new texture, never {@code null}
     * @throws IllegalStateException if SDL fails to create or upload the texture; the message names the canvas texture
     *                               and its size, and the SDL error, and nothing created for the texture is left
     *                               behind
     */
    @Override
    protected TextureData createCanvasTexture(int width, int height, ByteBuffer rgbaPixels) {
        return createAndUpload(SDLGPU.SDL_GPU_TEXTUREFORMAT_R8G8B8A8_UNORM, canvasLabel(width, height), rgbaPixels,
                width, height);
    }

    /**
     * Replaces a rectangle of a canvas texture, as described for {@link ImGuiBackend#updateCanvasTexture}.
     *
     * <p>Called on the host's UI thread. The pixels are uploaded with a command buffer of their own, which is
     * submitted before this method returns.
     *
     * @param texture    texture to update, created by {@link #createCanvasTexture}; never {@code null}
     * @param x          left edge of the rectangle in texture pixels
     * @param y          top edge of the rectangle in texture pixels
     * @param width      width of the rectangle in pixels
     * @param height     height of the rectangle in pixels
     * @param rgbaPixels tightly packed RGBA pixels of the rectangle; never {@code null}
     * @throws IllegalStateException if SDL fails to upload the pixels; the message names the canvas texture and its
     *                               size, and the SDL error
     */
    @Override
    protected void updateCanvasTexture(TextureData texture, int x, int y, int width, int height,
                                       ByteBuffer rgbaPixels) {
        upload(canvasLabel(texture.width(), texture.height()), rgbaPixels, texture.id(), x, y, width, height);
    }

    private TextureData createAndUpload(int format, String label, ByteBuffer pixels, int width, int height) {
        long texture = createGpuTexture(format, label, width, height);
        try {
            upload(label, pixels, texture, 0, 0, width, height);
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, () -> SDLGPU.SDL_ReleaseGPUTexture(gpuDevice, texture));
            throw failure;
        }
        return new TextureData(texture, width, height);
    }

    private long createGpuTexture(int format, String label, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_GPUTextureCreateInfo info = SDL_GPUTextureCreateInfo.calloc(stack)
                    .type(SDLGPU.SDL_GPU_TEXTURETYPE_2D)
                    .format(format)
                    .usage(SDLGPU.SDL_GPU_TEXTUREUSAGE_SAMPLER)
                    .width(width)
                    .height(height)
                    .layer_count_or_depth(1)
                    .num_levels(1)
                    .sample_count(SDLGPU.SDL_GPU_SAMPLECOUNT_1);
            long texture = SDLGPU.SDL_CreateGPUTexture(gpuDevice, info);
            if (texture == 0L) {
                throw sdlFailure("SDL_CreateGPUTexture", label);
            }
            return texture;
        }
    }

    private void upload(String label, ByteBuffer pixels, long texture, int x, int y, int width, int height) {
        int size = width * height * 4;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_GPUTransferBufferCreateInfo bufferInfo = SDL_GPUTransferBufferCreateInfo.calloc(stack)
                    .usage(SDLGPU.SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD)
                    .size(size);
            long transferBuffer = SDLGPU.SDL_CreateGPUTransferBuffer(gpuDevice, bufferInfo);
            if (transferBuffer == 0L) {
                throw sdlFailure("SDL_CreateGPUTransferBuffer", label);
            }
            try {
                ByteBuffer mapped = SDLGPU.SDL_MapGPUTransferBuffer(gpuDevice, transferBuffer, false, size);
                if (mapped == null) {
                    throw sdlFailure("SDL_MapGPUTransferBuffer", label);
                }
                MemoryUtil.memCopy(pixels, mapped);
                SDLGPU.SDL_UnmapGPUTransferBuffer(gpuDevice, transferBuffer);

                SDL_GPUTextureTransferInfo source = SDL_GPUTextureTransferInfo.calloc(stack)
                        .transfer_buffer(transferBuffer)
                        .offset(0)
                        .pixels_per_row(width)
                        .rows_per_layer(height);
                SDL_GPUTextureRegion destination = SDL_GPUTextureRegion.calloc(stack)
                        .texture(texture)
                        .x(x)
                        .y(y)
                        .w(width)
                        .h(height)
                        .d(1);
                submitCopy(label, source, destination);
            } finally {
                SDLGPU.SDL_ReleaseGPUTransferBuffer(gpuDevice, transferBuffer);
            }
        }
    }

    private void submitCopy(String label, SDL_GPUTextureTransferInfo source, SDL_GPUTextureRegion destination) {
        long commandBuffer = SDLGPU.SDL_AcquireGPUCommandBuffer(gpuDevice);
        if (commandBuffer == 0L) {
            throw sdlFailure("SDL_AcquireGPUCommandBuffer", label);
        }
        try {
            long copyPass = SDLGPU.SDL_BeginGPUCopyPass(commandBuffer);
            if (copyPass == 0L) {
                throw sdlFailure("SDL_BeginGPUCopyPass", label);
            }
            try {
                SDLGPU.SDL_UploadToGPUTexture(copyPass, source, destination, false);
            } finally {
                SDLGPU.SDL_EndGPUCopyPass(copyPass);
            }
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, () -> {
                if (!SDLGPU.SDL_CancelGPUCommandBuffer(commandBuffer)) {
                    throw sdlFailure("SDL_CancelGPUCommandBuffer", label);
                }
            });
            throw failure;
        }
        // A failed submit has consumed the command buffer, so it must not be cancelled.
        if (!SDLGPU.SDL_SubmitGPUCommandBuffer(commandBuffer)) {
            throw sdlFailure("SDL_SubmitGPUCommandBuffer", label);
        }
    }

    private static String canvasLabel(int width, int height) {
        return "canvas texture " + width + "x" + height;
    }

    private static IllegalStateException sdlFailure(String function, String label) {
        return new IllegalStateException(function + " failed for " + label + ": " + SDLError.SDL_GetError());
    }
}
