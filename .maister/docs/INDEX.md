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
What LorisUI is (a declarative, toolkit-agnostic Java UI library for game menus/HUDs rendered through a pluggable backend whose port is a single toolkit-neutral `render` call, currently Dear ImGui hosted in jMonkeyEngine 3), the problem it solves, target users, key features (custom components/styles via an `ImGuiBackend` subclass, jME input routing and screen lifecycle hooks), success criteria, differentiators, and firm constraints (dependency-free `loris-ui-api` / `lorisui.api`, latest Java LTS only, solo-developer-scale process).

### Roadmap (`project/roadmap.md`)
Current state (`1.0-SNAPSHOT`, modules `lorisui.api`/`lorisui.imgui`/automatic `lorisui.jme3`, components, known gaps) and dependency-ordered phases with effort estimates: Phase 1 build hygiene (Gradle toolchain done; JME3 snapshot, README with JDK 25 `--enable-native-access` and module-path notes, CI, formatter, code hygiene open), Phase 2 API stabilisation (unit tests, null handling, Javadoc, JPMS cleanup and jME screen lifecycle done; executor shutdown, texture unload/eviction on `ImGuiBackend` with path-key normalization, renderer teardown and re-attach, `setCurrentScreen` re-entrancy guard, fail-once missing images, optional bundle-name validation, jme3 `module-info`, B2 popup open flag, B3 panel IDs, input-validation gaps, ComboBox 0×0 default, content-function size arguments open), Phase 3 publishing (dependency-scope review with the `[1.92.7.1,1.93)` cap and bump policy done; publishing with a doclint gate, `lwjgl-stb` scope, versioning/changelog open), Phase 4 backends/hosts (backend validation done; registration API, custom backend in the jME host, neutral host SPI, B4 neutral fonts, B6 neutral styles, second host/backend open), plus future enhancements.

### Tech Stack (`project/tech-stack.md`)
Java 25 (LTS-only policy; records, sealed types, pattern matching, JPMS modules `lorisui.api`/`lorisui.imgui` with `requires transitive`, jme3 automatic module `lorisui.jme3`) selected through a Gradle toolchain (JDK 25 must be installed, no auto-provisioning), Gradle 9.6.0 Groovy DSL multi-project build, Dear ImGui via imgui-java 1.92.7.1 (SDL3 + OpenGL 3; `compileOnly` + `runtimeOnly` `[1.92.7.1,1.93)` range preferring 1.92.7.1, capped below the next minor, network needed for the first build/expired dynamic-version cache; custom renderers add imgui-java themselves; JDK 25 `--enable-native-access`), LWJGL 3.4.1 (`lwjgl-stb` `api` with Windows/Linux/macOS/macOS arm64 natives; `lwjgl`/`lwjgl-sdl` `compileOnly` in jme3), jMonkeyEngine 3.10.0-SNAPSHOT (via `mavenLocal()`), JUnit 6.0.0 and Mockito 5.24.0 (javaagent) with tests in `api`/`imgui`; Javadoc setup (`impl` excluded, no doclint gate), key dependency table with module/scope, version management (`imguiVersion` in root `ext`), and missing tooling (no CI, formatter, or publishing config yet).

