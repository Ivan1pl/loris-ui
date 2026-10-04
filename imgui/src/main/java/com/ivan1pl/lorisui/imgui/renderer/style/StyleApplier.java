package com.ivan1pl.lorisui.imgui.renderer.style;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;

public interface StyleApplier<StyleType extends Style> {
    Styles applyStyles(StyleType style, RendererProvider rendererProvider);
    void popStyles(Styles styles);

    interface Styles {}
}
