package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.sdl3.ImGuiImplSdl3;

/**
 * Renders LorisUI screens into an SDL3 window with Dear ImGui and OpenGL, one frame at a time.
 *
 * <p>This variant pairs Dear ImGui's SDL3 platform backend with its OpenGL 3 renderer backend and is used with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend ImGuiOpenGlBackend}. The lifecycle, the font
 * registration and the input queries it shares with the other variants are described in {@link ImGuiRenderer}. With
 * this variant:
 * <ul>
 *     <li>Call {@link #initialize()} with the window's OpenGL context current.</li>
 *     <li>Each frame, forward every SDL event of the window to {@link #processSdlEvent(long)} and call
 *     {@link #render} once. {@link #wantsTextInput()} reports whether the host should keep SDL text input
 *     started.</li>
 *     <li>Call {@link #dispose()} with the window's OpenGL context current, before the window or its OpenGL context is
 *     destroyed.</li>
 * </ul>
 *
 * <p>Initializing the renderer sets global SDL hints through Dear ImGui's SDL3 platform backend:
 * {@code SDL_HINT_MOUSE_FOCUS_CLICKTHROUGH} to {@code "1"}, {@code SDL_HINT_MOUSE_AUTO_CAPTURE} to {@code "0"} and
 * {@code SDL_HINT_BORDERLESS_WINDOWED_STYLE} to {@code "0"}. They affect every SDL window of the application and stay
 * set after {@link #dispose()}; an application that needs other values sets them again after {@link #initialize()}.
 *
 * <h2>Threading</h2>
 * The renderer is not thread-safe. All methods must be called on the host's UI thread, the thread that owns the
 * window's OpenGL context.
 */
public final class ImGuiSdlGl3Renderer extends ImGuiRenderer {
    private ImGuiImplSdl3 implSdl3;
    private ImGuiImplGl3 implGl3;

    /**
     * Creates a renderer that draws with the given backend into the given SDL3 window. Nothing is drawn and no Dear
     * ImGui, SDL or OpenGL state is touched until the renderer is {@linkplain #initialize() initialized}.
     *
     * @param backend   backend that draws the screen's components; must not be null
     * @param sdlWindow native handle of the SDL3 window to render into ({@code SDL_Window*})
     * @throws NullPointerException if {@code backend} is {@code null}
     */
    public ImGuiSdlGl3Renderer(ImGuiBackend backend, long sdlWindow) {
        super(backend, sdlWindow);
    }

    /**
     * Passes an SDL event of the window to Dear ImGui, so that the UI sees mouse, keyboard, text and window input.
     * Hosts forward every event of the window while the renderer is initialized.
     *
     * @param eventPtr native address of the {@code SDL_Event}
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processSdlEvent(long eventPtr) {
        checkInitialized();
        makeContextCurrent();
        implSdl3.processEvent(eventPtr);
    }

    /**
     * Renders one frame showing the given screen. Call it once per host frame, while the renderer is initialized,
     * with the window's OpenGL context current.
     *
     * <p>{@code w} and {@code h} are the size of the window in logical window coordinates, as reported by
     * {@code SDL_GetWindowSize}, not in framebuffer pixels; Dear ImGui's SDL3 backend applies the framebuffer scale of
     * HiDPI displays itself.
     *
     * <p>The frame is closed even when the screen throws, and the exception is then rethrown to the caller. For
     * example, the {@link IllegalStateException} with which {@link LorisScreen#render} reports a failed layout reaches
     * the host this way. If closing the frame fails too, that failure is added to the screen's exception as a
     * suppressed exception.
     *
     * @param screen            screen to render; must not be null
     * @param w                 screen width in logical window coordinates
     * @param h                 screen height in logical window coordinates
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
        ImGuiImplSdl3 sdl3 = new ImGuiImplSdl3();
        if (!sdl3.init(getWindowHandle())) {
            throw new IllegalStateException("Dear ImGui SDL3 platform backend failed to initialize");
        }
        implSdl3 = sdl3;
        ImGuiImplGl3 gl3 = new ImGuiImplGl3();
        if (!gl3.init()) {
            throw new IllegalStateException("Dear ImGui OpenGL renderer backend failed to initialize");
        }
        implGl3 = gl3;
    }

    @Override
    void newBackendFrame() {
        implGl3.newFrame();
        implSdl3.newFrame();
    }

    @Override
    void shutdownRendererBackend() {
        if (implGl3 != null) {
            implGl3.shutdown();
        }
    }

    @Override
    void shutdownPlatformBackend() {
        if (implSdl3 != null) {
            implSdl3.shutdown();
        }
    }

    @Override
    void clearBackends() {
        implGl3 = null;
        implSdl3 = null;
    }
}
