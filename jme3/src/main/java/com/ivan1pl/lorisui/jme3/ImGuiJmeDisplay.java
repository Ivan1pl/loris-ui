package com.ivan1pl.lorisui.jme3;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend;
import com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer;
import com.ivan1pl.lorisui.imgui.font.ImGuiFont;
import com.jme3.input.InputManager;
import com.jme3.input.lwjgl.SdlEventListener;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.JmeContext;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.texture.FrameBuffer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Displays LorisUI screens on top of a jME viewport, using the Dear ImGui backend.
 * <p>
 * The display holds a set of named {@link LorisScreen}s and draws the current one every frame. It also resolves
 * localization keys for the screens from the resource bundles registered with {@link #setResourceBundle}. When
 * created with the application's input manager, it consumes the mouse and keyboard events that the UI uses, so that
 * they do not reach the application's own input mappings.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *     <li>Create the display, register fonts, add screens and set the current screen, then add the display to a
 *     viewport with {@link ViewPort#addProcessor}.</li>
 *     <li>jME calls {@link #initialize} lazily, on the first frame (or window resize) after the display is added to a
 *     viewport. The current screen's {@link LorisScreen#onStartScreen()} fires then.</li>
 *     <li>Switching screens with {@link #setCurrentScreen} fires {@link LorisScreen#onEndScreen()} on the old screen
 *     and {@link LorisScreen#onStartScreen()} on the new one.</li>
 *     <li>jME calls {@link #cleanup} when the display is removed from its viewport ({@link ViewPort#removeProcessor}
 *     or {@link ViewPort#clearProcessors}). The current screen's {@link LorisScreen#onEndScreen()} fires then.</li>
 *     <li>The display is single-use. Once it has been initialized and then removed from its viewport, or once its
 *     initialization has failed, it cannot be initialized again: a later frame that would initialize it fails with
 *     an {@link IllegalStateException}. To show the UI again, remove the old display first, then create a new
 *     {@code ImGuiJmeDisplay} and add it to a viewport. The old display must go first because constructing a display
 *     and registering its fonts already change Dear ImGui state (style, fonts) shared with the attached one. Each
 *     replacement leaks one Dear ImGui context plus the previous display's GL objects and SDL cursors until renderer
 *     teardown is supported.</li>
 * </ul>
 * A screen receives {@link LorisScreen#onEndScreen()} only if its {@link LorisScreen#onStartScreen()} returned
 * normally, and at most once per such start. A screen whose {@link LorisScreen#onStartScreen()} throws is treated as
 * not started, so it receives no {@link LorisScreen#onEndScreen()} for that start, not even when the display is
 * removed. An exception thrown by a hook propagates out of the method that fired it.
 * <p>
 * jME does <em>not</em> call {@link #cleanup} on application exit. Applications that want
 * {@link LorisScreen#onEndScreen()} to fire at exit remove the display themselves, for example in an overridden
 * {@code destroy()}:
 * <pre>
 * &#64;Override
 * public void destroy() {
 *     guiViewPort.removeProcessor(display);
 *     super.destroy();
 * }
 * </pre>
 *
 * <h2>Threading</h2>
 * The display is not thread-safe. Call its methods on the jME application (render) thread, for example from
 * {@code simpleInitApp()}, {@code simpleUpdate(float)}, an app state or a component callback. The screen lifecycle
 * hooks run on that thread.
 */
public class ImGuiJmeDisplay implements SceneProcessor, BundleKeyResolver {
    private final ImGuiBackend backend = new ImGuiOpenGlBackend();
    private final LwjglWindow window;
    private final SdlEventListener sdlEventListener;
    private final InputManager inputManager;
    private final ImGuiOpenGlRenderer renderer;
    private final ImGuiInputSystem inputSys;
    private final Map<String, LorisScreen> screens = new HashMap<>();
    private final Map<String, ResourceBundle> resourceBundles = new HashMap<>();
    private RenderManager renderManager;
    private ViewPort vp;
    private int w;
    private int h;
    private boolean initialized = false;
    private boolean disposed = false;
    private String currentScreen;
    private LorisScreen startedScreen;

    /**
     * Creates a display for the application's window. Call it once the window exists, for example in
     * {@code simpleInitApp()}. The display shows nothing until it is added to a viewport.
     *
     * @param context      the application's context; must not be null and must be an LWJGL3 window
     *                     ({@link LwjglWindow}), so the application must use jME's LWJGL3 renderer
     * @param inputManager the application's input manager, used to consume the mouse and keyboard events that the UI
     *                     uses; or {@code null} to leave all input events to the application
     * @param viewport     the viewport the display will be added to, whose camera size is used until the display is
     *                     initialized; must not be null
     * @throws NullPointerException if {@code context} or {@code viewport} is null
     * @throws ClassCastException   if {@code context} is not an LWJGL3 window
     */
    public ImGuiJmeDisplay(JmeContext context, InputManager inputManager, ViewPort viewport) {
        this.window = (LwjglWindow) context;
        this.inputManager = inputManager;
        this.renderer = new ImGuiOpenGlRenderer(backend, this.window.getWindowHandle());
        this.inputSys = new ImGuiInputSystem(this.renderer);
        this.sdlEventListener = event -> this.renderer.processSdlEvent(event.address());
        this.w = viewport.getCamera().getWidth();
        this.h = viewport.getCamera().getHeight();
        this.vp = viewport;
    }

    /**
     * Loads a TrueType font from a file and registers it with this display.
     * The font covers the default glyph ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to
     * {@code U+00FF}).
     * <p>
     * Fonts must be registered before the display is initialized, so register them before adding the display to a
     * viewport: jME calls {@link #initialize} on the first frame (or window resize) after that.
     *
     * @param ttfFilePath the file-system path of a TrueType ({@code .ttf}) font file; must not be null
     * @param size        the font size in pixels
     * @return the registered font, which works only with this display
     * @throws IllegalStateException if the display is already initialized, or has been cleaned up
     */
    public Font registerFontFromFile(String ttfFilePath, int size) {
        checkFontRegistrationOpen();
        return ImGuiFont.loadFromFile(ttfFilePath, size);
    }

    /**
     * Loads the given characters of a TrueType font from a file and registers it with this display.
     * <p>
     * Fonts must be registered before the display is initialized, so register them before adding the display to a
     * viewport: jME calls {@link #initialize} on the first frame (or window resize) after that.
     *
     * @param ttfFilePath the file-system path of a TrueType ({@code .ttf}) font file; must not be null
     * @param size        the font size in pixels
     * @param glyphRanges the characters to load, in Dear ImGui's zero-terminated range-pair format: pairs of first and
     *                    last code points (both inclusive), followed by a {@code 0}, for example
     *                    {@code {0x0020, 0x00FF, 0x0100, 0x017F, 0}}; must not be null
     * @return the registered font, which works only with this display
     * @throws IllegalStateException if the display is already initialized, or has been cleaned up
     */
    public Font registerFontFromFile(String ttfFilePath, int size, short[] glyphRanges) {
        checkFontRegistrationOpen();
        return ImGuiFont.loadFromFile(ttfFilePath, size, glyphRanges);
    }

    /**
     * Loads a TrueType font from a class-path resource and registers it with this display.
     * The font covers the default glyph ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to
     * {@code U+00FF}).
     * <p>
     * The path is a class-path resource name, where a leading {@code /} is optional. On the module path the resource
     * must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     * <p>
     * Fonts must be registered before the display is initialized, so register them before adding the display to a
     * viewport: jME calls {@link #initialize} on the first frame (or window resize) after that.
     *
     * @param ttfResourcePath the class-path resource name of a TrueType ({@code .ttf}) font; must not be null
     * @param size            the font size in pixels
     * @return the registered font, which works only with this display
     * @throws IllegalStateException if the display is already initialized, or has been cleaned up
     * @throws IOException           if the resource does not exist or cannot be read
     */
    public Font registerFontFromResources(String ttfResourcePath, int size) throws IOException {
        checkFontRegistrationOpen();
        return ImGuiFont.loadFromResource(ttfResourcePath, size);
    }

    /**
     * Loads the given characters of a TrueType font from a class-path resource and registers it with this display.
     * <p>
     * The path is a class-path resource name, where a leading {@code /} is optional. On the module path the resource
     * must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     * <p>
     * Fonts must be registered before the display is initialized, so register them before adding the display to a
     * viewport: jME calls {@link #initialize} on the first frame (or window resize) after that.
     *
     * @param ttfResourcePath the class-path resource name of a TrueType ({@code .ttf}) font; must not be null
     * @param size            the font size in pixels
     * @param glyphRanges     the characters to load, in Dear ImGui's zero-terminated range-pair format: pairs of first
     *                        and last code points (both inclusive), followed by a {@code 0}, for example
     *                        {@code {0x0020, 0x00FF, 0x0100, 0x017F, 0}}; must not be null
     * @return the registered font, which works only with this display
     * @throws IllegalStateException if the display is already initialized, or has been cleaned up
     * @throws IOException           if the resource does not exist or cannot be read
     */
    public Font registerFontFromResources(String ttfResourcePath, int size, short[] glyphRanges) throws IOException {
        checkFontRegistrationOpen();
        return ImGuiFont.loadFromResource(ttfResourcePath, size, glyphRanges);
    }

    /**
     * Initializes the display and fires {@link LorisScreen#onStartScreen()} on the current screen. jME calls this
     * method lazily, on the first frame (or window resize) after the display is added to a viewport; applications
     * do not call it. From then on, fonts can no longer be registered.
     * <p>
     * If the screen's {@link LorisScreen#onStartScreen()} throws, the exception propagates, the display stays
     * initialized, and the screen is treated as not started: it receives no {@link LorisScreen#onEndScreen()} for
     * this start.
     * <p>
     * If initialization fails before the screen is started, the exception propagates, no screen hook fires, and the
     * display is cleaned up: it cannot be attached again.
     *
     * @param rm the application's render manager
     * @param vp the viewport the display was added to
     * @throws IllegalStateException if the display has been cleaned up, because a display cannot be attached again
     *                               after it is removed or after its initialization failed; or if no current screen
     *                               is set, in which case call {@link #setCurrentScreen} before adding the display to
     *                               a viewport. Both are checked before the display changes any state.
     */
    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        if (disposed) {
            throw new IllegalStateException("Display has been cleaned up (removed, or its initialization failed) and "
                    + "cannot be attached again; remove it and attach a new ImGuiJmeDisplay instead");
        }
        if (currentScreen == null) {
            throw new IllegalStateException(
                    "No current screen set; call setCurrentScreen before attaching the display");
        }
        try {
            renderer.initialize();
            this.window.registerSdlEventListener(sdlEventListener);
            if (inputManager != null) {
                inputManager.addRawInputListener(inputSys);
            }
        } catch (Throwable failure) {
            // jME retries initialize every frame while isInitialized() is false; make those retries fail fast
            disposed = true;
            removeListeners();
            throw failure;
        }
        this.renderManager = rm;
        initialized = true;
        this.vp = vp;

        Camera camera = vp.getCamera();
        this.w = camera.getWidth();
        this.h = camera.getHeight();

        startCurrentScreen();
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        this.w = w;
        this.h = h;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void preFrame(float tpf) {
    }

    @Override
    public void postQueue(RenderQueue rq) {
        renderManager.setCamera(vp.getCamera(), true);
        renderer.render(screens.get(currentScreen), w, h, this);
        renderManager.setCamera(vp.getCamera(), false);
    }

    @Override
    public void postFrame(FrameBuffer out) {
    }

    /**
     * Stops the display for good and fires {@link LorisScreen#onEndScreen()} on the current screen, if that screen's
     * {@link LorisScreen#onStartScreen()} returned normally. After this call the display no longer handles input, no
     * longer accepts fonts and cannot be initialized again. To show the UI again, create a new
     * {@code ImGuiJmeDisplay} and add it to a viewport; each replacement leaks one Dear ImGui context plus the
     * previous display's GL objects and SDL cursors until renderer teardown is supported. jME calls this method when
     * the display is removed from its viewport ({@link ViewPort#removeProcessor} or {@link ViewPort#clearProcessors}),
     * but not on application exit; see the class description for the exit pattern.
     * <p>
     * Does nothing if the display is not initialized, for example when it is removed before jME initializes it, after
     * its initialization failed, or twice. A display removed before it is initialized can therefore still be added to
     * a viewport later; one whose initialization failed cannot.
     * <p>
     * If {@link LorisScreen#onEndScreen()} throws, the exception propagates, and the display is stopped all the same.
     */
    @Override
    public void cleanup() {
        if (!initialized) {
            return;
        }
        removeListeners();
        initialized = false;
        disposed = true;
        endStartedScreen();
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
    }

    /**
     * Registers a screen under a name, so that it can be selected with {@link #setCurrentScreen}. Fires no lifecycle
     * hooks. Call it on the jME application thread.
     *
     * @param name   the name of the screen, unique within this display; must not be null
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
     * Selects the screen to display. Must be called on the jME application thread.
     * <p>
     * Does nothing if {@code name} is already the current screen. While the display is not initialized, it only
     * records the choice, and the screen's {@link LorisScreen#onStartScreen()} fires when the display is initialized.
     * Once the display has been cleaned up, it only records the name and fires no hooks, because the display is never
     * initialized again. While the display is initialized, the switch is immediate: during the call,
     * {@link LorisScreen#onEndScreen()} fires on the old screen, then {@link LorisScreen#onStartScreen()} on the new
     * one. The new screen is drawn from the next frame.
     * <p>
     * The switch always completes. If the old screen's {@link LorisScreen#onEndScreen()} throws, the new screen still
     * becomes current and its {@link LorisScreen#onStartScreen()} still fires. If the new screen's
     * {@link LorisScreen#onStartScreen()} throws, the new screen stays current but is treated as not started, so it
     * receives no {@link LorisScreen#onEndScreen()} for this start. A hook's exception propagates to the caller; if
     * both hooks throw different exceptions, the exception of {@link LorisScreen#onStartScreen()} is added as
     * suppressed to the exception of {@link LorisScreen#onEndScreen()}.
     * <p>
     * Calling this method from a component callback, such as a button action, is allowed. The old screen then
     * finishes drawing the current frame after its {@link LorisScreen#onEndScreen()} has fired.
     *
     * @param name the name of a screen registered with {@link #addScreen}; must not be null
     * @throws NullPointerException  if {@code name} is null; checked before any state change or hook
     * @throws IllegalStateException if no screen is registered under {@code name}; checked before any state change or
     *                               hook
     */
    public void setCurrentScreen(String name) {
        Objects.requireNonNull(name, "name");
        if (!screens.containsKey(name)) {
            throw new IllegalStateException("No screen registered under name: " + name);
        }
        if (name.equals(currentScreen)) {
            return;
        }
        this.currentScreen = name;
        if (!initialized) {
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
     * Loads a resource bundle for the default locale and registers it under a name, for use by
     * {@link #resolveResourceKey}. A bundle already registered under the same name is replaced.
     * <p>
     * On the module path, a bundle in a named application module must be outside any package of that module, or in
     * a package that the module opens unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is
     * not enough.
     *
     * @param name the bundle name, used as the first part of localization keys; must not be null
     * @param path the base name of the bundle, as accepted by {@link ResourceBundle#getBundle(String)}, for example
     *             {@code "bundles.global"}; must not be null
     * @throws NullPointerException               if {@code name} or {@code path} is null
     * @throws java.util.MissingResourceException if no bundle is found for {@code path}
     */
    public void setResourceBundle(String name, String path) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(path, "path");
        resourceBundles.put(name, ResourceBundle.getBundle(path));
    }

    /**
     * Returns the localized text for a key from the resource bundles registered with {@link #setResourceBundle}.
     * <p>
     * The key has the form {@code bundleName.key}: the part before the first dot names a registered bundle, and the
     * rest is the key within that bundle. For example, {@code "global.menu.start"} looks up {@code menu.start} in the
     * bundle registered as {@code global}. When arguments are given, the text is formatted with
     * {@link String#formatted(Object...)}; otherwise it is returned as is.
     *
     * @param key  the key in the form {@code bundleName.key}, where {@code bundleName} is registered; must not be
     *             null
     * @param args format arguments for the text; empty when the text is used as is
     * @return the localized text
     * @throws NullPointerException               if {@code key} is null
     * @throws IllegalArgumentException           if {@code key} contains no dot, so it names no bundle
     * @throws IllegalStateException              if no bundle is registered under the bundle name of {@code key}
     * @throws java.util.MissingResourceException if the bundle has no text for the key
     */
    @Override
    public String resolveResourceKey(String key, Object... args) {
        Objects.requireNonNull(key, "key");
        int separator = key.indexOf('.');
        if (separator < 0) {
            throw new IllegalArgumentException("Localization key has no bundle name, expected bundleName.key: " + key);
        }
        String bundleName = key.substring(0, separator);
        ResourceBundle bundle = resourceBundles.get(bundleName);
        if (bundle == null) {
            throw new IllegalStateException(
                    "No resource bundle registered under name: " + bundleName + " (localization key: " + key + ")");
        }
        String result = bundle.getString(key.substring(separator + 1));
        if (args.length > 0) {
            result = result.formatted(args);
        }
        return result;
    }

    private void checkFontRegistrationOpen() {
        if (disposed) {
            throw new IllegalStateException("Display has been cleaned up and no longer accepts fonts; register them "
                    + "with a new ImGuiJmeDisplay instead");
        }
        if (initialized) {
            throw new IllegalStateException(
                    "Fonts can only be registered before the display is initialized; register them before adding "
                            + "the display to a viewport");
        }
    }

    // Removing a listener that was never registered is a no-op in jME
    private void removeListeners() {
        if (inputManager != null) {
            inputManager.removeRawInputListener(inputSys);
        }
        this.window.removeSdlEventListener(sdlEventListener);
    }

    private void startCurrentScreen() {
        LorisScreen screen = screens.get(currentScreen);
        screen.onStartScreen();
        startedScreen = screen;
    }

    // Forgets the started screen before its hook runs, so a throwing onEndScreen() is never repeated
    private void endStartedScreen() {
        LorisScreen screen = startedScreen;
        startedScreen = null;
        if (screen != null) {
            screen.onEndScreen();
        }
    }
}
