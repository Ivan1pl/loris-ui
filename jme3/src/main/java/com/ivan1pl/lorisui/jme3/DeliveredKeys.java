package com.ivan1pl.lorisui.jme3;

import java.util.HashMap;
import java.util.Map;

/**
 * Pairs each key release with its key-down: a release reaches the application exactly when a key-down of the same
 * hold (the press or a repeat) reached it, whatever the UI captures at the moment of release. Otherwise the
 * application would see a key held down, or a release it never saw pressed, when the capture switches while the key
 * is held. Only the release of a key whose key-down was never seen follows the current capture. The
 * {@code lorisui.lwjgl} module keeps a copy without {@link #clear()}, {@code com.ivan1pl.lorisui.lwjgl.DeliveredKeys},
 * because the jME host cannot depend on it.
 */
final class DeliveredKeys {
    // Held keys, mapped to whether any key-down of the current hold reached the application
    private final Map<Integer, Boolean> heldKeys = new HashMap<>();

    // key identifies the physical key; down is true for a press or repeat and false for a release; uiCaptures is
    // whether the UI wants keyboard input now. Returns whether the UI consumes the event
    boolean isConsumed(int key, boolean down, boolean uiCaptures) {
        if (down) {
            heldKeys.merge(key, !uiCaptures, Boolean::logicalOr);
            return uiCaptures;
        }
        Boolean delivered = heldKeys.remove(key);
        return delivered == null ? uiCaptures : !delivered;
    }

    // Forgets every held key, so their releases follow the capture at that moment
    void clear() {
        heldKeys.clear();
    }
}
