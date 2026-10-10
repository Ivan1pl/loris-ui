package com.ivan1pl.lorisui.imgui.renderer.style.impl;

import com.ivan1pl.lorisui.api.component.style.ComboBoxItemStyle;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;
import imgui.ImGui;
import imgui.flag.ImGuiCol;

public class ComboBoxItemStyleApplier implements StyleApplier<ComboBoxItemStyle> {
    @Override
    public Styles applyStyles(ComboBoxItemStyle style, RendererProvider rendererProvider) {
        StylesImpl styles = new StylesImpl();

        applyColor(ImGuiCol.Header, style.getSelectedColor(), styles);
        applyColor(ImGuiCol.HeaderHovered, style.getHoveredColor(), styles);
        applyColor(ImGuiCol.HeaderActive, style.getActiveColor(), styles);

        return styles;
    }

    @Override
    public void popStyles(Styles styles) {
        int popColors = ((StylesImpl) styles).popColors;
        int popVars = ((StylesImpl) styles).popVars;
        if (popColors > 0) {
            ImGui.popStyleColor(popColors);
        }
        if (popVars > 0) {
            ImGui.popStyleVar(popVars);
        }
    }

    private static void applyColor(int imGuiCol, Color color, StylesImpl styles) {
        if (color != null) {
            ImGui.pushStyleColor(imGuiCol, color.getR(), color.getG(), color.getB(), color.getA());
            styles.popColors++;
        }
    }

    private static void applyVar(int imGuiStyleVar, Float var, StylesImpl styles) {
        if (var != null) {
            ImGui.pushStyleVar(imGuiStyleVar, var);
            styles.popVars++;
        }
    }

    private static class StylesImpl implements Styles {
        private int popColors = 0;
        private int popVars = 0;
    }
}
