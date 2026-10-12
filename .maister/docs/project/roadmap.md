# Development Roadmap

This roadmap lists the open work for LorisUI. Priorities follow the maintainer's goals: use LorisUI in their own games, stabilise the API, publish it as a reusable library, and support more backends. It is a solo project, so the phases are ordered by dependency rather than by date. Completed work is not tracked here; see the git history and the architecture and tech-stack documents for what exists.

## Current State (2026-10-12)
- **Version**: `1.0-SNAPSHOT`, publishable to the local Maven repository only; pre-1.0 API changes are made without shims
- **Modules**: `api` (`lorisui.api`), `imgui` (`lorisui.imgui`), the internal MIT-only renderer ports `imgui-gles3` (`lorisui.imgui.gles3`) and `imgui-vulkan` (`lorisui.imgui.vulkan`), `jme3` (automatic module `lorisui.jme3`), `lwjgl` (`lorisui.lwjgl`), `examples`
- **Components**: Panel, Button, Text, Image, TextInput, NumberInput, ComboBox, ProgressBar, TabbedPanel, Popup, ModalPopup, Spacer
- **Gaps**: no README, no CI, no remote publishing. There are no unit tests for `imgui-gles3` or `examples`; the native lifecycle (context, Impl objects, fonts, GL, GLES, GPU and Vulkan textures, GLFW callback chaining, re-attach), the GLES draw path under ANGLE and the whole Vulkan path are verified only manually by running the examples

## Phase 1: Foundation & Build Hygiene
- [ ] **JME3 snapshot dependency** — document the `mavenLocal()` prerequisite for `3.10.0-SNAPSHOT`, or move to a published jME release or a snapshot repository `[Effort: S]`
- [ ] **README** — purpose, module overview, build requirements (including network access for the first build, to list `imgui-java-app` versions), how to run the six examples (`:examples:runJme3Example` on jME's default ANGLE renderer, `runJme3Gl3Example`, `runLwjglSdlGl3Example`, `runLwjglGlfwGl3Example`, `runLwjglSdlGpuExample`, `runLwjglSdlVulkanExample`) or all at once with `:examples:runAllExamples` (F1 and "Hide UI" behaviour, `-Dlorisui.example.gpuDebug=true` for SDL_GPU or Vulkan validation, the latter needing `VK_LAYER_KHRONOS_validation`), the Vulkan 1.3 driver and system loader (MoltenVK from LWJGL's natives on macOS), macOS `-XstartOnFirstThread` for the SDL and GLFW examples, the JDK 25 `--enable-native-access` flag, and module-path notes (resources and resource bundles in a named module need an unconditionally opened package; custom renderers add imgui-java and `requires imgui.binding`) `[Effort: S]`
- [ ] **Minimal CI** — run `./gradlew build` on push (GitHub Actions or similar) `[Effort: S]`

## Phase 2: API Stabilisation
- [ ] **jme3 `module-info`** — blocked while jME3 jars split packages `[Effort: S]`
- [ ] **jME HiDPI manual check** — confirm the jME host's logical-size UI on a display with scale > 1 `[Effort: S]`
- [ ] **Fail once for missing images** — a missing or unreadable image fails the screen's render on every draw; cache the failure or report it once `[Effort: S]`
- [ ] **Bundle-name validation (optional)** — reject resource-bundle names containing `.` in `setResourceBundle`, since keys split at the first dot can't resolve them `[Effort: S]`
- [ ] **Popup open-flag protocol (B2)** — redesign how a popup's open state is bound. A `Popup` nested inside a panel uses the inherited `open(Supplier)` binding (default `false`), so it never shows after `open()` unless `open(() -> true)` is also set `[Effort: M]`
- [ ] **Optional or automatic panel IDs (B3)** — panel IDs are required stable keys that must be unique among siblings `[Effort: S]`
- [ ] **Input validation gaps** — `NumberInput.validateInput(null)` is silently ignored while `TextInput` throws `NullPointerException`; min/max/maxLength suppliers that return `null`, or a negative maxLength, fail late (when the value is committed); a `null` `ComboBox.Item.item` fails at draw time `[Effort: S]`
- [ ] **ComboBox default size** — a `ComboBox` built with neither `size` nor a sized empty-selection label comes out 0×0 `[Effort: S]`

## Phase 3: Publishing
- [ ] **Versioning & changelog** — choose a versioning scheme, add a CHANGELOG, and cut the first non-SNAPSHOT release `[Effort: S]`
- [ ] **Release to a Maven repository** (Maven Central or GitHub Packages) `[Effort: M]`

## Phase 4: Additional Backends & Hosts
- [ ] **Neutral host SPI** — a toolkit-neutral host contract (`UiRuntime`) covering frame begin/end and input capture `[Effort: M]`
- [ ] **Neutral `Font` abstraction (B4)** — `Font` instances come from a specific backend and work only with it `[Effort: M]`
- [ ] **Toolkit-neutral style vocabulary (B6)** — style properties are passed to the backend as given and may be ignored by backends that lack the feature `[Effort: M]`
- [ ] **Second rendering backend** — a non-ImGui implementation of `Backend` that proves `api` is fully toolkit-agnostic `[Effort: L]`

## Future Enhancements
- [ ] More components as the author's games need them (sliders, checkboxes, lists/tables, tooltips)
- [ ] Theming: a shared style/theme object instead of per-component styles
- [ ] Performance: avoid per-pixel `Color` allocation in texture masking, and cache the superclass renderer lookup
- [ ] Optional hover action for `TabbedPanel` tabs
- [ ] Track new Java LTS releases as they ship (next expected: Java 29, Sept 2027)

---
**Effort Scale**: `S`: 2-3 days | `M`: 1 week | `L`: 2+ weeks

*Generated during Maister initialization on 2026-10-10 from codebase analysis and maintainer goals; trimmed to open work on 2026-10-12.*
