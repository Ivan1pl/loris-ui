package com.ivan1pl.lorisui.lwjgl;

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
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDL_Event;

import java.io.IOException;

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
 *     {@link ImGuiOpenGlRenderer#registerFontFromFile(String, int)}.</li>
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
 * Initializing the display sets the global SDL hints listed in {@link ImGuiOpenGlRenderer#initialize()}; they affect
 * every SDL window of the application and stay set after the display is closed.
 *
 * <h2>Threading</h2>
 * The display is not thread-safe. Create it and call all of its methods on the thread that owns the window and its
 * OpenGL context, with that context current. The screen lifecycle hooks and component callbacks run on that thread.
 * On macOS, that is the process's main thread, so the JVM may need {@code -XstartOnFirstThread}.
 */
public final class ImGuiLwjglDisplay implements AutoCloseable {
    private final ImGuiOpenGlBackend backend = new ImGuiOpenGlBackend();
    private final ScreenManager screenManager = new ScreenManager();
    private final ResourceBundleKeyResolver resolver = new ResourceBundleKeyResolver();
    private final long sdlWindow;
    private final ImGuiOpenGlRenderer renderer;
    // Closing during a frame, a screen switch or a hook is deferred until the outermost one returns, so the renderer
    // is never disposed inside a frame and no hook fires inside a hook
    private final DeferredRelease deferredRelease = new DeferredRelease(this::release);
    private boolean initialized = false;
    private boolean textInputStarted = false;
    private boolean closed = false;

    /**
     * Creates a display for an SDL window. Call it on the thread that owns the window. The display shows nothing until
     * it is {@linkplain #initialize() initialized}, and the constructor does not touch Dear ImGui, OpenGL or SDL.
     *
     * @param sdlWindow the native handle of the application's SDL window ({@code SDL_Window*}), which must have an
     *                  OpenGL context; must not be {@code 0}
     * @throws IllegalArgumentException if {@code sdlWindow} is {@code 0}
     */
    public ImGuiLwjglDisplay(long sdlWindow) {
        if (sdlWindow == 0) {
            throw new IllegalArgumentException("sdlWindow must be the handle of an SDL window, not 0");
        }
        this.sdlWindow = sdlWindow;
        this.renderer = new ImGuiOpenGlRenderer(backend, sdlWindow);
    }

    /**
     * Reads a TrueType font from a file and registers it with this display. The font covers the default glyph
     * ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to {@code U+00FF}). The file is read during this
     * call. Fonts must be registered before the display is initialized.
     *
     * @param ttfFilePath the file-system path of a TrueType ({@code .ttf}) font file; must not be null
     * @param size        the font size in pixels; must be positive
     * @return the registered font, which works only with this display
     * @throws NullPointerException     if {@code ttfFilePath} is null
     * @throws IllegalArgumentException if {@code size} is not positive
     * @throws IllegalStateException    if the display is initialized or closed
     * @throws IOException              if the file cannot be read, or is too short to be a font (100 bytes or less)
     */
    public Font registerFontFromFile(String ttfFilePath, int size) throws IOException {
        checkRegistrationOpen();
        return renderer.registerFontFromFile(ttfFilePath, size);
    }

    /**
     * Reads the given characters of a TrueType font from a file and registers it with this display. The file is read
     * during this call. Fonts must be registered before the display is initialized.
     *
     * @param ttfFilePath the file-system path of a TrueType ({@code .ttf}) font file; must not be null
     * @param size        the font size in pixels; must be positive
     * @param glyphRanges the characters to load, in Dear ImGui's zero-terminated range-pair format: pairs of first and
     *                    last code points (both inclusive), followed by a {@code 0}, for example
     *                    {@code {0x0020, 0x00FF, 0x0100, 0x017F, 0}}; code points above {@code U+7FFF} are written
     *                    with a cast, for example {@code (short) 0x9FAF}; must not be null
     * @return the registered font, which works only with this display
     * @throws NullPointerException     if {@code ttfFilePath} or {@code glyphRanges} is null
     * @throws IllegalArgumentException if {@code size} is not positive, or if {@code glyphRanges} has no range, is not
     *                                  zero-terminated or has a range whose first code point is above its last
     * @throws IllegalStateException    if the display is initialized or closed
     * @throws IOException              if the file cannot be read, or is too short to be a font (100 bytes or less)
     */
    public Font registerFontFromFile(String ttfFilePath, int size, short[] glyphRanges) throws IOException {
        checkRegistrationOpen();
        return renderer.registerFontFromFile(ttfFilePath, size, glyphRanges);
    }

    /**
     * Reads a TrueType font from a class-path resource and registers it with this display. The font covers the
     * default glyph ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to {@code U+00FF}). The resource is
     * read during this call. Fonts must be registered before the display is initialized.
     * <p>
     * The path is a class-path resource name, where a leading {@code /} is optional. On the module path the resource
     * must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * @param ttfResourcePath the class-path resource name of a TrueType ({@code .ttf}) font; must not be null
     * @param size            the font size in pixels; must be positive
     * @return the registered font, which works only with this display
     * @throws NullPointerException     if {@code ttfResourcePath} is null
     * @throws IllegalArgumentException if {@code size} is not positive
     * @throws IllegalStateException    if the display is initialized or closed
     * @throws IOException              if the resource does not exist, cannot be read, or is too short to be a font
     *                                  (100 bytes or less)
     */
    public Font registerFontFromResources(String ttfResourcePath, int size) throws IOException {
        checkRegistrationOpen();
        return renderer.registerFontFromResources(ttfResourcePath, size);
    }

    /**
     * Reads the given characters of a TrueType font from a class-path resource and registers it with this display.
     * The resource is read during this call. Fonts must be registered before the display is initialized.
     * <p>
     * The path is a class-path resource name, where a leading {@code /} is optional. On the module path the resource
     * must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * @param ttfResourcePath the class-path resource name of a TrueType ({@code .ttf}) font; must not be null
     * @param size            the font size in pixels; must be positive
     * @param glyphRanges     the characters to load, in Dear ImGui's zero-terminated range-pair format: pairs of first
     *                        and last code points (both inclusive), followed by a {@code 0}, for example
     *                        {@code {0x0020, 0x00FF, 0x0100, 0x017F, 0}}; code points above {@code U+7FFF} are
     *                        written with a cast, for example {@code (short) 0x9FAF}; must not be null
     * @return the registered font, which works only with this display
     * @throws NullPointerException     if {@code ttfResourcePath} or {@code glyphRanges} is null
     * @throws IllegalArgumentException if {@code size} is not positive, or if {@code glyphRanges} has no range, is not
     *                                  zero-terminated or has a range whose first code point is above its last
     * @throws IllegalStateException    if the display is initialized or closed
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
     * the renderer of their nearest registered ancestor.
     * <p>
     * Renderers must be registered before the display is initialized. For a generic component type, the class
     * argument needs an unchecked cast, for example {@code (Class<ComboBox<String>>) (Class<?>) ComboBox.class}.
     *
     * @param componentClass  class of the components to draw; must not be null and must not be an interface
     * @param renderer        renderer for the components; must not be null
     * @param <ComponentType> type of the components
     * @throws NullPointerException     if {@code componentClass} or {@code renderer} is null
     * @throws IllegalArgumentException if {@code componentClass} is an interface, because only superclasses are
     *                                  searched for a registration
     * @throws IllegalStateException    if the display is initialized or closed
     */
    public <ComponentType extends Component> void registerRenderer(Class<ComponentType> componentClass,
                                                                   Renderer<? super ComponentType> renderer) {
        checkRegistrationOpen();
        backend.registerRenderer(componentClass, renderer);
    }

    /**
     * Registers the style applier that applies styles of the given class, replacing any style applier already
     * registered for that class, including a built-in one. Styles of a subclass without a registration of its own
     * are applied by the style applier of their nearest registered ancestor.
     * <p>
     * Style appliers must be registered before the display is initialized. For a generic style type, the class
     * argument needs an unchecked cast, as described for {@link #registerRenderer}.
     *
     * @param styleClass   class of the styles to apply; must not be null and must not be an interface
     * @param styleApplier style applier for the styles; must not be null
     * @param <StyleType>  type of the styles
     * @throws NullPointerException     if {@code styleClass} or {@code styleApplier} is null
     * @throws IllegalArgumentException if {@code styleClass} is an interface, because only superclasses are searched
     *                                  for a registration
     * @throws IllegalStateException    if the display is initialized or closed
     */
    public <StyleType extends Style> void registerStyleApplier(Class<StyleType> styleClass,
                                                               StyleApplier<? super StyleType> styleApplier) {
        checkRegistrationOpen();
        backend.registerStyleApplier(styleClass, styleApplier);
    }

    /**
     * Registers a screen under a name, so that it can be selected with {@link #setCurrentScreen}. Fires no lifecycle
     * hooks, and may be called in any state of the display, including from inside a hook and after {@link #close()}.
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
     * Selects the screen to display.
     * <p>
     * Does nothing if {@code name} is already the current screen. While the display is not initialized, it only
     * records the choice and fires no hook; the screen's {@link LorisScreen#onStartScreen()} fires when the display
     * is initialized. This includes the time after {@link #close()}, also while the release of a display closed
     * during a frame, a screen switch or a hook is still pending; a closed display starts no screen again. While the
     * display is initialized, the switch is immediate: during the call,
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
     * finishes drawing the current frame after its {@link LorisScreen#onEndScreen()} has fired. If a hook closes the
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
        deferredRelease.run(() -> screenManager.setCurrentScreen(name));
    }

    /**
     * Loads a resource bundle for the default locale and registers it under a name, for resolving the screens'
     * localization keys. A key has the form {@code bundleName.key}: the part before the first dot names a registered
     * bundle, and the rest is the key within that bundle. A bundle already registered under the same name is
     * replaced. May be called in any state of the display.
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
        checkNotClosed();
        if (initialized) {
            throw new IllegalStateException("Display is already initialized");
        }
        if (screenManager.getCurrentScreen() == null) {
            throw new IllegalStateException("No current screen set; call setCurrentScreen before initialize");
        }
        renderer.initialize();
        initialized = true;
        deferredRelease.run(screenManager::activate);
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
        if (!initialized) {
            return false;
        }
        renderer.processSdlEvent(sdlEventPtr);
        return switch (SDL_Event.ntype(sdlEventPtr)) {
            case SDLEvents.SDL_EVENT_MOUSE_MOTION, SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN,
                 SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP -> renderer.wantsMouseInput();
            case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> renderer.wantsMouseInput() && renderer.isHoveringScrollablePanel();
            case SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP,
                 SDLEvents.SDL_EVENT_TEXT_INPUT -> renderer.wantsKeyboardInput();
            default -> false;
        };
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
        checkNotClosed();
        if (!initialized) {
            throw new IllegalStateException("Display is not initialized; call initialize before render");
        }
        deferredRelease.run(() -> {
            renderer.render(screenManager.getCurrentScreen(), width, height, resolver);
            updateTextInput();
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
        if (closed) {
            return;
        }
        closed = true;
        if (!initialized) {
            return;
        }
        initialized = false;
        screenManager.beginDeactivation();
        deferredRelease.request();
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Display is closed; create a new ImGuiLwjglDisplay to show UI again");
        }
    }

    private void checkRegistrationOpen() {
        checkNotClosed();
        if (initialized) {
            throw new IllegalStateException("Fonts, renderers and style appliers can only be registered before the "
                    + "display is initialized");
        }
    }

    // Dear ImGui's SDL3 platform backend in C starts and stops SDL text input when Dear ImGui's text input request
    // changes, but imgui-java's port does not, and SDL3 delivers no text input events until text input is started
    private void updateTextInput() {
        if (!initialized) {
            return;
        }
        boolean wanted = renderer.wantsTextInput();
        if (wanted == textInputStarted) {
            return;
        }
        if (wanted) {
            // A refused start only disables typing; it is not recorded, so the next frame retries instead of failing
            // a frame that has already been drawn
            textInputStarted = SDLKeyboard.SDL_StartTextInput(sdlWindow);
        } else {
            stopTextInput();
        }
    }

    private void stopTextInput() {
        textInputStarted = false;
        SDLKeyboard.SDL_StopTextInput(sdlWindow);
    }

    private void release() {
        DeferredRelease.runAll(
                screenManager::deactivate,
                () -> {
                    if (textInputStarted) {
                        stopTextInput();
                    }
                },
                renderer::dispose,
                LorisScreen::shutdownLayoutUpdates);
    }
}
