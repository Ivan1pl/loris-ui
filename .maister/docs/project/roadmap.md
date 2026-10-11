# Development Roadmap

This roadmap lists the planned work for LorisUI. Priorities follow the maintainer's goals: use LorisUI in their own games, stabilise the API, publish it as a reusable library, and support more backends. It is a solo project, so the phases are ordered by dependency rather than by date.

## Current State (2026-10-11)
- **Version**: `1.0-SNAPSHOT` (unpublished)
- **Modules**: `api` (`lorisui.api`), `imgui` (`lorisui.imgui`), `jme3` (automatic module `lorisui.jme3`), `examples`
- **Components**: Panel, Button, Text, Image, TextInput, NumberInput, ComboBox, ProgressBar, TabbedPanel, Popup, ModalPopup, Spacer
- **Recent work**: extracted into a standalone library and refactored into `backend/`, `renderer/impl/` and `renderer/style/impl/` packages; Java 25 toolchain, unit tests, the `LorisScreen` threading fix and the unified null-handling convention; then a toolkit-neutral `Backend` port (`render` only), JPMS module renames, dependency-scope cleanup, the jME screen lifecycle, and user-facing Javadoc for the whole exported API (pre-1.0 breaking API changes)
- **Gaps**: no README, no CI, no publishing configuration, no doclint gate. Unit tests exist for `api` and `imgui` (JUnit 6 + Mockito), but not for `jme3` or `examples`

## Phase 1: Foundation & Build Hygiene
- [x] **Gradle toolchain** — Java 25 toolchain set for every Java module in root `allprojects`, so builds don't depend on the JDK running Gradle (JDK 25 must be installed; no auto-provisioning) `[Effort: S]`
- [ ] **JME3 snapshot dependency** — document the `mavenLocal()` prerequisite for `3.10.0-SNAPSHOT`, or move to a published jME release or a snapshot repository `[Effort: S]`
- [ ] **README** — purpose, module overview, build requirements (including network access for the first build, to list `imgui-java-app` versions), how to run `examples`, the JDK 25 `--enable-native-access` flag (restricted-method warnings from imgui-java and LWJGL), and module-path notes (resources and resource bundles in a named module need an unconditionally opened package; custom renderers add imgui-java and `requires imgui.binding`) `[Effort: S]`
- [ ] **Minimal CI** — run `./gradlew build` on push (GitHub Actions or similar) `[Effort: S]`
- [ ] **Formatter / `.editorconfig`** — lock in the existing 4-space style and import rules (Spotless or similar) `[Effort: S]`
- [ ] **Code hygiene** — rename the `IntBiFunction<R>` type parameter per the naming standard (`<ResultType>`), and expand the wildcard imports in `ImGuiBackend` and `PanelRenderer` `[Effort: S]`

## Phase 2: API Stabilisation
- [x] **Unit tests for pure logic** — `ImageMode` construction, `Color` packing and `mask`, builder defaults, `LorisScreen` layout handoff, `ImGuiBackend` renderer and style-applier lookup (superclass fallback, missing-renderer error), texture-cache keying and masking, class-path resource lookup `[Effort: M]`
- [ ] **`LorisScreen` threading fix** — mostly done; one sub-item remains open `[Effort: S]`
  - [x] Thread-safe `content` handoff (`volatile` content, atomic update flag)
  - [x] Exceptions from `layout()` (and a `null` result) surface once as `IllegalStateException` from `render()`
  - [x] Layout executor runs on a daemon thread (`lorisui-layout`), so it doesn't keep the JVM alive
  - [ ] Executor shutdown: the shared static executor is never shut down (no lifecycle hook yet)
