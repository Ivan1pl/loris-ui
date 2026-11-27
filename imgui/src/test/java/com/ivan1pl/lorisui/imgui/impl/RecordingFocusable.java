package com.ivan1pl.lorisui.imgui.impl;

import com.ivan1pl.lorisui.api.component.Focusable;

// Records the backend's focus write-back and request clears; tests set requests with explicit stamps
final class RecordingFocusable implements Focusable {
    private boolean focused;
    private boolean requested;
    private long requestTime;
    private int focusedTrueCount;
    private int focusedFalseCount;
    private int requestClearCount;

    void request(long requestTime) {
        this.requestTime = requestTime;
        requested = true;
    }

    int getFocusedTrueCount() {
        return focusedTrueCount;
    }

    int getFocusedFalseCount() {
        return focusedFalseCount;
    }

    int getRequestClearCount() {
        return requestClearCount;
    }

    // KeyboardFocus never reads it: the renderers pass the enabled state they evaluated
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused) {
            focusedTrueCount++;
        } else {
            focusedFalseCount++;
        }
        this.focused = focused;
    }

    @Override
    public boolean requestsFocus() {
        return requested;
    }

    @Override
    public void setRequestsFocus(boolean requestsFocus) {
        if (!requestsFocus) {
            requestClearCount++;
        }
        requested = requestsFocus;
    }

    @Override
    public long getFocusRequestTime() {
        return requestTime;
    }
}
