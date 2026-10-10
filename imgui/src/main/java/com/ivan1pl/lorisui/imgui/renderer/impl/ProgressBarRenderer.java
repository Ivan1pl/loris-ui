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
        float rounding = component.getRounding();
        Color backgroundColor = component.getBackgroundColor();
        Color barColor = component.getBarColor();
        if (rounding > 0f) {
            ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, rounding);
        }
        if (backgroundColor != null) {
            ImGui.pushStyleColor(ImGuiCol.FrameBg,
                    backgroundColor.getR(), backgroundColor.getG(), backgroundColor.getB(), backgroundColor.getA());
        }
        if (barColor != null) {
            ImGui.pushStyleColor(ImGuiCol.PlotHistogram,
                    barColor.getR(), barColor.getG(), barColor.getB(), barColor.getA());
        }
        ImGui.progressBar(component.getProgress(), component.getWidth(), component.getHeight(), "");
        if (barColor != null) {
            ImGui.popStyleColor(1);
        }
        if (backgroundColor != null) {
            ImGui.popStyleColor(1);
        }
        if (rounding > 0f) {
            ImGui.popStyleVar(1);
        }
    }
}
