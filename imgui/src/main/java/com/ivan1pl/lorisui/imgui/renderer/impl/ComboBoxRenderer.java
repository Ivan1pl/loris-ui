package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.ComboBox;
import com.ivan1pl.lorisui.api.component.Panel;
import com.ivan1pl.lorisui.api.component.style.ComboBoxItemStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxPopupStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxStyle;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiComboFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;

import java.util.Objects;

public class ComboBoxRenderer<ItemType> extends Renderer<ComboBox<ItemType>> {
    private static final int FLAGS_CHILD =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoBackground |
            ImGuiWindowFlags.NoInputs;

    @Override
    public void render(ComboBox<ItemType> component, int w, int h, RendererProvider rendererProvider,
                       BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
        ImGui.setNextItemWidth(component.getWidth());

        float paddingY = Math.max(0, (component.getHeight() - ImGui.getFontSize()) / 2);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, ImGui.getStyle().getFramePaddingX(), paddingY);

        Panel preview = component.getSelectedItemLabel();
        ComboBoxStyle style = component.getStyle();
        ComboBoxPopupStyle popupStyle = component.getPopupStyle();
        var styles = rendererProvider.getStyleApplier(style).applyStyles(style, rendererProvider);
        var popupStyles = rendererProvider.getStyleApplier(popupStyle).applyStyles(popupStyle, rendererProvider);
        boolean open = ImGui.beginCombo("##" + component, "", ImGuiComboFlags.HeightLargest);
        rendererProvider.getStyleApplier(popupStyle).popStyles(popupStyles);
        rendererProvider.getStyleApplier(style).popStyles(styles);
        ImGui.popStyleVar();

        if (open) {
            for (int i = 0; i < component.getItems().length; ++i) {
                ComboBox.Item<ItemType> item = component.getItems()[i];
                ImGui.pushID(item.item().toString());

                boolean selected = Objects.equals(component.getSelectedItem(), item.item());

                ComboBoxItemStyle itemStyle = component.getItemStyle();
                var itemStyles = rendererProvider.getStyleApplier(itemStyle).applyStyles(itemStyle, rendererProvider);
                if (ImGui.selectable("", selected, 0, item.label().getHeight(w, h))) {
                    component.setSelectedItem(item.item());
                }
                rendererProvider.getStyleApplier(itemStyle).popStyles(itemStyles);

                ImGui.sameLine();
                ImGui.beginChild("##comboItem#" + item.label(), item.label().getWidth(w, h),
                        item.label().getHeight(w, h), false, FLAGS_CHILD);
                rendererProvider.getRenderer(item.label())
                        .render(item.label(), w, h, rendererProvider, bundleKeyResolver, false, false);
                ImGui.endChild();

                if (selected) {
                    ImGui.setItemDefaultFocus();
                }

                ImGui.popID();
            }
            ImGui.endCombo();
        }

        float comboMinX = ImGui.getItemRectMinX();
        float comboMinY = ImGui.getItemRectMinY();
        ImGui.setCursorScreenPos(comboMinX, comboMinY);
        ImGui.beginChild("##comboPreview#" + preview, preview.getWidth(w, h), preview.getHeight(w, h), false,
                FLAGS_CHILD);
        rendererProvider.getRenderer(preview).render(preview, w, h, rendererProvider, bundleKeyResolver, false, false);
        ImGui.endChild();
    }
}
