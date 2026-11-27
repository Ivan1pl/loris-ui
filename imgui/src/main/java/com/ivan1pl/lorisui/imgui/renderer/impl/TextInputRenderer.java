package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.TextInput;
import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.ImGuiFont;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.type.ImString;

import java.util.Map;
import java.util.WeakHashMap;

public class TextInputRenderer extends Renderer<TextInput> {
    private final Map<TextInput, String> lastEditValue = new WeakHashMap<>();

    @Override
    public void render(TextInput component, int w, int h, RendererProvider rendererProvider,
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

    private void doRender(TextInput component, boolean enableInput, boolean enabled) {
        ImGui.pushItemWidth(component.getWidth());
        try {
            ImString imString = new ImString(component.getValue());
            imString.inputData.isResizable = true;
            if (InputKeyboardFocus.inputText(component, imString, 0, enableInput, enabled)) {
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
