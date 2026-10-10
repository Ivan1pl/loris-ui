# Development Roadmap

This roadmap lists the planned work for LorisUI. Priorities follow the maintainer's goals: use LorisUI in their own games, stabilise the API, publish it as a reusable library, and support more backends. It is a solo project, so the phases are ordered by dependency rather than by date.

## Current State (2026-10-10)
- **Version**: `1.0-SNAPSHOT` (unpublished)
- **Modules**: `api`, `imgui`, `jme3`, `examples`
- **Components**: Panel, Button, Text, Image, TextInput, NumberInput, ComboBox, ProgressBar, TabbedPanel, Popup, ModalPopup, Spacer
- **Recent work**: extracted into a standalone library, then refactored into `backend/`, `renderer/impl/` and `renderer/style/impl/` packages, with JPMS descriptors for `api` and `imgui`
- **Gaps**: no tests, no README or Javadoc, no CI, no publishing configuration

## Phase 1: Foundation & Build Hygiene
- [ ] **Gradle toolchain** — add `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }` so builds don't depend on the JDK running Gradle (the local default is 21, which can't compile this code) `[Effort: S]`
- [ ] **JME3 snapshot dependency** — document the `mavenLocal()` prerequisite for `3.10.0-SNAPSHOT`, or move to a published jME release or a snapshot repository `[Effort: S]`
- [ ] **README** — purpose, module overview, build requirements, how to run `examples` `[Effort: S]`
- [ ] **Minimal CI** — run `./gradlew build` on push (GitHub Actions or similar) `[Effort: S]`
- [ ] **Formatter / `.editorconfig`** — lock in the existing 4-space style and import rules (Spotless or similar) `[Effort: S]`

## Phase 2: API Stabilisation
- [ ] **Unit tests for pure logic** — `ImageMode` construction, `Color` packing and `mask`, builder defaults, `ImGuiBackend` renderer and style-applier lookup (superclass fallback, missing-renderer error), texture-cache keying `[Effort: M]`
- [ ] **`LorisScreen` threading fix** — make the `content` handoff thread-safe (`volatile` or atomic reference), surface exceptions thrown by `layout()`, and give the layout executor a proper lifecycle (shutdown, daemon thread) `[Effort: S]`
- [ ] **Texture lifecycle** — add texture unload/dispose to `Backend`, free GL textures in `ImGuiOpenGlBackend`, and make `Renderer.TEXTURES` safe for concurrent access and evictable `[Effort: M]`
- [ ] **Null-handling convention** — apply the agreed convention (see `standards/java/error-handling.md`): `null` = unset for optional properties, `Objects.requireNonNull` for required builder arguments `[Effort: S]`
- [ ] **Javadoc for `loris-ui-api`** — every exported type and public method `[Effort: M]`
- [ ] **JPMS cleanup** — rename `lorisui.api.main` / `lorisui.imgui.main` to `lorisui.api` / `lorisui.imgui` (decided: no `.main` suffix), and add a `module-info` for `jme3` (replacing the `--patch-module` workaround) if feasible `[Effort: M]`

## Phase 3: Publishing
- [ ] **`publishing {}` configuration** — POM metadata, sources and Javadoc jars for `api`, `imgui` and `jme3` (`maven-publish` is already applied) `[Effort: S]`
- [ ] **Dependency scope review** — decide whether `imgui-java-app` should stay `api` or move to `implementation` so ImGui types don't leak to consumers `[Effort: S]`
- [ ] **Versioning & changelog** — choose a versioning scheme, add a CHANGELOG, and cut the first non-SNAPSHOT release `[Effort: S]`
- [ ] **Release to a Maven repository** (Maven Central or GitHub Packages) `[Effort: M]`

## Phase 4: Additional Backends & Hosts
- [ ] **Validate the backend abstraction** — check that `Backend` and `LorisScreen` contain nothing ImGui- or OpenGL-specific, and move anything that does into the backends `[Effort: M]`
- [ ] **Second host integration** — for example plain LWJGL/GLFW or LibGDX, reusing the `imgui` module `[Effort: L]`
- [ ] **Second rendering backend** — a non-ImGui implementation of `Backend` that proves `api` is fully toolkit-agnostic `[Effort: L]`

## Future Enhancements
- [ ] More components as the author's games need them (sliders, checkboxes, lists/tables, tooltips)
- [ ] Theming: a shared style/theme object instead of per-component styles
- [ ] Performance: avoid per-pixel `Color` allocation in texture masking, and cache the superclass renderer lookup
- [ ] Remove `System.exit(0)` from the example's `destroy()` and resolve the `PanelRenderer` TODO
- [ ] Track new Java LTS releases as they ship (next expected: Java 29, Sept 2027)

---
**Effort Scale**: `S`: 2-3 days | `M`: 1 week | `L`: 2+ weeks

*Generated during Maister initialization on 2026-10-10 from codebase analysis and maintainer goals.*
