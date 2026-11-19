package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Canvas;
import com.ivan1pl.lorisui.api.component.MouseButton;

import java.util.HashMap;
import java.util.Map;

final class CanvasInputTracker {
    private static final MouseButton[] BUTTONS = MouseButton.values();

    private final Map<Integer, Entry> entries = new HashMap<>();
    private int currentFrame = 0;

    // A gesture is followed through the down and released bits alone, so it continues outside the canvas
    void update(Canvas canvas, int id, int frame, boolean enabled, boolean hovered, float mouseX, float mouseY,
                boolean mouseValid, int clickedButtons, int downButtons, int releasedButtons, float wheelDelta,
                float itemMinX, float itemMinY, Canvas.Region region) {
        if (frame != currentFrame) {
            startFrame(frame);
        }
        Entry entry = entries.get(id);
        if (entry == null) {
            entry = new Entry();
            entries.put(id, entry);
        } else if (entry.lastFrame == frame) {
            return;
        }
        entry.lastFrame = frame;

        if (!enabled) {
            entry.endGesture();
            return;
        }

        if (entry.owner != null) {
            int ownerBit = bit(entry.owner);
            if ((releasedButtons & ownerBit) != 0 || (downButtons & ownerBit) == 0) {
                entry.endGesture();
            }
        }

        double scaleX = (double) region.width() / canvas.getWidth();
        double scaleY = (double) region.height() / canvas.getHeight();
        int mapX = region.x() + (int) Math.floor((mouseX - itemMinX) * scaleX);
        int mapY = region.y() + (int) Math.floor((mouseY - itemMinY) * scaleY);

        if (entry.owner == null) {
            MouseButton pressed = hovered ? firstButton(clickedButtons) : null;
            if (pressed != null) {
                entry.startGesture(pressed, mouseValid, mouseX, mouseY);
                canvas.click(mapX, mapY, pressed);
            }
        } else if (!mouseValid) {
            entry.anchored = false;
        } else {
            drag(canvas, entry, mouseX, mouseY, scaleX, scaleY, mapX, mapY);
        }

        if (hovered && wheelDelta != 0f) {
            canvas.wheel(mapX, mapY, wheelDelta);
        }
    }

    static int bit(MouseButton button) {
        return 1 << button.ordinal();
    }

    private void startFrame(int frame) {
        int endedFrame = frame - 1;
        entries.values().removeIf(entry -> entry.lastFrame != endedFrame);
        currentFrame = frame;
    }

    private static void drag(Canvas canvas, Entry entry, float mouseX, float mouseY, double scaleX, double scaleY,
                             int mapX, int mapY) {
        if (entry.anchored) {
            entry.accX += (mouseX - entry.lastX) * scaleX;
            entry.accY += (mouseY - entry.lastY) * scaleY;
        }
        int dx = (int) entry.accX;
        int dy = (int) entry.accY;
        entry.accX -= dx;
        entry.accY -= dy;
        entry.anchor(mouseX, mouseY);
        if (dx != 0 || dy != 0) {
            canvas.drag(mapX, mapY, dx, dy, entry.owner);
        }
    }

    private static MouseButton firstButton(int buttons) {
        for (MouseButton button : BUTTONS) {
            if ((buttons & bit(button)) != 0) {
                return button;
            }
        }
        return null;
    }

    private static final class Entry {
        private MouseButton owner;
        private boolean anchored;
        private float lastX;
        private float lastY;
        private double accX;
        private double accY;
        private int lastFrame;

        private void startGesture(MouseButton button, boolean mouseValid, float mouseX, float mouseY) {
            owner = button;
            accX = 0;
            accY = 0;
            anchored = false;
            if (mouseValid) {
                anchor(mouseX, mouseY);
            }
        }

        private void anchor(float mouseX, float mouseY) {
            anchored = true;
            lastX = mouseX;
            lastY = mouseY;
        }

        private void endGesture() {
            owner = null;
            anchored = false;
        }
    }
}
