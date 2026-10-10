package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.backend.TextureData;
import com.ivan1pl.lorisui.api.core.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ImGuiOpenGlBackend extends ImGuiBackend {
    public TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        ByteBuffer encoded = null;
        ByteBuffer pixels = null;

        try (InputStream in = ImGuiOpenGlBackend.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Image not found: " + resourcePath);
            }

            byte[] bytes = in.readAllBytes();
            encoded = MemoryUtil.memAlloc(bytes.length);
            encoded.put(bytes).flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                var width = stack.mallocInt(1);
                var height = stack.mallocInt(1);
                var channels = stack.mallocInt(1);

                // Force RGBA output, regardless of source format.
                pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, STBImage.STBI_rgb_alpha);

                if (pixels == null) {
                    throw new IOException(
                            "Failed to decode image: " + resourcePath + ": " + STBImage.stbi_failure_reason());
                }

                int w = width.get(0);
                int h = height.get(0);

                // Apply your RGB mask here, if necessary.
                if (maskedR != null || maskedG != null || maskedB != null) {
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int i = (y * w + x) * 4;

                            int r = Byte.toUnsignedInt(pixels.get(i));
                            int g = Byte.toUnsignedInt(pixels.get(i + 1));
                            int b = Byte.toUnsignedInt(pixels.get(i + 2));
                            int a = Byte.toUnsignedInt(pixels.get(i + 3));

                            // Adapt this to your existing Color.mask() semantics.
                            Color color = new Color(r, g, b, a);
                            int rgba = color.mask(
                                    maskedR, maskedG, maskedB
                            ).getRGBA();

                            pixels.put(i,     (byte) (rgba >>> 24));
                            pixels.put(i + 1, (byte) (rgba >>> 16));
                            pixels.put(i + 2, (byte) (rgba >>> 8));
                            pixels.put(i + 3, (byte) rgba);
                        }
                    }
                }

                int texture = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);

                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL21.GL_SRGB_ALPHA,
                        w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

                return new TextureData(texture, w, h);
            }
        } finally {
            if (pixels != null) {
                STBImage.stbi_image_free(pixels);
            }
            if (encoded != null) {
                MemoryUtil.memFree(encoded);
            }
        }
    }
}
