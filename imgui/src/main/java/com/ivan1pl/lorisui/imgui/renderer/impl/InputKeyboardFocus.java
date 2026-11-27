package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Focusable;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import com.ivan1pl.lorisui.imgui.impl.KeyboardFocus;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.type.ImString;

final class InputKeyboardFocus {
    private InputKeyboardFocus() {}

    // Draws the input's text field, dimmed and non-interactive while disabled, and returns whether its text was edited
    // in this frame and whether Enter submitted it. It takes part in LorisUI keyboard focus and submits unless the
    // input takes no input: the slot is set during every frame, so null means enableInput is false
    static Result inputText(Focusable component, ImString value, int flags, boolean enableInput, boolean enabled) {
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
                return new Result(ImGui.inputText(label, value, flags), false);
            }

            int result = keyboardFocus.register(id, KeyboardFocus.Kind.INPUT, enabled, component);
            if ((result & KeyboardFocus.TAKE_INPUT_FOCUS) != 0) {
                ImGui.setKeyboardFocusHere();
            }
            boolean wasActive = imgui.internal.ImGui.getActiveID() == id;
            boolean edited = ImGui.inputText(label, value, flags);
            boolean active = ImGui.isItemActive();
            keyboardFocus.reportInput(id, active && enabled);
            boolean clicked = ImGui.isMouseClicked(ImGuiMouseButton.Left);
            // A disabled input is never hovered without AllowWhenDisabled, but its click still sets the anchor
            if (clicked && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
                keyboardFocus.reportClick(id);
            }
            // The active ID is read before the widget, because Dear ImGui clears it on Enter inside the widget, and
            // after the disabled release, so a disabled input never submits. Only an Enter that Dear ImGui handled as
            // ending the editing submits: the input is no longer active and no click ended it first. Alt and Super are
            // checked here because they never submit by the documented contract
            ImGuiIO io = ImGui.getIO();
            boolean submitted = wasActive && !active && !io.getKeyAlt() && !io.getKeySuper()
                    && (ImGui.isKeyPressed(ImGuiKey.Enter, true) || ImGui.isKeyPressed(ImGuiKey.KeypadEnter, true))
                    && !clicked;
            // Clearing the active ID after the widget keeps this frame's edit, so the caller still validates it
            if ((result & KeyboardFocus.RELEASE_INPUT) != 0 && active) {
                imgui.internal.ImGui.clearActiveID();
            }
            return new Result(edited, submitted);
        } finally {
            ImGui.endDisabled();
        }
    }

    record Result(boolean edited, boolean submitted) {}
}
