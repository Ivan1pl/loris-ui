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
What LorisUI is (a declarative, toolkit-agnostic Java UI library for game menus/HUDs rendered through a pluggable backend, currently Dear ImGui hosted in jMonkeyEngine 3), the problem it solves, target users, key features, success criteria, differentiators, and firm constraints (dependency-free `loris-ui-api`, latest Java LTS only, solo-developer-scale process).

### Roadmap (`project/roadmap.md`)
Current state (`1.0-SNAPSHOT`, modules, components, known gaps) and dependency-ordered phases with effort estimates: Phase 1 build hygiene (Gradle toolchain done; JME3 snapshot, README, CI, formatter open), Phase 2 API stabilisation (unit tests and null-handling convention done; `LorisScreen` threading done except executor shutdown; texture unload/eviction, Javadoc, JPMS review open), Phase 3 publishing (Maven publication, dependency scopes, versioning/changelog), Phase 4 additional backends/hosts, plus future enhancements.

### Tech Stack (`project/tech-stack.md`)
Java 25 (LTS-only policy; records, sealed types, pattern matching, JPMS) selected through a Gradle toolchain (JDK 25 must be installed, no auto-provisioning), Gradle 9.6.0 Groovy DSL multi-project build, Dear ImGui via imgui-java 1.92.7.1 (SDL3 + OpenGL 3), LWJGL 3.4.1, jMonkeyEngine 3.10.0-SNAPSHOT (via `mavenLocal()`), JUnit 6.0.0 and Mockito 5.24.0 (javaagent) with tests in `api`/`imgui`; key dependency table with module/scope, version management, and missing tooling (no CI, formatter, or publishing config yet).

### Architecture (`project/architecture.md`)
Layered multi-module design (`examples` → `loris-ui-jme3` → `loris-ui-imgui` → `loris-ui-api`) with a ports-and-adapters `Backend` abstraction and renderer/style-applier registries; per-module packages, JPMS exports and key files; setup, per-frame render and input data flow (layout failures rethrown from `render()`, frame always closed); extension points (new component, style, backend, host); known concerns (never-shut-down layout executor, render-thread-only but unevicted texture cache, texture leaks, ImGui leaking into the API, renderer push/pop robustness and shared `"##"` input label, jME `--patch-module` workaround).

---

## Technical Standards

### Global Standards

Located in `.maister/docs/standards/global/`

#### Coding Style (`standards/global/coding-style.md`)
Consistent naming conventions, automatic formatting, descriptive names, small focused functions, uniform indentation, removal of dead code, no backward-compatibility shims unless required, and DRY (extract repeated logic).

#### Commenting (`standards/global/commenting.md`)
Let code be self-documenting, comment sparingly (explain "why", not "what"), never leave change/history comments in code, and require Javadoc on exported public API (contract, null semantics, threading; none needed for `impl`/private code).

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
Strict layered module chain (`examples` → `loris-ui-jme3` → `loris-ui-imgui` → `loris-ui-api`) with a zero-dependency, drawing-free `loris-ui-api`; package structure under `com.ivan1pl.lorisui.<module>.<area>` with `impl` subpackages; JPMS module names `lorisui.<module>` without a `.main` suffix; only public contract packages exported, `impl` never exported.

#### Component Model (`standards/architecture/component-model.md`)
Empty marker interfaces (`Component`, `Style`, `Font`) downcast by backends, abstract classes for shared behaviour vs. interfaces for stateless contracts; class-keyed `Renderer`/`StyleApplier` registry in `ImGuiBackend` with superclass-chain lookup and fail-fast `IllegalStateException`; checklist for adding a new component or style.

### Java Standards

Located in `.maister/docs/standards/java/`

#### Language (`standards/java/language.md`)
Target the latest Java LTS only (Java 25); sealed abstract classes with static factories and nested final variants; exhaustive pattern `switch` without `default`; `instanceof` patterns over casts; records only for behaviour-free data carriers; unnamed `_` for ignored parameters; explicit local types with sparing `var`; loops over streams in library code.

