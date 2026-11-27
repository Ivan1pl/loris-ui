package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Focusable;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import com.ivan1pl.lorisui.imgui.impl.KeyboardFocus;
import imgui.ImGui;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiMouseButton;
import imgui.type.ImString;

final class InputKeyboardFocus {
    private InputKeyboardFocus() {}

    // Draws the input's text field, dimmed and non-interactive while disabled. It takes part in LorisUI keyboard
    // focus unless the input takes no input: the slot is set during every frame, so null means enableInput is false
    static boolean inputText(Focusable component, ImString value, int flags, boolean enableInput, boolean enabled) {
        String label = "##" + component;
        int id = ImGui.getID(label);
        // Released before the widget, so an input disabled while it is edited takes no keys in this frame
        if (!enabled && imgui.internal.ImGui.getActiveID() == id) {
            imgui.internal.ImGui.clearActiveID();
        }
        KeyboardFocus keyboardFocus = enableInput ? ImGuiState.keyboardFocus : null;
        ImGui.beginDisabled(!enabled);
        try {
            if (keyboardFocus == null) {
                return ImGui.inputText(label, value, flags);
            }

            int result = keyboardFocus.register(id, KeyboardFocus.Kind.INPUT, enabled, component);
            if ((result & KeyboardFocus.TAKE_INPUT_FOCUS) != 0) {
                ImGui.setKeyboardFocusHere();
            }
            boolean edited = ImGui.inputText(label, value, flags);
            boolean active = ImGui.isItemActive();
            keyboardFocus.reportInput(id, active && enabled);
            // A disabled input is never hovered without AllowWhenDisabled, but its click still sets the anchor
            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)
                    && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
                keyboardFocus.reportClick(id);
            }
            // Clearing the active ID after the widget keeps this frame's edit, so the caller still validates it
            if ((result & KeyboardFocus.RELEASE_INPUT) != 0 && active) {
                imgui.internal.ImGui.clearActiveID();
            }
            return edited;
        } finally {
            ImGui.endDisabled();
        }
    }
}
