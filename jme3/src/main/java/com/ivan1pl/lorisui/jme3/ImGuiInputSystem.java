package com.ivan1pl.lorisui.jme3;

import com.ivan1pl.lorisui.imgui.ImGuiState;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import imgui.ImGui;

public class ImGuiInputSystem implements RawInputListener {
    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
        // not supported
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
        // not supported
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        if (ImGui.getIO().getWantCaptureMouse() && (evt.getDeltaWheel() == 0 || ImGuiState.hoveringScrollableWindow)) {
            evt.setConsumed();
        }
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (ImGui.getIO().getWantCaptureMouse()) {
            evt.setConsumed();
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (ImGui.getIO().getWantCaptureKeyboard()) {
            evt.setConsumed();
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
        // not supported
    }
}
