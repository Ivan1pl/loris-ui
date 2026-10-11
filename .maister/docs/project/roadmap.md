# Development Roadmap

This roadmap lists the planned work for LorisUI. Priorities follow the maintainer's goals: use LorisUI in their own games, stabilise the API, publish it as a reusable library, and support more backends. It is a solo project, so the phases are ordered by dependency rather than by date.

## Current State (2026-10-11)
- **Version**: `1.0-SNAPSHOT` (publishable to the local Maven repository only)
- **Modules**: `api` (`lorisui.api`), `imgui` (`lorisui.imgui`), `jme3` (automatic module `lorisui.jme3`), `lwjgl` (`lorisui.lwjgl`), `examples`
- **Components**: Panel, Button, Text, Image, TextInput, NumberInput, ComboBox, ProgressBar, TabbedPanel, Popup, ModalPopup, Spacer
- **Recent work**: extracted into a standalone library and refactored into `backend/`, `renderer/impl/` and `renderer/style/impl/` packages; Java 25 toolchain, unit tests, the `LorisScreen` threading fix and the unified null-handling convention; then a toolkit-neutral `Backend` port (`render` only), JPMS module renames, dependency-scope cleanup, the jME screen lifecycle, and user-facing Javadoc for the whole exported API; then a Checkstyle build gate with `.editorconfig`, a renderer that owns its Dear ImGui context with teardown and re-attach, stable font handles registered on the renderer, a per-backend texture cache with unload, the registration API for renderers and style appliers, the host-neutral `ScreenManager` (with the hook re-entrancy guard and two-phase deactivation), `DeferredRelease` and `ResourceBundleKeyResolver`, unsigned glyph ranges, re-attachable jME displays, and the LWJGL/SDL3 host with its example (pre-1.0 breaking API changes); then local Maven publishing with sources and Javadoc jars and a Javadoc gate, jMonkeyEngine as a `compileOnly` dependency of `jme3`, the jME host sized in logical window coordinates, and the documented screen-size contract
- **Gaps**: no README, no CI, no remote publishing (artifacts go only to the local Maven repository). Unit tests exist for `api`, `imgui` and `lwjgl` (JUnit 6, Mockito in `api`), but not for `jme3` or `examples`; the native lifecycle (context, Impl objects, fonts, GL textures, re-attach) is verified only manually by running the examples

## Phase 1: Foundation & Build Hygiene
- [x] **Gradle toolchain** — Java 25 toolchain set for every Java module in root `allprojects`, so builds don't depend on the JDK running Gradle (JDK 25 must be installed; no auto-provisioning) `[Effort: S]`
- [ ] **JME3 snapshot dependency** — document the `mavenLocal()` prerequisite for `3.10.0-SNAPSHOT`, or move to a published jME release or a snapshot repository `[Effort: S]`
- [ ] **README** — purpose, module overview, build requirements (including network access for the first build, to list `imgui-java-app` versions), how to run the examples with `./gradlew :examples:runJme3Example` and `:examples:runLwjglExample` (in the jME example F1 toggles the UI and "Hide UI" removes it until F1; in the LWJGL example "Hide UI" closes the display for good), that macOS may need `-XstartOnFirstThread` for the SDL examples, the JDK 25 `--enable-native-access` flag (restricted-method warnings from imgui-java and LWJGL), and module-path notes (resources and resource bundles in a named module need an unconditionally opened package; custom renderers add imgui-java and `requires imgui.binding`) `[Effort: S]`
- [ ] **Minimal CI** — run `./gradlew build` on push (GitHub Actions or similar) `[Effort: S]`
- [x] **Formatter / `.editorconfig`** — Checkstyle 14.3.0 is a failing build gate for main and test sources of every Java project (`config/checkstyle/`, `maxWarnings = 0`), plus a minimal `.editorconfig`; there is no auto-formatter `[Effort: S]`
- [x] **Code hygiene** — `IntBiFunction<ResultType>` (and `ArrayStack<ElementType>`) follow the naming standard, now enforced by Checkstyle; wildcard imports are accepted (`java/formatting.md`) `[Effort: S]`

## Phase 2: API Stabilisation
- [x] **Unit tests for pure logic** — `ImageMode` construction, `Color` packing and `mask`, builder defaults, `LorisScreen` layout handoff, `ImGuiBackend` renderer and style-applier lookup (superclass fallback, missing-renderer error), texture-cache keying and masking, class-path resource lookup `[Effort: M]`
- [x] **`LorisScreen` threading fix** `[Effort: S]`
  - [x] Thread-safe `content` handoff (`volatile` content, atomic update flag)
  - [x] Exceptions from `layout()` (and a `null` result) surface once as `IllegalStateException` from `render()`
  - [x] Layout executor runs on a daemon thread (`lorisui-layout`), so it doesn't keep the JVM alive
  - [x] Layout work can be stopped: `LorisScreen.shutdownLayoutUpdates()` (called by both hosts on teardown) shuts the worker down, and the next requested layout starts it again
