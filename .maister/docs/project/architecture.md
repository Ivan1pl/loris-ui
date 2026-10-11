# System Architecture

## Overview
LorisUI is a layered, multi-module Java library. Game code describes UI as trees of plain component objects defined in a toolkit-agnostic API. A **backend** turns those trees into draw calls each time the host renders, and a **host integration** connects the backend to a game engine's render loop and input.

```
examples ──▶ loris-ui-jme3 ──▶ loris-ui-imgui ──▶ loris-ui-api
 (demo)       (jME3 host)      (ImGui/OpenGL       (component model,
                                 backend)           Backend port;
                                                    zero dependencies)
```

## Architecture Pattern
**Pattern**: Layered library with a ports-and-adapters style backend abstraction, plus registries of strategies (renderers and style appliers) inside the backend.

- **Port**: `api/.../api/backend/Backend.java` declares a single method, `render(int w, int h, List<Component> components, BundleKeyResolver bundleKeyResolver)`. It contains nothing toolkit-specific: textures, fonts and input capture are backend concerns.
- **Adapters**: `imgui/.../imgui/backend/ImGuiBackend.java` (abstract: registries, texture drawing, and the abstract texture loading) and `ImGuiOpenGlBackend.java` (final; OpenGL texture loading).
- **Host**: `jme3/.../jme3/ImGuiJmeDisplay.java` drives the backend from jMonkeyEngine.

**Rule**: dependencies only point towards `api`. `loris-ui-api` must not depend on any toolkit, engine or third-party library.

## System Structure

### loris-ui-api (`api/`)
- **Purpose**: the public, toolkit-agnostic contract: component model, styles, backend port, screens, fonts and i18n.
- **JPMS module**: `lorisui.api`. It exports `backend`, `component`, `component.style`, `core`, `font`, `resolver` and `screen`, each with a `package-info.java`; the module descriptor has a module doc comment.
- **Packages** (`com.ivan1pl.lorisui.api.*`):
  - `component`: the `Component` marker interface and concrete components (`Panel`, `Button`, `Text`, `Image`, `TextInput`, `NumberInput`, `ComboBox`, `ProgressBar`, `TabbedPanel`, `Popup`, `ModalPopup`, `Spacer`), plus `Layout`, `Background` and the sealed `ImageMode`
  - `component.style`: `Style` and its concrete styles (`ScrollbarStyle`, `ComboBoxStyle`, `ComboBoxPopupStyle`, `ComboBoxItemStyle`, `TextInputStyle`)
  - `backend`: the `Backend` interface (only)
  - `screen`: the abstract `LorisScreen` (template method for screens)
  - `core`: the final `Color` and `IntBiFunction`
  - `font`: the `Font` marker interface
  - `resolver`: `BundleKeyResolver` (i18n)
- **Conventions**: components are immutable-ish value holders built with nested static `Builder`s and non-public constructors. The Panel family is closed to the package: `Panel`'s constructor and its self-typed `BaseBuilder<PanelType, BuilderType>` are package-private, and `Button`, `Popup` and `ModalPopup` have private constructors.

