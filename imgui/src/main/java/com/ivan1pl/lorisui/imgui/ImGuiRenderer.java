package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.impl.ClassPathResources;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCol;
import imgui.internal.ImGuiContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renders LorisUI screens into a window with Dear ImGui, one frame at a time. This is the common base of the renderer
 * variants, each of which pairs one Dear ImGui platform backend with one Dear ImGui renderer backend.
 *
 * <p>The renderer is the entry point for host integrations, such as the jMonkeyEngine host. A host constructs the
 * variant that matches its window and graphics API: {@link ImGuiSdlGl3Renderer} for an SDL3 window with an OpenGL
 * context, {@link ImGuiGlfwGl3Renderer} for a GLFW window with an OpenGL context, or {@link ImGuiSdlGpuRenderer} for
 * an SDL3 window claimed for an SDL_GPU device. The renderer owns its own Dear ImGui context and releases everything
 * it created when it is disposed.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *     <li>Construct the renderer. Construction does not touch Dear ImGui, the window or the graphics API.</li>
 *     <li>Register fonts with the {@code registerFont*} methods. Fonts can be registered only while the renderer is
 *     not initialized. The renderer keeps each font's TrueType data in memory for its whole life, so that the font
 *     can be loaded again on every {@link #initialize()}; this is worth keeping in mind for large fonts, such as fonts
 *     with CJK characters.</li>
 *     <li>Call {@link #initialize()} once the window's graphics context is ready, as the variant describes.</li>
 *     <li>Each frame, forward the window's input events to the variant and call the variant's {@code render} method
 *     once. Between frames, {@link #wantsMouseInput()}, {@link #wantsKeyboardInput()} and
 *     {@link #isHoveringScrollablePanel()} report which input the UI consumes, and {@link #wantsTextInput()} reports
 *     whether the UI expects typed characters.</li>
 *     <li>Call {@link #dispose()} before the window or its graphics context is destroyed. It releases the Dear ImGui
 *     context, the platform and graphics resources of the renderer, and every texture the backend has loaded.</li>
 *     <li>Optionally call {@link #initialize()} again to show the UI again. The registered fonts are restored, and the
 *     {@link ImGuiFont} instances returned at registration stay valid.</li>
 * </ol>
 * {@link #initialize()} and {@link #dispose()} cannot be called while a frame is being rendered, for example from a
 * component callback; a host that wants to tear the UI down from inside a frame must defer the teardown until the
 * variant's {@code render} method has returned.
 *
 * <p>Dear ImGui UI state, such as selected tabs, scroll positions, open popups and keyboard focus, starts fresh after
 * {@link #dispose()} and {@link #initialize()}. State held by LorisUI components and their bindings is not affected.
 *
 * <p>Every method that talks to Dear ImGui first makes the renderer's context current and leaves it current when it
 * returns. An application that uses a Dear ImGui context of its own must make that context current again after calling
 * the renderer. Only one renderer may be in use at a time.
 *
 * <p>Initializing a renderer can change global state of its windowing library, such as SDL hints or GLFW's monitor
 * callback; each variant documents these effects.
 *
 * <h2>Dear ImGui assertions</h2>
 * A failed Dear ImGui assertion terminates the JVM: imgui-java reports it and then calls {@code System.exit(1)}, and
 * execution cannot continue past it. The renderer disables the assertions of Dear ImGui's error recovery, so an
 * exception thrown while a frame is drawn reaches the host as an exception instead; assertions that error recovery
 * does not cover still terminate the JVM. An application can install its own handler with imgui-java's
 * {@code ImGui.setAssertCallback(ImAssertCallback)}, for example to log the assertion to its own log or choose the
 * exit code; the JVM still terminates once the handler returns.
 *
 * <h2>Threading</h2>
 * The renderer is not thread-safe. All methods must be called on the host's UI thread, the thread that owns the
 * window and its graphics context.
 */
public abstract sealed class ImGuiRenderer permits ImGuiSdlGl3Renderer, ImGuiGlfwGl3Renderer, ImGuiSdlGpuRenderer {
    // Dear ImGui asserts that font data is longer than this, and a failed assertion terminates the JVM
    private static final int MAX_REJECTED_FONT_DATA_SIZE = 100;

    private final ImGuiBackend backend;
    private final long windowHandle;
    private final List<ImGuiFont> fonts = new ArrayList<>();
    private ImGuiContext context;
    private boolean rendering = false;

    ImGuiRenderer(ImGuiBackend backend, long windowHandle) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.windowHandle = windowHandle;
    }

    /**
     * Reads a TrueType font from a file and registers it with this renderer. The font covers the default glyph
     * ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to {@code U+00FF}). The file is read during this
     * call. Register only fonts from trusted sources, because Dear ImGui's font loader is not hardened against
     * malicious font data.
     *
     * @param file path of the TrueType file on the file system; must not be null
     * @param size font size in pixels; must be positive
     * @return the registered font, which works only with this renderer
     * @throws NullPointerException     if {@code file} is {@code null}
     * @throws IllegalArgumentException if {@code size} is not positive
     * @throws IllegalStateException    if the renderer is initialized
     * @throws IOException              if the file cannot be read, or is too short to be a font (100 bytes or less);
     *                                  the message names {@code file}
     */
    public ImGuiFont registerFontFromFile(String file, int size) throws IOException {
        checkFontRegistration(file, "file", size);
        return addFont(readFile(file), file, size, ImGuiFont.DEFAULT_GLYPH_RANGES);
    }

    /**
     * Reads the given characters of a TrueType font from a file and registers them with this renderer. The file is
     * read during this call. Register only fonts from trusted sources, because Dear ImGui's font loader is not
     * hardened against malicious font data.
     *
     * @param file        path of the TrueType file on the file system; must not be null
     * @param size        font size in pixels; must be positive
     * @param glyphRanges characters to include, in Dear ImGui's zero-terminated range-pair format (see
     *                    {@link ImGuiFont}); must not be null
     * @return the registered font, which works only with this renderer
     * @throws NullPointerException     if {@code file} or {@code glyphRanges} is {@code null}
     * @throws IllegalArgumentException if {@code size} is not positive, or if {@code glyphRanges} has no range, is not
     *                                  zero-terminated or has a range whose first code point is above its last
     * @throws IllegalStateException    if the renderer is initialized
     * @throws IOException              if the file cannot be read, or is too short to be a font (100 bytes or less);
     *                                  the message names {@code file}
     */
    public ImGuiFont registerFontFromFile(String file, int size, short[] glyphRanges) throws IOException {
        Objects.requireNonNull(glyphRanges, "glyphRanges");
        checkFontRegistration(file, "file", size);
        short[] ranges = ImGuiFont.buildGlyphRanges(glyphRanges);
        return addFont(readFile(file), file, size, ranges);
    }

    /**
     * Reads a TrueType font from a class-path resource and registers it with this renderer. The font covers the
     * default glyph ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to {@code U+00FF}). The resource is read
     * during this call. Register only fonts from trusted sources, because Dear ImGui's font loader is not hardened
     * against malicious font data.
     *
     * <p>{@code resource} is a class-path resource name, where a leading {@code /} is optional. On the module path the
     * resource must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * @param resource class-path resource name of the TrueType font; must not be null
     * @param size     font size in pixels; must be positive
     * @return the registered font, which works only with this renderer
     * @throws NullPointerException     if {@code resource} is {@code null}
     * @throws IllegalArgumentException if {@code size} is not positive
     * @throws IllegalStateException    if the renderer is initialized
     * @throws IOException              if the resource does not exist, cannot be read, or is too short to be a font
     *                                  (100 bytes or less); the message names {@code resource}
     */
    public ImGuiFont registerFontFromResources(String resource, int size) throws IOException {
        checkFontRegistration(resource, "resource", size);
        return addFont(readResource(resource), resource, size, ImGuiFont.DEFAULT_GLYPH_RANGES);
    }

    /**
     * Reads the given characters of a TrueType font from a class-path resource and registers them with this renderer.
     * The resource is read during this call. Register only fonts from trusted sources, because Dear ImGui's font
     * loader is not hardened against malicious font data.
     *
     * <p>{@code resource} is a class-path resource name, where a leading {@code /} is optional. On the module path the
     * resource must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * @param resource    class-path resource name of the TrueType font; must not be null
     * @param size        font size in pixels; must be positive
     * @param glyphRanges characters to include, in Dear ImGui's zero-terminated range-pair format (see
     *                    {@link ImGuiFont}); must not be null
     * @return the registered font, which works only with this renderer
     * @throws NullPointerException     if {@code resource} or {@code glyphRanges} is {@code null}
     * @throws IllegalArgumentException if {@code size} is not positive, or if {@code glyphRanges} has no range, is not
     *                                  zero-terminated or has a range whose first code point is above its last
     * @throws IllegalStateException    if the renderer is initialized
     * @throws IOException              if the resource does not exist, cannot be read, or is too short to be a font
     *                                  (100 bytes or less); the message names {@code resource}
     */
    public ImGuiFont registerFontFromResources(String resource, int size, short[] glyphRanges) throws IOException {
        Objects.requireNonNull(glyphRanges, "glyphRanges");
        checkFontRegistration(resource, "resource", size);
        short[] ranges = ImGuiFont.buildGlyphRanges(glyphRanges);
        return addFont(readResource(resource), resource, size, ranges);
    }

    /**
     * Prepares Dear ImGui for rendering into the window: creates the renderer's Dear ImGui context, loads the
     * registered fonts, initializes the variant's Dear ImGui platform and renderer backends and applies the LorisUI
     * default style. Call it once the window's graphics context is ready, as the variant describes (for an OpenGL
     * variant, with the window's OpenGL context current; for the SDL_GPU variant, with the window claimed for the GPU
     * device), before the first frame is rendered. After {@link #dispose()} it can be called again.
     *
     * <p>If initialization fails, everything it had set up is released again and the renderer stays uninitialized, so
     * the call can be retried, for example after a transient windowing or graphics failure. A failure caused by a
     * registered font recurs on every retry, because fonts cannot be unregistered.
     *
     * <p>Initializing can change global state of the windowing library, such as SDL hints or GLFW's monitor callback;
     * the variant documents these effects.
     *
     * @throws IllegalStateException if the renderer is already initialized or a frame is being rendered (both checked
     *                               before any state change); if Dear ImGui's platform or renderer backend fails to
     *                               initialize; or if a registered font cannot be loaded, for example because its data
     *                               is not a valid TrueType font
     */
    public void initialize() {
        if (rendering) {
            throw new IllegalStateException("Cannot initialize the renderer while a frame is being rendered");
        }
        if (context != null) {
            throw new IllegalStateException("Renderer is already initialized");
        }
        try {
            context = ImGui.createContext();
            makeContextCurrent();
            ImGui.styleColorsDark();
            ImGuiIO io = ImGui.getIO();
            io.setIniFilename(null);
            io.setLogFilename(null);
            io.setWantSaveIniSettings(false);
            // A renderer exception can leave windows and style stacks open when the frame is closed. Error recovery
            // unwinds them, but with its assert enabled imgui-java's default assert callback calls System.exit(1) and
            // the original exception is lost. The recovery tooltip is disabled too, as it offers to re-enable the
            // assert.
            io.setConfigErrorRecoveryEnableAssert(false);
            io.setConfigErrorRecoveryEnableTooltip(false);
            loadFonts(io.getFonts());
            io.getFonts().build();

            initBackends();

            ImGui.getStyle().setWindowBorderSize(0f);
            ImGui.getStyle().setWindowPadding(0f, 0f);
            ImGui.getStyle().setAlpha(1f);
            ImGui.getStyle().setItemSpacing(0f, 0f);
            ImGui.getStyle().setColor(ImGuiCol.ModalWindowDimBg, 0f, 0f, 0f, 0.6f);
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, this::release);
            throw failure;
        }
    }

    /**
     * Returns whether the UI wants mouse input, that is whether the pointer is over LorisUI content or a mouse drag
     * started on it. Hosts use this to keep mouse events that the UI handles away from the game.
     *
     * <p>The result describes the most recently rendered frame.
     *
     * @return {@code true} if Dear ImGui wants to capture the mouse; {@code false} if it does not, or if the renderer
     *         is not initialized
     */
    public boolean wantsMouseInput() {
        if (context == null) {
            return false;
        }
        makeContextCurrent();
        return ImGui.getIO().getWantCaptureMouse();
    }

    /**
     * Returns whether the UI wants keyboard input, that is whether a LorisUI text or number input has keyboard focus.
     * Hosts use this to keep key events that the UI handles away from the game.
     *
     * <p>The result describes the most recently rendered frame.
     *
     * @return {@code true} if Dear ImGui wants to capture the keyboard; {@code false} if it does not, or if the
     *         renderer is not initialized
     */
    public boolean wantsKeyboardInput() {
        if (context == null) {
            return false;
        }
        makeContextCurrent();
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    /**
     * Returns whether the UI wants text input, that is whether a LorisUI text or number input is active and expects
     * typed characters.
     *
     * <p>Some windowing libraries, such as SDL3, deliver typed characters only while text input is started for the
     * window, and the renderer does not start or stop it. A host on such a library starts text input when this method
     * begins to return {@code true} and stops it when it returns {@code false} again, unless its framework keeps text
     * input started itself, as jMonkeyEngine does.
     *
     * <p>The result describes the most recently rendered frame.
     *
     * @return {@code true} if Dear ImGui wants text input; {@code false} if it does not, or if the renderer is not
     *         initialized
     */
    public boolean wantsTextInput() {
        if (context == null) {
            return false;
        }
        makeContextCurrent();
        return ImGui.getIO().getWantTextInput();
    }

    /**
     * Returns whether the pointer is over a scrollable top-level panel. Hosts use this to decide whether a mouse
     * wheel event scrolls the UI or belongs to the game.
     *
     * <p>The result describes the most recently rendered frame.
     *
     * @return {@code true} if the pointer hovered a scrollable top-level panel during the last frame; {@code false}
     *         otherwise, or if the renderer is not initialized
     */
    public boolean isHoveringScrollablePanel() {
        if (context == null) {
            return false;
        }
        makeContextCurrent();
        return ImGuiState.hoveringScrollableWindow;
    }

    /**
     * Releases everything the renderer set up in {@link #initialize()}: every texture the backend has loaded, the
     * platform and graphics resources of Dear ImGui, and the renderer's Dear ImGui context. Call it with the window's
     * graphics context ready, as for {@link #initialize()}, before the window or its graphics context is destroyed.
     * Afterwards the renderer can be initialized again, and the registered fonts stay valid.
     *
     * <p>Does nothing if the renderer is not initialized. Every release step runs even when an earlier one fails, and
     * the renderer is uninitialized afterwards in any case.
     *
     * @throws IllegalStateException if a frame is being rendered, for example when called from a component callback;
     *                               nothing is released then
     * @throws RuntimeException      the first failure of a release step, after all other steps have run; failures of
     *                               later steps are added to it as suppressed exceptions
     */
    public void dispose() {
        if (rendering) {
            throw new IllegalStateException("Cannot dispose the renderer while a frame is being rendered");
        }
        if (context == null) {
            return;
        }
        release();
    }

    long getWindowHandle() {
        return windowHandle;
    }

    void makeContextCurrent() {
        ImGui.setCurrentContext(context);
        ImGuiState.currentContextPtr = context.ptr;
    }

    void checkInitialized() {
        if (context == null) {
            throw new IllegalStateException("Renderer is not initialized");
        }
    }

    void renderFrame(LorisScreen screen, int w, int h, BundleKeyResolver bundleKeyResolver, Runnable drawData) {
        checkInitialized();
        if (rendering) {
            throw new IllegalStateException("A frame is already being rendered");
        }
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(bundleKeyResolver, "bundleKeyResolver");
        makeContextCurrent();
        rendering = true;
        try {
            newBackendFrame();
            ImGui.getIO().setDisplaySize(w, h);
            ImGui.newFrame();

            ImGuiState.hoveringScrollableWindow = false;
            try {
                screen.render(backend, w, h, bundleKeyResolver);
            } catch (Throwable failure) {
                DeferredRelease.runAfterFailure(failure, () -> endFrame(drawData));
                throw failure;
            }
            endFrame(drawData);
        } finally {
            rendering = false;
        }
    }

    // initBackends creates fresh Dear ImGui backend objects and keeps each only after its init succeeded, so that a
    // failed initialize() releases only what exists; each shutdown hook skips an object that is not kept.
    // clearBackends drops the kept objects; the release always calls it last, even when a step threw an Error
    abstract void initBackends();
    abstract void newBackendFrame();
    abstract void shutdownRendererBackend();
    abstract void shutdownPlatformBackend();
    abstract void clearBackends();

    // Variants whose graphics device works asynchronously wait here before textures and backends are released
    void awaitDeviceIdle() {}

    private static void endFrame(Runnable drawData) {
        ImGui.render();
        drawData.run();
    }

    private void checkFontRegistration(String source, String sourceName, int size) {
        Objects.requireNonNull(source, sourceName);
        if (size <= 0) {
            throw new IllegalArgumentException("Font size must be positive: " + size);
        }
        if (context != null) {
            throw new IllegalStateException(
                    "Fonts can only be registered while the renderer is not initialized; register them before "
                            + "initializing it, or dispose it first");
        }
    }

    private ImGuiFont addFont(byte[] data, String source, int size, short[] glyphRanges) {
        ImGuiFont font = new ImGuiFont(data, source, size, glyphRanges);
        fonts.add(font);
        return font;
    }

    private static byte[] readFile(String file) throws IOException {
        byte[] data;
        try {
            data = Files.readAllBytes(Path.of(file));
        } catch (IOException e) {
            throw new IOException("Cannot read font file '" + file + "'", e);
        }
        return checkFontDataSize(data, "Font file '" + file + "'");
    }

    private static byte[] readResource(String resource) throws IOException {
        try (InputStream inputStream = ClassPathResources.open(resource)) {
            if (inputStream == null) {
                throw new IOException("Resource '" + resource + "' not found");
            }
            return checkFontDataSize(inputStream.readAllBytes(), "Font resource '" + resource + "'");
        }
    }

    private static byte[] checkFontDataSize(byte[] data, String source) throws IOException {
        if (data.length <= MAX_REJECTED_FONT_DATA_SIZE) {
            throw new IOException(source + " is empty or truncated (" + data.length + " bytes); a TrueType font has "
                    + "more than " + MAX_REJECTED_FONT_DATA_SIZE + " bytes");
        }
        return data;
    }

    private void loadFonts(ImFontAtlas atlas) {
        for (ImGuiFont font : fonts) {
            ImFontConfig config = new ImFontConfig();
            ImFont imFont;
            try {
                imFont = atlas.addFontFromMemoryTTF(font.getData(), font.getSize(), config, font.getGlyphRanges());
            } finally {
                config.destroy();
            }
            if (!imFont.isValidPtr()) {
                throw new IllegalStateException("Dear ImGui cannot load the font '" + font.getSource()
                        + "'; check that it is a valid TrueType font");
            }
            font.bind(context.ptr, imFont);
        }
    }

    private void release() {
        try {
            if (context != null) {
                ImGuiContext destroyed = context;
                DeferredRelease.runAll(
                        this::makeContextCurrent,
                        this::awaitDeviceIdle,
                        backend::unloadTextures,
                        this::shutdownRendererBackend,
                        this::shutdownPlatformBackend,
                        () -> ImGui.destroyContext(destroyed));
            }
        } finally {
            for (ImGuiFont font : fonts) {
                font.unbind();
            }
            clearBackends();
            context = null;
            ImGuiState.hoveringScrollableWindow = false;
            ImGuiState.currentContextPtr = 0L;
        }
    }
}
