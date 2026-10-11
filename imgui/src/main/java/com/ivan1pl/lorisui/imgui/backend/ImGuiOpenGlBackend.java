package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.imgui.backend.impl.TextureMasking;
import com.ivan1pl.lorisui.imgui.impl.ClassPathResources;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * The OpenGL implementation of {@link ImGuiBackend}, which loads textures from class-path resources into OpenGL
 * textures. Hosts that render with {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer} use this backend.
 */
public final class ImGuiOpenGlBackend extends ImGuiBackend {
    /**
     * Creates an OpenGL backend with the built-in renderers and style appliers.
     */
    public ImGuiOpenGlBackend() {}

    /**
     * Loads an image from a class-path resource into a new OpenGL texture, optionally recoloring it with channel
     * masks. Common image formats such as PNG and JPEG are supported.
     *
     * <p>{@code resourcePath} is a class-path resource name, where a leading {@code /} is optional. On the module path
     * the resource must be outside any Java package of a named module, or in a package that the application module
     * opens unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * <p>When any mask is non-null, every pixel is recolored with {@link Color#mask(Color, Color, Color)
     * Color.mask(maskedR, maskedG, maskedB)}; when all masks are {@code null}, the image is loaded unchanged.
     *
     * <p>Must be called on the host's UI thread, with the window's OpenGL context current.
     *
     * @param resourcePath class-path resource name of the image; must not be null
     * @param maskedR      color replacing the red channel, or {@code null} to keep it
     * @param maskedG      color replacing the green channel, or {@code null} to keep it
     * @param maskedB      color replacing the blue channel, or {@code null} to keep it
     * @return the loaded texture, never {@code null}
     * @throws IOException if the resource does not exist or cannot be decoded as an image (the message names
     *                     {@code resourcePath}), or if it cannot be read
     */
    @Override
    public TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        ByteBuffer encoded = null;
        ByteBuffer pixels = null;

        try (InputStream in = ClassPathResources.open(resourcePath)) {
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

                if (maskedR != null || maskedG != null || maskedB != null) {
                    TextureMasking.apply(pixels, w, h, maskedR, maskedG, maskedB);
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
