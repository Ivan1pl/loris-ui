package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Button;

import java.util.HashMap;
import java.util.Map;

final class ButtonInputTracker {
    private final Map<Integer, Integer> lastHoveredFrames = new HashMap<>();
    private int currentFrame = 0;
    private int pressedId;
    private boolean pressRecorded;
    private boolean pressProcessedThisFrame;
    private boolean releaseSeenThisFrame;

    // Keyboard focus start and key activation share the pointer triggers, so each trigger runs at most once per update
    boolean update(Button button, int id, int frame, boolean hovered, boolean mouseClicked, boolean mouseDown,
                   boolean mouseReleased, boolean keyboardFocusStarted, boolean keyActivated) {
        if (frame != currentFrame) {
            startFrame(frame);
        }

        if (mouseClicked) {
            if (!pressProcessedThisFrame) {
                pressRecorded = false;
                pressProcessedThisFrame = true;
            }
            if (hovered) {
                pressedId = id;
                pressRecorded = true;
            }
        }

        boolean pressedHere = pressRecorded && pressedId == id;

        boolean hoverStarted = hovered && !lastHoveredFrames.containsKey(id);
        if (hovered || (mouseDown && pressedHere)) {
            lastHoveredFrames.put(id, frame);
        }

        boolean clickDue = false;
        if (mouseReleased) {
            releaseSeenThisFrame = true;
            if (hovered && pressedHere) {
                clickDue = true;
                pressRecorded = false;
            }
        }

        if (hoverStarted || keyboardFocusStarted) {
            button.hover();
        }
        if (clickDue || keyActivated) {
            button.click();
        }
        return hovered && mouseDown && pressedHere;
    }

    private void startFrame(int frame) {
        int endedFrame = currentFrame;
        boolean consecutive = frame == endedFrame + 1;
        if (!consecutive || releaseSeenThisFrame) {
            pressRecorded = false;
        }
        if (consecutive) {
            lastHoveredFrames.values().removeIf(lastHovered -> lastHovered != endedFrame);
        } else {
            lastHoveredFrames.clear();
        }
        pressProcessedThisFrame = false;
        releaseSeenThisFrame = false;
        currentFrame = frame;
    }
}