- [ ] **Texture lifecycle** — `Renderer.TEXTURES` is documented as UI-thread-confined (no synchronization needed); still open: a texture unload/dispose operation on `ImGuiBackend`, freeing GL textures in `ImGuiOpenGlBackend`, and cache eviction. The cache is static and shared by all backends, and keyed by the raw path string, so `"/x.png"` and `"x.png"` become separate entries; normalize the key when this is reworked `[Effort: M]`
- [x] **Null-handling convention** — applied across components and styles (see `standards/java/error-handling.md`): optional `Supplier` properties default to `() -> null`, `Supplier` setters and required arguments use `Objects.requireNonNull`, constant overloads of optional properties accept `null` as unset `[Effort: S]`
- [x] **Javadoc** — user-facing Javadoc for every exported package of `lorisui.api` and `lorisui.imgui` and for `jme3`, with `package-info.java` per package and module doc comments; strict doclint (`-Xdoclint:all`) reports zero warnings `[Effort: M]`
- [x] **JPMS cleanup** — modules named `lorisui.api` / `lorisui.imgui` (`requires transitive lorisui.api`); `jme3` is an automatic module (`Automatic-Module-Name: lorisui.jme3`) with no compiler workarounds `[Effort: M]`
- [ ] **jme3 `module-info`** — blocked while jME3 jars split packages `[Effort: S]`
- [x] **jME screen lifecycle** — `ImGuiJmeDisplay` validates screen registration, switching and localization keys, fires matched `onStartScreen`/`onEndScreen` hooks with exception-safe switches, is single-use after `cleanup`, and the example removes the display in `destroy()` (jME doesn't call `cleanup` on exit) `[Effort: S]`
- [ ] **Renderer teardown and re-attach** — shut down `ImGuiImplGl3`/`ImGuiImplSdl3` and destroy the Dear ImGui context when a display is disposed, so GL/SDL resources are released and a removed display can be attached again. The renderer must keep the `ImGuiContext` returned by `ImGui.createContext()` and make it current for teardown to work, because a replacement renderer draws on the first renderer's context. Until then, each replacement display leaks one Dear ImGui context plus the previous display's GL objects and SDL cursors `[Effort: M]`
- [ ] **Re-entrancy guard for `setCurrentScreen`** — a call from inside a screen hook can leave the nested-started screen without its `onEndScreen()` `[Effort: S]`
- [ ] **Fail once for missing images** — a missing or unreadable image fails the screen's render on every draw; cache the failure or report it once `[Effort: S]`
- [ ] **Bundle-name validation (optional)** — reject resource-bundle names containing `.` in `setResourceBundle`, since keys split at the first dot can't resolve them `[Effort: S]`
- [ ] **Popup open-flag protocol (B2)** — redesign how a popup's open state is bound. A `Popup` nested inside a panel uses the inherited `open(Supplier)` binding (default `false`), so it never shows after `open()` unless `open(() -> true)` is also set `[Effort: M]`
- [ ] **Optional or automatic panel IDs (B3)** — panel IDs are required stable keys that must be unique among siblings `[Effort: S]`
- [ ] **Input validation gaps** — `NumberInput.validateInput(null)` is silently ignored while `TextInput` throws `NullPointerException`; min/max/maxLength suppliers that return `null`, or a negative maxLength, fail late (when the value is committed); a `null` `ComboBox.Item.item` fails at draw time; `ImGuiFont.loadFromFile` doesn't define its behavior for a missing file `[Effort: S]`
- [ ] **ComboBox default size** — a `ComboBox` built with neither `size` nor a sized empty-selection label comes out 0×0 `[Effort: S]`
- [ ] **Content-function size arguments** — content functions (`IntBiFunction`) receive the screen width and height, not the parent panel's size; decide whether nested content should get the parent size `[Effort: S]`

## Phase 3: Publishing
- [ ] **`publishing {}` configuration** — POM metadata, sources and Javadoc jars for `api`, `imgui` and `jme3` (`maven-publish` is already applied), plus a permanent doclint/Javadoc gate in the build `[Effort: S]`
- [x] **Dependency scope review** — `imgui-java-app` is `compileOnly` plus a `runtimeOnly` range bounded below the next minor (`[1.92.7.1,1.93)`), so ImGui types don't reach consumers' compile classpath and Maven consumers can't resolve an untested minor; raise the cap when moving to a new imgui-java minor `[Effort: S]`
- [ ] **`lwjgl-stb` scope** — stays `api` of `loris-ui-imgui`; revisit when publishing `[Effort: S]`
- [ ] **Versioning & changelog** — choose a versioning scheme, add a CHANGELOG, and cut the first non-SNAPSHOT release `[Effort: S]`
- [ ] **Release to a Maven repository** (Maven Central or GitHub Packages) `[Effort: M]`

## Phase 4: Additional Backends & Hosts
- [x] **Validate the backend abstraction** — `Backend` declares only `render`; texture loading and drawing live in `ImGuiBackend`, and hosts read input capture through `ImGuiOpenGlRenderer` instead of ImGui `[Effort: M]`
- [ ] **Renderer/style-applier registration API** — register custom renderers and appliers on `ImGuiBackend` instead of subclassing it and overriding `getRenderer`/`getStyleApplier` `[Effort: S]`
- [ ] **Custom backend in the jME host** — an `ImGuiJmeDisplay` constructor (or host) that accepts a custom `ImGuiBackend`, so custom components can be used with jME `[Effort: S]`
- [ ] **Neutral host SPI** — a toolkit-neutral host contract (`UiRuntime`) covering frame begin/end and input capture `[Effort: M]`
- [ ] **Neutral `Font` abstraction (B4)** — `Font` instances come from a specific backend and work only with it `[Effort: M]`
- [ ] **Toolkit-neutral style vocabulary (B6)** — style properties are passed to the backend as given and may be ignored by backends that lack the feature `[Effort: M]`
- [ ] **Second host integration** — for example plain LWJGL/GLFW or LibGDX, reusing the `imgui` module `[Effort: L]`
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
