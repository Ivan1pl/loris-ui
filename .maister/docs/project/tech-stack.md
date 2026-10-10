# Technology Stack

## Overview
This document describes the technology choices and rationale for LorisUI, a multi-module Java UI library. A toolkit-agnostic API is rendered through Dear ImGui and hosted in jMonkeyEngine 3.

## Languages

### Java 25 (LTS)
- **Usage**: 100% of source code (57 files, about 4.7k LOC: api about 2.5k, imgui about 1.5k, jme3 about 0.2k, examples about 0.4k)
- **Rationale**: modern language features and a JPMS module system. Policy: **track the latest LTS release** and never target non-LTS versions.
- **Key Features Used**:
  - Records (`TextureData`)
  - Sealed class hierarchies (`ImageMode` and its final nested variants)
  - Pattern-matching `switch`, including unnamed patterns `_` (Java 22+)
  - Instance/static `main()` without arguments (final in Java 25), used in `LorisJme3Example`
  - `var`, lambdas, generics with self-typed builders (`BaseBuilder<T, B>`)
  - JPMS `module-info.java` in `api` (`lorisui.api.main`) and `imgui` (`lorisui.imgui.main`)
- **Toolchain**: root `allprojects` sets a Java 25 Gradle toolchain for every module with the `java` plugin, so the JDK that runs Gradle no longer matters. A JDK 25 must be installed locally: there is no foojay resolver, so Gradle does not download JDKs.
  - If Gradle selects a JDK it auto-provisioned earlier (under `~/.gradle/jdks`), it prints a Gradle 10 deprecation warning about auto-provisioning without toolchain repositories. Fix it by deleting that JDK from `~/.gradle/jdks`; Gradle then auto-detects the system JDK 25.

### Gradle Groovy DSL
- Build scripts: `build.gradle`, `settings.gradle`, `*/build.gradle`

## Frameworks

### UI Toolkit
- **Dear ImGui** via `io.github.spair:imgui-java-app` **1.92.7.1**: immediate-mode GUI used by the `imgui` backend. LorisUI uses the SDL3 and OpenGL 3 platform/renderer implementations (`imgui.gl3`, `imgui.sdl3`), wrapped in `ImGuiOpenGlRenderer`.
  - Declared as an `api` dependency of `loris-ui-imgui`, so ImGui types are visible to consumers.

### Graphics / Native
- **LWJGL** **3.4.1**: `lwjgl-stb` for image decoding when loading textures, plus the OpenGL bindings (through imgui-java). Native classifiers for Windows, Linux and macOS are declared as `runtimeOnly`.

### Game Engine Host
- **jMonkeyEngine** **3.10.0-SNAPSHOT**: `jme3-core` and `jme3-lwjgl3` (in `jme3`, with `transitive = false`), plus `jme3-desktop` in `examples`. LorisUI integrates as a `SceneProcessor` (`ImGuiJmeDisplay`).
  - The snapshot isn't on Maven Central. It's resolved from `mavenLocal()`, so jME must be installed locally first.
  - `jme3/build.gradle` passes `--patch-module jme3.core=<jme3-lwjgl3 jar>` to the compiler.

### Testing
- **JUnit 6.0.0** (Jupiter) through `junit-bom`, with `junit-platform-launcher`, declared in every module. The JUnit Platform is enabled for all `Test` tasks by the lazy `tasks.withType(Test).configureEach { useJUnitPlatform() }` hook in root `allprojects`.
- **Mockito 5.24.0** (`mockito-core`, version in root `ext`) in the test-bearing modules (`api`, `imgui`). Each loads Mockito as a `-javaagent` from a non-transitive `mockitoAgent` configuration, avoiding the JDK 21+ dynamic-agent warning.
- **Status**: unit tests exist in `api` (builders, styles, `Color`, `ImageMode`, `LorisScreen`) and `imgui` (`ImGuiBackend` lookup, texture cache, texture masking). They don't need a GL context. `jme3` and `examples` have no tests.

## Database
None. LorisUI is a client-side UI library.

## Build Tools & Package Management
- **Gradle 9.6.0** (wrapper committed)
- Multi-project build: `:api` → `loris-ui-api`, `:imgui` → `loris-ui-imgui`, `:jme3` → `loris-ui-jme3`, `:examples`
- Plugins: `java-library` + `maven-publish` (library modules), `java` (examples, root)
- Repositories: `mavenLocal()` then `mavenCentral()`
- Group `com.ivan1pl.loris-ui`, version `1.0-SNAPSHOT`
- Shared versions are set as root `ext` properties (`jmonkeyVersion`, `lwjglVersion`, `mockitoVersion`) and referenced as `rootProject.ext.<name>`. There is no version catalog.
- Root `allprojects` also sets the Java 25 toolchain and JUnit Platform through lazy hooks (`plugins.withId('java')`, `tasks.withType(Test).configureEach`)

## Infrastructure

### Containerization
None.

### CI/CD
None yet (see the roadmap, Phase 1).

### Distribution
`maven-publish` is applied to `api`, `imgui` and `jme3`, but no `publishing {}` block is configured yet. The goal is to publish to a Maven repository.

## Development Tools

### IDE
IntelliJ IDEA (`.idea/`, git-ignored), with the project language level set to JDK 25.

### Linting & Formatting
None configured. By convention the code uses 4-space indentation and lines of about 120 columns.

### Type Checking
Provided by the Java compiler. No nullability annotations or static analysers are used.

## Key Dependencies

| Dependency | Version | Module | Scope |
|---|---|---|---|
| `io.github.spair:imgui-java-app` | 1.92.7.1 | imgui | api |
| `org.lwjgl:lwjgl-stb` (+ natives) | 3.4.1 | imgui | api / runtimeOnly |
| `org.jmonkeyengine:jme3-core` | 3.10.0-SNAPSHOT | jme3, examples | implementation |
| `org.jmonkeyengine:jme3-lwjgl3` | 3.10.0-SNAPSHOT | jme3, examples | implementation |
| `org.jmonkeyengine:jme3-desktop` | 3.10.0-SNAPSHOT | examples | implementation |
| `org.junit:junit-bom` | 6.0.0 | all | test |
| `org.mockito:mockito-core` | 5.24.0 | api, imgui | test (+ `mockitoAgent` javaagent) |

**`loris-ui-api` has no external dependencies, and this is a firm architectural constraint.**

## Version Management
- Library versions: root `build.gradle` `ext` block plus inline coordinates
- Project version: `allprojects { version = '1.0-SNAPSHOT' }`
- Java: latest LTS (currently 25). Upgrade when the next LTS ships (expected: Java 29).

---
*Last Updated*: 2026-10-10
*Auto-detected*: languages, versions, modules, dependencies, build config, testing setup (from build files and sources)
*User-provided*: LTS-only Java policy, dependency-free `api` constraint, publishing goal
