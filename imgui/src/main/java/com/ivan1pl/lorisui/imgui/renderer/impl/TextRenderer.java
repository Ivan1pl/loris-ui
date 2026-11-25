package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Text;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.ImGuiFont;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;

public class TextRenderer extends Renderer<Text> {
    private static final int FLAGS_CHILD =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoBackground;

    private final ImVec2 textSize = new ImVec2();

    @Override
    public void render(Text component, int w, int h, RendererProvider rendererProvider,
                       BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
        if (component.getFont() != null) {
            ((ImGuiFont) component.getFont()).useFont(() -> doRender(component, w, bundleKeyResolver, enableInput));
        } else {
            doRender(component, w, bundleKeyResolver, enableInput);
        }
    }

    private void doRender(Text component, int w, BundleKeyResolver bundleKeyResolver, boolean enableInput) {
        int flags = enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs);
        String text = component.getText(bundleKeyResolver);
        ImGui.calcTextSize(textSize, text, false, Float.POSITIVE_INFINITY);
        // alignTextToFramePadding offsets the text by the vertical frame padding, so it is added on both sides
        float height = textSize.y + 2f * ImGui.getStyle().getFramePaddingY();
        ImGui.beginChild(component + "_text", component.getWidth(w), height, false, flags);
        if (component.getWidth(w) > 0 &&
                (component.getAlign() == Text.Align.RIGHT || component.getAlign() == Text.Align.CENTER)) {
            float dummySize = component.getWidth(w) - textSize.x;
            if (component.getAlign() == Text.Align.CENTER) {
                dummySize /= 2f;
            }
            ImGui.dummy(dummySize, 0);
            ImGui.sameLine(dummySize, 0);
        }
        ImGui.alignTextToFramePadding();
        Color color = component.getColor();
        if (color == null) {
            ImGui.textUnformatted(text);
        } else {
            ImGui.pushStyleColor(ImGuiCol.Text, color.getR(), color.getG(), color.getB(), color.getA());
            ImGui.textUnformatted(text);
            ImGui.popStyleColor();
        }
        ImGui.endChild();
    }
}
