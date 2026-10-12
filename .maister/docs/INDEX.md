# Documentation Index

**IMPORTANT**: Read this file at the beginning of any development task to understand available documentation and standards.

## Quick Reference

### Project Documentation
Project-level documentation covering vision, goals, architecture, and technology choices.

### Technical Standards
Coding standards, conventions, and best practices organized by domain.

---

## Project Documentation

Located in `.maister/docs/project/`

### Vision (`project/vision.md`)
What LorisUI is (a declarative, toolkit-agnostic Java UI library for game menus/HUDs rendered through a pluggable backend whose port is a single toolkit-neutral `render` call, currently Dear ImGui on OpenGL 3 or SDL_GPU, hosted in jMonkeyEngine 3 or an application's own LWJGL loop with SDL3 or GLFW), the problem it solves, target users, key features (custom components/styles registered on the backend or a host display, `ImGuiJmeDisplay` and the three LWJGL displays `ImGuiLwjglSdlGl3Display`/`ImGuiLwjglGlfwGl3Display`/`ImGuiLwjglSdlGpuDisplay` with input routing, `ScreenManager` lifecycle hooks, re-attach and safe in-frame teardown), success criteria, differentiators, and firm constraints (dependency-free `loris-ui-api` / `lorisui.api`, latest Java LTS only, solo-developer-scale process).

### Roadmap (`project/roadmap.md`)
Current state (`1.0-SNAPSHOT`, publishable to the local Maven repository only, modules `lorisui.api`/`lorisui.imgui`/automatic `lorisui.jme3`/`lorisui.lwjgl`; recent GLFW + OpenGL 3 and SDL3 + SDL_GPU variants with the breaking SDL3 + OpenGL 3 renames; gaps: no README/CI/remote publishing, native lifecycle (incl. GPU textures, GLFW chaining) verified only manually, no jme3/examples tests; pre-1.0 breaking-change list for the renderer variants) and dependency-ordered phases with effort estimates: Phase 1 build hygiene (toolchain, Checkstyle, `.editorconfig` done; jME snapshot, README (four run tasks, macOS note for SDL and GLFW), CI open), Phase 2 API stabilisation (tests, threading, texture lifecycle, null handling, Javadoc incl. `lorisui.lwjgl`, JPMS, screen lifecycle, renderer teardown/re-attach, re-entrancy guard, shared `DeferredRelease`, unsigned glyph ranges, jME HiDPI size (runtime check pending), content-function sizes decided (screen size, not the parent's) done; missing images, popup/panel/input/ComboBox issues open), Phase 3 publishing (local `publishing {}` with sources/Javadoc jars and the Javadoc gate, dependency scopes done; versioning, remote release open), Phase 4 backends/hosts (registration API, LWJGL host and the GLFW+GL3 / SDL3+SDL_GPU platform/renderer variants done; neutral host SPI, fonts, styles, second rendering backend open).

### Tech Stack (`project/tech-stack.md`)
Java 25 (LTS-only policy; records, sealed types incl. the `ImGuiRenderer` hierarchy, pattern matching, JPMS modules `lorisui.api`/`lorisui.imgui`/`lorisui.lwjgl`, automatic `lorisui.jme3`) through a Gradle toolchain (JDK 25 installed, no auto-provisioning); Gradle 9.6.0 Groovy DSL with shared versions in root `ext` and four example run tasks (`--enable-native-access=ALL-UNNAMED`, the `lorisui.example.gpuDebug` pass-through, macOS `-XstartOnFirstThread` caveat); imgui-java 1.92.7.1 (SDL3 + OpenGL 3, GLFW + OpenGL 3, SDL3 + SDL_GPU; `compileOnly` + `runtimeOnly` `[1.92.7.1,1.93)`); LWJGL 3.4.1 scopes (`lwjgl-sdl` `compileOnly` in `imgui`, `lwjgl-glfw` `compileOnly` in `lwjgl` and `implementation` in `examples`); the three LWJGL displays; jMonkeyEngine 3.10.0-SNAPSHOT via `mavenLocal()` (`compileOnly` in `jme3`, `implementation` in `examples`); JUnit 6.0.0 tests in `api`/`imgui`/`lwjgl`, Mockito 5.24.0 in `api` only; Checkstyle 14.3.0 gate; Javadoc setup with the `-Xdoclint:all -Werror` gate; local publishing (`publishToMavenLocal`, POM, sources/Javadoc jars); dependency table; missing CI and remote publishing.

### Architecture (`project/architecture.md`)
Layered modules (`examples` → `loris-ui-jme3` / `loris-ui-lwjgl` → `loris-ui-imgui` → `loris-ui-api`) with a `Backend` port (`render` only), renderer/style-applier registries and the sealed `ImGuiRenderer` frame driver with three variants (`ImGuiSdlGl3Renderer`, `ImGuiGlfwGl3Renderer`, `ImGuiSdlGpuRenderer`); jME as `compileOnly` in `jme3`; per-module packages and key files (host-neutral `ScreenManager`, `DeferredRelease`, `ResourceBundleKeyResolver` in `api`; renderer lifecycle and release order, validated unsigned glyph ranges, `ImGuiFont` handles, per-backend texture cache with `long` IDs, `ImGuiOpenGlBackend`/`ImGuiSdlGpuBackend`, `DecodedImage`, Repeat tiling with `TextureTiling`, the jME display and the three LWJGL displays over a package-private base, GLFW callback chaining, four examples); setup, frame per variant (hosts pass the `SDL_GetWindowSize`/`glfwGetWindowSize` logical size; the SDL_GPU command-buffer/target contract; size and content functions receive the screen size at every nesting level, position functions only for top-level panels and popups), input and teardown flow; screen lifecycle (re-entrancy guard, two-phase deactivation, deferred teardown, jME re-attach, LWJGL final `close()` for all three displays, SDL text input, GLFW `close()` rejected inside callbacks and dispatch failures rethrown); extension points (registration, custom texture backends only with the GL3 renderers, a new graphics API needs a new sealed variant, new backend, new host); local Maven publishing with sources/Javadoc jars; known concerns (emulated jME display-scale modes above 1 unsupported, the jME display's viewport must render to the window (offscreen viewports such as render-to-texture or a `FilterPostProcessor` on the same viewport unsupported), Dear ImGui asserts end the JVM, manual-only native checks, global SDL hints and state, GLFW monitor callback, GLFW close/failure rules, GLFW HiDPI on Windows/X11, the sRGB-framebuffer requirement, `SDR_LINEAR` support, GPU release before submit, Repeat item count, texture eviction, missing images, push/pop robustness).

---

## Technical Standards

### Global Standards

Located in `.maister/docs/standards/global/`

#### Coding Style (`standards/global/coding-style.md`)
Consistent naming conventions, automated style checks (Checkstyle build gate plus `.editorconfig`, no auto-formatter), descriptive names, small focused functions, space-only 4-space indentation, removal of dead code (unused imports rejected by Checkstyle), no backward-compatibility shims unless required, and DRY (extract repeated logic).

#### Commenting (`standards/global/commenting.md`)
Let code be self-documenting, comment sparingly (explain "why", not "what"), never leave change/history comments in code, and require user-facing Javadoc on exported public API and `jme3` (contract, null semantics, threading; no implementation details; summary sentence, full `@param`/`@return`/`@throws`; `package-info.java` per exported package, module doc comments, American spelling; none needed for `impl`/private code), with shared phrasings for required arguments, optional constant overloads, Supplier getters and resource paths.

#### Conventions (`standards/global/conventions.md`)
Predictable project structure, up-to-date documentation, clean version control (clear commits, focused branches), environment variables for config/secrets, minimal dependencies, consistent code reviews, testing expectations, feature flags for incomplete work, changelog updates, building only what is needed, lightweight process for a solo maintainer (e.g. a single `./gradlew build` CI job), and never committing build or IDE outputs (`build/`, `.gradle`, `.idea/`, `*.iml`, etc.).

#### Error Handling (`standards/global/error-handling.md`)
Clear user-facing error messages, fail fast on invalid state, typed/specific exceptions, centralized error handling at boundaries, graceful degradation, retry with exponential backoff for transient failures, and guaranteed resource cleanup.

#### Minimal Implementation (`standards/global/minimal-implementation.md`)
Build only what is needed now, every piece of code has a clear purpose, delete exploration artifacts, no stubs for future features, no speculative abstractions, review diffs before commit, and treat unused code as debt.

#### Validation (`standards/global/validation.md`)
Always validate server-side, client-side validation only for feedback, validate early at boundaries, specific error messages, allowlists over blocklists, type and format checks, input sanitization, business-rule validation, and consistent enforcement across entry points.

### Architecture Standards

Located in `.maister/docs/standards/architecture/`

#### Modules (`standards/architecture/modules.md`)
Strict layered module chain (`examples` → `loris-ui-jme3` / `loris-ui-lwjgl` → `loris-ui-imgui` → `loris-ui-api`) with a zero-dependency, drawing-free `loris-ui-api` that holds the host-support helpers (`ScreenManager`, `DeferredRelease`, `ResourceBundleKeyResolver`) in exported packages because the automatic `lorisui.jme3` reads only exported packages; new backends depend only on `api`, new hosts only on `imgui`; packages `com.ivan1pl.lorisui.<module>.<area>` with `impl` subpackages; JPMS names `lorisui.<module>` without `.main`, `requires transitive` for siblings in exported signatures, plain `requires` for internal toolkits (`org.lwjgl.sdl` in `lorisui.imgui`, `org.lwjgl.glfw`/`org.lwjgl.sdl` in `lorisui.lwjgl`), `Automatic-Module-Name` for `jme3`; `impl` never exported; host-internal shared logic may be package-private in the exported package (`ImGuiLwjglDisplayBase`, `ImGuiLwjglSdlInput`).

#### Component Model (`standards/architecture/component-model.md`)
Empty marker interfaces (`Component`, `Style`, `Font`) downcast by backends; toolkit-neutral `Backend` port (`render` only) with per-backend textures through `RendererProvider` (`renderTexture(long id, ...)`) and `unloadTextures()`, both concrete backends final; input-capture queries (`wantsMouseInput`, `wantsKeyboardInput`, `wantsTextInput`, `isHoveringScrollablePanel`) on `ImGuiRenderer`; hosts pass the platform's logical window size (`SDL_GetWindowSize` / `glfwGetWindowSize`, Windows/X11 GLFW note) to the renderer variant's `render` every frame (never camera/viewport/`reshape`/framebuffer pixels), named for the jME display and all three LWJGL displays; every host provides an sRGB framebuffer (`GL_FRAMEBUFFER_SRGB`, jME gamma correction, `SDR_LINEAR` swapchain); fonts registered on the renderer as `ImGuiFont`; class-keyed `Renderer`/`StyleApplier` registry with final superclass-chain lookup and fail-fast `IllegalStateException`; checklist for built-in components/styles; custom ones via `registerRenderer`/`registerStyleApplier` on the backend or any of the four displays; a custom `ImGuiBackend` pairs only with the GL3 renderers, a new graphics API needs a new sealed `ImGuiRenderer` variant, and each bundled display uses its own backend.

### Java Standards

Located in `.maister/docs/standards/java/`

#### Language (`standards/java/language.md`)
Target the latest Java LTS only (Java 25); sealed abstract classes with static factories and nested final variants; a sealed hierarchy of top-level classes in one package (`ImGuiRenderer`) with a package-private constructor and package-private hooks; exhaustive pattern `switch` without `default`; `instanceof` patterns over casts; records only for behaviour-free data carriers (`TextureData` in `imgui.backend`, `ComboBox.Item`); unnamed `_` for ignored parameters; explicit local types with sparing `var`; loops over streams in library code.

#### Naming (`standards/java/naming.md`)
PascalCase types with role suffixes (`<Component>Style`, `<Component>Renderer`, `<Style>Applier`), `ImGui` prefix for toolkit-specific classes, camel-cased acronyms in type names (`OpenGl`, `Jme`, `Gl3`, `Lwjgl`, `Glfw`, `SdlGpu`, as in `ImGuiLwjglSdlGl3Display` and `ImGuiSdlGpuBackend`; checked by Checkstyle), platform before graphics API in variant names; full-word generic type parameters ending in `Type` (`ComponentType`, `StyleType`, `IntBiFunction<ResultType>`, `RendererType`), enforced by Checkstyle.

#### Formatting (`standards/java/formatting.md`)
Style enforced by the Checkstyle gate and `.editorconfig` (no auto-formatter); 4-space indentation, K&R braces, 120-column limit, 8-space continuation, aligned wrapped parameters, one fluent call per line, inline `{}` empty constructors; IntelliJ import layout (project/third-party block, blank line, `java.*`) sorted in case-sensitive ASCII order, no static imports, wildcard imports acceptable; member ordering from constants to nested `Builder` last.

#### Class Design (`standards/java/class-design.md`)
Private (mostly final) fields with JavaBean getters, mutable state limited to runtime UI state, unmodifiable wrapping of incoming collections; `@Override` on every override, narrowest-scope `@SuppressWarnings("unchecked")`, `@SafeVarargs` on final varargs builder methods.

#### Error Handling (`standards/java/error-handling.md`)
`IllegalStateException` for programming/lifecycle errors (before any state change), `IllegalArgumentException` for malformed arguments such as glyph ranges and `0` native handles, checked `IOException` with resource path, `RuntimeException` wrapping where checked exceptions can't propagate, balanced push/pop in `try/finally`, no custom exceptions; multi-step teardown with `DeferredRelease.runAll`/`runAfterFailure` (every step runs, first failure propagates, later ones suppressed, never self-suppressed, null steps rejected up front; release only what was set up, with fresh imgui-java Impl objects kept only after a successful init, except a failed `ImGuiImplSdlGpu3.init` shut down on the spot); deferred host teardown with `DeferredRelease.run`/`request`/`isPending` plus `ScreenManager.beginDeactivation()` (switches while pending only record); GLFW host rules (`close()` rejected inside the display's GLFW callbacks; dispatch failures caught and rethrown from the next `render()`/`close()`); unified null convention (optional `Supplier` properties default to `() -> null`, `requireNonNull` setters, null-accepting constant overloads, eager validation, required bindings checked in `build()`) and builder Javadoc rules; no nullability annotations or `Optional` fields.

### Components Standards

Located in `.maister/docs/standards/components/`

#### Builders (`standards/components/builders.md`)
Nested static `Builder` per Component/Style with private constructors, static `builder()` / `*Builder(id)` factories, prefix-free fluent setters and terminal `build()`; package-private self-typed `Panel.BaseBuilder<PanelType, BuilderType>` (with package-private `doBuild(...)`/`PanelFactory`) closing the Panel family to `api.component`; dynamic properties stored as `Supplier` / `IntBinaryOperator` / `Function<BundleKeyResolver, String>` / `Consumer` with constant overloads delegating to functional ones (e.g. ProgressBar `rounding` as `Supplier<Float>` defaulting to `() -> null`).

#### Renderers and Styles (`standards/components/renderers-and-styles.md`)
Renderer shape (`<Component>Renderer extends Renderer<...>` in `imgui.renderer.impl` with a no-argument constructor and no backend reference, child rendering via `RendererProvider`, `FLAGS_*` constants, component-derived ImGui IDs, font wrapping with `ImGuiFont.useFont` from the `imgui` package, balanced push/pop, textures through `rendererProvider.loadTexture`/`renderTexture` with `long` IDs (per-backend cache, normalized keys, no unloading while drawing; Repeat tiled by the backend, no wrap-mode dependency), internal `imgui.impl.ImGuiState`, input-capture queries on `ImGuiRenderer`); class-loader resource lookup (`ClassPathResources`, used by `DecodedImage`, leading `/` optional, `IOException` with path); Style class shape (`Supplier` attributes defaulting to `() -> null`, nullable unwrapped getters, constant overloads); `StyleApplier` shape (opaque `Styles` token counting pushed colours/vars, push only non-null, always paired with `popStyles`).

### Build Standards

Located in `.maister/docs/standards/build/`

#### Gradle (`standards/build/gradle.md`)
Gradle 9.6.0 wrapper, Groovy DSL; subprojects renamed `loris-ui-<dir>`; `java-library` then `maven-publish` for libraries, `java` for the demo, no root plugin; group, version, repositories (`mavenLocal()` first), Java 25 toolchain, Checkstyle, JUnit Platform, publishing and the Javadoc gate set once in root `allprojects` through lazy hooks; Checkstyle gate (`maxWarnings = 0`); Javadoc gate (`-Xdoclint:all -Werror`, `check` depends on `javadoc`, libraries only); local-only publishing (`maven` publication with sources/Javadoc jars, shared POM metadata, required per-module `description` + `ext.pomName` read lazily with `project.`-qualified providers, no repositories, `publishToMavenLocal`); shared versions in root `ext` referenced as `rootProject.ext.<name>` (Mockito in `api` only); dependency scopes (`api` only for exported types, toolkits `compileOnly` + capped `runtimeOnly` range resolved by runtime classpaths, `lwjgl-stb` `implementation`, per-platform natives, jME and LWJGL `compileOnly` in hosts (`lwjgl-glfw` in `lwjgl`), `lwjgl-sdl` `compileOnly` in `imgui`, `lwjgl-glfw` `implementation` in `examples`); javadoc excludes `impl` (also for future `impl` packages in `jme3`/`lwjgl`); `Automatic-Module-Name` for `jme3`; four example run tasks (`runJme3Example`, `runLwjglSdlGl3Example`, `runLwjglGlfwGl3Example`, `runLwjglSdlGpuExample`) with `--enable-native-access=ALL-UNNAMED` and the `lorisui.example.gpuDebug` system-property pass-through.

### Frontend Standards

*Not initialized for this project. If you need frontend standards, you can:*
- *Add them manually using the docs-manager skill*
- *Run `/maister:standards-discover --scope=frontend` to auto-discover*

### Backend Standards

*Not initialized for this project. If you need backend standards, you can:*
- *Add them manually using the docs-manager skill*
- *Run `/maister:standards-discover --scope=backend` to auto-discover*

### Testing Standards

Located in `.maister/docs/standards/testing/`

#### Test Writing (`standards/testing/test-writing.md`)
Behavior-focused, descriptive, fast, risk-based tests; shared JUnit 6 setup (junit-bom 6.0.0, lazy `Test` hook, same-package tests, no test `module-info`); Mockito with javaagent wiring in `api` only, hand-written fakes elsewhere (recording `Backend`, recording screens, `ImGuiBackend` subclasses), no static imports; no `imgui.ImGui.*` calls or imgui-java, GLFW or SDL types in tests; pure seams (backend registry and texture cache, masking, `TextureTiling`, missing-resource `DecodedImage`, resource lookup, `ScreenManager` incl. `beginDeactivation`, `DeferredRelease`, `ResourceBundleKeyResolver`, `ImGuiFont` glyph ranges, the GLFW display's package-private `dispatch` seam) and native-free gating tests (three renderer variants, `ImGuiSdlGpuBackend`, three LWJGL displays; static method references stay native-free); native lifecycle (GPU uploads, native GLFW chaining) verified manually; float tolerance; deadline polling, latch-gated workers and timed joins for async code.

---

## How to Use This Documentation

1. **Start Here**: Always read this INDEX.md first to understand what documentation exists
2. **Project Context**: Read relevant project documentation before starting work
3. **Standards**: This index only points to the standards — open and follow the specific standard files relevant to your task; don't rely on the index alone
4. **Keep Updated**: Update documentation when making significant changes
5. **Customize**: Adapt all documentation to your project's specific needs

## Updating Documentation

- Project documentation should be updated when goals, tech stack, or architecture changes
- Technical standards should be updated when team conventions evolve
- Always update INDEX.md when adding, removing, or significantly changing documentation
