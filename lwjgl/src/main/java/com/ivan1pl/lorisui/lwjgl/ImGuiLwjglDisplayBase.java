package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.resolver.ResourceBundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.api.screen.ScreenManager;
import com.ivan1pl.lorisui.imgui.ImGuiRenderer;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;

import java.io.IOException;

/**
 * The toolkit-neutral part of the LWJGL displays: registration, screens, resource bundles and the
 * initialize/close state machine. Subclasses add the platform input handling and the frame call.
 *
 * @param <RendererType> type of the Dear ImGui renderer variant that draws the screens
 */
abstract class ImGuiLwjglDisplayBase<RendererType extends ImGuiRenderer> {
    private static final String CLOSED_MESSAGE = "Display is closed; create a new display to show UI again";

    private final ImGuiBackend backend;
    private final RendererType renderer;
    private final ScreenManager screenManager = new ScreenManager();
    private final ResourceBundleKeyResolver resolver = new ResourceBundleKeyResolver();
    // Closing during a frame, a screen switch or a hook is deferred until the outermost one returns, so the renderer
    // is never disposed inside a frame and no hook fires inside a hook
    private final DeferredRelease deferredRelease = new DeferredRelease(this::release);
    private boolean initialized = false;
    private boolean closed = false;

    ImGuiLwjglDisplayBase(ImGuiBackend backend, RendererType renderer) {
        this.backend = backend;
        this.renderer = renderer;
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
     * hooks, and may be called in any state of the display, including from inside a hook and after the display's
     * {@code close()}.
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
     * is initialized. This includes the time after the display's {@code close()}, also while the release of a display
     * closed during a frame, a screen switch or a hook is still pending; a closed display starts no screen again.
     * While the display is initialized, the switch is immediate: during the call,
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

    final RendererType getRenderer() {
        return renderer;
    }

    final ScreenManager getScreenManager() {
        return screenManager;
    }

    final ResourceBundleKeyResolver getResolver() {
        return resolver;
    }

    final boolean isInitialized() {
        return initialized;
    }

    final void initializeDisplay() {
        checkNotClosed();
        if (initialized) {
            throw new IllegalStateException("Display is already initialized");
        }
        if (screenManager.getCurrentScreen() == null) {
            throw new IllegalStateException("No current screen set; call setCurrentScreen before initialize");
        }
        setUp();
        initialized = true;
        deferredRelease.run(screenManager::activate);
    }

    final void renderFrame(Runnable frame) {
        checkNotClosed();
        if (!initialized) {
            throw new IllegalStateException("Display is not initialized; call initialize before render");
        }
        deferredRelease.run(frame);
    }

    final void runDeferred(Runnable operation) {
        deferredRelease.run(operation);
    }

    final void closeDisplay() {
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

    abstract void setUp();

    abstract void releaseInput();

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException(CLOSED_MESSAGE);
        }
    }

    private void checkRegistrationOpen() {
        checkNotClosed();
        if (initialized) {
            throw new IllegalStateException("Fonts, renderers and style appliers can only be registered before the "
                    + "display is initialized");
        }
    }

    private void release() {
        DeferredRelease.runAll(
                screenManager::deactivate,
                this::releaseInput,
                renderer::dispose,
                LorisScreen::shutdownLayoutUpdates);
    }
}
