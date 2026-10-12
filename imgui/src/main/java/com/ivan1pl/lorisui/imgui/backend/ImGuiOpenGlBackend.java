package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.imgui.backend.impl.DecodedImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

import java.io.IOException;

/**
 * The OpenGL implementation of {@link ImGuiBackend}, which loads textures from class-path resources into OpenGL
 * textures and deletes them when they are unloaded. Hosts that render with
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGl3Renderer} or {@link com.ivan1pl.lorisui.imgui.ImGuiGlfwGl3Renderer}
 * use this backend.
 *
 * <p>Loading a texture restores the texture binding and the unpack state it changes, so engines that cache OpenGL
 * state, such as jMonkeyEngine, stay consistent with the context.
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
     * <p>When any mask is non-null, every pixel is recolored with {@link Color#mask(Color, Color, Color)
     * Color.mask(maskedR, maskedG, maskedB)}; when all masks are {@code null}, the image is loaded unchanged.
     *
     * <p>Called on the host's UI thread, with the window's OpenGL context current. The image is decoded before any
     * OpenGL call, and the texture binding and the unpack state changed for the upload are restored before this
     * method returns.
     *
     * @param resourcePath class-path resource name of the image, without a leading {@code /}; never {@code null}
     * @param maskedR      color replacing the red channel, or {@code null} to keep it
     * @param maskedG      color replacing the green channel, or {@code null} to keep it
     * @param maskedB      color replacing the blue channel, or {@code null} to keep it
     * @return the new texture, never {@code null}
     * @throws IOException if the resource does not exist or cannot be decoded as an image (the message names
     *                     {@code resourcePath}), or if it cannot be read
     */
    @Override
    protected TextureData createTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        try (DecodedImage image = DecodedImage.load(resourcePath, maskedR, maskedG, maskedB)) {
            int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int previousAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
            int previousRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
            int previousSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
            int previousSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
            int previousUnpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
            try {
                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);

                int texture = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);

                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL21.GL_SRGB_ALPHA, image.getWidth(), image.getHeight(), 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image.getPixels());

                return new TextureData(texture, image.getWidth(), image.getHeight());
            } finally {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousAlignment);
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, previousRowLength);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, previousSkipPixels);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, previousSkipRows);
                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, previousUnpackBuffer);
            }
        }
    }

    /**
     * Deletes the OpenGL texture. Called on the host's UI thread, with the window's OpenGL context current.
     *
     * @param texture texture to delete; never {@code null}
     */
    @Override
    protected void deleteTexture(TextureData texture) {
        GL11.glDeleteTextures((int) texture.id());
    }
}
