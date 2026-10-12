package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.imgui.backend.impl.DecodedImage;
import org.lwjgl.opengles.GLES30;

import java.io.IOException;

/**
 * The OpenGL ES 3.0 implementation of {@link ImGuiBackend}, which loads textures from class-path resources into
 * OpenGL ES textures and deletes them when they are unloaded. Hosts that render with
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGles3Renderer ImGuiSdlGles3Renderer}, such as jMonkeyEngine's
 * {@code ANGLE_GLES3} renderer, use this backend.
 *
 * <p>Textures are created in the {@code SRGB8_ALPHA8} format, so images look the same as on the OpenGL backend.
 * Loading a texture restores the texture binding and the unpack state it changes, so engines that cache OpenGL ES
 * state, such as jMonkeyEngine, stay consistent with the context.
 *
 * <p>The backend is used on the host's UI thread only, with an OpenGL ES 3.0 context current.
 */
public final class ImGuiOpenGlEsBackend extends ImGuiBackend {
    /**
     * Creates an OpenGL ES backend with the built-in renderers and style appliers. Makes no native calls.
     */
    public ImGuiOpenGlEsBackend() {}

    /**
     * Loads an image from a class-path resource into a new OpenGL ES texture, optionally recoloring it with channel
     * masks. Common image formats such as PNG and JPEG are supported.
     *
     * <p>When any mask is non-null, every pixel is recolored with {@link Color#mask(Color, Color, Color)
     * Color.mask(maskedR, maskedG, maskedB)}; when all masks are {@code null}, the image is loaded unchanged.
     *
     * <p>Called on the host's UI thread, with the window's OpenGL ES 3.0 context current. The image is decoded before
     * any OpenGL ES call, and the texture binding and the unpack state changed for the upload are restored before this
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
            int previousTexture = GLES30.glGetInteger(GLES30.GL_TEXTURE_BINDING_2D);
            int previousAlignment = GLES30.glGetInteger(GLES30.GL_UNPACK_ALIGNMENT);
            int previousRowLength = GLES30.glGetInteger(GLES30.GL_UNPACK_ROW_LENGTH);
            int previousSkipPixels = GLES30.glGetInteger(GLES30.GL_UNPACK_SKIP_PIXELS);
            int previousSkipRows = GLES30.glGetInteger(GLES30.GL_UNPACK_SKIP_ROWS);
            int previousUnpackBuffer = GLES30.glGetInteger(GLES30.GL_PIXEL_UNPACK_BUFFER_BINDING);
            try {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);

                int texture = GLES30.glGenTextures();
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture);

                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

                GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_SKIP_PIXELS, 0);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_SKIP_ROWS, 0);

                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_SRGB8_ALPHA8, image.getWidth(),
                        image.getHeight(), 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, image.getPixels());

                return new TextureData(texture, image.getWidth(), image.getHeight());
            } finally {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, previousTexture);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, previousAlignment);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, previousRowLength);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_SKIP_PIXELS, previousSkipPixels);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_SKIP_ROWS, previousSkipRows);
                GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, previousUnpackBuffer);
            }
        }
    }

    /**
     * Deletes the OpenGL ES texture. Called on the host's UI thread, with the window's OpenGL ES 3.0 context current.
     *
     * @param texture texture to delete; never {@code null}
     */
    @Override
    protected void deleteTexture(TextureData texture) {
        GLES30.glDeleteTextures((int) texture.id());
    }
}
