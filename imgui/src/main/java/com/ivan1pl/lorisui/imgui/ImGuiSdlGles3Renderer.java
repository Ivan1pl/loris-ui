package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.gles3.ImGuiImplGles3;
import imgui.ImGui;
import imgui.sdl3.ImGuiImplSdl3;

/**
 * Renders LorisUI screens into an SDL3 window with Dear ImGui and OpenGL ES 3.0, one frame at a time.
 *
 * <p>This variant pairs Dear ImGui's SDL3 platform backend with an OpenGL ES 3.0 renderer backend and is used with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlEsBackend ImGuiOpenGlEsBackend}, or with a custom backend that
 * creates OpenGL ES textures. jMonkeyEngine's {@code ANGLE_GLES3} renderer, for example, renders with it. The
 * lifecycle, the font registration and the input queries it shares with the other variants are described in
 * {@link ImGuiRenderer}. With this variant:
 * <ul>
 *     <li>Call {@link #initialize()} with the window's OpenGL ES 3.0 context current, for example on jMonkeyEngine's
 *     render thread.</li>
 *     <li>Each frame, forward every SDL event of the window to {@link #processSdlEvent(long)} and call
 *     {@link #render} once. {@link #wantsTextInput()} reports whether the host should keep SDL text input
 *     started.</li>
 *     <li>Call {@link #dispose()} with the window's OpenGL ES context current, before the window or its context is
 *     destroyed.</li>
 * </ul>
 *
 * <p>OpenGL ES 3.0 has no draw call with a base vertex, and Dear ImGui's indices are 16-bit, so a single Dear ImGui
 * window can hold at most 65,535 vertices. A window above that fails a Dear ImGui assertion, which terminates the JVM
 * (see {@link ImGuiRenderer}). Typical menus and HUDs stay far below the limit; very long lists or large amounts of
 * text in one panel can reach it.
 *
 * <p>Initializing the renderer sets global SDL hints through Dear ImGui's SDL3 platform backend:
 * {@code SDL_HINT_MOUSE_FOCUS_CLICKTHROUGH} to {@code "1"}, {@code SDL_HINT_MOUSE_AUTO_CAPTURE} to {@code "0"} and
 * {@code SDL_HINT_BORDERLESS_WINDOWED_STYLE} to {@code "0"}. They affect every SDL window of the application and stay
 * set after {@link #dispose()}; an application that needs other values sets them again after {@link #initialize()}.
 *
 * <h2>Threading</h2>
 * The renderer is not thread-safe. All methods must be called on the host's UI thread, the thread that owns the
 * window's OpenGL ES context.
 */
public final class ImGuiSdlGles3Renderer extends ImGuiRenderer {
    private ImGuiImplSdl3 implSdl3;
    private ImGuiImplGles3 implGles3;

    /**
     * Creates a renderer that draws with the given backend into the given SDL3 window. Nothing is drawn and no Dear
     * ImGui, SDL or OpenGL ES state is touched until the renderer is {@linkplain #initialize() initialized}.
     *
     * @param backend   backend that draws the screen's components and creates OpenGL ES textures, such as an
     *                  {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlEsBackend ImGuiOpenGlEsBackend}, not used
     *                  by any other renderer; must not be null
     * @param sdlWindow native handle of the SDL3 window to render into ({@code SDL_Window*}); must not be {@code 0}
     * @throws NullPointerException     if {@code backend} is {@code null}
     * @throws IllegalArgumentException if {@code sdlWindow} is {@code 0}
     */
    public ImGuiSdlGles3Renderer(ImGuiBackend backend, long sdlWindow) {
        super(backend, sdlWindow);
        if (sdlWindow == 0L) {
            throw new IllegalArgumentException("SDL window handle must not be 0");
        }
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
     * with the window's OpenGL ES context current.
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
        renderFrame(screen, w, h, bundleKeyResolver, () -> implGles3.renderDrawData(ImGui.getDrawData()));
    }

    @Override
    void initBackends() {
        ImGuiImplSdl3 sdl3 = new ImGuiImplSdl3();
        if (!sdl3.init(getWindowHandle())) {
            throw new IllegalStateException("Dear ImGui SDL3 platform backend failed to initialize");
        }
        implSdl3 = sdl3;
        ImGuiImplGles3 gles3 = new ImGuiImplGles3();
        try {
            gles3.init();
        } catch (Throwable failure) {
            // A partial init already owns GL objects, so the failed instance is released here and never kept
            DeferredRelease.runAfterFailure(failure, gles3::shutdown);
            throw failure;
        }
        implGles3 = gles3;
    }

    @Override
    void newBackendFrame() {
        implSdl3.newFrame();
    }

    @Override
    void shutdownRendererBackend() {
        if (implGles3 != null) {
            implGles3.shutdown();
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
        implGles3 = null;
        implSdl3 = null;
    }
}
