package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.backend.Backend;
import com.ivan1pl.lorisui.api.component.*;
import com.ivan1pl.lorisui.api.component.style.*;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import com.ivan1pl.lorisui.imgui.renderer.impl.*;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;
import com.ivan1pl.lorisui.imgui.renderer.style.impl.*;
import imgui.ImGui;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Base class of the Dear ImGui backends, which draw LorisUI components as Dear ImGui widgets.
 *
 * <p>Each component is drawn by the {@link Renderer} that {@link #getRenderer} returns for it, and each style is
 * applied by the {@link StyleApplier} that {@link #getStyleApplier} returns. The backend has built-in renderers and
 * style appliers for every component and style in {@code lorisui.api}.
 *
 * <p><b>Custom components and styles.</b> To draw your own component or style types, register their renderers with
 * {@link #registerRenderer} and their style appliers with {@link #registerStyleApplier}. A registration for a built-in
 * type replaces the built-in renderer or style applier. The backend passes itself to renderers as their
 * {@link RendererProvider}, so registrations also apply to child components.
 *
 * <p><b>Textures.</b> Renderers draw images with {@link #loadTexture(String)} and {@link #renderTexture}. Each backend
 * owns the textures it loads and returns the same texture for the same image resource and masks until
 * {@link #unloadTextures} releases them all; textures requested afterwards are loaded again. Textures cannot be
 * unloaded while a frame is being drawn, for example from a component callback.
 *
 * <p>A backend for another graphics API subclasses this class and implements {@link #createTexture} and
 * {@link #deleteTexture}; {@link ImGuiOpenGlBackend}, the OpenGL implementation, is final. A host passes the backend to
 * {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer}; the bundled jMonkeyEngine and LWJGL hosts always use their
 * own {@code ImGuiOpenGlBackend}.
 *
 * <p>The backend is used on the host's UI thread only.
 */
public abstract class ImGuiBackend implements Backend, RendererProvider {
    private static final Object NO_COLOR_KEY = new Object();

    private final Map<Class<? extends Component>, Renderer<?>> renderers;
    private final Map<Class<? extends Style>, StyleApplier<?>> styleAppliers;
    // Keyed by path, then by the red, green and blue masks, so that a lookup allocates no key object. A path with a
    // leading '/' is an alias that shares the nested map of its normalized path.
    private final Map<String, Map<Object, Map<Object, Map<Object, TextureData>>>> textures = new HashMap<>();
    private boolean drawing;

    /**
     * Creates a backend with the built-in renderers and style appliers for all components and styles in
     * {@code lorisui.api}.
     */
    protected ImGuiBackend() {
        Map<Class<? extends Component>, Renderer<?>> renderers = new HashMap<>();
        renderers.put(Panel.class, new PanelRenderer());
        renderers.put(Spacer.class, new SpacerRenderer());
        renderers.put(Image.class, new ImageRenderer());
        renderers.put(Text.class, new TextRenderer());
        renderers.put(NumberInput.class, new NumberInputRenderer());
        renderers.put(TextInput.class, new TextInputRenderer());
        renderers.put(ComboBox.class, new ComboBoxRenderer<>());
        renderers.put(ProgressBar.class, new ProgressBarRenderer());
        renderers.put(TabbedPanel.class, new TabbedPanelRenderer());
        this.renderers = renderers;

        Map<Class<? extends Style>, StyleApplier<?>> styleAppliers = new HashMap<>();
        styleAppliers.put(ScrollbarStyle.class, new ScrollbarStyleApplier());
        styleAppliers.put(ComboBoxStyle.class, new ComboBoxStyleApplier());
        styleAppliers.put(ComboBoxPopupStyle.class, new ComboBoxPopupStyleApplier());
        styleAppliers.put(ComboBoxItemStyle.class, new ComboBoxItemStyleApplier());
        styleAppliers.put(TextInputStyle.class, new TextInputStyleApplier());
        this.styleAppliers = styleAppliers;
    }

    /**
     * Registers the renderer that draws components of the given class, replacing any renderer already registered for
     * that class, including a built-in one. The registration takes effect from the next lookup. Components of a
     * subclass without a registration of its own are drawn by the renderer of their nearest registered ancestor.
     *
     * <p>For a generic component type, the class argument needs an unchecked cast, for example
     * {@code (Class<ComboBox<String>>) (Class<?>) ComboBox.class}.
     *
     * <p>Must be called on the host's UI thread.
     *
     * @param componentClass  class of the components to draw; must not be null and must not be an interface
     * @param renderer        renderer for the components; must not be null
     * @param <ComponentType> type of the components
     * @throws NullPointerException     if {@code componentClass} or {@code renderer} is null
     * @throws IllegalArgumentException if {@code componentClass} is an interface, because the lookup walks only the
     *                                  superclass chain
     */
    public final <ComponentType extends Component> void registerRenderer(Class<ComponentType> componentClass,
                                                                         Renderer<? super ComponentType> renderer) {
        Objects.requireNonNull(componentClass, "componentClass");
        Objects.requireNonNull(renderer, "renderer");
        if (componentClass.isInterface()) {
            throw new IllegalArgumentException("Cannot register a renderer for interface: " + componentClass);
        }
        renderers.put(componentClass, renderer);
    }

    /**
     * Registers the style applier that applies styles of the given class, replacing any style applier already
     * registered for that class, including a built-in one. The registration takes effect from the next lookup. Styles
     * of a subclass without a registration of its own are applied by the style applier of their nearest registered
     * ancestor.
     *
     * <p>For a generic style type, the class argument needs an unchecked cast, as described for
     * {@link #registerRenderer}.
     *
     * <p>Must be called on the host's UI thread.
     *
     * @param styleClass   class of the styles to apply; must not be null and must not be an interface
     * @param styleApplier style applier for the styles; must not be null
     * @param <StyleType>  type of the styles
     * @throws NullPointerException     if {@code styleClass} or {@code styleApplier} is null
     * @throws IllegalArgumentException if {@code styleClass} is an interface, because the lookup walks only the
     *                                  superclass chain
     */
    public final <StyleType extends Style> void registerStyleApplier(Class<StyleType> styleClass,
                                                                     StyleApplier<? super StyleType> styleApplier) {
        Objects.requireNonNull(styleClass, "styleClass");
        Objects.requireNonNull(styleApplier, "styleApplier");
        if (styleClass.isInterface()) {
            throw new IllegalArgumentException("Cannot register a style applier for interface: " + styleClass);
        }
        styleAppliers.put(styleClass, styleApplier);
    }

    /**
     * Returns the renderer for the given component. The lookup starts with the component's class and walks up its
     * superclass chain, so a subclass without its own renderer is drawn by the renderer of its nearest ancestor that
     * has one (for example, a {@link Button} is drawn as a {@link Panel}).
     *
     * @param component       component to draw; must not be null
     * @param <ComponentType> type of the component
     * @return the renderer for the component, never {@code null}
     * @throws IllegalStateException if neither the component's class nor any of its superclasses has a renderer
     */
    @Override
    public final <ComponentType extends Component> Renderer<? super ComponentType> getRenderer(
            ComponentType component) {
        Class<?> componentClass = component.getClass();
        do {
            @SuppressWarnings("unchecked")
            Renderer<? super ComponentType> renderer = (Renderer<? super ComponentType>) renderers.get(componentClass);
            if (renderer != null) {
                return renderer;
            }
            componentClass = componentClass.getSuperclass();
        } while (componentClass != null);
        throw new IllegalStateException("Missing renderer for class: " + component.getClass());
    }

    /**
     * Returns the style applier for the given style. The lookup starts with the style's class and walks up its
     * superclass chain, so a subclass without its own style applier is applied by the style applier of its nearest
     * ancestor that has one.
     *
     * @param style       style to apply; must not be null
     * @param <StyleType> type of the style
     * @return the style applier for the style, never {@code null}
     * @throws IllegalStateException if neither the style's class nor any of its superclasses has a style applier
     */
    @Override
    public final <StyleType extends Style> StyleApplier<? super StyleType> getStyleApplier(StyleType style) {
        Class<?> styleClass = style.getClass();
        do {
            @SuppressWarnings("unchecked")
            StyleApplier<? super StyleType> styleApplier =
                    (StyleApplier<? super StyleType>) styleAppliers.get(styleClass);
            if (styleApplier != null) {
                return styleApplier;
            }
            styleClass = styleClass.getSuperclass();
        } while (styleClass != null);
        throw new IllegalStateException("Missing style applier for class: " + style.getClass());
    }

    /**
     * Draws the given components in list order, each as a top-level element with input enabled, using the renderer
     * that {@link #getRenderer} returns for it. Must be called on the host's UI thread, while a Dear ImGui frame is
     * open.
     *
     * @param w                 display width in pixels
     * @param h                 display height in pixels
     * @param components        components to draw; must not be null and is not modified
     * @param bundleKeyResolver resolver for localized text; must not be null
     * @throws IllegalStateException if there is no renderer or style applier for a component or style being drawn
     */
    @Override
    public void render(int w, int h, List<Component> components, BundleKeyResolver bundleKeyResolver) {
        boolean wasDrawing = drawing;
        drawing = true;
        try {
            components.forEach(c -> getRenderer(c).render(c, w, h, this, bundleKeyResolver, true, true));
        } finally {
            drawing = wasDrawing;
        }
    }

    @Override
    public final TextureData loadTexture(String resourcePath) throws IOException {
        return loadTexture(resourcePath, null, null, null);
    }

    @Override
    public final TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        Map<Object, Map<Object, Map<Object, TextureData>>> byRed = textures.get(resourcePath);
        if (byRed == null) {
            byRed = textures.computeIfAbsent(normalize(resourcePath), _ -> new HashMap<>());
            textures.put(resourcePath, byRed);
        }
        Map<Object, Map<Object, TextureData>> byGreen = byRed.get(maskKey(maskedR));
        Map<Object, TextureData> byBlue = byGreen == null ? null : byGreen.get(maskKey(maskedG));
        TextureData texture = byBlue == null ? null : byBlue.get(maskKey(maskedB));
        if (texture != null) {
            return texture;
        }

        texture = createTexture(normalize(resourcePath), maskedR, maskedG, maskedB);
        byRed.computeIfAbsent(maskKey(maskedR), _ -> new HashMap<>())
                .computeIfAbsent(maskKey(maskedG), _ -> new HashMap<>())
                .put(maskKey(maskedB), texture);
        return texture;
    }

    /**
     * Releases every texture this backend has loaded. Textures requested afterwards are loaded again. The backend
     * forgets all textures even when releasing one of them fails.
     *
     * <p>Textures cannot be unloaded while a frame is being drawn, for example from a component callback, because the
     * frame may still draw them. Must be called on the host's UI thread with the graphics context current.
     *
     * @throws IllegalStateException if called while the backend is drawing a frame; no texture is released then
     * @throws RuntimeException      the first failure of releasing a texture, after all other textures have been
     *                               released; failures of later textures are added to it as suppressed exceptions
     */
    public final void unloadTextures() {
        if (drawing) {
            throw new IllegalStateException("Cannot unload textures while a frame is being drawn");
        }
        Set<TextureData> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map<Object, Map<Object, Map<Object, TextureData>>> byRed : textures.values()) {
            for (Map<Object, Map<Object, TextureData>> byGreen : byRed.values()) {
                for (Map<Object, TextureData> byBlue : byGreen.values()) {
                    distinct.addAll(byBlue.values());
                }
            }
        }
        textures.clear();

        RuntimeException failure = null;
        for (TextureData texture : distinct) {
            try {
                deleteTexture(texture);
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else if (e != failure) {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Loads an image resource into a new texture, optionally recoloring it with channel masks. Each call creates a new
     * texture; renderers load textures through {@link #loadTexture(String, Color, Color, Color)} instead, which calls
     * this method only for textures that are not loaded yet.
     *
     * <p>{@code resourcePath} is a class-path resource name without a leading {@code /}. When any mask is non-null,
     * every pixel is recolored with {@link Color#mask(Color, Color, Color) Color.mask(maskedR, maskedG, maskedB)}; when
     * all masks are {@code null}, the image is loaded unchanged.
     *
     * <p>Called on the host's UI thread, with the graphics context current.
     *
     * @param resourcePath class-path resource name of the image, without a leading {@code /}; never {@code null}
     * @param maskedR      color replacing the red channel, or {@code null} to keep it
     * @param maskedG      color replacing the green channel, or {@code null} to keep it
     * @param maskedB      color replacing the blue channel, or {@code null} to keep it
     * @return the new texture, never {@code null}
     * @throws IOException if the resource does not exist, cannot be read or cannot be decoded as an image; the message
     *                     names {@code resourcePath}
     */
    protected abstract TextureData createTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException;

    /**
     * Releases a texture created by {@link #createTexture}. Called once per texture by {@link #unloadTextures}, on the
     * host's UI thread with the graphics context current.
     *
     * @param texture texture to release; never {@code null}
     */
    protected abstract void deleteTexture(TextureData texture);

    @Override
    public void renderTexture(int id, ImageMode imageMode, int width, int height, int textureWidth, int textureHeight) {
        switch (imageMode) {
            case ImageMode.Repeat _ -> renderTextureRepeat(id, width, height, textureWidth, textureHeight);
            case ImageMode.Fit _ -> renderTextureFit(id, width, height);
            case ImageMode.SubImage subImage -> renderTextureSubImage(
                    subImage, id, width, height, textureWidth, textureHeight);
            case ImageMode.Sprite sprite -> renderTextureSprite(sprite, id, width, height, textureWidth, textureHeight);
            case ImageMode.SpriteResize spriteResize -> renderTextureSpriteResize(
                    spriteResize, id, width, height, textureWidth, textureHeight);
            case ImageMode.SubImageResize subImageResize -> renderTextureSubImageResize(
                    subImageResize, id, width, height, textureWidth, textureHeight);
        }
    }

    private static String normalize(String resourcePath) {
        return resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
    }

    private static Object maskKey(Color mask) {
        return mask == null ? NO_COLOR_KEY : mask;
    }

    private void renderTextureRepeat(int id, int width, int height, int textureWidth, int textureHeight) {
        ImGui.image(id, width, height, 0f, 0f,
                ((float) width) / ((float) textureWidth),
                ((float) height) / ((float) textureHeight));
    }

    private void renderTextureFit(int id, int width, int height) {
        ImGui.image(id, width, height, 0f, 0f, 1f, 1f);
    }

    private void renderTextureSubImage(ImageMode.SubImage subImage, int id, int width, int height, int textureWidth,
                                       int textureHeight) {
        ImGui.image(id, width, height,
                (float) subImage.getFromX() / (float) textureWidth,
                (float) subImage.getFromY() / (float) textureHeight,
                (float) (subImage.getToX() + 1) / (float) textureWidth,
                (float) (subImage.getToY() + 1) / (float) textureHeight);
    }

    private void renderTextureSprite(ImageMode.Sprite sprite, int id, int width, int height, int textureWidth,
                                     int textureHeight) {
        int columns = textureWidth / sprite.getSpriteWidth();
        int row = (sprite.getSpriteIndex() - 1) / columns;
        int column = sprite.getSpriteIndex() - 1 - row * columns;
        float scaleX = 1f / ((float) textureWidth);
        float scaleY = 1f / ((float) textureHeight);
        float u0 = ((float) (column * sprite.getSpriteWidth())) * scaleX;
        float v0 = ((float) (row * sprite.getSpriteHeight())) * scaleY;
        float u1 = u0 + ((float) sprite.getSpriteWidth()) * scaleX;
        float v1 = v0 + ((float) sprite.getSpriteHeight()) * scaleY;
        ImGui.image(id, width, height, u0, v0, u1, v1);
    }

    private void renderTextureSpriteResize(ImageMode.SpriteResize spriteResize, int id, int width, int height,
                                           int textureWidth, int textureHeight) {
        int columns = textureWidth / spriteResize.getSpriteWidth();
        int row = (spriteResize.getSpriteIndex() - 1) / columns;
        int column = spriteResize.getSpriteIndex() - 1 - row * columns;
        float scaleX = 1f / ((float) textureWidth);
        float scaleY = 1f / ((float) textureHeight);
        float u0 = ((float) (column * spriteResize.getSpriteWidth())) * scaleX;
        float v0 = ((float) (row * spriteResize.getSpriteHeight())) * scaleY;
        float u1 = u0 + ((float) spriteResize.getX1()) * scaleX;
        float u2 = u1 + ((float) spriteResize.getX2()) * scaleX;
        float u3 = u2 + ((float) spriteResize.getX3()) * scaleX;
        float u4 = u0 + ((float) spriteResize.getX4()) * scaleX;
        float u5 = u4 + ((float) spriteResize.getX5()) * scaleX;
        float u6 = u5 + ((float) spriteResize.getX6()) * scaleX;
        float u7 = u0 + ((float) spriteResize.getX7()) * scaleX;
        float u8 = u7 + ((float) spriteResize.getX8()) * scaleX;
        float u9 = u8 + ((float) spriteResize.getX9()) * scaleX;
        float v1 = v0 + ((float) spriteResize.getY1()) * scaleY;
        float v2 = v1 + ((float) spriteResize.getY2()) * scaleY;
        float v3 = v2 + ((float) spriteResize.getY3()) * scaleY;
        int middleWidth1 = width - spriteResize.getX1() - spriteResize.getX3();
        int middleWidth2 = width - spriteResize.getX4() - spriteResize.getX6();
        int middleWidth3 = width - spriteResize.getX7() - spriteResize.getX9();
        int middleHeight = height - spriteResize.getY1() - spriteResize.getY3();
        // row 1
        ImGui.image(id, spriteResize.getX1(), spriteResize.getY1(), u0, v0, u1, v1);
        ImGui.sameLine(spriteResize.getX1(), 0);
        ImGui.image(id, middleWidth1, spriteResize.getY1(), u1, v0, u2, v1);
        ImGui.sameLine(spriteResize.getX1() + middleWidth1, 0);
        ImGui.image(id, spriteResize.getX3(), spriteResize.getY1(), u2, v0, u3, v1);
        // row 2
        ImGui.image(id, spriteResize.getX4(), middleHeight, u0, v1, u4, v2);
        ImGui.sameLine(spriteResize.getX4(), 0);
        ImGui.image(id, middleWidth2, middleHeight, u4, v1, u5, v2);
        ImGui.sameLine(spriteResize.getX4() + middleWidth2, 0);
        ImGui.image(id, spriteResize.getX6(), middleHeight, u5, v1, u6, v2);
        // row 3
        ImGui.image(id, spriteResize.getX7(), spriteResize.getY3(), u0, v2, u7, v3);
        ImGui.sameLine(spriteResize.getX7(), 0);
        ImGui.image(id, middleWidth3, spriteResize.getY3(), u7, v2, u8, v3);
        ImGui.sameLine(spriteResize.getX7() + middleWidth3, 0);
        ImGui.image(id, spriteResize.getX9(), spriteResize.getY3(), u8, v2, u9, v3);
    }

    private void renderTextureSubImageResize(ImageMode.SubImageResize subImageResize, int id, int width, int height,
                                             int textureWidth, int textureHeight) {
        float u0 = (float) subImageResize.getFromX() / (float) textureWidth;
        float v0 = (float) subImageResize.getFromY() / (float) textureHeight;
        float u1 = (float) (subImageResize.getFromX() + subImageResize.getX1()) / (float) textureWidth;
        float u2 = (float) (subImageResize.getFromX() + subImageResize.getX1() +
                subImageResize.getX2()) / (float) textureWidth;
        float u3 = (float) (subImageResize.getFromX() + subImageResize.getX1() + subImageResize.getX2() +
                subImageResize.getX3()) / (float) textureWidth;
        float u4 = (float) (subImageResize.getFromX() + subImageResize.getX4()) / (float) textureWidth;
        float u5 = (float) (subImageResize.getFromX() + subImageResize.getX4() + subImageResize.getX5()) /
                (float) textureWidth;
        float u6 = (float) (subImageResize.getFromX() + subImageResize.getX4() + subImageResize.getX5() +
                subImageResize.getX6()) / textureWidth;
        float u7 = (float) (subImageResize.getFromX() + subImageResize.getX7()) / (float) textureWidth;
        float u8 = (float) (subImageResize.getFromX() + subImageResize.getX7() + subImageResize.getX8()) /
                (float) textureWidth;
        float u9 = (float) (subImageResize.getFromX() + subImageResize.getX7() + subImageResize.getX8() +
                subImageResize.getX9()) / (float) textureWidth;
        float v1 = (float) (subImageResize.getFromY() + subImageResize.getY1()) / (float) textureHeight;
        float v2 = (float) (subImageResize.getFromY() + subImageResize.getY1() + subImageResize.getY2()) /
                (float) textureHeight;
        float v3 = (float) (subImageResize.getFromY() + subImageResize.getY1() + subImageResize.getY2() +
                subImageResize.getY3()) / (float) textureHeight;
        int middleWidth1 = width - subImageResize.getX1() - subImageResize.getX3();
        int middleWidth2 = width - subImageResize.getX4() - subImageResize.getX6();
        int middleWidth3 = width - subImageResize.getX7() - subImageResize.getX9();
        int middleHeight = height - subImageResize.getY1() - subImageResize.getY3();
        // row 1
        ImGui.image(id, subImageResize.getX1(), subImageResize.getY1(), u0, v0, u1, v1);
        ImGui.sameLine(subImageResize.getX1(), 0);
        ImGui.image(id, middleWidth1, subImageResize.getY1(), u1, v0, u2, v1);
        ImGui.sameLine(subImageResize.getX1() + middleWidth1, 0);
        ImGui.image(id, subImageResize.getX3(), subImageResize.getY1(), u2, v0, u3, v1);
        // row 2
        ImGui.image(id, subImageResize.getX4(), middleHeight, u0, v1, u4, v2);
        ImGui.sameLine(subImageResize.getX4(), 0);
        ImGui.image(id, middleWidth2, middleHeight, u4, v1, u5, v2);
        ImGui.sameLine(subImageResize.getX4() + middleWidth2, 0);
        ImGui.image(id, subImageResize.getX6(), middleHeight, u5, v1, u6, v2);
        // row 3
        ImGui.image(id, subImageResize.getX7(), subImageResize.getY3(), u0, v2, u7, v3);
        ImGui.sameLine(subImageResize.getX7(), 0);
        ImGui.image(id, middleWidth3, subImageResize.getY3(), u7, v2, u8, v3);
        ImGui.sameLine(subImageResize.getX7() + middleWidth3, 0);
        ImGui.image(id, subImageResize.getX9(), subImageResize.getY3(), u8, v2, u9, v3);
    }
}
