package com.ivan1pl.lorisui.imgui.renderer;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.backend.TextureData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws one kind of {@link Component} with Dear ImGui.
 *
 * <p>Renderers are part of the extension SPI: {@link ImGuiBackend#getRenderer} returns the renderer for each component.
 * To draw a custom component, extend this class and return the renderer from an {@link ImGuiBackend} subclass. A
 * renderer draws child components through the renderers that its {@link RendererProvider} returns for them, and applies
 * styles through the provider's style appliers.
 *
 * <p>Renderers are used only on the host's UI thread.
 *
 * @param <ComponentType> the component type this renderer draws
 */
public abstract class Renderer<ComponentType extends Component> {
    // Confined to the render (GL) thread, so a plain HashMap is enough.
    // Complex key represented as nested maps to prevent object allocation just to look up a key
    private static final Map<String, Map<Object, Map<Object, Map<Object, TextureData>>>> TEXTURES = new HashMap<>();
    private static final Object NO_COLOR_KEY = new Object();

    /**
     * Creates a renderer.
     */
    protected Renderer() {}

    /**
     * Draws the given component with Dear ImGui. Called on the host's UI thread while a frame is being rendered.
     *
     * @param component         component to draw; never {@code null}
     * @param w                 display width in pixels; pass it unchanged to child components, whose position and
     *                          size bindings receive it
     * @param h                 display height in pixels; pass it unchanged to child components, whose position and
     *                          size bindings receive it
     * @param rendererProvider  provider of the renderers for child components and the style appliers for styles
     * @param bundleKeyResolver resolver for localized text
     * @param topLevel          {@code true} when the component is a top-level element of the screen, placed at its
     *                          own position on the display; {@code false} when it is drawn inside a parent component.
     *                          Pass {@code false} when drawing child components.
     * @param enableInput       {@code true} when the component may react to mouse and keyboard input; {@code false}
     *                          when it is only displayed, for example as the label of a combo box item. Child
     *                          components must not receive {@code true} when this is {@code false}.
     */
    public abstract void render(ComponentType component, int w, int h, RendererProvider rendererProvider,
                                BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput);

    /**
     * Returns the unmasked texture for the given image resource, loading it through {@code backend} only on the first
     * request for that path. Later requests return the same texture. The cache is shared by all renderers and keeps
     * textures for the lifetime of the application. Must be called on the host's UI thread, typically from
     * {@link #render}.
     *
     * @param backend backend that loads the texture when it is not cached yet; must not be null
     * @param path    path of the image resource, as accepted by {@link ImGuiBackend#loadTexture}; must not be null
     * @return the cached or newly loaded texture
     * @throws IOException if the texture is not cached yet and the backend cannot load it
     */
    protected static TextureData loadTexture(ImGuiBackend backend, String path) throws IOException {
        return loadTexture(backend, path, null, null, null);
    }

    /**
     * Returns the texture for the given image resource recolored with the given channel masks, loading it through
     * {@code backend} only on the first request for that path and mask combination. Later requests return the same
     * texture. Masks are compared with {@link Color#equals}, and a {@code null} mask is cached separately from any
     * non-null mask. The cache is shared by all renderers and keeps textures for the lifetime of the application. Must
     * be called on the host's UI thread, typically from {@link #render}.
     *
     * @param backend backend that loads the texture when it is not cached yet; must not be null
     * @param path    path of the image resource, as accepted by {@link ImGuiBackend#loadTexture}; must not be null
     * @param maskedR color replacing the red channel, or {@code null} to keep it
     * @param maskedG color replacing the green channel, or {@code null} to keep it
     * @param maskedB color replacing the blue channel, or {@code null} to keep it
     * @return the cached or newly loaded texture
     * @throws IOException if the texture is not cached yet and the backend cannot load it
     */
    protected static TextureData loadTexture(ImGuiBackend backend, String path, Color maskedR, Color maskedG,
                                             Color maskedB)
            throws IOException {
        TextureData textureData = getFromCache(path, maskedR, maskedG, maskedB);
        if (textureData != null) {
            return textureData;
        }

        textureData = backend.loadTexture(path, maskedR, maskedG, maskedB);
        putToCache(path, maskedR, maskedG, maskedB, textureData);
        return textureData;
    }

    private static TextureData getFromCache(String path, Color maskedR, Color maskedG, Color maskedB) {
        var map = TEXTURES.get(path);
        if (map == null) {
            return null;
        }
        var innerR = map.get(maskedR == null ? NO_COLOR_KEY : maskedR);
        if (innerR == null) {
            return null;
        }
        var innerG = innerR.get(maskedG == null ? NO_COLOR_KEY : maskedG);
        if (innerG == null) {
            return null;
        }
        return innerG.get(maskedB == null ? NO_COLOR_KEY : maskedB);
    }

    private static void putToCache(String path, Color maskedR, Color maskedG, Color maskedB, TextureData textureData) {
        var map = TEXTURES.get(path);
        if (map == null) {
            TEXTURES.put(path, map = new HashMap<>());
        }
        Object key = maskedR == null ? NO_COLOR_KEY : maskedR;
        var innerR = map.get(key);
        if (innerR == null) {
            map.put(key, innerR = new HashMap<>());
        }
        key = maskedG == null ? NO_COLOR_KEY : maskedG;
        var innerG = innerR.get(key);
        if (innerG == null) {
            innerR.put(key, innerG = new HashMap<>());
        }
        innerG.put(maskedB == null ? NO_COLOR_KEY : maskedB, textureData);
    }
}
