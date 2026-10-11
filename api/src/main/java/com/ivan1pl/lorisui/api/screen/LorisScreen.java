package com.ivan1pl.lorisui.api.screen;

import com.ivan1pl.lorisui.api.backend.Backend;
import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for application screens: builds a component tree with {@link #layout()} and draws it each time the
 * host renders through {@link #render}.
 * <p>
 * Threading: {@link #render} must be called on the host's UI thread, the thread the host renders on.
 * {@link #layout()} runs asynchronously, off the host's UI thread, so implementations must not touch state confined
 * to the UI thread. Its result is safely handed over and drawn by subsequent {@link #render} calls once it is
 * complete; until the first layout completes, the screen draws no components. {@link #setLayoutNeedsUpdate} may be
 * called from any thread.
 * <p>
 * Lifecycle: hosts call {@link #onStartScreen()} when the screen becomes the displayed screen and
 * {@link #onEndScreen()} when it stops being displayed, both on the host's UI thread. Each {@link #onEndScreen()}
 * follows a matching {@link #onStartScreen()}.
 * <p>
 * Failures: if {@link #layout()} throws or returns {@code null}, the next call to {@link #render} throws an
 * {@link IllegalStateException} caused by the original failure. The failure is rethrown exactly once; subsequent
 * {@link #render} calls draw the last successful layout. Hosts should treat the exception as fatal.
 */
public abstract class LorisScreen {
    private static final Object LAYOUT_UPDATER_LOCK = new Object();

    private static ExecutorService layoutUpdater;

    private final AtomicBoolean layoutNeedsUpdate = new AtomicBoolean(true);
    private final AtomicReference<Throwable> layoutFailure = new AtomicReference<>();
    private volatile List<Component> content = Collections.emptyList();

    /**
     * Creates a screen that requests its first layout on the first {@link #render} call.
     */
    protected LorisScreen() {}

    /**
     * Sets whether the layout must be rebuilt. When set to {@code true}, the next {@link #render} call starts a new
     * {@link #layout()}, whose result is drawn by subsequent {@link #render} calls once it completes. May be called
     * from any thread.
     *
     * @param layoutNeedsUpdate {@code true} to request a new layout, {@code false} to cancel a request that has not
     *                          been started yet
     */
    protected void setLayoutNeedsUpdate(boolean layoutNeedsUpdate) {
        this.layoutNeedsUpdate.set(layoutNeedsUpdate);
    }

    /**
     * Draws the most recent layout through the backend and starts a new layout if one was requested. Must be called
     * on the host's UI thread, each time the host renders.
     *
     * @param backend           backend that draws the components; must not be null
     * @param w                 screen width in logical window coordinates
     * @param h                 screen height in logical window coordinates
     * @param bundleKeyResolver resolver for localized text; must not be null
     * @throws IllegalStateException if the last requested {@link #layout()} failed or returned {@code null}; the
     *                               cause is the original failure. Each failure is thrown once and nothing is
     *                               drawn in that call.
     */
    public void render(Backend backend, int w, int h, BundleKeyResolver bundleKeyResolver) {
        Throwable failure = layoutFailure.getAndSet(null);
        if (failure != null) {
            throw new IllegalStateException("Layout of " + getClass().getName() + " failed", failure);
        }
        if (layoutNeedsUpdate.getAndSet(false)) {
            synchronized (LAYOUT_UPDATER_LOCK) {
                if (layoutUpdater == null) {
                    layoutUpdater = Executors.newSingleThreadExecutor(
                            Thread.ofPlatform().name("lorisui-layout").daemon(true).factory());
                }
                layoutUpdater.execute(this::updateLayout);
            }
        }
        backend.render(w, h, content, bundleKeyResolver);
    }

    /**
     * Stops the background work that runs {@link #layout()} for all screens. Layouts that were already requested
     * still complete and are drawn by later {@link #render} calls. The work resumes automatically when a screen next
     * needs a layout. Hosts call this when they are disposed. Does nothing if no layout work is active, and may be
     * called from any thread.
     */
    public static void shutdownLayoutUpdates() {
        synchronized (LAYOUT_UPDATER_LOCK) {
            if (layoutUpdater != null) {
                layoutUpdater.shutdown();
                layoutUpdater = null;
            }
        }
    }

    private void updateLayout() {
        try {
            List<Component> newLayout = layout();
            if (newLayout == null) {
                layoutFailure.set(new NullPointerException("layout() returned null"));
                return;
            }
            content = newLayout;
        } catch (Throwable t) {
            layoutFailure.set(t);
        }
    }

    /**
     * Called by the host when this screen becomes the displayed screen, on the host's UI thread.
     */
    public abstract void onStartScreen();

    /**
     * Called by the host when this screen stops being the displayed screen, on the host's UI thread. Each call
     * follows a matching {@link #onStartScreen()}.
     */
    public abstract void onEndScreen();

    /**
     * Builds the component tree of this screen. Runs asynchronously, off the host's UI thread, so implementations
     * must not touch state confined to the UI thread.
     *
     * @return the components to draw; must not be null (an empty list draws nothing). A {@code null} result or
     *         a thrown exception is reported by the next {@link #render} call as an {@link IllegalStateException}.
     */
    protected abstract List<Component> layout();
}
