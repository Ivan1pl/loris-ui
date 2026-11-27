package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.ComboBox;
import com.ivan1pl.lorisui.api.component.Panel;
import com.ivan1pl.lorisui.api.component.style.ComboBoxItemStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxPopupStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxStyle;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import com.ivan1pl.lorisui.imgui.impl.KeyboardFocus;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiComboFlags;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiSelectableFlags;
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
        boolean enabled = component.isEnabled();
        // The slot is set during every frame, so null means this combo takes no input
        KeyboardFocus keyboardFocus = enableInput ? ImGuiState.keyboardFocus : null;
        int comboId = 0;
        int focusResult = 0;
        if (keyboardFocus != null) {
            comboId = ImGui.getID("##" + component);
            focusResult = keyboardFocus.register(comboId, KeyboardFocus.Kind.COMBO, enabled, component);
            if ((focusResult & KeyboardFocus.ACTIVATE) != 0) {
                // Dear ImGui's internal combo popup ID, seeded with the combo's ID as in BeginCombo
                ImGui.openPopup(imgui.internal.ImGui.getIDWithSeed("##ComboPopup", null, comboId));
            }
        }

        // Spans the frame, the popup window and the preview child, each of which begins and ends inside it
        ImGui.beginDisabled(!enabled);
        try {
            Panel preview = component.getSelectedItemLabel();
            boolean open = beginCombo(component, rendererProvider, enabled,
                    (focusResult & KeyboardFocus.FOCUS_LOOK) != 0);

            if (open) {
                if (keyboardFocus != null) {
                    keyboardFocus.pushComboScope(comboId);
                }
                try {
                    // The renderer closes a disabled combo itself, also when it takes no keyboard focus
                    if ((focusResult & KeyboardFocus.CLOSE_COMBO_SCOPE) != 0 || !enabled) {
                        ImGui.closeCurrentPopup();
                    }
                    renderItems(component, w, h, rendererProvider, bundleKeyResolver, keyboardFocus);
                } finally {
                    if (keyboardFocus != null) {
                        keyboardFocus.popScope();
                    }
                }
                ImGui.endCombo();
            }

            // The combo frame is the last item again. While the combo's own popup is open it blocks the frame's
            // hover, so only then is a blocked hover accepted; a closed combo under another popup is not clicked. A
            // disabled frame is never hovered without AllowWhenDisabled, but its click still sets the anchor
            int hoveredFlags = open
                    ? ImGuiHoveredFlags.AllowWhenBlockedByPopup | ImGuiHoveredFlags.AllowWhenDisabled
                    : ImGuiHoveredFlags.AllowWhenDisabled;
            if (keyboardFocus != null && ImGui.isMouseClicked(ImGuiMouseButton.Left)
                    && ImGui.isItemHovered(hoveredFlags)) {
                keyboardFocus.reportClick(comboId);
            }

            float comboMinX = ImGui.getItemRectMinX();
            float comboMinY = ImGui.getItemRectMinY();
            ImGui.setCursorScreenPos(comboMinX, comboMinY);
            ImGui.beginChild("##comboPreview#" + preview, preview.getWidth(w, h), preview.getHeight(w, h), false,
                    FLAGS_CHILD);
            rendererProvider.getRenderer(preview)
                    .render(preview, w, h, rendererProvider, bundleKeyResolver, false, false);
            ImGui.endChild();
        } finally {
            ImGui.endDisabled();
        }
    }

    private boolean beginCombo(ComboBox<ItemType> component, RendererProvider rendererProvider, boolean enabled,
                               boolean focusLook) {
        ImGui.setNextItemWidth(component.getWidth());

        float paddingY = Math.max(0, (component.getHeight() - ImGui.getFontSize()) / 2);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, ImGui.getStyle().getFramePaddingX(), paddingY);

        ComboBoxStyle disabledStyle = enabled ? null : component.getDisabledStyle();
        ComboBoxStyle style = disabledStyle != null ? disabledStyle : component.getStyle();
        ComboBoxPopupStyle popupStyle = component.getPopupStyle();
        var styles = rendererProvider.getStyleApplier(style).applyStyles(style, rendererProvider);
        var popupStyles = rendererProvider.getStyleApplier(popupStyle).applyStyles(popupStyle, rendererProvider);
        if (focusLook) {
            // Read after the combo style is applied, so the focus look is the combo's own hovered frame color
            ImGui.pushStyleColor(ImGuiCol.FrameBg, ImGui.getStyleColorVec4(ImGuiCol.FrameBgHovered));
        }
        try {
            return ImGui.beginCombo("##" + component, "", ImGuiComboFlags.HeightLargest);
        } finally {
            if (focusLook) {
                ImGui.popStyleColor();
            }
            rendererProvider.getStyleApplier(popupStyle).popStyles(popupStyles);
            rendererProvider.getStyleApplier(style).popStyles(styles);
            ImGui.popStyleVar();
        }
    }

    private void renderItems(ComboBox<ItemType> component, int w, int h, RendererProvider rendererProvider,
                             BundleKeyResolver bundleKeyResolver, KeyboardFocus keyboardFocus) {
        for (int i = 0; i < component.getItems().length; ++i) {
            ComboBox.Item<ItemType> item = component.getItems()[i];
            ImGui.pushID(item.item().toString());

            boolean selected = Objects.equals(component.getSelectedItem(), item.item());

            int itemId = 0;
            int focusResult = 0;
            if (keyboardFocus != null) {
                itemId = ImGui.getID("");
                focusResult = keyboardFocus.registerComboItem(itemId, selected);
            }
            boolean activated = (focusResult & KeyboardFocus.ACTIVATE) != 0;
            int selectableFlags = (focusResult & KeyboardFocus.FOCUS_LOOK) != 0 ? ImGuiSelectableFlags.Highlight : 0;

            ComboBoxItemStyle itemStyle = component.getItemStyle();
            var itemStyles = rendererProvider.getStyleApplier(itemStyle).applyStyles(itemStyle, rendererProvider);
            boolean clicked = ImGui.selectable("", selected, selectableFlags, 0, item.label().getHeight(w, h));
            rendererProvider.getStyleApplier(itemStyle).popStyles(itemStyles);
            if (clicked || activated) {
                component.setSelectedItem(item.item());
            }
            if (clicked && keyboardFocus != null) {
                keyboardFocus.reportClick(itemId);
            }
            if (activated) {
                ImGui.closeCurrentPopup();
            }

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
    }
}
