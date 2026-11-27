package com.ivan1pl.lorisui.api.component;

final class FocusState {
    private volatile boolean focused;
    private volatile boolean requested;
    private volatile long requestTime;

    boolean isFocused() {
        return focused;
    }

    void setFocused(boolean focused) {
        this.focused = focused;
    }

    boolean requestsFocus() {
        return requested;
    }

    void setRequestsFocus(boolean requestsFocus) {
        if (requestsFocus) {
            // The stamp is written before the flag, so a reader that sees the flag also sees this stamp or a newer one.
            requestTime = System.nanoTime();
            requested = true;
        } else {
            requested = false;
        }
    }

    long getFocusRequestTime() {
        return requestTime;
    }
}
