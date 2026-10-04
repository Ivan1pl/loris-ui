package com.ivan1pl.lorisui.imgui.renderer.style;

import com.ivan1pl.lorisui.api.component.style.ComboBoxStyle;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;

public class ComboBoxStyleApplier implements StyleApplier<ComboBoxStyle> {
    @Override
    public Styles applyStyles(ComboBoxStyle style, RendererProvider rendererProvider) {
        StylesImpl styles = new StylesImpl();

        applyColor(ImGuiCol.FrameBg, style.getBackgroundColor(), styles);
        applyColor(ImGuiCol.FrameBgHovered, style.getBackgroundColorHovered(), styles);
        applyColor(ImGuiCol.Border, style.getBorderColor(), styles);
        applyColor(ImGuiCol.Button, style.getArrowButtonColor(), styles);
        applyColor(ImGuiCol.ButtonHovered, style.getArrowButtonColorHovered(), styles);
        applyColor(ImGuiCol.Text, style.getArrowColor(), styles);
        applyVar(ImGuiStyleVar.FrameRounding, style.getRounding(), styles);
        applyVar(ImGuiStyleVar.FrameBorderSize, style.getBorderThickness(), styles);

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
