package com.ivan1pl.lorisui.imgui.renderer;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.ImageMode;
import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.imgui.backend.TextureData;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;

import java.io.IOException;

/**
 * Finds the renderer for a component and the style applier for a style, and loads and draws textures.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend ImGuiBackend} implements this interface and passes itself to
 * every {@link Renderer}, which uses it to draw child components, apply styles and draw images.
 */
public interface RendererProvider {
    /**
     * Returns the renderer for the given component.
     *
     * @param component       component to draw; must not be null
     * @param <ComponentType> type of the component
     * @return the renderer for the component, never {@code null}
     * @throws IllegalStateException if there is no renderer for the component
     */
    <ComponentType extends Component> Renderer<? super ComponentType> getRenderer(ComponentType component);

    /**
     * Returns the style applier for the given style.
     *
     * @param style       style to apply; must not be null
     * @param <StyleType> type of the style
     * @return the style applier for the style, never {@code null}
     * @throws IllegalStateException if there is no style applier for the style
     */
    <StyleType extends Style> StyleApplier<? super StyleType> getStyleApplier(StyleType style);

    /**
     * Returns the texture for the given image resource, loading it only when it is not loaded yet. Equal paths, with
     * or without the leading {@code /}, return the same texture until the backend's textures are unloaded. A failed
     * load is not remembered, so the next request tries again.
     *
     * <p>{@code resourcePath} is a class-path resource name, where a leading {@code /} is optional. On the module path
     * the resource must be outside any Java package of a named module, or in a package that the application module
     * opens unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * <p>Must be called on the host's UI thread with the window's graphics context ready, typically from
     * {@link Renderer#render}.
     *
     * @param resourcePath class-path resource name of the image; must not be null
     * @return the texture, never {@code null}
     * @throws NullPointerException if {@code resourcePath} is null
     * @throws IOException          if the texture is not loaded yet and the resource does not exist, cannot be read or
     *                              cannot be decoded as an image
     */
    TextureData loadTexture(String resourcePath) throws IOException;

    /**
     * Returns the texture for the given image resource recolored with the given channel masks, loading it only when
     * it is not loaded yet. Equal paths, with or without the leading {@code /}, and equal masks (compared with
     * {@link Color#equals}) return the same texture until the backend's textures are unloaded. A {@code null} mask
     * differs from any non-null mask. A failed load is not remembered, so the next request tries again.
     *
     * <p>When any mask is non-null, every pixel is recolored with {@link Color#mask(Color, Color, Color)
     * Color.mask(maskedR, maskedG, maskedB)}; when all masks are {@code null}, the image is loaded unchanged.
     *
     * <p>{@code resourcePath} is a class-path resource name, where a leading {@code /} is optional. On the module path
     * the resource must be outside any Java package of a named module, or in a package that the application module
     * opens unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * <p>Must be called on the host's UI thread with the window's graphics context ready, typically from
     * {@link Renderer#render}.
     *
     * @param resourcePath class-path resource name of the image; must not be null
     * @param maskedR      color replacing the red channel, or {@code null} to keep it
     * @param maskedG      color replacing the green channel, or {@code null} to keep it
     * @param maskedB      color replacing the blue channel, or {@code null} to keep it
     * @return the texture, never {@code null}
     * @throws NullPointerException if {@code resourcePath} is null
     * @throws IOException          if the texture is not loaded yet and the resource does not exist, cannot be read or
     *                              cannot be decoded as an image
     */
    TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) throws IOException;

    /**
     * Draws a texture as a {@code width} by {@code height} pixel image at the current Dear ImGui cursor position, using
     * the given image mode to choose which part of the texture is shown and how it is scaled. An
     * {@link ImageMode.Repeat} image is drawn as a grid of texture tiles at their native size, cut at the right and
     * bottom edges, so it looks the same with every graphics API. Every tile is a separate Dear ImGui item, so the cost
     * grows with the number of tiles; prefer textures that are not much smaller than the area they cover. Must be
     * called on the host's UI thread, from a renderer while it draws.
     *
     * @param id            texture ID, as returned in {@link TextureData#id()}
     * @param imageMode     how the texture is mapped onto the image; must not be null
     * @param width         width of the drawn image in pixels
     * @param height        height of the drawn image in pixels
     * @param textureWidth  full width of the texture in pixels
     * @param textureHeight full height of the texture in pixels
     * @throws IllegalArgumentException if {@code imageMode} is {@link ImageMode.Repeat} and {@code textureWidth} or
     *                                  {@code textureHeight} is {@code 0} or less; nothing is drawn then
     */
    void renderTexture(long id, ImageMode imageMode, int width, int height, int textureWidth, int textureHeight);
}
