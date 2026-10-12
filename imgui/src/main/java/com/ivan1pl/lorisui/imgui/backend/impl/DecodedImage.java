package com.ivan1pl.lorisui.imgui.backend.impl;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.imgui.impl.ClassPathResources;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class DecodedImage implements AutoCloseable {
    private final ByteBuffer pixels;
    private final int width;
    private final int height;

    private DecodedImage(ByteBuffer pixels, int width, int height) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
    }

    public ByteBuffer getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void close() {
        STBImage.stbi_image_free(pixels);
    }

    public static DecodedImage load(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        ByteBuffer encoded = null;

        try (InputStream in = ClassPathResources.open(resourcePath)) {
            if (in == null) {
                throw new IOException("Image not found: " + resourcePath);
            }

            byte[] bytes = in.readAllBytes();
            encoded = MemoryUtil.memAlloc(bytes.length);
            encoded.put(bytes).flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                // Force RGBA output, regardless of source format.
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels,
                        STBImage.STBI_rgb_alpha);

                if (pixels == null) {
                    throw new IOException(
                            "Failed to decode image: " + resourcePath + ": " + STBImage.stbi_failure_reason());
                }

                int w = width.get(0);
                int h = height.get(0);

                if (maskedR != null || maskedG != null || maskedB != null) {
                    try {
                        TextureMasking.apply(pixels, w, h, maskedR, maskedG, maskedB);
                    } catch (Throwable failure) {
                        STBImage.stbi_image_free(pixels);
                        throw failure;
                    }
                }

                return new DecodedImage(pixels, w, h);
            }
        } finally {
            if (encoded != null) {
                MemoryUtil.memFree(encoded);
            }
        }
    }
}
