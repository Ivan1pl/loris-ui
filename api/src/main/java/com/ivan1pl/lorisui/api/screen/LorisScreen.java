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
 * A screen whose component tree is built by {@link #layout()} and drawn every frame by {@link #render}.
 * <p>
 * Threading: {@link #layout()} runs off the render thread, on a single daemon thread named {@code lorisui-layout}
 * that is shared by all screens. Its result is handed over to the render thread safely and is drawn from the next
 * frame on; until the first layout completes, the screen renders no components. {@link #render} must be called on
 * the render thread only, while {@link #setLayoutNeedsUpdate} may be called from any thread.
 * <p>
 * Failures: if {@link #layout()} throws or returns {@code null}, the next call to {@link #render} throws an
 * {@link IllegalStateException} caused by the original failure. The failure is rethrown exactly once; later frames
 * render the last successful layout. Hosts should treat the exception as fatal.
 */
public abstract class LorisScreen {
    private static final ExecutorService LAYOUT_UPDATER = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("lorisui-layout").daemon(true).factory());

    private final AtomicBoolean layoutNeedsUpdate = new AtomicBoolean(true);
    private final AtomicReference<Throwable> layoutFailure = new AtomicReference<>();
    private volatile List<Component> content = Collections.emptyList();

    protected LorisScreen() {}

    /**
     * Marks whether the layout must be rebuilt. When set to {@code true}, the next {@link #render} call schedules
     * {@link #layout()} on the layout thread. May be called from any thread.
     *
     * @param layoutNeedsUpdate {@code true} to request a new layout, {@code false} to cancel a request that has not
     *                          been scheduled yet
     */
    protected void setLayoutNeedsUpdate(boolean layoutNeedsUpdate) {
        this.layoutNeedsUpdate.set(layoutNeedsUpdate);
    }

    /**
     * Renders the current content through the backend and schedules a new layout if one was requested. Must be
     * called on the render thread.
     *
     * @param backend           backend that draws the components; must not be null
     * @param w                 available width in pixels
     * @param h                 available height in pixels
     * @param bundleKeyResolver resolver for localised text; must not be null
     * @throws IllegalStateException if the last scheduled {@link #layout()} failed or returned {@code null}; the
     *                               cause is the original failure. Each failure is thrown once and nothing is
     *                               rendered in that call.
     */
    public void render(Backend backend, int w, int h, BundleKeyResolver bundleKeyResolver) {
        Throwable failure = layoutFailure.getAndSet(null);
        if (failure != null) {
            throw new IllegalStateException("Layout of " + getClass().getName() + " failed", failure);
        }
        if (layoutNeedsUpdate.getAndSet(false)) {
            LAYOUT_UPDATER.execute(this::updateLayout);
        }
        backend.render(w, h, content, bundleKeyResolver);
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

    public abstract void onStartScreen();
    public abstract void onEndScreen();

    /**
     * Builds the component tree of this screen. Called on the shared {@code lorisui-layout} daemon thread, never on
     * the render thread, so implementations must not touch render-thread-only state.
     *
     * @return the components to render; must not be null (an empty list renders nothing). A {@code null} result or
     *         a thrown exception is reported by the next {@link #render} call as an {@link IllegalStateException}.
     */
    protected abstract List<Component> layout();
}
