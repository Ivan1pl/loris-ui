package com.ivan1pl.lorisui.imgui.renderer;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;

/**
 * Draws one kind of {@link Component} with Dear ImGui.
 *
 * <p>Renderers are part of the extension SPI: {@link ImGuiBackend#getRenderer} returns the renderer for each component.
 * To draw a custom component, extend this class and register the renderer with {@link ImGuiBackend#registerRenderer}.
 * A renderer draws child components through the renderers that its {@link RendererProvider} returns for them, applies
 * styles through the provider's style appliers, and loads and draws textures through the provider's
 * {@link RendererProvider#loadTexture(String) loadTexture} and {@link RendererProvider#renderTexture renderTexture}.
 *
 * <p>Renderers are used only on the host's UI thread.
 *
 * @param <ComponentType> the component type this renderer draws
 */
public abstract class Renderer<ComponentType extends Component> {
    /**
     * Creates a renderer.
     */
    protected Renderer() {}

    /**
     * Draws the given component with Dear ImGui. Called on the host's UI thread while a frame is being rendered.
     *
     * @param component         component to draw; never {@code null}
     * @param w                 screen width in logical window coordinates; pass it unchanged to child components,
     *                          whose position, size and content bindings receive it
     * @param h                 screen height in logical window coordinates; pass it unchanged to child components,
     *                          whose position, size and content bindings receive it
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
}
