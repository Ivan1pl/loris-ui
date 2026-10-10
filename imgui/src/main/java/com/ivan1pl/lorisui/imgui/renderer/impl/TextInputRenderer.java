package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.TextInput;
import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.font.ImGuiFont;
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
        TextInputStyle style = component.getStyle();
        var styles = rendererProvider.getStyleApplier(style).applyStyles(style, rendererProvider);
        try {
            if (component.getFont() != null) {
                ((ImGuiFont) component.getFont()).useFont(() -> doRender(component));
            } else {
                doRender(component);
            }
        } finally {
            rendererProvider.getStyleApplier(style).popStyles(styles);
        }
    }

    private void doRender(TextInput component) {
        ImGui.pushItemWidth(component.getWidth());
        try {
            ImString imString = new ImString(component.getValue());
            imString.inputData.isResizable = true;
            if (ImGui.inputText("##", imString, 0)) {
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
