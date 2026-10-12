package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.ImGuiGlfwGl3Renderer;
import com.ivan1pl.lorisui.imgui.ImGuiRenderer;
import com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.system.Callback;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Displays LorisUI screens in an application's own GLFW window, using the Dear ImGui backend with OpenGL 3.
 * <p>
 * The display is embedded in an application that owns the GLFW window, its OpenGL context and the main loop. The
 * display holds a set of named {@link LorisScreen}s and draws the current one when the application asks it to. It
 * resolves the screens' localization keys from the resource bundles registered with {@link #setResourceBundle}, and
 * receives the window's input through GLFW callbacks that it chains in front of the application's own.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *     <li>Create the display for the window; register fonts, custom renderers and style appliers, and resource
 *     bundles; add screens and set the current screen. Register only fonts from trusted sources, as described for
 *     {@link ImGuiRenderer#registerFontFromFile(String, int)}. Set the application's own GLFW callbacks for the
 *     window.</li>
 *     <li>Call {@link #initialize()} with the window's OpenGL context current. The display sets up Dear ImGui with the
 *     registered fonts, chains its input callbacks in front of the application's, and the current screen's
 *     {@link LorisScreen#onStartScreen()} fires. If the setup fails, the display releases what it had set up and
 *     stays uninitialized, so {@code initialize()} can be called again; if a registered font caused the failure,
 *     create a new display instead.</li>
 *     <li>Each frame, process the window's events with {@code glfwPollEvents} or {@code glfwWaitEvents}, then call
 *     {@link #render(int, int)} once, after clearing and before swapping the window.</li>
 *     <li>While the display is initialized, switching screens with {@link #setCurrentScreen} fires
 *     {@link LorisScreen#onEndScreen()} on the old screen and {@link LorisScreen#onStartScreen()} on the new one.
 *     Before {@link #initialize()}, and from the moment the display is closed, even while its release is still
 *     pending, {@link #setCurrentScreen} only records the choice.</li>
 *     <li>Call {@link #close()} before the OpenGL context or the window is destroyed. The current screen's
 *     {@link LorisScreen#onEndScreen()} fires, the display gives the window's input callbacks back to the
 *     application and releases its Dear ImGui and OpenGL resources, including every texture it loaded, and the
 *     background work that computes screen layouts stops until a screen is drawn again. Closing is final: to show UI
 *     again, create a new display. Fonts must then be registered with the new display.</li>
 * </ul>
 * Closing the display from a component callback, such as a button action, or from a screen's
 * {@link LorisScreen#onStartScreen()} or {@link LorisScreen#onEndScreen()} run by {@link #render(int, int)},
 * {@link #setCurrentScreen} or {@link #initialize()} is allowed. The display is closed at once: the methods that
 * require an open display throw, and the window's input goes only to the application's callbacks. The release
 * itself completes when the current frame or screen switch has finished, so the application must let that call
 * return before it destroys the OpenGL context or the window.
 * <p>
 * A screen receives {@link LorisScreen#onEndScreen()} only if its {@link LorisScreen#onStartScreen()} returned
 * normally, and at most once per such start. A screen whose {@link LorisScreen#onStartScreen()} throws is treated as
 * not started, so it receives no {@link LorisScreen#onEndScreen()} for that start, not even when the display is
 * closed. An exception thrown by a hook propagates out of the method that fired it. The hooks must not call
 * {@link #setCurrentScreen}; switch screens later, for example from a component callback.
 * <p>
 * Only one display may be initialized at a time. While initialized, the display leaves its Dear ImGui context current;
 * an application that uses a Dear ImGui context of its own must make that context current again after calling the
 * display.
 *
 * <h2>Input</h2>
 * The display chains seven callbacks of the window: window focus, cursor enter, cursor position, mouse button,
 * scroll, key and character. Each event goes to the UI first, and then to the application's callback that was set
 * when {@link #initialize()} was called, unless the UI consumes the event:
 * <ul>
 *     <li>cursor position and mouse button events are consumed while the UI wants mouse input, that is while the
 *     pointer is over LorisUI content or a mouse drag started on it;</li>
 *     <li>scroll events are consumed while the UI wants mouse input and the pointer is over a scrollable panel;</li>
 *     <li>key events, including repeats, and character events are consumed while a LorisUI text or number input has
 *     keyboard focus;</li>
 *     <li>window focus and cursor enter events are never consumed.</li>
 * </ul>
 * Whether an event is consumed depends on the most recently rendered frame. Before {@link #initialize()} and after
 * {@link #close()}, every event goes to the application's callbacks. GLFW delivers characters whenever the window has
 * keyboard focus, so the display does not start or stop text input. The other callbacks of the window, such as the
 * window size, framebuffer size, close and refresh callbacks, are left to the application.
 * <p>
 * The application must follow these rules:
 * <ul>
 *     <li>Set the application's callbacks for the seven events before {@link #initialize()}. A callback set
 *     afterwards replaces the display's, so the UI stops receiving that input; {@link #close()} leaves such a newer
 *     callback in place. Never call or free the callback object that the {@code glfwSet*Callback} function returns
 *     then: it belongs to the display, which frees it when it is closed.</li>
 *     <li>Never call {@link #close()} from any GLFW callback, including the callbacks the display leaves to the
 *     application, such as those set with {@code glfwSetWindowCloseCallback}, {@code glfwSetWindowSizeCallback} or
 *     {@code glfwSetWindowRefreshCallback}. Called from one of the display's seven chained callbacks, or from code
 *     they run, it throws {@link IllegalStateException}; a call from any other GLFW callback is not detected. Request
 *     the close in the callback, for example with a flag, and call {@link #close()} after {@code glfwPollEvents} or
 *     {@code glfwWaitEvents} returns.</li>
 *     <li>Never call {@link #render(int, int)} from a GLFW callback, such as a window refresh callback: a close
 *     requested by the UI during that frame would release the display inside the callback.</li>
 *     <li>Expect failures thrown during the display's callbacks, by LorisUI or by the application's chained callback,
 *     from the next {@link #render(int, int)} or {@link #close()} call, because exceptions cannot propagate through
 *     GLFW. Until then, the window's events reach neither the UI nor the application.</li>
 *     <li>Call {@link #close()} before the window is destroyed.</li>
 *     <li>Free the application's own callbacks, for example with {@code Callbacks.glfwFreeCallbacks}, only after
 *     {@link #close()}.</li>
 * </ul>
 * <p>
 * Dear ImGui's GLFW backend replaces GLFW's global monitor callback, set with {@code glfwSetMonitorCallback}, while
 * the display initializes; the display then sets the application's monitor callback again, as described for
 * {@link ImGuiGlfwGl3Renderer}.
 *
 * <h2>Threading</h2>
 * The display is not thread-safe. Create it and call all of its methods on the thread that owns the window and its
 * OpenGL context, with that context current. For GLFW, that is the process's main thread, so on macOS the JVM may need
 * {@code -XstartOnFirstThread}. The screen lifecycle hooks, component callbacks and the chained application callbacks
 * run on that thread.
 */
public final class ImGuiLwjglGlfwGl3Display extends ImGuiLwjglDisplayBase<ImGuiGlfwGl3Renderer>
        implements AutoCloseable {
    private static final String CLOSE_IN_CALLBACK_MESSAGE = "Cannot close the display from inside a GLFW callback; "
            + "request the close there and call close() after glfwPollEvents returns";

    private final long glfwWindow;
    private final ChainedCallback<GLFWWindowFocusCallback> windowFocusCallback =
            new ChainedCallback<>(GLFW::glfwSetWindowFocusCallback);
    private final ChainedCallback<GLFWCursorEnterCallback> cursorEnterCallback =
            new ChainedCallback<>(GLFW::glfwSetCursorEnterCallback);
    private final ChainedCallback<GLFWCursorPosCallback> cursorPosCallback =
            new ChainedCallback<>(GLFW::glfwSetCursorPosCallback);
    private final ChainedCallback<GLFWMouseButtonCallback> mouseButtonCallback =
            new ChainedCallback<>(GLFW::glfwSetMouseButtonCallback);
    private final ChainedCallback<GLFWScrollCallback> scrollCallback =
            new ChainedCallback<>(GLFW::glfwSetScrollCallback);
    private final ChainedCallback<GLFWKeyCallback> keyCallback = new ChainedCallback<>(GLFW::glfwSetKeyCallback);
    private final ChainedCallback<GLFWCharCallback> charCallback = new ChainedCallback<>(GLFW::glfwSetCharCallback);
    // Counts the callback dispatches in progress; the release must never run inside one
    private int dispatchDepth = 0;
    // The first failure of a dispatch, kept until the next render or close rethrows it
    private Throwable pendingFailure = null;

    /**
     * Creates a display for a GLFW window. Call it on the thread that owns the window. The display shows nothing until
     * it is {@linkplain #initialize() initialized}, and the constructor does not touch Dear ImGui, OpenGL or GLFW.
     *
     * @param glfwWindow the native handle of the application's GLFW window ({@code GLFWwindow*}), which must have an
     *                   OpenGL context; must not be {@code 0}
     * @throws IllegalArgumentException if {@code glfwWindow} is {@code 0}
     */
    public ImGuiLwjglGlfwGl3Display(long glfwWindow) {
        ImGuiOpenGlBackend backend = new ImGuiOpenGlBackend();
        super(backend, new ImGuiGlfwGl3Renderer(backend, glfwWindow));
        this.glfwWindow = glfwWindow;
    }

    /**
     * Initializes the display and fires {@link LorisScreen#onStartScreen()} on the current screen. Call it with the
     * window's OpenGL context current, before the first {@link #render(int, int)} call, and after the application has
     * set its own GLFW callbacks for the window. From then on, fonts, renderers and style appliers can no longer be
     * registered.
     * <p>
     * The display chains its input callbacks in front of the application's callbacks that are set when this method is
     * called, as described in the class documentation. GLFW's global monitor callback is the same after the call as
     * before it.
     * <p>
     * If setting up Dear ImGui fails, the exception propagates, no screen hook fires, and the display releases what it
     * had set up, gives the window's callbacks back to the application and stays uninitialized, so the call can be
     * retried, for example after a transient OpenGL failure. A failure caused by a registered font recurs on every
     * retry, because fonts cannot be unregistered; create a new display with other fonts instead.
     * <p>
     * If the screen's {@link LorisScreen#onStartScreen()} throws, the exception propagates, the display stays
     * initialized, and the screen is treated as not started: it receives no {@link LorisScreen#onEndScreen()} for
     * this start. If the hook closes the display, the release completes when the hook has returned, and a failure of
     * the release then propagates from this method.
     *
     * @throws IllegalStateException if the display is closed or already initialized, or if no current screen is set
     *                               (call {@link #setCurrentScreen} first), all checked before any state change; if
     *                               Dear ImGui's GLFW platform or OpenGL renderer backend fails to initialize; or if a
     *                               registered font cannot be loaded, for example because its data is not a valid
     *                               TrueType font
     */
    public void initialize() {
        initializeDisplay();
    }

    /**
     * Draws the current screen. Call it once per frame, after the window's events have been processed with
     * {@code glfwPollEvents} or {@code glfwWaitEvents}, with the window's OpenGL context current. The display does not
     * clear the window or swap its buffers: the application clears and sets its viewport for its own drawing before
     * this call, and swaps the window after it. Never call it from a GLFW callback, such as a window refresh callback:
     * a close requested by the UI during that frame would release the display inside the callback.
     * <p>
     * {@code width} and {@code height} are the size of the window in screen coordinates, as reported by
     * {@code glfwGetWindowSize}, never the framebuffer size reported by {@code glfwGetFramebufferSize}; the
     * framebuffer scale of HiDPI displays is applied automatically. On Windows and X11, GLFW measures windows in
     * pixels, so the window size equals the framebuffer size and the UI is not scaled up on HiDPI monitors.
     * <p>
     * If one of the display's GLFW callbacks failed since the previous {@code render} or {@link #close()} call, this
     * method throws that failure instead of drawing; the display stays initialized, and the next call draws normally.
     * <p>
     * If the display is closed during the frame, for example from a component callback, the release completes when
     * the frame has been drawn; a failure of the release then propagates from this method.
     *
     * @param width  the window width in screen coordinates
     * @param height the window height in screen coordinates
     * @throws RuntimeException      the failure of one of the display's GLFW callbacks, by LorisUI or by the
     *                               application's chained callback, since the previous {@code render} or
     *                               {@link #close()} call; a checked exception is wrapped in a
     *                               {@code RuntimeException}, and an {@link Error} is thrown as it is
     * @throws IllegalStateException if the display is not initialized or is closed, if called from a component
     *                               callback during a frame, if the screen's most recent layout failed (see
     *                               {@link LorisScreen#render}), or if a component or style of the screen has no
     *                               registered renderer or style applier
     */
    public void render(int width, int height) {
        rethrowPendingFailure(null);
        renderFrame(() -> getRenderer().render(getScreenManager().getCurrentScreen(), width, height, getResolver()));
    }

    /**
     * Closes the display. The display is closed at once: from then on, the window's input goes only to the
     * application's callbacks, and registering fonts, renderers or style appliers, {@link #initialize()} and
     * {@link #render(int, int)} throw {@link IllegalStateException}. Closing is final; to show UI again, create a new
     * display.
     * <p>
     * Never call it from any GLFW callback, including the callbacks the display leaves to the application, such as
     * those set with {@code glfwSetWindowCloseCallback}, {@code glfwSetWindowSizeCallback} or
     * {@code glfwSetWindowRefreshCallback}. Called while one of the display's seven chained GLFW callbacks runs,
     * including from the application's chained callback or from a screen hook or component callback run by it, it
     * throws {@link IllegalStateException} before any state change, and the display stays open; a call from any other
     * GLFW callback is not detected. Request the close in the callback instead, and call this method after
     * {@code glfwPollEvents} or {@code glfwWaitEvents} returns.
     * <p>
     * If the display is initialized, it then fires {@link LorisScreen#onEndScreen()} on the current screen, if that
     * screen's {@link LorisScreen#onStartScreen()} returned normally, gives the seven input callbacks back to the
     * application, releases its Dear ImGui and OpenGL resources, including every texture it loaded, and stops the
     * background work that computes screen layouts until a screen is drawn again. A callback that the application set
     * after {@link #initialize()} stays in place, and the display never frees the application's callbacks. Call it
     * with the window's OpenGL context current, before the context or the window is destroyed, and before the
     * application frees its own callbacks.
     * <p>
     * When called during a frame, for example from a component callback, or during a screen switch or a screen's
     * lifecycle hook, the release, including {@link LorisScreen#onEndScreen()}, runs when that frame, switch or hook
     * has finished, and its failure propagates from the method that was running. The end hook then fires on the
     * screen that was started when the display was closed; a screen selected with {@link #setCurrentScreen} after
     * that is only recorded.
     * <p>
     * Does nothing if the display is already closed, apart from throwing a pending callback failure. Every release
     * step runs even when an earlier one fails. If {@link LorisScreen#onEndScreen()} or a release step throws, the
     * first exception propagates after all steps have run, and the display is closed all the same.
     * <p>
     * If one of the display's GLFW callbacks failed since the previous {@link #render(int, int)} or {@code close}
     * call, this method closes the display and then throws that failure; a failure of the release is added to it as a
     * suppressed exception.
     *
     * @throws IllegalStateException if called while one of the display's seven chained GLFW callbacks runs, or code
     *                               they run, checked before any state change; a call from any other GLFW callback
     *                               is not detected
     * @throws RuntimeException      the failure of one of the display's GLFW callbacks, by LorisUI or by the
     *                               application's chained callback, since the previous {@link #render(int, int)} or
     *                               {@code close} call, with a checked exception wrapped in a
     *                               {@code RuntimeException} and an {@link Error} thrown as it is; otherwise the
     *                               first failure of {@link LorisScreen#onEndScreen()} or of a release step, with the
     *                               failures of later steps added to it as suppressed exceptions
     */
    @Override
    public void close() {
        if (dispatchDepth > 0) {
            throw new IllegalStateException(CLOSE_IN_CALLBACK_MESSAGE);
        }
        try {
            closeDisplay();
        } catch (Throwable releaseFailure) {
            rethrowPendingFailure(releaseFailure);
            throw releaseFailure;
        }
        rethrowPendingFailure(null);
    }

    @Override
    void setUp() {
        try {
            initializeRenderer();
            installCallbacks();
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, this::releaseInput);
            DeferredRelease.runAfterFailure(failure, getRenderer()::dispose);
            throw failure;
        }
    }

    @Override
    void releaseInput() {
        DeferredRelease.runAll(
                () -> windowFocusCallback.uninstall(glfwWindow),
                () -> cursorEnterCallback.uninstall(glfwWindow),
                () -> cursorPosCallback.uninstall(glfwWindow),
                () -> mouseButtonCallback.uninstall(glfwWindow),
                () -> scrollCallback.uninstall(glfwWindow),
                () -> keyCallback.uninstall(glfwWindow),
                () -> charCallback.uninstall(glfwWindow));
    }

    // Runs one GLFW event. Exceptions cannot propagate through GLFW, so the first failure is kept for the next render
    // or close, and later events are dropped until then
    void dispatch(Runnable event) {
        if (pendingFailure != null) {
            return;
        }
        dispatchDepth++;
        try {
            runDeferred(event);
        } catch (Throwable failure) {
            pendingFailure = failure;
        } finally {
            dispatchDepth--;
        }
    }

    // Dear ImGui's GLFW backend replaces the global monitor callback with a stub of its own, which the display frees
    // after giving the application's callback back. The application's callback itself is never freed
    private void initializeRenderer() {
        GLFWMonitorCallback appMonitor = GLFW.glfwSetMonitorCallback(null);
        try {
            getRenderer().initialize();
        } finally {
            GLFWMonitorCallback imguiMonitor = GLFW.glfwSetMonitorCallback(appMonitor);
            if (imguiMonitor != null) {
                imguiMonitor.free();
            }
        }
    }

    private void installCallbacks() {
        windowFocusCallback.install(glfwWindow, GLFWWindowFocusCallback.create(this::onWindowFocus));
        cursorEnterCallback.install(glfwWindow, GLFWCursorEnterCallback.create(this::onCursorEnter));
        cursorPosCallback.install(glfwWindow, GLFWCursorPosCallback.create(this::onCursorPos));
        mouseButtonCallback.install(glfwWindow, GLFWMouseButtonCallback.create(this::onMouseButton));
        scrollCallback.install(glfwWindow, GLFWScrollCallback.create(this::onScroll));
        keyCallback.install(glfwWindow, GLFWKeyCallback.create(this::onKey));
        charCallback.install(glfwWindow, GLFWCharCallback.create(this::onChar));
    }

    private void onWindowFocus(long window, boolean focused) {
        forward(windowFocusCallback, renderer -> {
            renderer.processGlfwWindowFocus(focused);
            return false;
        }, app -> app.invoke(window, focused));
    }

    private void onCursorEnter(long window, boolean entered) {
        forward(cursorEnterCallback, renderer -> {
            renderer.processGlfwCursorEnter(entered);
            return false;
        }, app -> app.invoke(window, entered));
    }

    private void onCursorPos(long window, double x, double y) {
        forward(cursorPosCallback, renderer -> {
            renderer.processGlfwCursorPos(x, y);
            return renderer.wantsMouseInput();
        }, app -> app.invoke(window, x, y));
    }

    private void onMouseButton(long window, int button, int action, int mods) {
        forward(mouseButtonCallback, renderer -> {
            renderer.processGlfwMouseButton(button, action, mods);
            return renderer.wantsMouseInput();
        }, app -> app.invoke(window, button, action, mods));
    }

    private void onScroll(long window, double xOffset, double yOffset) {
        forward(scrollCallback, renderer -> {
            renderer.processGlfwScroll(xOffset, yOffset);
            return renderer.wantsMouseInput() && renderer.isHoveringScrollablePanel();
        }, app -> app.invoke(window, xOffset, yOffset));
    }

    private void onKey(long window, int key, int scancode, int action, int mods) {
        forward(keyCallback, renderer -> {
            renderer.processGlfwKey(key, scancode, action, mods);
            return renderer.wantsKeyboardInput();
        }, app -> app.invoke(window, key, scancode, action, mods));
    }

    private void onChar(long window, int codepoint) {
        forward(charCallback, renderer -> {
            renderer.processGlfwChar(codepoint);
            return renderer.wantsKeyboardInput();
        }, app -> app.invoke(window, codepoint));
    }

    // passToUi forwards the event to the renderer and returns whether the UI consumes it; while the display is not
    // initialized, the event goes only to the application
    private <CallbackType extends Callback> void forward(ChainedCallback<CallbackType> callback,
                                                         Predicate<ImGuiGlfwGl3Renderer> passToUi,
                                                         Consumer<CallbackType> passToApp) {
        dispatch(() -> {
            boolean consumed = isInitialized() && passToUi.test(getRenderer());
            CallbackType app = callback.getApp();
            if (!consumed && app != null) {
                passToApp.accept(app);
            }
        });
    }

    private void rethrowPendingFailure(Throwable releaseFailure) {
        Throwable failure = pendingFailure;
        if (failure == null) {
            return;
        }
        pendingFailure = null;
        if (failure instanceof Error error) {
            throw withSuppressed(error, releaseFailure);
        }
        throw withSuppressed(failure instanceof RuntimeException runtimeFailure
                ? runtimeFailure
                : new RuntimeException(failure), releaseFailure);
    }

    private static <FailureType extends Throwable> FailureType withSuppressed(FailureType failure,
                                                                             Throwable suppressed) {
        if (suppressed != null && suppressed != failure) {
            failure.addSuppressed(suppressed);
        }
        return failure;
    }

    @FunctionalInterface
    private interface CallbackSetter<CallbackType extends Callback> {
        CallbackType set(long window, CallbackType callback);
    }

    // One of the display's window callbacks, chained in front of the application's callback that GLFW returned when
    // the display's callback was set. The application's callback object is never freed: the application owns it
    private static final class ChainedCallback<CallbackType extends Callback> {
        private final CallbackSetter<CallbackType> setter;
        private CallbackType own;
        private CallbackType app;
        private boolean installed = false;

        private ChainedCallback(CallbackSetter<CallbackType> setter) {
            this.setter = setter;
        }

        private CallbackType getApp() {
            return app;
        }

        private void install(long window, CallbackType callback) {
            own = callback;
            app = setter.set(window, callback);
            installed = true;
        }

        private void uninstall(long window) {
            try {
                if (installed) {
                    CallbackType current = setter.set(window, app);
                    // The application replaced the display's callback after initialize, so its newer one stays
                    if (current == null || current.address() != own.address()) {
                        setter.set(window, current);
                    }
                }
            } finally {
                if (own != null) {
                    own.free();
                }
                own = null;
                app = null;
                installed = false;
            }
        }
    }
}
