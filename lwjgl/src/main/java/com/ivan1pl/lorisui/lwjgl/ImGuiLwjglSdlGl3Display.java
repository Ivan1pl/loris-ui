package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.ImGuiRenderer;
import com.ivan1pl.lorisui.imgui.ImGuiSdlGl3Renderer;
import com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend;

/**
 * Displays LorisUI screens in an application's own SDL3 window, using the Dear ImGui backend.
 * <p>
 * The display is embedded in an application that owns the SDL window, its OpenGL context and the main loop. The
 * display holds a set of named {@link LorisScreen}s and draws the current one when the application asks it to. It
 * resolves the screens' localization keys from the resource bundles registered with {@link #setResourceBundle}, and
 * tells the application which input events the UI consumes.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *     <li>Create the display for the window; register fonts, custom renderers and style appliers, and resource
 *     bundles; add screens and set the current screen. Register only fonts from trusted sources, as described for
 *     {@link ImGuiRenderer#registerFontFromFile(String, int)}.</li>
 *     <li>Call {@link #initialize()} with the window's OpenGL context current. The display sets up Dear ImGui with the
 *     registered fonts, and the current screen's {@link LorisScreen#onStartScreen()} fires. If the setup fails, the
 *     display releases what it had set up and stays uninitialized, so {@code initialize()} can be called again; if a
 *     registered font caused the failure, create a new display instead.</li>
 *     <li>Each frame, pass every SDL event of the window to {@link #processEvent(long)}, skipping the application's
 *     own handling of the events it consumes, and call {@link #render(int, int)} once, after clearing and before
 *     swapping the window.</li>
 *     <li>While the display is initialized, switching screens with {@link #setCurrentScreen} fires
 *     {@link LorisScreen#onEndScreen()} on the old screen and {@link LorisScreen#onStartScreen()} on the new one.
 *     Before {@link #initialize()}, and from the moment the display is closed, even while its release is still
 *     pending, {@link #setCurrentScreen} only records the choice.</li>
 *     <li>Call {@link #close()} before the OpenGL context or the window is destroyed. The current screen's
 *     {@link LorisScreen#onEndScreen()} fires, the display releases its Dear ImGui, OpenGL and SDL resources,
 *     including every texture it loaded, and the background work that computes screen layouts stops until a screen is
 *     drawn again. Closing is final: to show UI again, create a new display. Fonts must then be registered with the
 *     new display.</li>
 * </ul>
 * Closing the display from a component callback, such as a button action, or from a screen's
 * {@link LorisScreen#onStartScreen()} or {@link LorisScreen#onEndScreen()} is allowed. The display is closed at
 * once: {@link #processEvent(long)} returns {@code false} and the other methods that require an open display throw.
 * The release itself completes when the current frame or screen switch has finished, so the application must let
 * that {@link #render(int, int)}, {@link #setCurrentScreen} or {@link #initialize()} call return before it destroys
 * the OpenGL context or the window.
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
 * <p>
 * Initializing the display sets the global SDL hints listed in {@link ImGuiSdlGl3Renderer}; they affect every SDL
 * window of the application and stay set after the display is closed.
 *
 * <h2>Threading</h2>
 * The display is not thread-safe. Create it and call all of its methods on the thread that owns the window and its
 * OpenGL context, with that context current. The screen lifecycle hooks and component callbacks run on that thread.
 * On macOS, that is the process's main thread, so the JVM may need {@code -XstartOnFirstThread}.
 */
public final class ImGuiLwjglSdlGl3Display extends ImGuiLwjglDisplayBase<ImGuiSdlGl3Renderer> implements AutoCloseable {
    private final ImGuiLwjglSdlInput sdlInput;

    /**
     * Creates a display for an SDL window. Call it on the thread that owns the window. The display shows nothing until
     * it is {@linkplain #initialize() initialized}, and the constructor does not touch Dear ImGui, OpenGL or SDL.
     *
     * @param sdlWindow the native handle of the application's SDL window ({@code SDL_Window*}), which must have an
     *                  OpenGL context; must not be {@code 0}
     * @throws IllegalArgumentException if {@code sdlWindow} is {@code 0}
     */
    public ImGuiLwjglSdlGl3Display(long sdlWindow) {
        if (sdlWindow == 0) {
            throw new IllegalArgumentException("sdlWindow must be the handle of an SDL window, not 0");
        }
        ImGuiOpenGlBackend backend = new ImGuiOpenGlBackend();
        super(backend, new ImGuiSdlGl3Renderer(backend, sdlWindow));
        this.sdlInput = new ImGuiLwjglSdlInput(sdlWindow, getRenderer());
    }

    /**
     * Initializes the display and fires {@link LorisScreen#onStartScreen()} on the current screen. Call it with the
     * window's OpenGL context current, before the first {@link #render(int, int)} call. From then on, fonts,
     * renderers and style appliers can no longer be registered.
     * <p>
     * If setting up Dear ImGui fails, the exception propagates, no screen hook fires, and the display releases what it
     * had set up and stays uninitialized, so the call can be retried, for example after a transient SDL or OpenGL
     * failure. A failure caused by a registered font recurs on every retry, because fonts cannot be unregistered;
     * create a new display with other fonts instead.
     * <p>
     * If the screen's {@link LorisScreen#onStartScreen()} throws, the exception propagates, the display stays
     * initialized, and the screen is treated as not started: it receives no {@link LorisScreen#onEndScreen()} for
     * this start. If the hook closes the display, the release completes when the hook has returned, and a failure of
     * the release then propagates from this method.
     *
     * @throws IllegalStateException if the display is closed or already initialized, or if no current screen is set
     *                               (call {@link #setCurrentScreen} first), all checked before any state change; if
     *                               Dear ImGui's SDL3 platform or OpenGL renderer backend fails to initialize; or if a
     *                               registered font cannot be loaded, for example because its data is not a valid
     *                               TrueType font
     */
    public void initialize() {
        initializeDisplay();
    }

