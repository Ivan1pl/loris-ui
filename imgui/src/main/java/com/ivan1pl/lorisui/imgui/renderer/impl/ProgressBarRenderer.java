package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.ProgressBar;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;

public class ProgressBarRenderer extends Renderer<ProgressBar> {
    @Override
    public void render(ProgressBar component, int w, int h, RendererProvider rendererProvider,
                       BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
        Float rounding = component.getRounding();
        Color backgroundColor = component.getBackgroundColor();
        Color barColor = component.getBarColor();
        int popVars = 0;
        int popColors = 0;
        if (rounding != null) {
            ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, rounding);
            popVars++;
        }
        if (backgroundColor != null) {
            ImGui.pushStyleColor(ImGuiCol.FrameBg,
                    backgroundColor.getR(), backgroundColor.getG(), backgroundColor.getB(), backgroundColor.getA());
            popColors++;
        }
        if (barColor != null) {
            ImGui.pushStyleColor(ImGuiCol.PlotHistogram,
                    barColor.getR(), barColor.getG(), barColor.getB(), barColor.getA());
            popColors++;
        }
        try {
            ImGui.progressBar(component.getProgress(), component.getWidth(), component.getHeight(), "");
        } finally {
            if (popColors > 0) {
                ImGui.popStyleColor(popColors);
            }
            if (popVars > 0) {
                ImGui.popStyleVar(popVars);
            }
        }
    }
}
