package com.ivan1pl.lorisui.api.screen;

import java.util.Objects;

/**
 * Defers the release of a host's resources until the host's running operations have finished, and runs releases that
 * consist of several steps without losing a failure.
 * <p>
 * A host (a display that draws LorisUI screens) runs each of its operations that can call application code, such as
 * drawing a frame, switching screens or starting the first screen, through {@link #run}. Application code may ask the
 * host to stop while such an operation is running, for example from a component callback or a screen's lifecycle
 * hook; the host then calls {@link #request()}. When no operation is running, the release runs at once. Otherwise it
 * runs once, when the outermost running operation has finished, so that nothing is released while a frame is still
 * being drawn and no lifecycle hook fires inside another one.
 * <p>
 * A failure of a deferred release propagates from the operation after which it ran. If that operation throws, its
 * exception propagates instead, and the failure of the release is added to it as a suppressed exception.
 * <p>
 * {@link #runAll} runs the steps of a release, and {@link #runAfterFailure} runs a cleanup after a failure, both
 * keeping every failure: the first one propagates, and the others are added to it as suppressed exceptions.
 *
 * <h2>Threading</h2>
 * Not thread-safe. Call its methods on the host's UI thread.
 */
public final class DeferredRelease {
    private final Runnable release;
    private int runningOperations;
    private boolean pending;

    /**
     * Creates a deferred release with no running operation and no pending request.
     *
     * @param release the release of the host's resources, run by {@link #request()} or after the outermost running
     *                operation; must not be null
     * @throws NullPointerException if {@code release} is null
     */
    public DeferredRelease(Runnable release) {
        this.release = Objects.requireNonNull(release, "release");
    }

    /**
     * Runs an operation of the host. If the release is requested while the operation runs, it runs after the
     * operation has finished, unless the operation was itself called from inside another operation run through this
     * method; then it runs after the outermost one.
     *
     * @param operation the operation; must not be null
     * @throws NullPointerException if {@code operation} is null; checked before the operation runs
     * @throws RuntimeException     the exception of the operation, with any failure of the release that ran after it
     *                              added as suppressed; or, if the operation completes normally, the failure of the
     *                              release that ran after it
     * @throws Error                an {@link Error} thrown by the operation, or by the release that ran after it; an
     *                              {@code Error} of the release propagates even if the operation failed, and the
     *                              operation's exception is then not preserved
     */
    public void run(Runnable operation) {
        Objects.requireNonNull(operation, "operation");
        runningOperations++;
        try {
            operation.run();
        } catch (Throwable failure) {
            finishOperation(failure);
            throw failure;
        }
        finishOperation(null);
    }

    /**
     * Requests the release. If no operation is running, the release runs during this call. Otherwise it runs once,
     * after the outermost running operation; requesting it again before then has no further effect.
     *
     * @throws RuntimeException the failure of the release, if it runs during this call
     */
    public void request() {
        if (runningOperations > 0) {
            pending = true;
            return;
        }
        release.run();
    }

    /**
     * Returns whether an operation is running, so that a release requested now would be deferred.
     *
     * @return {@code true} while an operation started with {@link #run} has not finished
     */
    public boolean isRunning() {
        return runningOperations > 0;
    }

    /**
     * Returns whether the release has been requested and waits for the running operations to finish.
     *
     * @return {@code true} from a deferred {@link #request()} until the release starts
     */
    public boolean isPending() {
        return pending;
    }

    /**
     * Runs the steps of a release in order. Every step runs, even when an earlier one throws a
     * {@link RuntimeException}; an {@link Error} propagates at once.
     *
     * @param steps the steps; the array and its elements must not be null
     * @throws NullPointerException if {@code steps} or any of its elements is null; checked before any step runs
     * @throws RuntimeException     the first failure of a step, after all steps have run; failures of later steps are
     *                              added to it as suppressed exceptions, except the same exception again
     */
    public static void runAll(Runnable... steps) {
        Objects.requireNonNull(steps, "steps");
        for (Runnable step : steps) {
            Objects.requireNonNull(step, "step");
        }
        RuntimeException failure = null;
        for (Runnable step : steps) {
            try {
                step.run();
            } catch (RuntimeException stepFailure) {
                if (failure == null) {
                    failure = stepFailure;
                } else if (stepFailure != failure) {
                    failure.addSuppressed(stepFailure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Runs a cleanup after an operation has failed, keeping the operation's failure as the one to propagate: a
     * {@link RuntimeException} thrown by the cleanup is added to {@code failure} as a suppressed exception, unless it
     * is {@code failure} itself, and an {@link Error} propagates. The caller rethrows {@code failure} afterwards.
     *
     * @param failure the failure of the operation; must not be null
     * @param cleanup the cleanup; must not be null
     * @throws NullPointerException if {@code failure} or {@code cleanup} is null; checked before the cleanup runs
     */
    public static void runAfterFailure(Throwable failure, Runnable cleanup) {
        Objects.requireNonNull(failure, "failure");
        Objects.requireNonNull(cleanup, "cleanup");
        try {
            cleanup.run();
        } catch (RuntimeException cleanupFailure) {
            if (cleanupFailure != failure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }

    private void finishOperation(Throwable failure) {
        runningOperations--;
        if (runningOperations > 0 || !pending) {
            return;
        }
        pending = false;
        if (failure == null) {
            release.run();
        } else {
            runAfterFailure(failure, release);
        }
    }
}