    /**
     * Passes an SDL event of the window to the UI and returns whether the UI consumes it. Pass every event of the
     * window, and skip the application's own handling of the event when this method returns {@code true}.
     * <p>
     * Mouse motion and mouse button events are consumed while the UI wants mouse input, that is while the pointer is
     * over LorisUI content or a mouse drag started on it. Mouse wheel events are consumed while the UI wants mouse
     * input and the pointer is over a scrollable panel. Key and text input events are consumed while a LorisUI text or
     * number input has keyboard focus. Other events, such as window events, are passed to the UI but never consumed.
     * The result describes the most recently rendered frame.
     * <p>
     * Returns {@code false} without reading the event while the display is not initialized, including after
     * {@link #close()}.
     *
     * @param sdlEventPtr the native address of the {@code SDL_Event}, for example {@code event.address()} of an LWJGL
     *                    {@code SDL_Event}
     * @return {@code true} if the UI consumes the event; {@code false} if the application should handle it
     */
    public boolean processEvent(long sdlEventPtr) {
        if (!isInitialized()) {
            return false;
        }
        getRenderer().processSdlEvent(sdlEventPtr);
        return sdlInput.isConsumed(sdlEventPtr);
    }

    /**
     * Draws the current screen. Call it once per frame with the window's OpenGL context current. The display does not
     * clear the window or swap its buffers: the application clears and sets its viewport for its own drawing before
     * this call, and swaps the window after it.
     * <p>
     * {@code width} and {@code height} are the size of the window in logical window coordinates, as reported by
     * {@code SDL_GetWindowSize}, not in framebuffer pixels; the framebuffer scale of HiDPI displays is applied
     * automatically.
     * <p>
     * At the end of the frame, the display starts SDL text input for the window when a LorisUI text or number input
     * has gained keyboard focus, so that typed characters arrive as {@code SDL_EVENT_TEXT_INPUT} events, and stops it
     * when the focus has left. An application that starts or stops SDL text input for the same window itself
     * interferes with typing into the UI. If SDL fails to start text input, the frame is still drawn without an error
     * and the display tries again after the next frame.
     * <p>
     * If the display is closed during the frame, for example from a component callback, the release completes when
     * the frame has been drawn; a failure of the release then propagates from this method.
     *
     * @param width  the window width in logical window coordinates
     * @param height the window height in logical window coordinates
     * @throws IllegalStateException if the display is not initialized or is closed, if called from a component
     *                               callback during a frame, if the screen's most recent layout failed (see
     *                               {@link LorisScreen#render}), or if a component or style of the screen has no
     *                               registered renderer or style applier
     */
    public void render(int width, int height) {
        renderFrame(() -> {
            getRenderer().render(getScreenManager().getCurrentScreen(), width, height, getResolver());
            if (isInitialized()) {
                sdlInput.updateTextInput();
            }
        });
    }

    /**
     * Closes the display. The display is closed at once: {@link #processEvent(long)} returns {@code false} from then
     * on, and registering fonts, renderers or style appliers, {@link #initialize()} and {@link #render(int, int)}
     * throw {@link IllegalStateException}. Closing is final; to show UI again, create a new display.
     * <p>
     * If the display is initialized, it then fires {@link LorisScreen#onEndScreen()} on the current screen, if that
     * screen's {@link LorisScreen#onStartScreen()} returned normally, stops SDL text input for the window if the
     * display started it, releases its Dear ImGui, OpenGL and SDL resources, including every texture it loaded, and
     * stops the background work that computes screen layouts until a screen is drawn again. Call it with the window's
     * OpenGL context current, before the context or the window is destroyed.
     * <p>
     * When called during a frame, for example from a component callback, or during a screen switch or a screen's
     * lifecycle hook, the release, including {@link LorisScreen#onEndScreen()}, runs when that frame, switch or hook
     * has finished, and its failure propagates from the method that was running. The end hook then fires on the
     * screen that was started when the display was closed; a screen selected with {@link #setCurrentScreen} after
     * that is only recorded.
     * <p>
     * Does nothing if the display is already closed. Every release step runs even when an earlier one fails. If
     * {@link LorisScreen#onEndScreen()} or a release step throws, the first exception propagates after all steps have
     * run, and the display is closed all the same.
     *
     * @throws RuntimeException the first failure of {@link LorisScreen#onEndScreen()} or of a release step; failures
     *                          of later steps are added to it as suppressed exceptions
     */
    @Override
    public void close() {
        closeDisplay();
    }

    @Override
    void setUp() {
        getRenderer().initialize();
    }

    @Override
    void releaseInput() {
        sdlInput.stopTextInputIfStarted();
    }
}
