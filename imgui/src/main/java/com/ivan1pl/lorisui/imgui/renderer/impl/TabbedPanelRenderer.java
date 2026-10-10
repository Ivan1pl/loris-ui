package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.TabbedPanel;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

public class TabbedPanelRenderer extends Renderer<TabbedPanel> {
    private static final int FLAGS_CHILD =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoBackground;

    @Override
    public void render(TabbedPanel component, int w, int h, RendererProvider rendererProvider,
                       BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
        String id = component + "_tabbed_panel";
        int flags = enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs);
        ImGui.beginChild(id, component.getWidth(), component.getHeight(), false, flags);
        if (!component.getTabsContents().isEmpty()) {
            rendererProvider.getRenderer(component.getTabsPanel()).render(
                    component.getTabsPanel(), w, h, rendererProvider, bundleKeyResolver, false, enableInput);
            Component contents = component.getTabsContents().get(component.getTabIndex()).get();
            rendererProvider.getRenderer(contents).render(
                    contents, w, h, rendererProvider, bundleKeyResolver, false, enableInput);
        }
        ImGui.endChild();
    }
}
