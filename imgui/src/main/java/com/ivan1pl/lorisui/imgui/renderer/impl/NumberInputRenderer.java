package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.NumberInput;
import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.ImGuiFont;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;

import java.util.HashMap;
import java.util.Map;

public class NumberInputRenderer extends Renderer<NumberInput> {
    private final Map<NumberInput, String> lastEditValue = new HashMap<>();

    @Override
    public void render(NumberInput component, int w, int h, RendererProvider rendererProvider,
                       BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
        boolean enabled = component.isEnabled();
        TextInputStyle disabledStyle = enabled ? null : component.getDisabledStyle();
        TextInputStyle style = disabledStyle != null ? disabledStyle : component.getStyle();
        var styles = rendererProvider.getStyleApplier(style).applyStyles(style, rendererProvider);
        try {
            if (component.getFont() != null) {
                ((ImGuiFont) component.getFont()).useFont(() -> doRender(component, enableInput, enabled));
            } else {
                doRender(component, enableInput, enabled);
            }
        } finally {
            rendererProvider.getStyleApplier(style).popStyles(styles);
        }
    }

    private void doRender(NumberInput component, boolean enableInput, boolean enabled) {
        ImGui.pushItemWidth(component.getWidth());
        try {
            ImString imString = new ImString(component.getValue());
            imString.inputData.isResizable = true;
            if (InputKeyboardFocus.inputText(component, imString, ImGuiInputTextFlags.CharsDecimal, enableInput,
                    enabled)) {
                lastEditValue.put(component, imString.get());
            } else {
                String value = lastEditValue.remove(component);
                if (value != null) {
                    component.validateInput(value);
                }
            }
        } finally {
            ImGui.popItemWidth();
        }
    }
}
