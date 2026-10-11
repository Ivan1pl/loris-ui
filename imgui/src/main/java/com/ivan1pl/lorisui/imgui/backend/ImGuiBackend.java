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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class of the Dear ImGui backends, which draw LorisUI components as Dear ImGui widgets.
 *
 * <p>Each component is drawn by the {@link Renderer} that {@link #getRenderer} returns for it, and each style is
 * applied by the {@link StyleApplier} that {@link #getStyleApplier} returns. The backend has built-in renderers and
 * style appliers for every component and style in {@code lorisui.api}. Renderers use {@link #loadTexture} and
 * {@link #renderTexture} to draw images.
 *
 * <p><b>Custom components and styles.</b> To draw your own component or style types, subclass this class, override
 * {@link #getRenderer} and {@link #getStyleApplier} to return your implementations for your types, and delegate to
 * {@code super} for all other types, so the built-in ones keep working. The backend passes itself to renderers as their
 * {@link RendererProvider}, so the overrides also apply to child components. The subclass must also implement
 * {@link #loadTexture} for its graphics API. {@link ImGuiOpenGlBackend}, the OpenGL implementation, is final and
 * cannot be extended. A host passes the backend to {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer}; the bundled
 * jMonkeyEngine host always uses {@code ImGuiOpenGlBackend} and does not yet accept a custom backend.
 *
 * <p>The backend is used on the host's UI thread only.
 */
public abstract class ImGuiBackend implements Backend, RendererProvider {
    private final Map<Class<? extends Component>, Renderer<?>> renderers;
    private final Map<Class<? extends Style>, StyleApplier<?>> styleAppliers;

    /**
     * Creates a backend with the built-in renderers and style appliers for all components and styles in
     * {@code lorisui.api}.
     */
    protected ImGuiBackend() {
        Map<Class<? extends Component>, Renderer<?>> renderers = new HashMap<>();
        renderers.put(Panel.class, new PanelRenderer(this));
        renderers.put(Spacer.class, new SpacerRenderer());
        renderers.put(Image.class, new ImageRenderer(this));
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
     * Returns the renderer for the given component. The lookup starts with the component's class and walks up its
     * superclass chain, so a subclass without its own renderer is drawn by the renderer of its nearest ancestor that
     * has one (for example, a {@link Button} is drawn as a {@link Panel}).
     *
     * <p>Override this method to draw custom components: return your renderer for your component types and delegate
     * to {@code super} for all other components.
     *
     * @param component       component to draw; must not be null
     * @param <ComponentType> type of the component
     * @return the renderer for the component, never {@code null}
     * @throws IllegalStateException if neither the component's class nor any of its superclasses has a renderer
     */
    @SuppressWarnings("unchecked")
    @Override
    public <ComponentType extends Component> Renderer<? super ComponentType> getRenderer(ComponentType component) {
        Class<?> componentClass = component.getClass();
        Renderer<? super ComponentType> renderer;
        do {
            renderer = (Renderer<? super ComponentType>) renderers.get(componentClass);
            componentClass = componentClass.getSuperclass();
        } while (renderer == null && componentClass != null);
        if (renderer == null) {
            throw new IllegalStateException("Missing renderer for class: " + component.getClass());
        }
        return renderer;
    }

    /**
     * Returns the style applier for the given style. The lookup starts with the style's class and walks up its
     * superclass chain, so a subclass without its own style applier is applied by the style applier of its nearest
     * ancestor that has one.
     *
     * <p>Override this method to apply custom styles: return your style applier for your style types and delegate to
     * {@code super} for all other styles.
     *
     * @param style       style to apply; must not be null
     * @param <StyleType> type of the style
     * @return the style applier for the style, never {@code null}
     * @throws IllegalStateException if neither the style's class nor any of its superclasses has a style applier
     */
    @SuppressWarnings("unchecked")
    @Override
    public <StyleType extends Style> StyleApplier<? super StyleType> getStyleApplier(StyleType style) {
        Class<?> styleClass = style.getClass();
        StyleApplier<? super StyleType> styleApplier;
        do {
            styleApplier = (StyleApplier<? super StyleType>) styleAppliers.get(styleClass);
            styleClass = styleClass.getSuperclass();
        } while (styleApplier == null && styleClass != null);
        if (styleApplier == null) {
            throw new IllegalStateException("Missing style applier for class: " + style.getClass());
        }
        return styleApplier;
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
        components.forEach(c -> getRenderer(c).render(c, w, h, this, bundleKeyResolver, true, true));
    }

    /**
     * Loads an image resource into a new texture, optionally recoloring it with channel masks. Each call loads a new
     * texture; renderers should use {@link Renderer}'s cached {@code loadTexture} helpers instead of calling this
     * method directly. How {@code resourcePath} is resolved depends on the implementation.
     *
     * <p>When any mask is non-null, every pixel is recolored with {@link Color#mask(Color, Color, Color)
     * Color.mask(maskedR, maskedG, maskedB)}; when all masks are {@code null}, the image is loaded unchanged.
     *
     * <p>Must be called on the host's UI thread, with the graphics context current.
     *
     * @param resourcePath path of the image resource; must not be null
     * @param maskedR      color replacing the red channel, or {@code null} to keep it
     * @param maskedG      color replacing the green channel, or {@code null} to keep it
     * @param maskedB      color replacing the blue channel, or {@code null} to keep it
     * @return the loaded texture, never {@code null}
     * @throws IOException if the resource does not exist, cannot be read or cannot be decoded as an image
     */
    public abstract TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
            throws IOException;

    /**
     * Draws a texture as a {@code width} by {@code height} pixel image at the current Dear ImGui cursor position, using
     * the given image mode to choose which part of the texture is shown and how it is scaled. Must be called on the
     * host's UI thread, from a renderer while it draws.
     *
     * @param id            texture ID, as returned in {@link TextureData#id()}
     * @param imageMode     how the texture is mapped onto the image; must not be null
     * @param width         width of the drawn image in pixels
     * @param height        height of the drawn image in pixels
     * @param textureWidth  full width of the texture in pixels
     * @param textureHeight full height of the texture in pixels
     */
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
                (float) (subImage.getToX()+1) / (float) textureWidth,
                (float) (subImage.getToY()+1) / (float) textureHeight);
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
