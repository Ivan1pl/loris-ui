# Component Model & Backend Extension

### Marker Interfaces and Abstract Base Classes
Toolkit-neutral abstractions in `api` are empty marker interfaces (`Component`, `Style`, `Font`), as is the opaque `StyleApplier.Styles` token in `imgui`; backends downcast to their concrete type (e.g. `(ImGuiFont) component.getFont()`).
Use abstract classes for shared behaviour / template methods (`Renderer`, `LorisScreen`, `ImGuiBackend`, `Panel.BaseBuilder`); use interfaces for stateless contracts (`Backend`, `RendererProvider`, `StyleApplier`, `BundleKeyResolver`).

### Toolkit-Neutral `Backend` Port
`Backend` declares only `render(int w, int h, List<Component> components, BundleKeyResolver bundleKeyResolver)`. Toolkit concerns stay in the backend module:
- **Textures** belong to each `ImGuiBackend` instance. Renderers load and draw them only through the `RendererProvider` they receive: `TextureData loadTexture(String resourcePath)`, `TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)` (both `throws IOException`, cached per backend with keys normalized to class-path style, failures not cached) and `renderTexture(int id, ImageMode imageMode, int width, int height, int textureWidth, int textureHeight)`. `ImGuiBackend` implements the two `loadTexture` methods as `public final` on top of its graphics-API hooks `protected abstract TextureData createTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) throws IOException` (called on a cache miss with the normalized path, no leading `/`) and `protected abstract void deleteTexture(TextureData texture)` (implemented by the final `ImGuiOpenGlBackend`). `public final unloadTextures()` releases every texture and throws `IllegalStateException` while the backend is drawing; renderer teardown calls it. The `TextureData` record lives in `imgui.backend`.
- **Input capture** is exposed to hosts by `ImGuiOpenGlRenderer` (`wantsMouseInput()`, `wantsKeyboardInput()`, `wantsTextInput()`, `isHoveringScrollablePanel()`), never by the API. `wantsTextInput()` tells hosts whose framework does not start SDL text input itself (the LWJGL host) when to start and stop it.
- **Fonts** are registered with `ImGuiOpenGlRenderer` (or through a host display) and returned as `ImGuiFont` handles in the `com.ivan1pl.lorisui.imgui` package.

Do not add toolkit-specific methods or types to `Backend`.

### Hosts Pass the Logical Window Size
Every host passes the window's logical size (window coordinates as reported by `SDL_GetWindowSize`, not framebuffer pixels) to `ImGuiOpenGlRenderer.render(screen, w, h, resolver)` on every frame; Dear ImGui's SDL3 backend applies the HiDPI framebuffer scale. The same `w` / `h` are the screen size that position, size and content functions receive.
- `ImGuiJmeDisplay` queries `SDLVideo.SDL_GetWindowSize` on its window handle each frame in `drawCurrentScreen()`, inside a `MemoryStack` frame closed before rendering, and throws `IllegalStateException` with `SDLError.SDL_GetError()` when the query fails.
- `ImGuiLwjglDisplay.render(width, height)` takes the size the application reads with `SDL_GetWindowSize`.
- Never size the UI from the jME camera, the viewport or `SceneProcessor.reshape`; those are framebuffer pixels on HiDPI displays. A new host (and the planned neutral host SPI) follows the same contract.

```java
try (MemoryStack stack = MemoryStack.stackPush()) {
    IntBuffer widthBuffer = stack.mallocInt(1);
    IntBuffer heightBuffer = stack.mallocInt(1);
    if (!SDLVideo.SDL_GetWindowSize(window.getWindowHandle(), widthBuffer, heightBuffer)) {
        throw new IllegalStateException("Could not read the window size: " + SDLError.SDL_GetError());
    }
    width = widthBuffer.get(0);
    height = heightBuffer.get(0);
}
```

### Class-Keyed Renderer and StyleApplier Registry
Each `api` Component has exactly one `Renderer` and each `Style` exactly one `StyleApplier`, registered by class in the `ImGuiBackend` constructor (written directly to the maps, so the constructor calls no overridable method). Lookup (`getRenderer` / `getStyleApplier`, both `public final`) walks the superclass chain, so subclasses (Button, Popup, ModalPopup) reuse their parent's renderer unless registered explicitly. A missing registration fails fast with `IllegalStateException`.
- New component: class with Builder in `api/component` + `imgui/renderer/impl/<Name>Renderer` (no-argument constructor) + one `renderers.put(...)` line.
- New style: class in `api/component/style` + `imgui/renderer/style/impl/<Name>Applier` + one `styleAppliers.put(...)` line.

```java
renderers.put(ProgressBar.class, new ProgressBarRenderer());
styleAppliers.put(TextInputStyle.class, new TextInputStyleApplier());
```

### Custom Components and Styles Through Registration
Library users plug in their own components and styles by registering a `Renderer` / `StyleApplier`, never by subclassing the backend:
- **On a backend**: `ImGuiBackend.registerRenderer(Class<ComponentType>, Renderer<? super ComponentType>)` and `registerStyleApplier(Class<StyleType>, StyleApplier<? super StyleType>)`, both `public final`. A registration replaces any existing mapping, including a built-in one, and takes effect from the next lookup; subclasses without their own mapping use the nearest registered ancestor. Null arguments throw `NullPointerException`; an interface class throws `IllegalArgumentException`, because lookup walks only the superclass chain. Not gated by lifecycle; UI thread only.
- **On a display**: `ImGuiJmeDisplay` and `ImGuiLwjglDisplay` offer the same two methods, delegating to their private backend. They throw `IllegalStateException` while the display is initialized or unusable (jME: failed initialization; LWJGL: closed). Registrations persist across jME re-attach.
- **Generic component types** need an unchecked class cast, kept to the narrowest scope: `(Class<ComboBox<String>>) (Class<?>) ComboBox.class`.
- Custom renderers load and draw textures through their `RendererProvider`, and apply fonts with `((ImGuiFont) font).useFont(...)`.
- imgui-java is only a runtime dependency of `loris-ui-imgui`, so code with a custom `Renderer` adds imgui-java to its own build and, on the module path, `requires imgui.binding`.
- Subclass `ImGuiBackend` only to support another graphics API (implementing `createTexture` / `deleteTexture`); `ImGuiOpenGlBackend` is final, and the bundled hosts always use their own `ImGuiOpenGlBackend`.

```java
ImGuiJmeDisplay display = new ImGuiJmeDisplay(context, inputManager, guiViewPort);
display.registerRenderer(Gauge.class, new GaugeRenderer());
display.registerStyleApplier(GaugeStyle.class, new GaugeStyleApplier());
```
