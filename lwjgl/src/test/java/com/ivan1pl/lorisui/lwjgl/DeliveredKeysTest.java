package com.ivan1pl.lorisui.lwjgl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DeliveredKeysTest {
    // SDL scancodes
    private static final int KEY_W = 26;
    private static final int KEY_TAB = 43;

    private final DeliveredKeys deliveredKeys = new DeliveredKeys();

    @Test
    void shouldPassReleaseWhoseKeyDownReachedApplicationWhileUiCaptures() {
        Assertions.assertFalse(deliveredKeys.isConsumed(KEY_W, true, false));

        Assertions.assertFalse(deliveredKeys.isConsumed(KEY_W, false, true));
    }

    @Test
    void shouldConsumeReleaseWhoseKeyDownWasConsumedAfterCaptureEnds() {
        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_W, true, true));

        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_W, false, false));
    }

    @Test
    void shouldApplyCurrentCaptureToKeyDowns() {
        Assertions.assertFalse(deliveredKeys.isConsumed(KEY_W, true, false));
        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_W, true, true));
    }

    @Test
    void shouldPassReleaseWhenRepeatReachedApplicationAfterConsumedPress() {
        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_W, true, true));
        Assertions.assertFalse(deliveredKeys.isConsumed(KEY_W, true, false));
        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_W, true, true));

        Assertions.assertFalse(deliveredKeys.isConsumed(KEY_W, false, true));
    }

    @Test
    void shouldConsumeReleaseWhenEveryKeyDownOfHoldWasConsumed() {
        deliveredKeys.isConsumed(KEY_W, true, true);
        deliveredKeys.isConsumed(KEY_W, true, true);

        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_W, false, false));
    }

    @Test
    void shouldApplyCurrentCaptureToReleaseOfUntrackedKey() {
        Assertions.assertFalse(deliveredKeys.isConsumed(KEY_W, false, false));
        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_TAB, false, true));
    }

    @Test
    void shouldStartEachHoldAfterReleaseAnew() {
        deliveredKeys.isConsumed(KEY_W, true, false);
        deliveredKeys.isConsumed(KEY_W, false, true);
        deliveredKeys.isConsumed(KEY_W, true, true);

        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_W, false, false));
        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_W, false, true));
    }

    @Test
    void shouldTrackEachKeySeparately() {
        deliveredKeys.isConsumed(KEY_W, true, false);
        deliveredKeys.isConsumed(KEY_TAB, true, true);

        Assertions.assertTrue(deliveredKeys.isConsumed(KEY_TAB, false, false));
        Assertions.assertFalse(deliveredKeys.isConsumed(KEY_W, false, true));
    }
}
