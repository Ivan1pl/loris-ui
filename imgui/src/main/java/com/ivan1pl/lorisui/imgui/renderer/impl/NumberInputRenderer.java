package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.NumberInput;
import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.font.ImGuiFont;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NumberInputRenderer extends Renderer<NumberInput> {
    private final Map<NumberInput, String> lastEditValue = new HashMap<>();

    @Override
    public void render(NumberInput component, int w, int h, RendererProvider rendererProvider,
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

    private void doRender(NumberInput component) {
        ImGui.pushItemWidth(component.getWidth());
        byte[] buffer = component.getBuffer();
        ImString imString = new ImString(new String(buffer), buffer.length);
        if (ImGui.inputText("##", imString, ImGuiInputTextFlags.CharsDecimal)) {
            int firstZeroIndex = 0;
            buffer = imString.getData();
            for (byte b : buffer) {
                if (b != 0) {
                    firstZeroIndex++;
                }
            }
            lastEditValue.put(component, new String(buffer, 0, firstZeroIndex, StandardCharsets.UTF_8));
        } else {
            String value = lastEditValue.remove(component);
            if (value != null) {
                component.validateInput(value);
            }
        }
        ImGui.popItemWidth();
    }
}
