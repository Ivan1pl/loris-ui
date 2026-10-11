package com.ivan1pl.lorisui.api.screen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Keeps a set of named {@link LorisScreen}s, tracks the current one and fires its lifecycle hooks.
 * <p>
 * A host (a display that draws LorisUI screens) owns one manager. The application registers screens with
 * {@link #addScreen} and selects one with {@link #setCurrentScreen}. The host calls {@link #activate()} when it is
 * ready to display the UI and {@link #deactivate()} when it stops displaying it, and draws
 * {@link #getCurrentScreen()} every frame in between.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *     <li>{@link #activate()} fires {@link LorisScreen#onStartScreen()} on the current screen.</li>
 *     <li>While the manager is active, {@link #setCurrentScreen} switches immediately: it fires
 *     {@link LorisScreen#onEndScreen()} on the old screen, then {@link LorisScreen#onStartScreen()} on the new one.
 *     While the manager is inactive, it only records the choice.</li>
 *     <li>{@link #deactivate()} fires {@link LorisScreen#onEndScreen()} on the started screen.</li>
 *     <li>A host that must stop displaying the UI while hooks may not fire, for example during a frame or a lifecycle
 *     hook, calls {@link #beginDeactivation()} at once and {@link #deactivate()} later. In between, the manager is
 *     inactive, so {@link #setCurrentScreen} only records the choice, while the started screen receives its
 *     {@link LorisScreen#onEndScreen()} from {@link #deactivate()}.</li>
 *     <li>The manager can be activated again after {@link #deactivate()}, for example when the host is attached
 *     again; the current screen's {@link LorisScreen#onStartScreen()} then fires again.</li>
 * </ul>
 * A screen receives {@link LorisScreen#onEndScreen()} only if its {@link LorisScreen#onStartScreen()} returned
 * normally, and at most once per such start. A screen whose {@link LorisScreen#onStartScreen()} throws is treated as
 * not started, so it receives no {@link LorisScreen#onEndScreen()} for that start, not even on
 * {@link #deactivate()}. An exception thrown by a hook propagates out of the method that fired it.
 * <p>
 * A switch always completes, even when a hook throws: the new screen becomes current before the old screen's
 * {@link LorisScreen#onEndScreen()} fires, and its {@link LorisScreen#onStartScreen()} fires even if the old screen's
 * hook throws. If both hooks throw different exceptions, the exception of {@link LorisScreen#onStartScreen()} is
 * added as suppressed to the exception of {@link LorisScreen#onEndScreen()}.
 * <p>
 * The lifecycle hooks must not change the lifecycle: {@link #setCurrentScreen}, {@link #activate()} and
 * {@link #deactivate()} throw an {@link IllegalStateException} when called from inside an
 * {@link LorisScreen#onStartScreen()} or {@link LorisScreen#onEndScreen()} fired by this manager, before changing
 * any state. {@link #addScreen}, {@link #beginDeactivation()} and {@link #getCurrentScreen()} may be called from
 * inside a hook. To switch screens in response to a hook, do it later, for example from a component callback.
 *
 * <h2>Threading</h2>
 * The manager is not thread-safe. Call its methods on the host's UI thread; the lifecycle hooks run on that thread.
 */
public final class ScreenManager {
    private final Map<String, LorisScreen> screens = new HashMap<>();
    private String currentScreen;
    private LorisScreen startedScreen;
    private boolean active;
    private boolean inHook;

    /**
     * Creates an inactive manager with no screens and no current screen.
     */
    public ScreenManager() {}

    /**
     * Registers a screen under a name, so that it can be selected with {@link #setCurrentScreen}. Fires no lifecycle
     * hooks, and may be called from inside a hook.
     *
     * @param name   the name of the screen, unique within this manager; must not be null
     * @param screen the screen; must not be null
     * @throws NullPointerException  if {@code name} or {@code screen} is null
     * @throws IllegalStateException if a screen is already registered under {@code name}
     */
    public void addScreen(String name, LorisScreen screen) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(screen, "screen");
        if (screens.containsKey(name)) {
            throw new IllegalStateException("Screen already registered under name: " + name);
        }
        screens.put(name, screen);
    }

    /**
     * Selects the current screen.
     * <p>
     * Does nothing if {@code name} is already the current screen. While the manager is inactive, it only records the
     * choice, and the screen's {@link LorisScreen#onStartScreen()} fires on {@link #activate()}. While the manager is
     * active, the switch is immediate: the new screen becomes current, so {@link #getCurrentScreen()} already returns
     * it while the old screen's {@link LorisScreen#onEndScreen()} runs; then {@link LorisScreen#onEndScreen()} fires
     * on the old screen, if it was started, and {@link LorisScreen#onStartScreen()} on the new one.
     * <p>
     * The switch always completes. If the old screen's {@link LorisScreen#onEndScreen()} throws, the new screen's
     * {@link LorisScreen#onStartScreen()} still fires. If the new screen's {@link LorisScreen#onStartScreen()} throws,
     * the new screen stays current but is treated as not started, so it receives no {@link LorisScreen#onEndScreen()}
     * for this start. A hook's exception propagates to the caller; if both hooks throw different exceptions, the
     * exception of {@link LorisScreen#onStartScreen()} is added as suppressed to the exception of
     * {@link LorisScreen#onEndScreen()}.
     *
     * @param name the name of a screen registered with {@link #addScreen}; must not be null
     * @throws IllegalStateException if called from inside a lifecycle hook fired by this manager, or if no screen is
     *                               registered under {@code name}; checked before any state change or hook
     * @throws NullPointerException  if {@code name} is null; checked before any state change or hook
     */
    public void setCurrentScreen(String name) {
        checkNotInHook("setCurrentScreen");
        Objects.requireNonNull(name, "name");
        if (!screens.containsKey(name)) {
            throw new IllegalStateException("No screen registered under name: " + name);
        }
        if (name.equals(currentScreen)) {
            return;
        }
        currentScreen = name;
        if (!active) {
            return;
        }
        try {
            endStartedScreen();
        } catch (Throwable endFailure) {
            try {
                startCurrentScreen();
            } catch (Throwable startFailure) {
                if (startFailure != endFailure) {
                    endFailure.addSuppressed(startFailure);
                }
            }
            throw endFailure;
        }
        startCurrentScreen();
    }

    /**
     * Activates the manager and fires {@link LorisScreen#onStartScreen()} on the current screen. A host calls this
     * when it is ready to display the UI.
     * <p>
     * If the screen's {@link LorisScreen#onStartScreen()} throws, the exception propagates, the manager stays active,
     * and the screen is treated as not started: it receives no {@link LorisScreen#onEndScreen()} for this start.
     *
     * @throws IllegalStateException if called from inside a lifecycle hook fired by this manager, if the manager is
     *                               already active, if a deactivation started with {@link #beginDeactivation()} has
     *                               not been completed with {@link #deactivate()}, or if no current screen is set;
     *                               checked before any state change or hook
     */
    public void activate() {
        checkNotInHook("activate");
        if (active) {
            throw new IllegalStateException("Screen manager is already active");
        }
        if (startedScreen != null) {
            throw new IllegalStateException("Screen manager is still being deactivated; call deactivate first");
        }
        if (currentScreen == null) {
            throw new IllegalStateException("No current screen set; call setCurrentScreen before activating");
        }
        active = true;
        startCurrentScreen();
    }

    /**
     * Deactivates the manager without firing a hook, so that {@link #setCurrentScreen} only records the choice from
     * now on. The started screen stays started until {@link #deactivate()} fires its
     * {@link LorisScreen#onEndScreen()}. A host calls this when it stops displaying the UI at a moment when hooks must
     * not fire, such as during a frame or a lifecycle hook, and calls {@link #deactivate()} once that has finished.
     * <p>
     * Does nothing if the manager is inactive. May be called from inside a hook. A screen switch or an
     * {@link #activate()} that is in progress when this method is called still completes, including the new screen's
     * {@link LorisScreen#onStartScreen()}.
     */
    public void beginDeactivation() {
        active = false;
    }

    /**
     * Deactivates the manager and fires {@link LorisScreen#onEndScreen()} on the started screen, if any: the screen
     * whose {@link LorisScreen#onStartScreen()} last returned normally. A host calls this when it stops displaying the
     * UI, or to complete a deactivation started with {@link #beginDeactivation()}; the started screen is then the one
     * that was current at that time, even if another screen has been selected since.
     * <p>
     * Does nothing if the manager is inactive and no screen is started. If {@link LorisScreen#onEndScreen()} throws,
     * the exception propagates, and the manager is inactive all the same. A later {@link #activate()} starts the
     * current screen again.
     *
     * @throws IllegalStateException if called from inside a lifecycle hook fired by this manager; checked before any
     *                               state change or hook, so the manager stays active
     */
    public void deactivate() {
        checkNotInHook("deactivate");
        active = false;
        endStartedScreen();
    }

    /**
     * Returns the current screen.
     *
     * @return the current screen, or {@code null} if none is selected
     */
    public LorisScreen getCurrentScreen() {
        return currentScreen == null ? null : screens.get(currentScreen);
    }

    private void checkNotInHook(String method) {
        if (inHook) {
            throw new IllegalStateException(method + " cannot be called from inside onStartScreen or onEndScreen; "
                    + "call it after the hook returns");
        }
    }

    private void startCurrentScreen() {
        LorisScreen screen = screens.get(currentScreen);
        fireHook(screen::onStartScreen);
        startedScreen = screen;
    }

    // Forgets the started screen before its hook runs, so a throwing onEndScreen() is never repeated
    private void endStartedScreen() {
        LorisScreen screen = startedScreen;
        startedScreen = null;
        if (screen != null) {
            fireHook(screen::onEndScreen);
        }
    }

    private void fireHook(Runnable hook) {
        inHook = true;
        try {
            hook.run();
        } finally {
            inHook = false;
        }
    }
}