- [x] **Texture lifecycle** — the texture cache is per `ImGuiBackend` instance with keys normalized to class-path style (`"/x.png"` and `"x.png"` share one texture); renderers reach it through `RendererProvider.loadTexture`/`renderTexture`; `unloadTextures()` deletes every texture (`ImGuiOpenGlBackend.deleteTexture` frees the GL name) and runs automatically on renderer teardown. Per-path eviction was left out `[Effort: M]`
- [x] **Null-handling convention** — applied across components and styles (see `standards/java/error-handling.md`): optional `Supplier` properties default to `() -> null`, `Supplier` setters and required arguments use `Objects.requireNonNull`, constant overloads of optional properties accept `null` as unset `[Effort: S]`
- [x] **Javadoc** — user-facing Javadoc for every exported package of `lorisui.api`, `lorisui.imgui` and `lorisui.lwjgl` and for `jme3`, with `package-info.java` per package and module doc comments, including runtime notes (`--enable-native-access`, macOS `-XstartOnFirstThread` for the LWJGL host, Dear ImGui assertions ending the JVM, global SDL hints, font memory); strict doclint (`-Xdoclint:all`) reports zero warnings `[Effort: M]`
- [x] **JPMS cleanup** — modules named `lorisui.api` / `lorisui.imgui` (`requires transitive lorisui.api`); `jme3` is an automatic module (`Automatic-Module-Name: lorisui.jme3`) with no compiler workarounds `[Effort: M]`
- [ ] **jme3 `module-info`** — blocked while jME3 jars split packages `[Effort: S]`
- [x] **jME screen lifecycle** — screen registration, switching and localization-key validation, matched `onStartScreen`/`onEndScreen` hooks with exception-safe switches (now in the host-neutral `ScreenManager`), and the example removes the display in `destroy()` (jME doesn't call `cleanup` on exit) `[Effort: S]`
- [x] **Renderer teardown and re-attach** — `ImGuiOpenGlRenderer` owns its context: `initialize()` creates the context and fresh `ImGuiImplSdl3`/`ImGuiImplGl3` objects and replays the registered fonts; `dispose()` releases textures, Gl3, Sdl3 and the context. Neither runs inside a frame, and both hosts defer teardown requested mid-frame or from a hook. A removed jME display can be attached again; Dear ImGui UI state resets on re-attach `[Effort: M]`
- [x] **Re-entrancy guard for `setCurrentScreen`** — `ScreenManager` rejects `setCurrentScreen`, `activate()` and `deactivate()` from inside a screen hook with `IllegalStateException` before changing state `[Effort: S]`
- [x] **Shared deferred teardown** — the host-neutral `DeferredRelease` (`run`/`request`/`isPending`, `runAll`, `runAfterFailure`) replaces the helpers each host and the renderer carried, and `ScreenManager.beginDeactivation()` makes screen switches during a pending teardown only record the choice; both are unit-tested `[Effort: S]`
- [x] **Glyph ranges above U+7FFF** — font glyph ranges are built as unsigned 16-bit code points, so CJK ranges load, and malformed ranges fail at registration with `IllegalArgumentException`; a font Dear ImGui cannot load fails `initialize()` with `IllegalStateException` naming its source, and font data of 100 bytes or less (a JVM-ending Dear ImGui assertion) fails registration with `IOException` `[Effort: S]`
- [x] **jME HiDPI size** — the jME host passes the window size from `SDL_GetWindowSize` (logical window coordinates) each frame, like the LWJGL host, so the UI is sized correctly on HiDPI displays and follows resizes; jME's emulated display-scale modes (above 1) are documented as unsupported; manual check on a display with scale > 1 pending `[Effort: S]`
- [ ] **Fail once for missing images** — a missing or unreadable image fails the screen's render on every draw; cache the failure or report it once `[Effort: S]`
- [ ] **Bundle-name validation (optional)** — reject resource-bundle names containing `.` in `setResourceBundle`, since keys split at the first dot can't resolve them `[Effort: S]`
- [ ] **Popup open-flag protocol (B2)** — redesign how a popup's open state is bound. A `Popup` nested inside a panel uses the inherited `open(Supplier)` binding (default `false`), so it never shows after `open()` unless `open(() -> true)` is also set `[Effort: M]`
- [ ] **Optional or automatic panel IDs (B3)** — panel IDs are required stable keys that must be unique among siblings `[Effort: S]`
- [ ] **Input validation gaps** — `NumberInput.validateInput(null)` is silently ignored while `TextInput` throws `NullPointerException`; min/max/maxLength suppliers that return `null`, or a negative maxLength, fail late (when the value is committed); a `null` `ComboBox.Item.item` fails at draw time `[Effort: S]`
- [ ] **ComboBox default size** — a `ComboBox` built with neither `size` nor a sized empty-selection label comes out 0×0 `[Effort: S]`
- [x] **Content-function size arguments** — decided: content functions (`IntBiFunction`), like size bindings, receive the screen width and height, not the parent panel's size, at every nesting level, and position bindings receive it for top-level panels and popups (a nested panel is placed by its parent's layout); the contract and the unit (logical window pixels) are documented in the `api` Javadoc `[Effort: S]`

