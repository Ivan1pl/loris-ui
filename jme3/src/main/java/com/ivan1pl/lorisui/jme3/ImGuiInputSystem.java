package com.ivan1pl.lorisui.jme3;

import com.ivan1pl.lorisui.imgui.ImGuiRenderer;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

final class ImGuiInputSystem implements RawInputListener {
    private final ImGuiRenderer renderer;
    private final DeliveredKeys deliveredKeys = new DeliveredKeys();

    ImGuiInputSystem(ImGuiRenderer renderer) {
        this.renderer = renderer;
    }

    // Called when the display stops receiving input, so a later attach starts without stale keys
    void forgetDeliveredKeys() {
        deliveredKeys.clear();
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
        if (deliveredKeys.isConsumed(evt.getKeyCode(), evt.isPressed(), renderer.wantsKeyboardInput())) {
            evt.setConsumed();
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
        // not supported
    }
}
