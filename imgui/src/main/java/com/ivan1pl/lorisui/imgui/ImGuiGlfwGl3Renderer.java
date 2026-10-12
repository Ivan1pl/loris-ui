package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

/**
 * Renders LorisUI screens into a GLFW window with Dear ImGui and OpenGL, one frame at a time.
 *
 * <p>This variant pairs Dear ImGui's GLFW platform backend with its OpenGL 3 renderer backend and is used with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend ImGuiOpenGlBackend}. The lifecycle, the font
 * registration and the input queries it shares with the other variants are described in {@link ImGuiRenderer}. With
 * this variant:
 * <ul>
 *     <li>Call {@link #initialize()} with the window's OpenGL context current.</li>
 *     <li>Forward the window's input events from the host's own GLFW callbacks. The renderer installs no callbacks on
 *     the window, so each callback of the host passes its arguments to the matching {@code processGlfw*} method:
 *     window focus to {@link #processGlfwWindowFocus(boolean)}, cursor enter to
 *     {@link #processGlfwCursorEnter(boolean)}, cursor position to {@link #processGlfwCursorPos(double, double)},
 *     mouse buttons to {@link #processGlfwMouseButton(int, int, int)}, scrolling to
 *     {@link #processGlfwScroll(double, double)}, keys to {@link #processGlfwKey(int, int, int, int)} and characters
 *     to {@link #processGlfwChar(int)}. The methods report nothing back; whether an event also belongs to the game is
 *     the host's decision, made with {@link #wantsMouseInput()}, {@link #wantsKeyboardInput()} and
 *     {@link #isHoveringScrollablePanel()}. GLFW delivers characters whenever the window has keyboard focus, so the
 *     host does not need to start text input.</li>
 *     <li>Each frame, after the window's events have been processed, call {@link #render} once.</li>
 *     <li>Call {@link #dispose()} with the window's OpenGL context current, before the window or its OpenGL context is
 *     destroyed.</li>
 * </ul>
 *
 * <p>Initializing the renderer replaces GLFW's global monitor callback, set with {@code glfwSetMonitorCallback}, with a
 * callback of Dear ImGui's GLFW backend that does nothing. The renderer does not restore the previous callback, not
 * even in {@link #dispose()}. {@code ImGuiLwjglGlfwGl3Display} restores the application's monitor callback; a host that
 * uses the renderer directly sets its own monitor callback again after {@link #initialize()} and frees the callback
 * that {@code glfwSetMonitorCallback} returns then.
 *
 * <h2>Threading</h2>
 * The renderer is not thread-safe. All methods must be called on the thread that owns the window's OpenGL context,
 * which for GLFW is the process's main thread, because GLFW windows must be created and their events processed there.
 * On macOS, the JVM may therefore need {@code -XstartOnFirstThread}.
 */
public final class ImGuiGlfwGl3Renderer extends ImGuiRenderer {
    private ImGuiImplGlfw implGlfw;
    private ImGuiImplGl3 implGl3;

    /**
     * Creates a renderer that draws with the given backend into the given GLFW window. Nothing is drawn and no Dear
     * ImGui, GLFW or OpenGL state is touched until the renderer is {@linkplain #initialize() initialized}.
     *
     * @param backend    backend that draws the screen's components, not used by any other renderer; must not be
     *                   null
     * @param glfwWindow native handle of the GLFW window to render into ({@code GLFWwindow*}); must not be {@code 0}
     * @throws NullPointerException     if {@code backend} is {@code null}
     * @throws IllegalArgumentException if {@code glfwWindow} is {@code 0}
     */
    public ImGuiGlfwGl3Renderer(ImGuiBackend backend, long glfwWindow) {
        super(backend, glfwWindow);
        if (glfwWindow == 0L) {
            throw new IllegalArgumentException("GLFW window handle must not be 0");
        }
    }

