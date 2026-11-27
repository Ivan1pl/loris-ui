package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ScrollStateTest {
    private static final long TIMEOUT_MILLIS = 5_000;

    @Test
    void shouldReportNoRequestAndZeroPositionWhenNew() {
        ScrollState state = new ScrollState();

        Assertions.assertAll(
                () -> Assertions.assertEquals(0f, state.getPosition(), 1e-6f),
                () -> Assertions.assertFalse(state.isRequested()),
                () -> Assertions.assertEquals(0L, state.getRequestTime()));
    }

    @Test
    void shouldStoreValueAndStampWhenRequested() {
        ScrollState state = new ScrollState();

        long before = System.nanoTime();
        state.request(0.25f);
        long after = System.nanoTime();

        long stamp = state.getRequestTime();
        Assertions.assertTrue(state.isRequested());
        Assertions.assertEquals(0.25f, state.getRequestedPosition(), 1e-6f);
        Assertions.assertTrue(stamp - before >= 0, "stamp taken before the call");
        Assertions.assertTrue(after - stamp >= 0, "stamp taken after the call");
    }

    @Test
    void shouldReplaceValueWithNoOlderStampWhenRequestRenewed() {
        ScrollState state = new ScrollState();
        state.request(0.25f);
        long first = state.getRequestTime();

        state.request(0.75f);

        Assertions.assertTrue(state.isRequested());
        Assertions.assertEquals(0.75f, state.getRequestedPosition(), 1e-6f);
        Assertions.assertTrue(state.getRequestTime() - first >= 0);
    }

    @Test
    void shouldKeepValueAndStampWhenRequestCleared() {
        ScrollState state = new ScrollState();
        state.request(0.5f);
        long stamp = state.getRequestTime();

        state.clear();

        Assertions.assertFalse(state.isRequested());
        Assertions.assertEquals(0.5f, state.getRequestedPosition(), 1e-6f);
        Assertions.assertEquals(stamp, state.getRequestTime());
    }

    @Test
    void shouldReturnReportedPositionWhenReported() {
        ScrollState state = new ScrollState();

        state.report(0.4f);
        float first = state.getPosition();
        state.report(1f);

        Assertions.assertEquals(0.4f, first, 1e-6f);
        Assertions.assertEquals(1f, state.getPosition(), 1e-6f);
        Assertions.assertFalse(state.isRequested(), "reporting makes no request");
    }

    @Test
    void shouldSeeRequestFromAnotherThreadWhenPending() throws InterruptedException {
        ScrollState state = new ScrollState();
        CountDownLatch start = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            awaitUninterruptibly(start);
            state.request(0.6f);
        });
        worker.start();

        long before = System.nanoTime();
        start.countDown();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
        while (!state.isRequested() && deadline - System.nanoTime() > 0) {
            Thread.onSpinWait();
        }
        long stamp = state.getRequestTime();
        float value = state.getRequestedPosition();
        worker.join(TIMEOUT_MILLIS);

        Assertions.assertFalse(worker.isAlive());
        Assertions.assertTrue(state.isRequested());
        Assertions.assertTrue(stamp - before >= 0, "the pending request carries its own stamp");
        Assertions.assertEquals(0.6f, value, 1e-6f, "the pending request carries its own value");
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
