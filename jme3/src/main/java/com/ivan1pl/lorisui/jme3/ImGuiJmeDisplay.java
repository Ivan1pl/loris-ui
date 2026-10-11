package com.ivan1pl.lorisui.jme3;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.resolver.ResourceBundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.api.screen.ScreenManager;
import com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer;
import com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;
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
import java.util.Objects;

/**
 * Displays LorisUI screens on top of a jME viewport, using the Dear ImGui backend.
 * <p>
 * The display holds a set of named {@link LorisScreen}s and draws the current one every frame. It resolves the
 * screens' localization keys from the resource bundles registered with {@link #setResourceBundle}. When created with
 * the application's input manager, it consumes the mouse and keyboard events that the UI uses, so that they do not
 * reach the application's own input mappings.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *     <li>Create the display; register fonts, custom renderers and style appliers, and resource bundles; add screens
 *     and set the current screen. Then add the display to a viewport with {@link ViewPort#addProcessor}. Creating a
 *     display does not affect a display that is already attached. Register only fonts from trusted sources, as
 *     described for {@link ImGuiOpenGlRenderer#registerFontFromFile(String, int)}.</li>
 *     <li>jME calls {@link #initialize} lazily, on the first frame (or window resize) after the display is added to a
 *     viewport. The display then sets up Dear ImGui with the registered fonts, and the current screen's
 *     {@link LorisScreen#onStartScreen()} fires.</li>
 *     <li>While the display is initialized, switching screens with {@link #setCurrentScreen} fires
 *     {@link LorisScreen#onEndScreen()} on the old screen and {@link LorisScreen#onStartScreen()} on the new one.
 *     Before jME initializes the display, and from the moment it is removed, even while its release is still pending,
 *     {@link #setCurrentScreen} only records the choice, and the recorded screen starts when the display is
 *     initialized.</li>
 *     <li>jME calls {@link #cleanup} when the display is removed from its viewport ({@link ViewPort#removeProcessor}
 *     or {@link ViewPort#clearProcessors}). The current screen's {@link LorisScreen#onEndScreen()} fires, the display
 *     releases its Dear ImGui, OpenGL and SDL resources, including every texture it loaded, and the background work
 *     that computes screen layouts stops until a screen is drawn again.</li>
 *     <li>A removed display can be added to a viewport again. It then sets up Dear ImGui again, restores the
 *     registered fonts (the {@link Font} instances returned at registration stay valid) and starts the current screen
 *     again. Screens, resource bundles, renderers and style appliers stay registered. Dear ImGui UI state, such as
 *     selected tabs, scroll positions, open popups and keyboard focus, starts fresh; state held by LorisUI components
 *     and their bindings is not affected.</li>
 * </ul>
 * Removing the display from a component callback, such as a button action, or from a screen's
 * {@link LorisScreen#onStartScreen()} or {@link LorisScreen#onEndScreen()} is allowed. The display stops handling
 * input at once and {@link #isInitialized()} returns {@code false}, but the release completes only when the current
 * frame or screen switch has finished. Adding the display to a viewport again in the same callback is safe, because
 * jME initializes it on a later frame.
 * <p>
 * A screen receives {@link LorisScreen#onEndScreen()} only if its {@link LorisScreen#onStartScreen()} returned
 * normally, and at most once per such start. A screen whose {@link LorisScreen#onStartScreen()} throws is treated as
 * not started, so it receives no {@link LorisScreen#onEndScreen()} for that start, not even when the display is
 * removed. An exception thrown by a hook propagates out of the method that fired it. The hooks must not call
 * {@link #setCurrentScreen}; switch screens later, for example from a component callback.
 * <p>
 * If initialization fails, the display releases what it had set up and becomes unusable: a later frame that would
 * initialize it fails with an {@link IllegalStateException}. Remove it and attach a new {@code ImGuiJmeDisplay}
 * instead.
 * <p>
 * Only one display may be attached at a time. While attached, the display leaves its Dear ImGui context current; an
 * application that uses a Dear ImGui context of its own must make that context current again after each frame.
 * <p>
 * jME does <em>not</em> call {@link #cleanup} on application exit. Applications remove the display themselves, for
 * example in an overridden {@code destroy()}, so that {@link LorisScreen#onEndScreen()} fires, the Dear ImGui, OpenGL
 * and SDL resources are released while the window still exists, and the layout work stops:
 * <pre>
 * &#64;Override
 * public void destroy() {
 *     guiViewPort.removeProcessor(display);
 *     super.destroy();
 * }
 * </pre>
 *
 * <h2>Known limitation</h2>
 * The display sizes the UI by the viewport's camera, which is measured in pixels, while Dear ImGui expects logical
 * window coordinates. On HiDPI displays with a scale factor above 1, the UI is therefore scaled incorrectly.
 *
 * <h2>Threading</h2>
 * The display is not thread-safe. Call its methods on the jME application (render) thread, for example from
 * {@code simpleInitApp()}, {@code simpleUpdate(float)}, an app state or a component callback. The screen lifecycle
 * hooks run on that thread.
 */
