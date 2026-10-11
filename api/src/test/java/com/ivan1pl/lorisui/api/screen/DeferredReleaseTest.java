package com.ivan1pl.lorisui.api.screen;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class DeferredReleaseTest {
    private final List<String> events = new ArrayList<>();
    private final DeferredRelease release = new DeferredRelease(() -> events.add("release"));

    @Test
    void shouldReleaseAtOnceWhenNoOperationIsRunning() {
        release.request();

        Assertions.assertEquals(List.of("release"), events);
        Assertions.assertFalse(release.isPending());
    }

    @Test
    void shouldDeferReleaseRequestedDuringOperationUntilOperationHasFinished() {
        release.run(() -> {
            release.request();
            events.add("operation:" + release.isRunning() + ":" + release.isPending());
        });

        Assertions.assertEquals(List.of("operation:true:true", "release"), events);
        Assertions.assertFalse(release.isRunning());
        Assertions.assertFalse(release.isPending());
    }

    @Test
    void shouldReleaseOnceAfterOutermostOfNestedOperations() {
        release.run(() -> {
            release.run(() -> {
                release.request();
                events.add("inner");
            });
            release.request();
            events.add("outer");
        });

        Assertions.assertEquals(List.of("inner", "outer", "release"), events);
    }

    @Test
    void shouldAddReleaseFailureToFailureOfOperation() {
        IllegalStateException releaseFailure = new IllegalStateException("release");
        IllegalArgumentException operationFailure = new IllegalArgumentException("operation");
        DeferredRelease failingRelease = new DeferredRelease(() -> {
            throw releaseFailure;
        });

        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> failingRelease.run(() -> {
                    failingRelease.request();
                    throw operationFailure;
                }));

        Assertions.assertSame(operationFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[] { releaseFailure }, thrown.getSuppressed());
        Assertions.assertFalse(failingRelease.isPending());
    }

    @Test
    void shouldPropagateReleaseFailureFromOperationThatCompletesNormally() {
        IllegalStateException releaseFailure = new IllegalStateException("release");
        DeferredRelease failingRelease = new DeferredRelease(() -> {
            throw releaseFailure;
        });

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> failingRelease.run(failingRelease::request));

        Assertions.assertSame(releaseFailure, thrown);
        Assertions.assertFalse(failingRelease.isRunning());
    }

    @Test
    void shouldRunAllStepsAndThrowFirstFailureWithLaterFailuresSuppressed() {
        IllegalStateException first = new IllegalStateException("first");
        IllegalArgumentException second = new IllegalArgumentException("second");

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> DeferredRelease.runAll(
                        () -> events.add("a"),
                        () -> {
                            events.add("b");
                            throw first;
                        },
                        () -> {
                            events.add("c");
                            throw second;
                        },
                        () -> events.add("d")));

        Assertions.assertSame(first, thrown);
        Assertions.assertArrayEquals(new Throwable[] { second }, thrown.getSuppressed());
        Assertions.assertEquals(List.of("a", "b", "c", "d"), events);
    }

    @Test
    void shouldNotAddFailureToItselfAsSuppressed() {
        IllegalStateException failure = new IllegalStateException("shared");
        Runnable failingStep = () -> {
            throw failure;
        };

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> DeferredRelease.runAll(failingStep, failingStep, () -> events.add("last")));
        DeferredRelease.runAfterFailure(failure, failingStep);

        Assertions.assertSame(failure, thrown);
        Assertions.assertEquals(0, failure.getSuppressed().length);
        Assertions.assertEquals(List.of("last"), events);
    }

    @Test
    void shouldRejectNullStepBeforeRunningAnyStep() {
        Assertions.assertThrows(NullPointerException.class,
                () -> DeferredRelease.runAll(() -> events.add("a"), null));
        Assertions.assertThrows(NullPointerException.class, () -> new DeferredRelease(null));
        Assertions.assertThrows(NullPointerException.class, () -> release.run(null));

        Assertions.assertEquals(List.of(), events);
        Assertions.assertFalse(release.isRunning());
    }
}
