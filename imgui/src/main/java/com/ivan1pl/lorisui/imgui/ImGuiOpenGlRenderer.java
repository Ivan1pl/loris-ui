package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.gl3.ImGuiImplGl3;
import imgui.sdl3.ImGuiImplSdl3;

import java.util.Objects;

/**
 * Renders LorisUI screens into an SDL3 window with Dear ImGui and OpenGL, one frame at a time.
 *
 * <p>This is the entry point for host integrations, such as the jMonkeyEngine host. The Dear ImGui state behind a
 * renderer is shared by the whole application, so only one renderer, for one window, may be in use at a time.
 * Replacing it with a new renderer (for example when a host display is replaced) does not release the previous
 * renderer's resources. The host loads its {@link com.ivan1pl.lorisui.imgui.font.ImGuiFont fonts}, calls
 * {@link #initialize()} once, forwards every SDL event of the window to {@link #processSdlEvent(long)}, and calls
 * {@link #render} once per frame. Between frames it can ask {@link #wantsMouseInput()}, {@link #wantsKeyboardInput()}
 * and {@link #isHoveringScrollablePanel()} which input the UI consumes.
 *
 * <p>All methods must be called on the host's UI thread, the thread that owns the window's OpenGL context.
 */
public final class ImGuiOpenGlRenderer {
    private final long windowHandle;
    private final ImGuiBackend backend;
    private final ImGuiImplSdl3 implSdl3;
    private final ImGuiImplGl3 implGl3;

    /**
     * Creates a renderer that draws with the given backend into the given window. The Dear ImGui context is created
     * here, so fonts can be loaded once the renderer exists.
     *
     * @param backend      backend that draws the screen's components; must not be null
     * @param windowHandle native handle of the SDL3 window to render into
     * @throws NullPointerException if {@code backend} is {@code null}
     */
    public ImGuiOpenGlRenderer(ImGuiBackend backend, long windowHandle) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.windowHandle = windowHandle;
        ImGui.createContext();
        ImGui.styleColorsDark();
        this.implSdl3 = new ImGuiImplSdl3();
        this.implGl3 = new ImGuiImplGl3();
    }

    /**
     * Prepares Dear ImGui for rendering into the window and applies the LorisUI default style. Call it once, after all
     * fonts are loaded and before the first {@link #render} call, with the window's OpenGL context current.
     */
    public void initialize() {
        ImGui.getIO().setIniFilename(null);
        ImGui.getIO().setLogFilename(null);
        ImGui.getIO().setWantSaveIniSettings(false);
        // A renderer exception can leave windows and style stacks open when render() closes the frame. Error recovery
        // unwinds them, but with its assert enabled imgui-java's default assert callback calls System.exit(1) and the
        // original exception is lost. The recovery tooltip is disabled too, as it offers to re-enable the assert.
        ImGui.getIO().setConfigErrorRecoveryEnableAssert(false);
        ImGui.getIO().setConfigErrorRecoveryEnableTooltip(false);
        ImGui.getIO().getFonts().build();
        this.implSdl3.init(windowHandle);
        this.implGl3.init();
        ImGui.getStyle().setWindowBorderSize(0f);
        ImGui.getStyle().setWindowPadding(0f, 0f);
        ImGui.getStyle().setAlpha(1f);
        ImGui.getStyle().setItemSpacing(0f, 0f);
        ImGui.getStyle().setColor(ImGuiCol.ModalWindowDimBg, 0f, 0f, 0f, 0.6f);
    }

    /**
     * Passes an SDL event of the window to Dear ImGui, so that the UI sees mouse, keyboard, text and window input.
     * Hosts forward every event of the window, after {@link #initialize()}.
     *
     * @param eventPtr native address of the {@code SDL_Event}
     */
    public void processSdlEvent(long eventPtr) {
        implSdl3.processEvent(eventPtr);
    }

    /**
     * Renders one frame showing the given screen. Call it once per host frame, after {@link #initialize()}, with the
     * window's OpenGL context current.
     *
     * <p>The frame is closed even when the screen throws, and the exception is then rethrown to the caller. For
     * example, the {@link IllegalStateException} with which {@link LorisScreen#render} reports a failed layout reaches
     * the host this way.
     *
     * @param screen            screen to render; must not be null
     * @param w                 display width in pixels
     * @param h                 display height in pixels
     * @param bundleKeyResolver resolver for localized text; must not be null
     * @throws IllegalStateException if the screen's most recent layout failed (see {@link LorisScreen#render}), or if
     *                               the backend has no renderer or style applier for one of its components or styles
     */
    public void render(LorisScreen screen, int w, int h, BundleKeyResolver bundleKeyResolver) {
        implGl3.newFrame();
        implSdl3.newFrame();
        ImGui.getIO().setDisplaySize(w, h);
        ImGui.newFrame();

        ImGuiState.hoveringScrollableWindow = false;
        try {
            screen.render(backend, w, h, bundleKeyResolver);
        } finally {
            ImGui.render();
            implGl3.renderDrawData(ImGui.getDrawData());
        }
    }

    /**
     * Returns whether the UI wants mouse input, that is whether the pointer is over LorisUI content or a mouse drag
     * started on it. Hosts use this to keep mouse events that the UI handles away from the game.
     *
     * <p>The result describes the most recently rendered frame. Must be called on the host's UI thread.
     *
     * @return {@code true} if Dear ImGui wants to capture the mouse
     */
    public boolean wantsMouseInput() {
        return ImGui.getIO().getWantCaptureMouse();
    }

    /**
     * Returns whether the UI wants keyboard input, that is whether a LorisUI text or number input has keyboard focus.
     * Hosts use this to keep key events that the UI handles away from the game.
     *
     * <p>The result describes the most recently rendered frame. Must be called on the host's UI thread.
     *
     * @return {@code true} if Dear ImGui wants to capture the keyboard
     */
    public boolean wantsKeyboardInput() {
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    /**
     * Returns whether the pointer is over a scrollable top-level panel. Hosts use this to decide whether a mouse
     * wheel event scrolls the UI or belongs to the game.
     *
     * <p>The result describes the most recently rendered frame. Must be called on the host's UI thread.
     *
     * @return {@code true} if the pointer hovered a scrollable top-level panel during the last frame
     */
    public boolean isHoveringScrollablePanel() {
        return ImGuiState.hoveringScrollableWindow;
    }
}
