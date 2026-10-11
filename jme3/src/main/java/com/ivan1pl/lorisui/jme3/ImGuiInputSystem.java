package com.ivan1pl.lorisui.jme3;

import com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

final class ImGuiInputSystem implements RawInputListener {
    private final ImGuiOpenGlRenderer renderer;

    ImGuiInputSystem(ImGuiOpenGlRenderer renderer) {
        this.renderer = renderer;
    }

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
        if (renderer.wantsMouseInput() && (evt.getDeltaWheel() == 0 || renderer.isHoveringScrollablePanel())) {
            evt.setConsumed();
        }
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (renderer.wantsMouseInput()) {
            evt.setConsumed();
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (renderer.wantsKeyboardInput()) {
            evt.setConsumed();
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
        // not supported
    }
}
