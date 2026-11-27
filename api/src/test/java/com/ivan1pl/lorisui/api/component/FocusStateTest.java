package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class FocusStateTest {
    private static final long TIMEOUT_MILLIS = 5_000;

    @Test
    void shouldStartUnfocusedWithoutRequest() {
        FocusState state = new FocusState();

        Assertions.assertAll(
                () -> Assertions.assertFalse(state.isFocused()),
                () -> Assertions.assertFalse(state.requestsFocus()),
                () -> Assertions.assertEquals(0L, state.getFocusRequestTime()));
    }

    @Test
    void shouldKeepRequestTimeWhenRequestIsCancelled() {
        FocusState state = new FocusState();
        state.setRequestsFocus(true);
        long stamp = state.getFocusRequestTime();

        state.setRequestsFocus(false);

        Assertions.assertFalse(state.requestsFocus());
        Assertions.assertEquals(stamp, state.getFocusRequestTime());
    }

    @Test
    void shouldStampRequestWithCurrentNanoTime() {
        FocusState state = new FocusState();

        long before = System.nanoTime();
        state.setRequestsFocus(true);
        long after = System.nanoTime();

        long stamp = state.getFocusRequestTime();
        Assertions.assertTrue(state.requestsFocus());
        Assertions.assertTrue(stamp - before >= 0, "stamp taken before the call");
        Assertions.assertTrue(after - stamp >= 0, "stamp taken after the call");
    }

    @Test
    void shouldStampRenewedRequestNoOlderThanPreviousOne() {
        FocusState state = new FocusState();
        state.setRequestsFocus(true);
        long first = state.getFocusRequestTime();

        state.setRequestsFocus(true);

        Assertions.assertTrue(state.requestsFocus());
        Assertions.assertTrue(state.getFocusRequestTime() - first >= 0);
    }

    @Test
    void shouldReturnFocusedValueThatWasSet() {
        FocusState state = new FocusState();

        state.setFocused(true);
        boolean focused = state.isFocused();
        state.setFocused(false);

        Assertions.assertTrue(focused);
        Assertions.assertFalse(state.isFocused());
    }

    @Test
    void shouldSeeRequestMadeOnAnotherThreadAsPendingWithItsStamp() throws InterruptedException {
        FocusState state = new FocusState();
        CountDownLatch start = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            awaitUninterruptibly(start);
            state.setRequestsFocus(true);
        });
        worker.start();

        long before = System.nanoTime();
        start.countDown();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
        while (!state.requestsFocus() && deadline - System.nanoTime() > 0) {
            Thread.onSpinWait();
        }
        long stamp = state.getFocusRequestTime();
        worker.join(TIMEOUT_MILLIS);

        Assertions.assertFalse(worker.isAlive());
        Assertions.assertTrue(state.requestsFocus());
        Assertions.assertTrue(stamp - before >= 0, "the pending request carries its own stamp");
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timed out waiting for the test thread");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