### loris-ui-imgui (`imgui/`)
- **Purpose**: renders the API component model with Dear ImGui on OpenGL.
- **JPMS module**: `lorisui.imgui`, with `requires transitive lorisui.api` so consumers of the backend read the API types. It exports `imgui`, `imgui.backend`, `imgui.font`, `imgui.renderer` and `imgui.renderer.style` (each with a `package-info.java`; the descriptor has a module doc comment). **It does not export** `imgui.impl`, `backend.impl`, `renderer.impl` or `renderer.style.impl`, which are internal.
- **Key Files** (`com.ivan1pl.lorisui.imgui.*`):
  - `backend/ImGuiBackend.java`: implements `Backend` and `RendererProvider`; protected constructor. The constructor fills the registries `Map<Class<? extends Component>, Renderer<?>>` and `Map<Class<? extends Style>, StyleApplier<?>>`. `getRenderer`/`getStyleApplier` walk up the superclass chain, so a subclass of a component falls back to its parent's renderer; a missing entry throws `IllegalStateException`. It also declares the texture API: `public abstract TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) throws IOException`, and the concrete `public void renderTexture(...)`, which maps `ImageMode` variants to ImGui draw calls with an exhaustive pattern `switch`.
  - `backend/TextureData.java`: the `TextureData(int id, int width, int height)` record returned by `loadTexture`
  - `backend/ImGuiOpenGlBackend.java`: final; concrete `loadTexture` (class-loader resource lookup, STB decode, RGB-channel color masking, GL texture upload)
  - `backend/impl/TextureMasking.java`: the pure pixel-masking loop used by `loadTexture`
  - `renderer/Renderer.java`: abstract per-component renderer base (explicit protected constructor) with `protected static loadTexture(ImGuiBackend, path[, masks])` helpers backed by a static cache (`TEXTURES`) keyed by path and masks, confined to the host's UI (GL) thread
  - `renderer/RendererProvider.java`: renderer and style-applier lookup contract, used by container renderers to render their children
  - `renderer/impl/*Renderer.java`: one renderer per component type
  - `renderer/style/StyleApplier.java` and `renderer/style/impl/*StyleApplier.java`: one applier per style type, which push and pop ImGui style vars and colors
  - `ImGuiOpenGlRenderer.java`: final; creates a Dear ImGui context and owns the SDL3 + GL3 platform/renderer implementations and runs the ImGui frame (always closed in `finally`). The Dear ImGui context is global, so only one renderer (for one window) may be in use at a time. A replacement renderer keeps working on the first renderer's context, because Dear ImGui leaves the previously current context current, and replacing a renderer releases nothing of the previous one (no teardown yet). It exposes the input-capture queries a host needs: `wantsMouseInput()`, `wantsKeyboardInput()` and `isHoveringScrollablePanel()`, each describing the most recently rendered frame.
  - `impl/ImGuiState.java`: internal, final; global flags shared across renderers (`hoveringScrollableWindow`), read by hosts only through `ImGuiOpenGlRenderer.isHoveringScrollablePanel()`
  - `impl/ClassPathResources.java`: internal; opens class-path resources through the class loader, with a leading `/` optional (used by `ImGuiFont` and `ImGuiOpenGlBackend`)
  - `font/ImGuiFont.java`: the ImGui implementation of `Font`
- **Toolkit dependency**: imgui-java is a runtime dependency only (`compileOnly` plus `runtimeOnly` `[1.92.7.1,1.93)`); no ImGui type appears in an exported signature, so it is not part of the module's compile-time API.

