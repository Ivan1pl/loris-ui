package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import com.ivan1pl.lorisui.imgui.impl.KeyboardFocus;
import imgui.ImGui;
import imgui.type.ImString;

final class InputKeyboardFocus {
    private InputKeyboardFocus() {}

    // Draws the input's text field, taking part in LorisUI keyboard focus while navigation is on
    static boolean inputText(Component component, ImString value, int flags, boolean enableInput) {
        String label = "##" + component;
        KeyboardFocus keyboardFocus = enableInput ? ImGuiState.keyboardFocus : null;
        if (keyboardFocus == null) {
            return ImGui.inputText(label, value, flags);
        }

        int id = ImGui.getID(label);
        int result = keyboardFocus.register(id, KeyboardFocus.Kind.INPUT, true);
        if ((result & KeyboardFocus.TAKE_INPUT_FOCUS) != 0) {
            ImGui.setKeyboardFocusHere();
        }
        boolean edited = ImGui.inputText(label, value, flags);
        boolean active = ImGui.isItemActive();
        keyboardFocus.reportInput(id, active);
        if (ImGui.isItemClicked()) {
            keyboardFocus.reportClick(id);
        }
        // Clearing the active ID after the widget keeps this frame's edit, so the caller still validates it
        if ((result & KeyboardFocus.RELEASE_INPUT) != 0 && active) {
            imgui.internal.ImGui.clearActiveID();
        }
        return edited;
    }
}
