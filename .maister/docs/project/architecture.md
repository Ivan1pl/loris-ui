# System Architecture

## Overview
LorisUI is a layered, multi-module Java library. Game code describes UI as trees of plain component objects defined in a toolkit-agnostic API. A **backend** turns those trees into draw calls every frame, and a **host integration** connects the backend to a game engine's render loop and input.

```
examples ──▶ loris-ui-jme3 ──▶ loris-ui-imgui ──▶ loris-ui-api
 (demo)       (jME3 host)      (ImGui/OpenGL       (component model,
                                 backend)           Backend contract;
                                                    zero dependencies)
```

## Architecture Pattern
**Pattern**: Layered library with a ports-and-adapters style backend abstraction, plus registries of strategies (renderers and style appliers) inside the backend.

- **Port**: `api/.../api/backend/Backend.java` defines what any rendering backend must provide: `render(...)`, `renderTexture(...)` and `loadTexture(...)`.
- **Adapters**: `imgui/.../imgui/backend/ImGuiBackend.java` (abstract, toolkit logic) and `ImGuiOpenGlBackend.java` (concrete OpenGL texture loading).
- **Host**: `jme3/.../jme3/ImGuiJmeDisplay.java` drives the backend from jMonkeyEngine.

**Rule**: dependencies only point towards `api`. `loris-ui-api` must not depend on any toolkit, engine or third-party library.

## System Structure

### loris-ui-api (`api/`)
- **Purpose**: the public, toolkit-agnostic contract: component model, styles, backend interface, screens, fonts and i18n.
- **JPMS module**: `lorisui.api.main`. It exports `backend`, `component`, `component.style`, `core`, `font`, `resolver` and `screen`.
- **Packages** (`com.ivan1pl.lorisui.api.*`):
  - `component`: the `Component` marker interface and concrete components (`Panel`, `Button`, `Text`, `Image`, `TextInput`, `NumberInput`, `ComboBox`, `ProgressBar`, `TabbedPanel`, `Popup`, `ModalPopup`, `Spacer`), plus `Layout`, `Background` and the sealed `ImageMode`
  - `component.style`: `Style` and its concrete styles (`ScrollbarStyle`, `ComboBoxStyle`, `ComboBoxPopupStyle`, `ComboBoxItemStyle`, `TextInputStyle`)
  - `backend`: the `Backend` interface and the `TextureData` record
  - `screen`: the abstract `LorisScreen` (template method for screens)
  - `core`: `Color` and `IntBiFunction`
  - `font`: the `Font` marker interface
  - `resolver`: `BundleKeyResolver` (i18n)
- **Conventions**: components are immutable-ish value holders built with nested static `Builder`s. Container builders use a self-typed generic `BaseBuilder<T, B>`.

### loris-ui-imgui (`imgui/`)
- **Purpose**: renders the API component model with Dear ImGui on OpenGL.
- **JPMS module**: `lorisui.imgui.main`. It exports `imgui`, `imgui.backend`, `imgui.font`, `imgui.renderer` and `imgui.renderer.style`. **It does not export** `renderer.impl` or `renderer.style.impl`, which are internal.
- **Key Files** (`com.ivan1pl.lorisui.imgui.*`):
  - `backend/ImGuiBackend.java`: implements `Backend` and `RendererProvider`. It builds the registries `Map<Class<? extends Component>, Renderer<?>>` and `Map<Class<? extends Style>, StyleApplier<?>>`. Lookups walk up the superclass chain, so a subclass of a component falls back to its parent's renderer. A missing entry throws `IllegalStateException`. It also maps `ImageMode` variants to ImGui draw calls using an exhaustive pattern `switch`.
  - `backend/ImGuiOpenGlBackend.java`: concrete `loadTexture` (STB decode, RGB-channel colour masking, GL texture upload)
  - `backend/impl/TextureMasking.java`: the pure pixel-masking loop used by `loadTexture` (internal, not exported)
  - `renderer/Renderer.java`: abstract per-component renderer base with a static texture cache (`TEXTURES`), confined to the render (GL) thread
  - `renderer/RendererProvider.java`: renderer and style-applier lookup contract, used by container renderers to render their children
  - `renderer/impl/*Renderer.java`: one renderer per component type
  - `renderer/style/StyleApplier.java` and `renderer/style/impl/*StyleApplier.java`: one applier per style type, which push and pop ImGui style vars and colours
  - `ImGuiOpenGlRenderer.java`: owns the ImGui context and the SDL3 + GL3 platform/renderer implementations, and runs the ImGui frame (always closed in `finally`)
  - `ImGuiState.java`: global flags shared across renderers (for example `hoveringScrollableWindow`)
  - `font/ImGuiFont.java`: the ImGui implementation of `Font`

### loris-ui-jme3 (`jme3/`)
- **Purpose**: integrates the ImGui backend into jMonkeyEngine 3.
- **JPMS**: none (classpath). Compilation uses `--patch-module jme3.core=<jme3-lwjgl3 jar>`.
- **Key Files**:
  - `ImGuiJmeDisplay.java`: implements `SceneProcessor` (render hook) and `BundleKeyResolver` (i18n). It manages screens (`addScreen`, `setCurrentScreen`), resource bundles (`setResourceBundle`) and fonts (`registerFont*`, allowed only before initialisation).
  - `ImGuiInputSystem.java`: forwards jME raw input and SDL events to ImGui