### loris-ui-jme3 (`jme3/`)
- **Purpose**: integrates the ImGui backend into jMonkeyEngine 3.
- **JPMS**: no `module-info.java` (jME3 jars split packages, so a named module can't read them). The jar is an automatic module named `lorisui.jme3` through the `Automatic-Module-Name` manifest attribute. The package has a `package-info.java`.
- **Key Files**:
  - `ImGuiJmeDisplay.java`: implements `SceneProcessor` (render hook) and `BundleKeyResolver` (i18n). It manages screens (`addScreen`, `setCurrentScreen`), resource bundles (`setResourceBundle`) and fonts (`registerFont*`, allowed only before initialization). It always creates its own `ImGuiOpenGlBackend` and `ImGuiOpenGlRenderer`.
  - `ImGuiInputSystem.java`: package-private, final; a jME `RawInputListener` that marks mouse and key events consumed using only the `ImGuiOpenGlRenderer` input-capture queries. jme3 has no `imgui.*` imports.

### examples (`examples/`)
- **Purpose**: demo application and manual test bed.
- **Key Files**: `example/jme3/LorisJme3Example.java` (a jME `SimpleApplication` with OpenGL 3.3 at 800×600, which removes the display in `destroy()`) and `example/shared/ExampleScreen.java` (a host-agnostic `LorisScreen` that exercises the components)

## Data Flow

**Setup**
1. The host app creates an `ImGuiJmeDisplay(context, inputManager, guiViewPort)`, registers fonts and resource bundles, adds `LorisScreen`s, selects the current screen, and attaches the display to the GUI viewport as a scene processor.
2. jME calls `initialize` lazily, on the first frame (or window resize, through `RenderManager.notifyReshape`) after the display is added to a viewport. The ImGui renderer is set up, the input listeners are registered, and `onStartScreen()` is called on the current screen.

**Per frame** (`ImGuiJmeDisplay.postQueue`):
1. `ImGuiOpenGlRenderer.render(screen, w, h, resolver)` starts an ImGui frame.
2. `LorisScreen.render(backend, w, h, resolver)`:
   - If the previous `layout()` call threw or returned `null`, the failure is rethrown once as an `IllegalStateException` (cause = original failure) and nothing is rendered in that call; later frames render the last successful layout. Hosts should treat it as fatal.
   - If the layout was invalidated (`setLayoutNeedsUpdate(true)`, callable from any thread; the update flag is an `AtomicBoolean`), `layout()` is scheduled on the shared single-thread daemon executor (`lorisui-layout`). The new `List<Component>` is published through a `volatile` field and replaces `content` asynchronously; in the meantime the previous content keeps rendering (an empty list before the first layout).
   - Calls `backend.render(w, h, content, resolver)`.
3. `ImGuiBackend.render` looks up the `Renderer` for each component, and each renderer emits ImGui calls. Containers (Panel, TabbedPanel, Popup) recurse through `RendererProvider`. Styles are applied through `StyleApplier`s. Text keys are resolved through `BundleKeyResolver`. Images are loaded or fetched from the cache through `Renderer.loadTexture(ImGuiBackend, ...)` and drawn with `ImGuiBackend.renderTexture` according to their `ImageMode`. A missing or unreadable image (`Image` or panel `Background`) makes the renderer throw a `RuntimeException` wrapping the `IOException`; failures are not cached, so the screen's render fails again on every draw.
4. ImGui draw data is rendered with OpenGL 3. `ImGuiOpenGlRenderer.render` ends the ImGui frame in a `finally` block, so the frame is closed even when `LorisScreen.render` throws.

**Input**: SDL events reach ImGui IO through an SDL event listener that calls `ImGuiOpenGlRenderer.processSdlEvent`. In parallel, `ImGuiInputSystem` marks jME raw input events consumed when `wantsMouseInput()`/`wantsKeyboardInput()` report that the UI wants them; mouse-wheel events are consumed only while `isHoveringScrollablePanel()`. Component callbacks (for example button actions) run during rendering on the host's UI thread.

## Screen Lifecycle (jME host)
All `ImGuiJmeDisplay` calls and screen hooks run on the jME render (application) thread. A screen receives `onEndScreen()` only if its `onStartScreen()` returned normally, and at most once per start; a screen whose `onStartScreen()` threw counts as not started. Hook exceptions propagate out of the method that fired them.
- `addScreen(name, screen)`: rejects null arguments (`NullPointerException`) and throws `IllegalStateException` for a name that is already registered. Fires no hooks.
- `setCurrentScreen(name)`: `NullPointerException` for null, `IllegalStateException` for an unknown name, both before any state change; switching to the current screen does nothing. Before initialization, and after cleanup, it only records the name (after cleanup no hooks ever fire for it). After initialization the switch is immediate: `onEndScreen()` on the old screen, then `onStartScreen()` on the new one, during the call; the new screen is drawn from the next frame. The switch always completes: if the old screen's `onEndScreen()` throws, the new screen still becomes current and is started, then the end failure is rethrown (a different start failure is added to it as suppressed). It may be called from a component callback, in which case the old screen finishes drawing the current frame after its `onEndScreen()`.
- `initialize`: throws `IllegalStateException` if the display was disposed or no current screen is set, before any side effect. If setting up the renderer or registering the listeners fails, it marks the display disposed, removes any registered listeners and rethrows, so jME's per-frame retry fails fast instead of setting up the renderer again; no hooks fire. Otherwise it ends with `onStartScreen()`.
- `cleanup`: jME calls it when the display is removed from its viewport (`ViewPort.removeProcessor`/`clearProcessors`). If the display was initialized, it removes the listeners, marks the display disposed and calls `onEndScreen()` on the started screen; otherwise it is a no-op (a display removed before it is initialized can still be attached later; one whose initialization failed cannot).
- **Single use**: once an initialized display is cleaned up, or its initialization failed, re-attaching it (`initialize`) and `registerFont*` throw `IllegalStateException`. To show the UI again, applications remove the old display first (constructing a display already resets the shared Dear ImGui style, and registering fonts adds them to the shared atlas), then create and attach a new one. This is supported but leaks: the new display's renderer keeps working on the first display's Dear ImGui context, so each replacement leaks one context (the unused one the new renderer created) plus the previous display's GL objects (program, buffers) and SDL cursors, none of which are released until renderer teardown exists. Two displays attached at the same time would share one context in the same frame, which is not supported.
- **Application exit**: jME does not call `cleanup` on exit. Applications that want `onEndScreen()` at exit remove the display themselves; `LorisJme3Example` overrides `destroy()` and calls `guiViewPort.removeProcessor(imGuiJmeDisplay)` before `super.destroy()`.

## Extension Points
- **New built-in component**: add a class to `api/component` (with a Builder), add a renderer in `imgui/renderer/impl`, and register it in the `ImGuiBackend` constructor.
- **New built-in style**: add a class to `api/component/style`, add an applier in `imgui/renderer/style/impl`, and register it in the `ImGuiBackend` constructor.
- **Custom components or styles (library users)**: subclass `ImGuiBackend`, override `getRenderer`/`getStyleApplier` to return your `Renderer`/`StyleApplier` for your types, delegate to `super` for everything else (superclass-chain lookup, `IllegalStateException` when nothing matches), and implement `loadTexture` for the graphics API (`ImGuiOpenGlBackend` is final, so it can't be extended). Renderers load and draw textures through `ImGuiBackend.loadTexture`/`renderTexture`. A custom renderer calls Dear ImGui directly, so its code adds imgui-java to its own build and, on the module path, `requires imgui.binding`. The custom backend is passed to `ImGuiOpenGlRenderer` by the host; the jME host always uses its own `ImGuiOpenGlBackend`, so it can't host a custom backend yet (see the roadmap).
- **New backend**: implement `Backend.render` (and its own registry mechanism) in a new module that depends only on `loris-ui-api`.
- **New host**: wrap `ImGuiOpenGlRenderer` and `ImGuiOpenGlBackend` in the target engine's render hook, the same way `ImGuiJmeDisplay` does, and use the renderer's input-capture queries for input routing.

## Known Architectural Concerns
- **Threading in `LorisScreen`**: the content handoff (`volatile` content, atomic update flag), failure surfacing (rethrown once from `render()`) and the daemon layout thread are in place. Remaining by design: all screens share one static executor that is never shut down; being a daemon thread, it does not keep the JVM alive.
- **Shared static state**: `Renderer.TEXTURES` is UI-thread-only by contract (a plain `HashMap`), but unbounded, never evicted, shared by all backends, and keyed by the raw path string, so `"/x.png"` and `"x.png"` load the same resource into two entries. `ImGuiState` holds global mutable flags.
- **Resource lifecycle**: `ImGuiBackend` has no texture-unload operation, so GL textures leak for the lifetime of the process. `ImGuiOpenGlRenderer` has no teardown (no `ImGuiImplGl3`/`ImGuiImplSdl3` shutdown, no `destroyContext`), so the GL and SDL resources of a disposed `ImGuiJmeDisplay` are never released; this is why the display is single-use and why each replacement display leaks.
- **Screen switch re-entrancy**: calling `setCurrentScreen` from inside an `onStartScreen()`/`onEndScreen()` hook can leave the screen started by the nested call without its `onEndScreen()`.
- **Missing images**: a texture that fails to load is retried and fails again on every draw instead of failing once.
- **Renderer robustness**: `ImGuiOpenGlRenderer` disables ImGui's error-recovery assert and tooltip, so state left open by a throwing renderer (windows, IDs, style stacks) is unwound when the frame closes and the original exception propagates to the host; the trade-off is that an imbalance that does not throw is recovered silently instead of failing fast. The input renderers pop their item width in `finally`, the combo box uses `setNextItemWidth`, and the progress bar pops its style in `finally`, but other push/pop pairs (combo style vars, per-item IDs, `beginChild`/`endChild` in the panel and combo renderers) are still not in `try/finally`. `TextInput` and `NumberInput` both use the bare `"##"` ImGui label without a per-component ID, so two inputs in the same window share an ImGui ID.

## External Integrations
- Dear ImGui (imgui-java 1.92.x, 1.92.7.1 or later, at runtime; SDL3 + GL3)
- LWJGL 3.4.1 (OpenGL, STB, SDL)
- jMonkeyEngine 3.10.0-SNAPSHOT

## Database Schema
Not applicable.

## Configuration
- Build-time: Gradle `ext` versions in the root `build.gradle`
- Runtime: done in code through `ImGuiJmeDisplay` (fonts, resource bundles, screens). Example settings live in `LorisJme3Example.createDefaultSettings()`.
- i18n: `ResourceBundle`s registered by name (for example `bundles.global`)
- Resources (fonts, textures) are class-path resource names; a leading `/` is optional. On the module path they must be outside any package of a named module, or in a package the application module opens unconditionally.

## Deployment Architecture
LorisUI is a library, so it isn't deployed as a service. It's meant to be published as Maven artifacts (`loris-ui-api`, `loris-ui-imgui`, `loris-ui-jme3`) and consumed by desktop game applications. imgui-java and the `lwjgl-stb` natives for Windows, Linux, macOS and macOS arm64 are pulled in at runtime. On JDK 25, applications pass `--enable-native-access` (`ALL-UNNAMED` on the class path, the module names on the module path) to avoid the restricted-method warnings of imgui-java and LWJGL.

---
*Based on codebase analysis performed 2026-10-10; updated 2026-10-11 for the backend port, JPMS, dependency-scope, jME lifecycle and Javadoc work*
