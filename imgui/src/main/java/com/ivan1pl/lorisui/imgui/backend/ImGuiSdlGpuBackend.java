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
 * <p>Textures are created in the {@code R8G8B8A8_UNORM_SRGB} format, so images look the same as on the OpenGL
 * backend when the window's swapchain uses the {@code SDR_LINEAR} composition. Each texture is uploaded with its own
 * command buffer, which is submitted at once.
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
            long texture = createGpuTexture(resourcePath, image.getWidth(), image.getHeight());
            try {
                upload(resourcePath, image, texture);
            } catch (Throwable failure) {
                DeferredRelease.runAfterFailure(failure, () -> SDLGPU.SDL_ReleaseGPUTexture(gpuDevice, texture));
                throw failure;
            }
            return new TextureData(texture, image.getWidth(), image.getHeight());
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

    private long createGpuTexture(String resourcePath, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_GPUTextureCreateInfo info = SDL_GPUTextureCreateInfo.calloc(stack)
                    .type(SDLGPU.SDL_GPU_TEXTURETYPE_2D)
                    .format(SDLGPU.SDL_GPU_TEXTUREFORMAT_R8G8B8A8_UNORM_SRGB)
                    .usage(SDLGPU.SDL_GPU_TEXTUREUSAGE_SAMPLER)
                    .width(width)
                    .height(height)
                    .layer_count_or_depth(1)
                    .num_levels(1)
                    .sample_count(SDLGPU.SDL_GPU_SAMPLECOUNT_1);
            long texture = SDLGPU.SDL_CreateGPUTexture(gpuDevice, info);
            if (texture == 0L) {
                throw sdlFailure("SDL_CreateGPUTexture", resourcePath);
            }
            return texture;
        }
    }

    private void upload(String resourcePath, DecodedImage image, long texture) {
        ByteBuffer pixels = image.getPixels();
        int size = pixels.remaining();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_GPUTransferBufferCreateInfo bufferInfo = SDL_GPUTransferBufferCreateInfo.calloc(stack)
                    .usage(SDLGPU.SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD)
                    .size(size);
            long transferBuffer = SDLGPU.SDL_CreateGPUTransferBuffer(gpuDevice, bufferInfo);
            if (transferBuffer == 0L) {
                throw sdlFailure("SDL_CreateGPUTransferBuffer", resourcePath);
            }
            try {
                ByteBuffer mapped = SDLGPU.SDL_MapGPUTransferBuffer(gpuDevice, transferBuffer, false, size);
                if (mapped == null) {
                    throw sdlFailure("SDL_MapGPUTransferBuffer", resourcePath);
                }
                MemoryUtil.memCopy(pixels, mapped);
                SDLGPU.SDL_UnmapGPUTransferBuffer(gpuDevice, transferBuffer);

                SDL_GPUTextureTransferInfo source = SDL_GPUTextureTransferInfo.calloc(stack)
                        .transfer_buffer(transferBuffer)
                        .offset(0)
                        .pixels_per_row(image.getWidth())
                        .rows_per_layer(image.getHeight());
                SDL_GPUTextureRegion destination = SDL_GPUTextureRegion.calloc(stack)
                        .texture(texture)
                        .w(image.getWidth())
                        .h(image.getHeight())
                        .d(1);
                submitCopy(resourcePath, source, destination);
            } finally {
                SDLGPU.SDL_ReleaseGPUTransferBuffer(gpuDevice, transferBuffer);
            }
        }
    }

    private void submitCopy(String resourcePath, SDL_GPUTextureTransferInfo source,
                            SDL_GPUTextureRegion destination) {
        long commandBuffer = SDLGPU.SDL_AcquireGPUCommandBuffer(gpuDevice);
        if (commandBuffer == 0L) {
            throw sdlFailure("SDL_AcquireGPUCommandBuffer", resourcePath);
        }
        try {
            long copyPass = SDLGPU.SDL_BeginGPUCopyPass(commandBuffer);
            if (copyPass == 0L) {
                throw sdlFailure("SDL_BeginGPUCopyPass", resourcePath);
            }
            try {
                SDLGPU.SDL_UploadToGPUTexture(copyPass, source, destination, false);
            } finally {
                SDLGPU.SDL_EndGPUCopyPass(copyPass);
            }
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, () -> {
                if (!SDLGPU.SDL_CancelGPUCommandBuffer(commandBuffer)) {
                    throw sdlFailure("SDL_CancelGPUCommandBuffer", resourcePath);
                }
            });
            throw failure;
        }
        // A failed submit has consumed the command buffer, so it must not be cancelled.
        if (!SDLGPU.SDL_SubmitGPUCommandBuffer(commandBuffer)) {
            throw sdlFailure("SDL_SubmitGPUCommandBuffer", resourcePath);
        }
    }

    private static IllegalStateException sdlFailure(String function, String resourcePath) {
        return new IllegalStateException(
                function + " failed for texture " + resourcePath + ": " + SDLError.SDL_GetError());
    }
}