### examples (`examples/`)
- **Purpose**: demo application and manual test bed.
- **Key Files**: `example/jme3/LorisJme3Example.java` (a jME `SimpleApplication` with OpenGL 3.3 at 800×600) and `example/shared/ExampleScreen.java` (a host-agnostic `LorisScreen` that exercises the components)

## Data Flow

**Setup**
1. The host app creates an `ImGuiJmeDisplay(context, inputManager, guiViewPort)`, registers fonts and resource bundles, adds `LorisScreen`s, selects the current screen, and attaches the display to the GUI viewport as a scene processor.
2. On `initialize`, the ImGui renderer is set up, the input listeners are registered, and `onStartScreen()` is called on the current screen.

**Per frame** (`ImGuiJmeDisplay.postQueue`):
1. `ImGuiOpenGlRenderer.render(screen, w, h, resolver)` starts an ImGui frame.
2. `LorisScreen.render(backend, w, h, resolver)`:
   - If the previous `layout()` call threw or returned `null`, the failure is rethrown once as an `IllegalStateException` (cause = original failure) and nothing is rendered in that call; later frames render the last successful layout. Hosts should treat it as fatal.
   - If the layout was invalidated (`setLayoutNeedsUpdate(true)`, callable from any thread; the update flag is an `AtomicBoolean`), `layout()` is scheduled on the shared single-thread daemon executor (`lorisui-layout`). The new `List<Component>` is published through a `volatile` field and replaces `content` asynchronously; in the meantime the previous content keeps rendering (an empty list before the first layout).
   - Calls `backend.render(w, h, content, resolver)`.
3. `ImGuiBackend.render` looks up the `Renderer` for each component, and each renderer emits ImGui calls. Containers (Panel, TabbedPanel, Popup) recurse through `RendererProvider`. Styles are applied through `StyleApplier`s. Text keys are resolved through `BundleKeyResolver`. Images are loaded or fetched from the cache through `loadTexture` and drawn with `renderTexture` according to their `ImageMode`.
4. ImGui draw data is rendered with OpenGL 3. `ImGuiOpenGlRenderer.render` ends the ImGui frame in a `finally` block, so the frame is closed even when `LorisScreen.render` throws.

**Input**: jME raw input and SDL events go to `ImGuiInputSystem`, then to ImGui IO. Component callbacks (for example button actions) run during rendering on the render thread.

## Extension Points
- **New component**: add a class to `api/component` (with a Builder), add a renderer in `imgui/renderer/impl`, and register it in the `ImGuiBackend` constructor.
- **New style**: add a class to `api/component/style`, add an applier in `imgui/renderer/style/impl`, and register it in `ImGuiBackend`.
- **New backend**: implement `Backend` (and ideally its own registry mechanism) in a new module that depends only on `loris-ui-api`.
- **New host**: wrap `ImGuiOpenGlRenderer` and `ImGuiOpenGlBackend` in the target engine's render hook, the same way `ImGuiJmeDisplay` does.

## Known Architectural Concerns
- **Threading in `LorisScreen`**: the content handoff (`volatile` content, atomic update flag), failure surfacing (rethrown once from `render()`) and the daemon layout thread are resolved. Remaining by design: all screens share one static executor that is never shut down; being a daemon thread, it does not keep the JVM alive.
- **Shared static state**: `Renderer.TEXTURES` is render-thread-only by contract (a plain `HashMap`, documented in `Renderer`), but still unbounded and never evicted. `ImGuiState` holds global mutable flags.
- **Resource lifecycle**: `Backend` has no texture-unload operation, so GL textures leak for the lifetime of the process.
- **ImGui leaks into the public API**: `imgui-java-app` is an `api` dependency of `loris-ui-imgui`. (Input components no longer expose ImGui buffers: `TextInput`/`NumberInput` work with plain values through `getValue()`.)
- **Renderer robustness**: `ImGuiOpenGlRenderer` disables ImGui's error-recovery assert and tooltip, so state left open by a throwing renderer (windows, IDs, style stacks) is unwound when the frame closes and the original exception propagates to the host; the trade-off is that an imbalance that does not throw is recovered silently instead of failing fast. The input renderers pop their item width in `finally` and the combo box uses `setNextItemWidth`, but other push/pop pairs (combo style vars, per-item IDs, `beginChild`/`endChild` in the panel and combo renderers) are still not in `try/finally`. `TextInput` and `NumberInput` both use the bare `"##"` ImGui label without a per-component ID, so two inputs in the same window share an ImGui ID.
- **jME module workaround**: the `--patch-module` compiler argument, and the lack of a `module-info` in `jme3`.

## External Integrations
- Dear ImGui (imgui-java 1.92.7.1, SDL3 + GL3)
- LWJGL 3.4.1 (OpenGL, STB)
- jMonkeyEngine 3.10.0-SNAPSHOT

## Database Schema
Not applicable.

## Configuration
- Build-time: Gradle `ext` versions in the root `build.gradle`
- Runtime: done in code through `ImGuiJmeDisplay` (fonts, resource bundles, screens). Example settings live in `LorisJme3Example.createDefaultSettings()`.
- i18n: `ResourceBundle`s registered by name (for example `bundles.global`)

## Deployment Architecture
LorisUI is a library, so it isn't deployed as a service. It's meant to be published as Maven artifacts (`loris-ui-api`, `loris-ui-imgui`, `loris-ui-jme3`) and consumed by desktop game applications. LWJGL natives for Windows, Linux and macOS are pulled in at runtime.

---
*Based on codebase analysis performed 2026-10-10; updated 2026-10-10 after the toolchain, threading and null-handling work*