#### Naming (`standards/java/naming.md`)
PascalCase types with role suffixes (`<Component>Style`, `<Component>Renderer`, `<Style>Applier`), `ImGui` prefix for toolkit-specific classes, camel-cased acronyms (`OpenGl`, `Jme`); full-word generic type parameters ending in `Type` (`ComponentType`, `StyleType`).

#### Formatting (`standards/java/formatting.md`)
4-space indentation, K&R braces, 120-column limit, 8-space continuation, aligned wrapped parameters, one fluent call per line, inline `{}` empty constructors; IntelliJ import layout (project/third-party block, blank line, `java.*`), no static or wildcard imports; member ordering from constants to nested `Builder` last.

#### Class Design (`standards/java/class-design.md`)
Private (mostly final) fields with JavaBean getters, mutable state limited to runtime UI state, unmodifiable wrapping of incoming collections; `@Override` on every override, narrowest-scope `@SuppressWarnings("unchecked")`, `@SafeVarargs` on final varargs builder methods.

#### Error Handling (`standards/java/error-handling.md`)
`IllegalStateException` for programming/lifecycle errors, checked `IOException` with resource path for loading, `RuntimeException` wrapping where checked exceptions can't propagate, try-with-resources and balanced ImGui push/pop in `try/finally`, no custom exceptions by default; unified null convention: optional `Supplier` properties default to `() -> null` with plain `.get()` getters, Supplier setters `requireNonNull`, constant overloads of optional properties accept `null` as unset, required arguments validated eagerly (before wrapping in lambdas) and skippable required bindings checked in `build()`; canonical builder Javadoc sentence; no nullability annotations or `Optional` fields/parameters.

### Components Standards

Located in `.maister/docs/standards/components/`

#### Builders (`standards/components/builders.md`)
Nested static `Builder` per Component/Style with private constructors, static `builder()` / `*Builder(id)` factories, prefix-free fluent setters and terminal `build()`; self-typed `Panel.BaseBuilder` with `doBuild(...)` for Panel subclasses; dynamic properties stored as `Supplier` / `IntBinaryOperator` / `Function<BundleKeyResolver, String>` / `Consumer` with constant overloads delegating to functional ones.

#### Renderers and Styles (`standards/components/renderers-and-styles.md`)
Renderer shape (`<Component>Renderer extends Renderer<...>` in `imgui.renderer.impl`, child rendering via `RendererProvider`, `FLAGS_*` constants, component-derived ImGui IDs, font wrapping, balanced push/pop, cached texture loading); Style class shape (`Supplier` attributes defaulting to `() -> null`, nullable unwrapped getters, constant overloads); `StyleApplier` shape (opaque `Styles` token counting pushed colours/vars, push only non-null, always paired with `popStyles`).

### Build Standards

Located in `.maister/docs/standards/build/`

#### Gradle (`standards/build/gradle.md`)
Gradle wrapper pinned to 9.6.0 with Groovy DSL; short subproject directories renamed to `loris-ui-<dir>` for published modules; `java-library` + `maven-publish` for libraries, `java` for the demo; group, version, repositories (`mavenLocal()` first, then `mavenCentral()`), Java 25 toolchain and JUnit Platform set once in root `allprojects` through lazy hooks (`plugins.withId('java')`, `tasks.withType(Test).configureEach`; no plain `java {}`/`test {}` there); shared versions (`jmonkeyVersion`, `lwjglVersion`, `mockitoVersion`) in root `ext`, referenced as `rootProject.ext.<name>`; jME3 dependencies `transitive = false` in the library module.

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
Test behavior rather than implementation, clear descriptive test names, mock external dependencies, keep tests fast, risk-based test prioritization, balance coverage against velocity, focus on critical paths, choose appropriate test depth (unit vs. integration vs. end-to-end), and the shared JUnit 6 setup (junit-bom 6.0.0, junit-jupiter, platform launcher via the lazy `Test` hook; same-package tests in `<module>/src/test/java`, no test `module-info`); Mockito with javaagent wiring per test-bearing module, hand-written fakes where simpler, no static imports; no `imgui.ImGui.*` calls in tests; unique paths for texture-cache tests; float tolerance; deadline polling for async code.

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
