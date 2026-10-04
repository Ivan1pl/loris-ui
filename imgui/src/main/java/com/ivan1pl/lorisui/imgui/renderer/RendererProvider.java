package com.ivan1pl.lorisui.imgui.renderer;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;

public interface RendererProvider {
    <ComponentType extends Component> Renderer<? super ComponentType> getRenderer(ComponentType component);
    <StyleType extends Style> StyleApplier<? super StyleType> getStyleApplier(StyleType style);
}