public class ImGuiJmeDisplay implements SceneProcessor {
    private final ImGuiOpenGlBackend backend = new ImGuiOpenGlBackend();
    private final ScreenManager screenManager = new ScreenManager();
    private final ResourceBundleKeyResolver resolver = new ResourceBundleKeyResolver();
    private final LwjglWindow window;
    private final InputManager inputManager;
    private final ImGuiOpenGlRenderer renderer;
    private final ImGuiInputSystem inputSys;
    private final SdlEventListener sdlEventListener;
    // Removal requested during a frame, a screen switch or a hook is deferred until the outermost one returns, so the
    // renderer is never disposed inside a frame and no hook fires inside a hook
    private final DeferredRelease deferredTeardown = new DeferredRelease(this::tearDown);
    private RenderManager renderManager;
    private ViewPort vp;
    private int w;
    private int h;
    private boolean initialized = false;
    private boolean failed = false;
    // True while tearDown runs, when the release is no longer pending but onEndScreen can still call the display
    private boolean releasing = false;

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
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(viewport, "viewport");
        this.window = (LwjglWindow) context;
        this.inputManager = inputManager;
        this.renderer = new ImGuiOpenGlRenderer(backend, window.getWindowHandle());
        this.inputSys = new ImGuiInputSystem(renderer);
        this.sdlEventListener = event -> renderer.processSdlEvent(event.address());
        this.vp = viewport;
        this.w = viewport.getCamera().getWidth();
        this.h = viewport.getCamera().getHeight();
    }

    /**
     * Reads a TrueType font from a file and registers it with this display. The font covers the default glyph
     * ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to {@code U+00FF}). The file is read during this
     * call.
     * <p>
     * Fonts must be registered while the display is not attached, so register them before adding the display to a
     * viewport: jME calls {@link #initialize} on the first frame (or window resize) after that.
     *
     * @param ttfFilePath the file-system path of a TrueType ({@code .ttf}) font file; must not be null
     * @param size        the font size in pixels; must be positive
     * @return the registered font, which works only with this display and stays valid when the display is attached
     *         again
     * @throws NullPointerException     if {@code ttfFilePath} is null
     * @throws IllegalArgumentException if {@code size} is not positive
     * @throws IllegalStateException    if the display is attached, is still being released after its removal, or its
     *                                  initialization failed
     * @throws IOException              if the file cannot be read, or is too short to be a font (100 bytes or less)
     */
    public Font registerFontFromFile(String ttfFilePath, int size) throws IOException {
        checkRegistrationOpen();
        return renderer.registerFontFromFile(ttfFilePath, size);
    }

    /**
     * Reads the given characters of a TrueType font from a file and registers it with this display. The file is read
     * during this call.
     * <p>
     * Fonts must be registered while the display is not attached, so register them before adding the display to a
     * viewport: jME calls {@link #initialize} on the first frame (or window resize) after that.
     *
     * @param ttfFilePath the file-system path of a TrueType ({@code .ttf}) font file; must not be null
     * @param size        the font size in pixels; must be positive
     * @param glyphRanges the characters to load, in Dear ImGui's zero-terminated range-pair format: pairs of first and
     *                    last code points (both inclusive), followed by a {@code 0}, for example
     *                    {@code {0x0020, 0x00FF, 0x0100, 0x017F, 0}}; code points above {@code U+7FFF} are written
     *                    with a cast, for example {@code (short) 0x9FAF}; must not be null
     * @return the registered font, which works only with this display and stays valid when the display is attached
     *         again
     * @throws NullPointerException     if {@code ttfFilePath} or {@code glyphRanges} is null
     * @throws IllegalArgumentException if {@code size} is not positive, or if {@code glyphRanges} has no range, is not
     *                                  zero-terminated or has a range whose first code point is above its last
     * @throws IllegalStateException    if the display is attached, is still being released after its removal, or its
     *                                  initialization failed
     * @throws IOException              if the file cannot be read, or is too short to be a font (100 bytes or less)
     */
    public Font registerFontFromFile(String ttfFilePath, int size, short[] glyphRanges) throws IOException {
        checkRegistrationOpen();
        return renderer.registerFontFromFile(ttfFilePath, size, glyphRanges);
    }

    /**
     * Reads a TrueType font from a class-path resource and registers it with this display. The font covers the
     * default glyph ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to {@code U+00FF}). The resource is
     * read during this call.
     * <p>
     * The path is a class-path resource name, where a leading {@code /} is optional. On the module path the resource
     * must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     * <p>
     * Fonts must be registered while the display is not attached, so register them before adding the display to a
     * viewport: jME calls {@link #initialize} on the first frame (or window resize) after that.
     *
     * @param ttfResourcePath the class-path resource name of a TrueType ({@code .ttf}) font; must not be null
     * @param size            the font size in pixels; must be positive
     * @return the registered font, which works only with this display and stays valid when the display is attached
     *         again
     * @throws NullPointerException     if {@code ttfResourcePath} is null
     * @throws IllegalArgumentException if {@code size} is not positive
     * @throws IllegalStateException    if the display is attached, is still being released after its removal, or its
     *                                  initialization failed
     * @throws IOException              if the resource does not exist, cannot be read, or is too short to be a font
     *                                  (100 bytes or less)
     */
    public Font registerFontFromResources(String ttfResourcePath, int size) throws IOException {
        checkRegistrationOpen();
        return renderer.registerFontFromResources(ttfResourcePath, size);
    }

    /**
     * Reads the given characters of a TrueType font from a class-path resource and registers it with this display.
     * The resource is read during this call.
     * <p>
     * The path is a class-path resource name, where a leading {@code /} is optional. On the module path the resource
     * must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     * <p>
     * Fonts must be registered while the display is not attached, so register them before adding the display to a
     * viewport: jME calls {@link #initialize} on the first frame (or window resize) after that.
     *
     * @param ttfResourcePath the class-path resource name of a TrueType ({@code .ttf}) font; must not be null
     * @param size            the font size in pixels; must be positive
     * @param glyphRanges     the characters to load, in Dear ImGui's zero-terminated range-pair format: pairs of first
     *                        and last code points (both inclusive), followed by a {@code 0}, for example
     *                        {@code {0x0020, 0x00FF, 0x0100, 0x017F, 0}}; code points above {@code U+7FFF} are
     *                        written with a cast, for example {@code (short) 0x9FAF}; must not be null
     * @return the registered font, which works only with this display and stays valid when the display is attached
     *         again
     * @throws NullPointerException     if {@code ttfResourcePath} or {@code glyphRanges} is null
     * @throws IllegalArgumentException if {@code size} is not positive, or if {@code glyphRanges} has no range, is not
     *                                  zero-terminated or has a range whose first code point is above its last
     * @throws IllegalStateException    if the display is attached, is still being released after its removal, or its
     *                                  initialization failed
     * @throws IOException              if the resource does not exist, cannot be read, or is too short to be a font
     *                                  (100 bytes or less)
     */
    public Font registerFontFromResources(String ttfResourcePath, int size, short[] glyphRanges) throws IOException {
        checkRegistrationOpen();
        return renderer.registerFontFromResources(ttfResourcePath, size, glyphRanges);
    }

    /**
     * Registers the renderer that draws components of the given class, replacing any renderer already registered for
     * that class, including a built-in one. Components of a subclass without a registration of its own are drawn by
     * the renderer of their nearest registered ancestor. The registration stays in effect when the display is attached
     * again.
     * <p>
     * Renderers must be registered while the display is not attached, so register them before adding the display to
     * a viewport. For a generic component type, the class argument needs an unchecked cast, for example
     * {@code (Class<ComboBox<String>>) (Class<?>) ComboBox.class}.
     *
     * @param componentClass  class of the components to draw; must not be null and must not be an interface
     * @param renderer        renderer for the components; must not be null
     * @param <ComponentType> type of the components
     * @throws NullPointerException     if {@code componentClass} or {@code renderer} is null
     * @throws IllegalArgumentException if {@code componentClass} is an interface, because only superclasses are
     *                                  searched for a registration
     * @throws IllegalStateException    if the display is attached, is still being released after its removal, or its
     *                                  initialization failed
     */
    public <ComponentType extends Component> void registerRenderer(Class<ComponentType> componentClass,
                                                                   Renderer<? super ComponentType> renderer) {
        checkRegistrationOpen();
        backend.registerRenderer(componentClass, renderer);
    }

    /**
     * Registers the style applier that applies styles of the given class, replacing any style applier already
     * registered for that class, including a built-in one. Styles of a subclass without a registration of its own
     * are applied by the style applier of their nearest registered ancestor. The registration stays in effect when
     * the display is attached again.
     * <p>
     * Style appliers must be registered while the display is not attached, so register them before adding the
     * display to a viewport. For a generic style type, the class argument needs an unchecked cast, as described for
     * {@link #registerRenderer}.
     *
     * @param styleClass   class of the styles to apply; must not be null and must not be an interface
     * @param styleApplier style applier for the styles; must not be null
     * @param <StyleType>  type of the styles
     * @throws NullPointerException     if {@code styleClass} or {@code styleApplier} is null
     * @throws IllegalArgumentException if {@code styleClass} is an interface, because only superclasses are searched
     *                                  for a registration
     * @throws IllegalStateException    if the display is attached, is still being released after its removal, or its
     *                                  initialization failed
     */
    public <StyleType extends Style> void registerStyleApplier(Class<StyleType> styleClass,
                                                               StyleApplier<? super StyleType> styleApplier) {
        checkRegistrationOpen();
        backend.registerStyleApplier(styleClass, styleApplier);
    }

    /**
     * Initializes the display and fires {@link LorisScreen#onStartScreen()} on the current screen. jME calls this
     * method lazily, on the first frame (or window resize) after the display is added to a viewport; applications
     * do not call it. From then on until the display is removed, fonts, renderers and style appliers can no longer be
     * registered.
     * <p>
     * If the screen's {@link LorisScreen#onStartScreen()} throws, the exception propagates, the display stays
     * initialized, and the screen is treated as not started: it receives no {@link LorisScreen#onEndScreen()} for
     * this start.
     * <p>
     * If initialization fails before the screen is started, the exception propagates, no screen hook fires, and the
     * display releases what it had set up and becomes unusable: it cannot be attached again.
     *
     * @param rm the application's render manager
     * @param vp the viewport the display was added to
     * @throws IllegalStateException if an earlier initialization of the display failed; if the display is already
     *                               initialized or is still being released after its removal; or if no current
     *                               screen is set, in which case call {@link #setCurrentScreen} before adding the
     *                               display to a viewport. All are checked before the display changes any state.
     *                               Also if a registered font cannot be loaded, for example because its data is not
     *                               a valid TrueType font; the display then becomes unusable as described above.
     */
    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        if (failed) {
            throw new IllegalStateException("Display initialization failed earlier and the display cannot be attached "
                    + "again; remove it and attach a new ImGuiJmeDisplay instead");
        }
        if (initialized || isBeingReleased()) {
            throw new IllegalStateException("Display is already initialized, or its release after removal has not "
                    + "completed yet");
        }
        if (screenManager.getCurrentScreen() == null) {
            throw new IllegalStateException(
                    "No current screen set; call setCurrentScreen before attaching the display");
        }
        try {
            renderer.initialize();
            window.registerSdlEventListener(sdlEventListener);
            if (inputManager != null) {
                inputManager.addRawInputListener(inputSys);
            }
        } catch (Throwable failure) {
            // jME retries initialize every frame while isInitialized() is false; make those retries fail fast
            failed = true;
            DeferredRelease.runAfterFailure(failure,
                    () -> DeferredRelease.runAll(this::removeListeners, renderer::dispose));
            throw failure;
        }
        this.renderManager = rm;
        this.vp = vp;
        Camera camera = vp.getCamera();
        this.w = camera.getWidth();
        this.h = camera.getHeight();
        initialized = true;
        deferredTeardown.run(screenManager::activate);
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        this.w = w;
        this.h = h;
    }

    /**
     * Returns whether the display is initialized, that is attached to a viewport and set up by jME. Returns
     * {@code false} from the moment the display is removed, even when its release completes only after the current
     * frame or screen switch.
     *
     * @return {@code true} if the display is initialized
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void preFrame(float tpf) {
    }

    /**
     * Draws the current screen. jME calls this method every frame while the display is attached; applications do not
     * call it. Does nothing if the display is not initialized.
     * <p>
     * If the display is removed during the frame, for example from a component callback, the release completes when
     * the frame has been drawn; a failure of the release then propagates from this method.
     *
     * @param rq the viewport's render queue
     * @throws IllegalStateException if the screen's most recent layout failed (see {@link LorisScreen#render}), or if
     *                               a component or style of the screen has no registered renderer or style applier
     */
    @Override
    public void postQueue(RenderQueue rq) {
        if (!initialized) {
            return;
        }
        deferredTeardown.run(this::drawCurrentScreen);
    }

    @Override
    public void postFrame(FrameBuffer out) {
    }

    /**
     * Stops the display and fires {@link LorisScreen#onEndScreen()} on the current screen, if that screen's
     * {@link LorisScreen#onStartScreen()} returned normally. Then releases the display's Dear ImGui, OpenGL and SDL
     * resources, including every texture it loaded, and stops the background work that computes screen layouts until
     * a screen is drawn again. jME calls this method when the display is removed from its viewport
     * ({@link ViewPort#removeProcessor} or {@link ViewPort#clearProcessors}), but not on application exit; see the
     * class description for the exit pattern.
     * <p>
     * Afterwards the display can be added to a viewport again; fonts, renderers and style appliers can be registered
     * again until then.
     * <p>
     * When called during a frame, for example from a component callback, or during a screen switch or a screen's
     * lifecycle hook, the display stops handling input at once, and the rest of the release, including
     * {@link LorisScreen#onEndScreen()}, runs when that frame or switch has finished. The end hook then fires on the
     * screen that was started when the display was removed; a screen selected with {@link #setCurrentScreen} after
     * that is only recorded, and starts when the display is attached again.
     * <p>
     * Does nothing if the display is not initialized, for example when it is removed before jME initializes it, after
     * its initialization failed, or twice.
     * <p>
     * Every release step runs even when an earlier one fails. If {@link LorisScreen#onEndScreen()} or a release step
     * throws, the first exception propagates after all steps have run, and the display is stopped all the same.
     *
     * @throws RuntimeException the first failure of {@link LorisScreen#onEndScreen()} or of a release step; failures
     *                          of later steps are added to it as suppressed exceptions
     */
    @Override
    public void cleanup() {
        if (!initialized) {
            return;
        }
        initialized = false;
        screenManager.beginDeactivation();
        boolean deferred = deferredTeardown.isRunning();
        deferredTeardown.request();
        if (deferred) {
            removeListeners();
        }
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
    }

    /**
     * Registers a screen under a name, so that it can be selected with {@link #setCurrentScreen}. Fires no lifecycle
     * hooks, and may be called from inside a hook. Call it on the jME application thread.
     *
     * @param name   the name of the screen, unique within this display; must not be null
     * @param screen the screen; must not be null
     * @throws NullPointerException  if {@code name} or {@code screen} is null
     * @throws IllegalStateException if a screen is already registered under {@code name}
     */
    public void addScreen(String name, LorisScreen screen) {
        screenManager.addScreen(name, screen);
    }

    /**
     * Selects the screen to display. Must be called on the jME application thread.
     * <p>
     * Does nothing if {@code name} is already the current screen. While the display is not initialized, it only
     * records the choice and fires no hook, and the screen's {@link LorisScreen#onStartScreen()} fires when the
     * display is initialized. This includes the time after the display is removed during a frame, a screen switch or
     * a hook, while its release is still pending. While the display is initialized, the switch is immediate: during
     * the call, {@link LorisScreen#onEndScreen()} fires on the old screen, then {@link LorisScreen#onStartScreen()} on
     * the new one. The new screen is drawn from the next frame.
     * <p>
     * The switch always completes. If the old screen's {@link LorisScreen#onEndScreen()} throws, the new screen still
     * becomes current and its {@link LorisScreen#onStartScreen()} still fires. If the new screen's
     * {@link LorisScreen#onStartScreen()} throws, the new screen stays current but is treated as not started, so it
     * receives no {@link LorisScreen#onEndScreen()} for this start. A hook's exception propagates to the caller; if
     * both hooks throw different exceptions, the exception of {@link LorisScreen#onStartScreen()} is added as
     * suppressed to the exception of {@link LorisScreen#onEndScreen()}.
     * <p>
     * Calling this method from a component callback, such as a button action, is allowed. The old screen then
     * finishes drawing the current frame after its {@link LorisScreen#onEndScreen()} has fired. If a hook removes the
     * display during the switch, the release completes when the switch has finished, and a failure of the release
     * then propagates from this method.
     *
     * @param name the name of a screen registered with {@link #addScreen}; must not be null
     * @throws NullPointerException  if {@code name} is null; checked before any state change or hook
     * @throws IllegalStateException if no screen is registered under {@code name}, or if called from inside
     *                               {@link LorisScreen#onStartScreen()} or {@link LorisScreen#onEndScreen()}; checked
     *                               before any state change or hook
     */
    public void setCurrentScreen(String name) {
        deferredTeardown.run(() -> screenManager.setCurrentScreen(name));
    }

    /**
     * Loads a resource bundle for the default locale and registers it under a name, for resolving the screens'
     * localization keys. A key has the form {@code bundleName.key}: the part before the first dot names a registered
     * bundle, and the rest is the key within that bundle. A bundle already registered under the same name is
     * replaced.
     * <p>
     * On the module path, a bundle in a named application module must be outside any package of that module, or in
     * a package that the module opens unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is
     * not enough.
     *
     * @param name the bundle name, used as the first part of localization keys; must not be null
     * @param path the base name of the bundle, as accepted by {@link java.util.ResourceBundle#getBundle(String)}, for
     *             example {@code "bundles.global"}; must not be null
     * @throws NullPointerException               if {@code name} or {@code path} is null
     * @throws java.util.MissingResourceException if no bundle is found for {@code path}
     */
    public void setResourceBundle(String name, String path) {
        resolver.setResourceBundle(name, path);
    }

    private void checkRegistrationOpen() {
        if (failed) {
            throw new IllegalStateException("Display initialization failed and the display can no longer be used; "
                    + "create a new ImGuiJmeDisplay instead");
        }
        if (initialized || isBeingReleased()) {
            throw new IllegalStateException("Fonts, renderers and style appliers can only be registered while the "
                    + "display is not attached; register them before adding the display to a viewport");
        }
    }

    private boolean isBeingReleased() {
        return releasing || deferredTeardown.isPending();
    }

    private void drawCurrentScreen() {
        Camera camera = vp.getCamera();
        renderManager.setCamera(camera, true);
        try {
            renderer.render(screenManager.getCurrentScreen(), w, h, resolver);
        } finally {
            renderManager.setCamera(camera, false);
        }
    }

    private void tearDown() {
        releasing = true;
        try {
            DeferredRelease.runAll(
                    this::removeListeners,
                    screenManager::deactivate,
                    renderer::dispose,
                    LorisScreen::shutdownLayoutUpdates);
        } finally {
            releasing = false;
        }
    }

    // Removing a listener that was never registered is a no-op in jME
    private void removeListeners() {
        if (inputManager != null) {
            inputManager.removeRawInputListener(inputSys);
        }
        window.removeSdlEventListener(sdlEventListener);
    }
}
