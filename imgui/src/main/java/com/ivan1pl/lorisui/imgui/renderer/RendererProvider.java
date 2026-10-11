package com.ivan1pl.lorisui.imgui.renderer;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;

/**
 * Finds the renderer for a component and the style applier for a style.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend ImGuiBackend} implements this interface and passes itself to
 * every {@link Renderer}, which uses it to draw child components and apply styles.
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
}
