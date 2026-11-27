package com.ivan1pl.lorisui.api.component;

final class ScrollState {
    private volatile float position;
    private volatile float requestedPosition;
    private volatile long requestTime;
    private volatile boolean requested;

    float getPosition() {
        return position;
    }

    void report(float position) {
        this.position = position;
    }

    boolean isRequested() {
        return requested;
    }

    void request(float position) {
        // The value is written before the stamp and the stamp before the flag. A reader that reads the flag, then the
        // stamp, then the value sees the value of the request that set that stamp, or a value from a concurrent or
        // later request. Such a request writes a newer stamp and sets the flag again, so the stamp check of the
        // reader keeps the newest value pending.
        requestedPosition = position;
        requestTime = System.nanoTime();
        requested = true;
    }

    void clear() {
        requested = false;
    }

    long getRequestTime() {
        return requestTime;
    }

    float getRequestedPosition() {
        return requestedPosition;
    }
}