### Architecture (`project/architecture.md`)
Layered multi-module design (`examples` → `loris-ui-jme3` → `loris-ui-imgui` → `loris-ui-api`) with a ports-and-adapters `Backend` port (`render` only) and renderer/style-applier registries; per-module packages, JPMS modules (`lorisui.api`, `lorisui.imgui` with `requires transitive`, automatic `lorisui.jme3`), exports and key files (texture API and `TextureData` in `imgui.backend`, internal `imgui.impl` `ImGuiState`/`ClassPathResources`, `ImGuiOpenGlRenderer` input-capture queries, one renderer in use at a time; imgui-java runtime-only); setup, per-frame render and input data flow (layout failures rethrown from `render()`, missing images fail every draw, frame always closed); jME screen lifecycle (registration/switch validation, start/end hooks paired only for normally returned starts, exception-safe switches, single-use display after `cleanup` or failed initialization, replacement display leaking until renderer teardown, no `cleanup` on exit, `destroy()` removal pattern); extension points (built-in component/style, custom components via an `ImGuiBackend` subclass that also implements `loadTexture`, final `ImGuiOpenGlBackend`, jME host not pluggable yet, imgui-java on the custom renderer's build, backend, host); known concerns (never-shut-down layout executor, unevicted static texture cache keyed by raw path, texture leaks, no renderer/ImGui context teardown, re-entrant `setCurrentScreen` edge, missing images not failing once, renderer push/pop robustness and shared `"##"` input label).

---

## Technical Standards

### Global Standards

Located in `.maister/docs/standards/global/`

#### Coding Style (`standards/global/coding-style.md`)
Consistent naming conventions, automatic formatting, descriptive names, small focused functions, uniform indentation, removal of dead code, no backward-compatibility shims unless required, and DRY (extract repeated logic).

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
Strict layered module chain (`examples` → `loris-ui-jme3` → `loris-ui-imgui` → `loris-ui-api`) with a zero-dependency, drawing-free `loris-ui-api`; package structure under `com.ivan1pl.lorisui.<module>.<area>` with `impl` subpackages (including `imgui.impl`); JPMS module names `lorisui.<module>` without a `.main` suffix, `requires transitive` for siblings in exported signatures, `Automatic-Module-Name` for modules that can't be named (`jme3`); only public contract packages exported, `impl` never exported.

#### Component Model (`standards/architecture/component-model.md`)
Empty marker interfaces (`Component`, `Style`, `Font`) downcast by backends, abstract classes for shared behaviour vs. interfaces for stateless contracts; toolkit-neutral `Backend` port (`render` only) with texture methods and `TextureData` on `ImGuiBackend`/`imgui.backend` and input-capture queries on `ImGuiOpenGlRenderer`; class-keyed `Renderer`/`StyleApplier` registry in `ImGuiBackend` with superclass-chain lookup and fail-fast `IllegalStateException`; checklist for adding a built-in component or style; custom components/styles via an `ImGuiBackend` subclass overriding `getRenderer`/`getStyleApplier` (delegating to `super`) and implementing `loadTexture` (`ImGuiOpenGlBackend` is final; the jME host does not accept a custom backend yet; custom renderers add imgui-java to their own build).

### Java Standards

Located in `.maister/docs/standards/java/`

#### Language (`standards/java/language.md`)
Target the latest Java LTS only (Java 25); sealed abstract classes with static factories and nested final variants; exhaustive pattern `switch` without `default`; `instanceof` patterns over casts; records only for behaviour-free data carriers (`TextureData` in `imgui.backend`, `ComboBox.Item`); unnamed `_` for ignored parameters; explicit local types with sparing `var`; loops over streams in library code.

#### Naming (`standards/java/naming.md`)
PascalCase types with role suffixes (`<Component>Style`, `<Component>Renderer`, `<Style>Applier`), `ImGui` prefix for toolkit-specific classes, camel-cased acronyms (`OpenGl`, `Jme`); full-word generic type parameters ending in `Type` (`ComponentType`, `StyleType`).

#### Formatting (`standards/java/formatting.md`)
4-space indentation, K&R braces, 120-column limit, 8-space continuation, aligned wrapped parameters, one fluent call per line, inline `{}` empty constructors; IntelliJ import layout (project/third-party block, blank line, `java.*`), no static or wildcard imports; member ordering from constants to nested `Builder` last.

#### Class Design (`standards/java/class-design.md`)
Private (mostly final) fields with JavaBean getters, mutable state limited to runtime UI state, unmodifiable wrapping of incoming collections; `@Override` on every override, narrowest-scope `@SuppressWarnings("unchecked")`, `@SafeVarargs` on final varargs builder methods.

#### Error Handling (`standards/java/error-handling.md`)
`IllegalStateException` for programming/lifecycle errors, checked `IOException` with resource path for loading, `RuntimeException` wrapping where checked exceptions can't propagate, try-with-resources and balanced ImGui push/pop in `try/finally`, no custom exceptions by default; unified null convention: optional `Supplier` properties default to `() -> null` with plain `.get()` getters, Supplier setters `requireNonNull`, constant overloads of optional properties accept `null` as unset, required arguments validated eagerly (before wrapping in lambdas) and skippable required bindings checked in `build()`; builder Javadoc states what is built, the factory, required properties and defaults, with per-setter null/validation behavior in `@param`/`@throws`; no nullability annotations or `Optional` fields/parameters.

### Components Standards

Located in `.maister/docs/standards/components/`

#### Builders (`standards/components/builders.md`)
Nested static `Builder` per Component/Style with private constructors, static `builder()` / `*Builder(id)` factories, prefix-free fluent setters and terminal `build()`; package-private self-typed `Panel.BaseBuilder<PanelType, BuilderType>` (with package-private `doBuild(...)`/`PanelFactory`) closing the Panel family to `api.component`; dynamic properties stored as `Supplier` / `IntBinaryOperator` / `Function<BundleKeyResolver, String>` / `Consumer` with constant overloads delegating to functional ones (e.g. ProgressBar `rounding` as `Supplier<Float>` defaulting to `() -> null`).

#### Renderers and Styles (`standards/components/renderers-and-styles.md`)
Renderer shape (`<Component>Renderer extends Renderer<...>` in `imgui.renderer.impl`, child rendering via `RendererProvider`, `FLAGS_*` constants, component-derived ImGui IDs, font wrapping, balanced push/pop, cached `Renderer.loadTexture(ImGuiBackend, ...)` plus `ImGuiBackend.renderTexture`, internal `imgui.impl.ImGuiState`); class-loader resource lookup (`ClassPathResources`, leading `/` optional, `IOException` with path); Style class shape (`Supplier` attributes defaulting to `() -> null`, nullable unwrapped getters, constant overloads); `StyleApplier` shape (opaque `Styles` token counting pushed colours/vars, push only non-null, always paired with `popStyles`).

### Build Standards

Located in `.maister/docs/standards/build/`

#### Gradle (`standards/build/gradle.md`)
Gradle wrapper pinned to 9.6.0 with Groovy DSL; short subproject directories renamed to `loris-ui-<dir>` for published modules; `java-library` + `maven-publish` for libraries, `java` for the demo; group, version, repositories (`mavenLocal()` first, then `mavenCentral()`), Java 25 toolchain and JUnit Platform set once in root `allprojects` through lazy hooks (`plugins.withId('java')`, `tasks.withType(Test).configureEach`; no plain `java {}`/`test {}` there); shared versions (`jmonkeyVersion`, `lwjglVersion`, `mockitoVersion`, `imguiVersion`) in root `ext`, referenced as `rootProject.ext.<name>`; jME3 dependencies `transitive = false` in the library module; dependency scopes (`api` only for types in exported signatures, toolkit libraries `compileOnly` + `runtimeOnly` rich-version range capped below the next minor (not semver; Maven ignores `prefer`) with the offline-build note, per-platform `runtimeOnly` natives including `natives-macos-arm64`, application-supplied LWJGL `compileOnly` in jme3); javadoc `exclude '**/impl/**'` with `addPathOption('sourcepath')`; jar `Automatic-Module-Name` for non-modular libraries.

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
Test behavior rather than implementation, clear descriptive test names, mock external dependencies, keep tests fast, risk-based test prioritization, balance coverage against velocity, focus on critical paths, choose appropriate test depth (unit vs. integration vs. end-to-end), and the shared JUnit 6 setup (junit-bom 6.0.0, junit-jupiter, platform launcher via the lazy `Test` hook; same-package tests in `<module>/src/test/java`, no test `module-info`); Mockito with javaagent wiring per test-bearing module, hand-written fakes where simpler (a recording `Backend` implements only `render`), no static imports; no `imgui.ImGui.*` calls and no imgui-java types in tests (`compileOnly`); texture-cache tests mock `ImGuiBackend` and use unique paths; float tolerance; deadline polling for async code.

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
