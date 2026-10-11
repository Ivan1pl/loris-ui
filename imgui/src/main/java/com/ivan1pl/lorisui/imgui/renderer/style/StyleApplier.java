package com.ivan1pl.lorisui.imgui.renderer.style;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;

/**
 * Applies one kind of {@link Style} to the Dear ImGui widgets that are drawn next.
 *
 * <p>Style appliers are part of the extension SPI:
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#getStyleApplier ImGuiBackend.getStyleApplier} returns the
 * applier for each style. A renderer calls {@link #applyStyles} before drawing the styled widgets and always pairs it
 * with {@link #popStyles}, passing the returned token, in a {@code try/finally} block so that the style is removed even
 * when drawing throws. Nested styles are removed in the reverse order in which they were applied.
 *
 * <p>Style appliers are used only on the host's UI thread.
 *
 * @param <StyleType> the style type this applier applies
 */
public interface StyleApplier<StyleType extends Style> {
    /**
     * Applies the given style to the widgets drawn after this call, until the matching {@link #popStyles} call. Only
     * the style's non-null properties are applied; {@code null} properties keep the current value.
     *
     * @param style            style to apply; must not be null
     * @param rendererProvider provider of the backend's renderers and style appliers, for appliers that delegate to
     *                         other style appliers
     * @return a token recording what was applied, to pass to {@link #popStyles}; never {@code null}
     */
    Styles applyStyles(StyleType style, RendererProvider rendererProvider);

    /**
     * Removes exactly the changes recorded in the given token, restoring the style that was in effect before the
     * matching {@link #applyStyles} call.
     *
     * @param styles token returned by the matching {@link #applyStyles} call of this applier; must not be null
     */
    void popStyles(Styles styles);

    /**
     * Opaque token returned by {@link #applyStyles} and passed back to {@link #popStyles}. Each style applier defines
     * its own implementation; callers only pass the token back to the applier that returned it.
     */
    interface Styles {}
}