    /**
     * Passes a focus change of the window to Dear ImGui. Call it from the window's focus callback.
     *
     * @param focused whether the window gained keyboard focus ({@code true}) or lost it ({@code false})
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processGlfwWindowFocus(boolean focused) {
        checkInitialized();
        makeContextCurrent();
        implGlfw.windowFocusCallback(getWindowHandle(), focused);
    }

    /**
     * Passes the cursor entering or leaving the window's content area to Dear ImGui. Call it from the window's cursor
     * enter callback.
     *
     * @param entered whether the cursor entered the content area ({@code true}) or left it ({@code false})
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processGlfwCursorEnter(boolean entered) {
        checkInitialized();
        makeContextCurrent();
        implGlfw.cursorEnterCallback(getWindowHandle(), entered);
    }

    /**
     * Passes a cursor movement to Dear ImGui. Call it from the window's cursor position callback.
     *
     * @param x new cursor x position, in screen coordinates relative to the left edge of the window's content area
     * @param y new cursor y position, in screen coordinates relative to the top edge of the window's content area
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processGlfwCursorPos(double x, double y) {
        checkInitialized();
        makeContextCurrent();
        implGlfw.cursorPosCallback(getWindowHandle(), x, y);
    }

    /**
     * Passes a mouse button press or release to Dear ImGui. Call it from the window's mouse button callback.
     *
     * @param button GLFW mouse button that was pressed or released ({@code GLFW_MOUSE_BUTTON_*})
     * @param action GLFW action ({@code GLFW_PRESS} or {@code GLFW_RELEASE})
     * @param mods   GLFW modifier key bits held down ({@code GLFW_MOD_*})
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processGlfwMouseButton(int button, int action, int mods) {
        checkInitialized();
        makeContextCurrent();
        implGlfw.mouseButtonCallback(getWindowHandle(), button, action, mods);
    }

    /**
     * Passes a scroll of the mouse wheel or touchpad to Dear ImGui. Call it from the window's scroll callback.
     *
     * @param xOffset horizontal scroll offset
     * @param yOffset vertical scroll offset
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processGlfwScroll(double xOffset, double yOffset) {
        checkInitialized();
        makeContextCurrent();
        implGlfw.scrollCallback(getWindowHandle(), xOffset, yOffset);
    }

    /**
     * Passes a key press, repeat or release to Dear ImGui. Call it from the window's key callback.
     *
     * @param key      GLFW key ({@code GLFW_KEY_*}), or {@code GLFW_KEY_UNKNOWN}
     * @param scancode platform-specific scancode of the key
     * @param action   GLFW action ({@code GLFW_PRESS}, {@code GLFW_REPEAT} or {@code GLFW_RELEASE})
     * @param mods     GLFW modifier key bits held down ({@code GLFW_MOD_*})
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processGlfwKey(int key, int scancode, int action, int mods) {
        checkInitialized();
        makeContextCurrent();
        implGlfw.keyCallback(getWindowHandle(), key, scancode, action, mods);
    }

    /**
     * Passes a typed character to Dear ImGui. Call it from the window's character callback.
     *
     * @param codepoint Unicode code point of the typed character
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processGlfwChar(int codepoint) {
        checkInitialized();
        makeContextCurrent();
        implGlfw.charCallback(getWindowHandle(), codepoint);
    }

    /**
     * Renders one frame showing the given screen. Call it once per host frame, while the renderer is initialized,
     * with the window's OpenGL context current.
     *
     * <p>{@code w} and {@code h} are the size of the window in screen coordinates, as reported by
     * {@code glfwGetWindowSize}, never the framebuffer size reported by {@code glfwGetFramebufferSize}; Dear ImGui's
     * GLFW backend applies the framebuffer scale of HiDPI displays itself. On Windows and X11, GLFW measures windows in
     * pixels, so the window size equals the framebuffer size and the UI is not scaled up on HiDPI monitors.
     *
     * <p>The frame is closed even when the screen throws, and the exception is then rethrown to the caller. For
     * example, the {@link IllegalStateException} with which {@link LorisScreen#render} reports a failed layout reaches
     * the host this way. If closing the frame fails too, that failure is added to the screen's exception as a
     * suppressed exception.
     *
     * @param screen            screen to render; must not be null
     * @param w                 screen width in window screen coordinates
     * @param h                 screen height in window screen coordinates
     * @param bundleKeyResolver resolver for localized text; must not be null
     * @throws NullPointerException  if {@code screen} or {@code bundleKeyResolver} is {@code null}
     * @throws IllegalStateException if the renderer is not initialized or a frame is already being rendered (checked
     *                               before a frame is started), if the screen's most recent layout failed (see
     *                               {@link LorisScreen#render}), or if the backend has no renderer or style applier
     *                               for one of its components or styles
     */
    public void render(LorisScreen screen, int w, int h, BundleKeyResolver bundleKeyResolver) {
        renderFrame(screen, w, h, bundleKeyResolver, () -> implGl3.renderDrawData(ImGui.getDrawData()));
    }

    @Override
    void initBackends() {
        ImGuiImplGlfw glfw = new ImGuiImplGlfw();
        // The host forwards the window's events, so Dear ImGui's backend installs no window callbacks
        if (!glfw.init(getWindowHandle(), false)) {
            throw new IllegalStateException("Dear ImGui GLFW platform backend failed to initialize");
        }
        implGlfw = glfw;
        ImGuiImplGl3 gl3 = new ImGuiImplGl3();
        if (!gl3.init()) {
            throw new IllegalStateException("Dear ImGui OpenGL renderer backend failed to initialize");
        }
        implGl3 = gl3;
    }

    @Override
    void newBackendFrame() {
        implGl3.newFrame();
        implGlfw.newFrame();
    }

    @Override
    void shutdownRendererBackend() {
        if (implGl3 != null) {
            implGl3.shutdown();
        }
    }

    @Override
    void shutdownPlatformBackend() {
        if (implGlfw != null) {
            implGlfw.shutdown();
        }
    }

    @Override
    void clearBackends() {
        implGl3 = null;
        implGlfw = null;
    }
}