## Phase 3: Publishing
- [x] **`publishing {}` configuration** — a root `maven-publish` hook publishes `api`, `imgui`, `jme3` and `lwjgl` with sources and Javadoc jars and POM metadata (per-module name and description required) to the local Maven repository (`publishToMavenLocal`, no remote repository), and `javadoc` with `-Xdoclint:all -Werror` gates `check`; `jme3` declares jME as `compileOnly`, so its POM carries no jME dependency `[Effort: S]`
- [x] **Dependency scope review** — `imgui-java-app` is `compileOnly` plus a `runtimeOnly` range bounded below the next minor (`[1.92.7.1,1.93)`), so ImGui types don't reach consumers' compile classpath and Maven consumers can't resolve an untested minor; raise the cap when moving to a new imgui-java minor `[Effort: S]`
- [x] **`lwjgl-stb` scope** — an `implementation` dependency of `loris-ui-imgui` (runtime scope for consumers), with the natives `runtimeOnly` `[Effort: S]`
- [ ] **Versioning & changelog** — choose a versioning scheme, add a CHANGELOG, and cut the first non-SNAPSHOT release `[Effort: S]`
- [ ] **Release to a Maven repository** (Maven Central or GitHub Packages) `[Effort: M]`

## Phase 4: Additional Backends & Hosts
- [x] **Validate the backend abstraction** — `Backend` declares only `render`; texture loading and drawing live in `ImGuiBackend`, and hosts read input capture through `ImGuiOpenGlRenderer` instead of ImGui `[Effort: M]`
- [x] **Renderer/style-applier registration API** — `registerRenderer`/`registerStyleApplier` on `ImGuiBackend` (replacing existing mappings; `getRenderer`/`getStyleApplier` are final) and on both displays before initialization `[Effort: S]`
- [x] ~~**Custom backend in the jME host**~~ — superseded by display-level registration: custom components work in the jME host without a custom backend `[Effort: S]`
- [ ] **Neutral host SPI** — a toolkit-neutral host contract (`UiRuntime`) covering frame begin/end and input capture `[Effort: M]`
- [ ] **Neutral `Font` abstraction (B4)** — `Font` instances come from a specific backend and work only with it `[Effort: M]`
- [ ] **Toolkit-neutral style vocabulary (B6)** — style properties are passed to the backend as given and may be ignored by backends that lack the feature `[Effort: M]`
- [x] **Second host integration** — `loris-ui-lwjgl` (`ImGuiLwjglDisplay`) embeds LorisUI in an application's own LWJGL SDL3 window and loop, reusing the `imgui` module; `LorisLwjglExample` renders the shared `ExampleScreen` `[Effort: L]`
- [ ] **Second rendering backend** — a non-ImGui implementation of `Backend` that proves `api` is fully toolkit-agnostic `[Effort: L]`

## Future Enhancements
- [ ] More components as the author's games need them (sliders, checkboxes, lists/tables, tooltips)
- [ ] Theming: a shared style/theme object instead of per-component styles
- [ ] Performance: avoid per-pixel `Color` allocation in texture masking, and cache the superclass renderer lookup
- [x] Remove `System.exit(0)` from the example's `destroy()`
- [ ] Resolve the `PanelRenderer` TODO (check that the button released is the one that received the press)
- [ ] Track new Java LTS releases as they ship (next expected: Java 29, Sept 2027)

---
**Effort Scale**: `S`: 2-3 days | `M`: 1 week | `L`: 2+ weeks

*Generated during Maister initialization on 2026-10-10 from codebase analysis and maintainer goals; updated 2026-10-11.*
