# Technology Stack

## Overview
This document describes the technology choices and rationale for LorisUI, a multi-module Java UI library. A toolkit-agnostic API is rendered through Dear ImGui and hosted in jMonkeyEngine 3.

## Languages

### Java 25 (LTS)
- **Usage**: 100% of source code (72 main source files, about 8.2k lines including Javadoc: api about 5.3k, imgui about 2.0k, jme3 about 0.45k, examples about 0.45k; plus 23 test classes)
- **Rationale**: modern language features and a JPMS module system. Policy: **track the latest LTS release** and never target non-LTS versions.
- **Key Features Used**:
  - Records (`TextureData` in `imgui.backend`, `ComboBox.Item`)
  - Sealed class hierarchies (`ImageMode` and its final nested variants)
  - Pattern-matching `switch`, including unnamed patterns `_` (Java 22+)
  - Instance/static `main()` without arguments (final in Java 25), used in `LorisJme3Example`
  - `var`, lambdas, generics with self-typed builders (`BaseBuilder<PanelType, BuilderType>`)
  - JPMS `module-info.java` in `api` (`lorisui.api`) and `imgui` (`lorisui.imgui`, `requires transitive lorisui.api`); `jme3` is an automatic module (`Automatic-Module-Name: lorisui.jme3`)
- **Toolchain**: root `allprojects` sets a Java 25 Gradle toolchain for every module with the `java` plugin, so the JDK that runs Gradle does not matter. A JDK 25 must be installed locally: there is no foojay resolver, so Gradle does not download JDKs.
  - If Gradle selects a JDK it auto-provisioned earlier (under `~/.gradle/jdks`), it prints a Gradle 10 deprecation warning about auto-provisioning without toolchain repositories. Fix it by deleting that JDK from `~/.gradle/jdks`; Gradle then auto-detects the system JDK 25.

### Gradle Groovy DSL
- Build scripts: `build.gradle`, `settings.gradle`, `*/build.gradle`

## Frameworks

### UI Toolkit
- **Dear ImGui** via `io.github.spair:imgui-java-app` **1.92.7.1**: immediate-mode GUI used by the `imgui` backend. LorisUI uses the SDL3 and OpenGL 3 platform/renderer implementations (`imgui.gl3`, `imgui.sdl3`), wrapped in `ImGuiOpenGlRenderer`.
  - No ImGui type appears in an exported signature, so `loris-ui-imgui` compiles against the pinned version (`compileOnly`) and declares a `runtimeOnly` rich-version range (`require "[1.92.7.1,1.93)"`, `prefer "1.92.7.1"`). The range is capped below the next minor because imgui-java is not semver and a published POM carries only the range (Maven ignores `prefer`); raise the cap when moving to a new imgui-java minor. Consumers get imgui-java at runtime but not on their compile classpath; the version is `ext.imguiVersion`.
  - Code that calls Dear ImGui directly (a custom `Renderer`) adds imgui-java to its own build and, on the module path, `requires imgui.binding`.
  - On JDK 25, imgui-java and LWJGL load native code, so applications pass `--enable-native-access` to avoid restricted-method warnings.
  - **Offline builds**: resolving the range needs Gradle's version listing. The first build on a machine, and the first build after Gradle's dynamic-version cache expires, need network access; after that `--offline` works and `prefer` keeps builds on 1.92.7.1.

### Graphics / Native
- **LWJGL** **3.4.1**: `lwjgl-stb` (an `api` dependency of `loris-ui-imgui`) for image decoding when loading textures, plus the OpenGL bindings (through imgui-java). `lwjgl-stb` native classifiers for Windows, Linux, macOS and macOS arm64 (`natives-windows`, `natives-linux`, `natives-macos`, `natives-macos-arm64`) are declared as `runtimeOnly`. `loris-ui-jme3` compiles against `lwjgl` and `lwjgl-sdl` as `compileOnly`, because jME's `SdlEventListener` signature uses `SDL_Event`; at runtime the application's `jme3-lwjgl3` supplies them.

### Game Engine Host
- **jMonkeyEngine** **3.10.0-SNAPSHOT**: `jme3-core` and `jme3-lwjgl3` (in `jme3`, with `transitive = false`), plus `jme3-desktop` in `examples`. LorisUI integrates as a `SceneProcessor` (`ImGuiJmeDisplay`).
  - The snapshot isn't on Maven Central. It's resolved from `mavenLocal()`, so jME must be installed locally first.
  - jME3 jars split packages, so `loris-ui-jme3` stays on the classpath (no `module-info.java`) and sets `Automatic-Module-Name: lorisui.jme3` in its jar manifest.

### Testing
- **JUnit 6.0.0** (Jupiter) through `junit-bom`, with `junit-platform-launcher`, declared in every module. The JUnit Platform is enabled for all `Test` tasks by the lazy `tasks.withType(Test).configureEach { useJUnitPlatform() }` hook in root `allprojects`.
- **Mockito 5.24.0** (`mockito-core`, version in root `ext`) in the test-bearing modules (`api`, `imgui`). Each loads Mockito as a `-javaagent` from a non-transitive `mockitoAgent` configuration, avoiding the JDK 21+ dynamic-agent warning.
- **Status**: unit tests exist in `api` (builders, styles, `Color`, `ImageMode`, `LorisScreen`) and `imgui` (`ImGuiBackend` lookup, texture cache, texture masking, class-path resource lookup). They don't need a GL context or imgui-java on the test compile classpath. `jme3` and `examples` have no tests.

## Database
None. LorisUI is a client-side UI library.

## Build Tools & Package Management
- **Gradle 9.6.0** (wrapper committed)
- Multi-project build: `:api` → `loris-ui-api`, `:imgui` → `loris-ui-imgui`, `:jme3` → `loris-ui-jme3`, `:examples`
- Plugins: `java-library` + `maven-publish` (library modules), `java` (examples, root)
- Repositories: `mavenLocal()` then `mavenCentral()`
- Group `com.ivan1pl.loris-ui`, version `1.0-SNAPSHOT`
- Shared versions are set as root `ext` properties (`jmonkeyVersion`, `lwjglVersion`, `mockitoVersion`, `imguiVersion`) and referenced as `rootProject.ext.<name>`. There is no version catalog.
- Root `allprojects` also sets the Java 25 toolchain and JUnit Platform through lazy hooks (`plugins.withId('java')`, `tasks.withType(Test).configureEach`)

## Infrastructure

### Containerization
None.

### CI/CD
None yet (see the roadmap, Phase 1).

### Distribution
`maven-publish` is applied to `api`, `imgui` and `jme3`, but no `publishing {}` block is configured yet. The goal is to publish to a Maven repository.

### Javadoc
Every exported package of `lorisui.api` and `lorisui.imgui`, and the `jme3` package, has user-facing Javadoc, a `package-info.java`, and (for the two named modules) a module doc comment. The `imgui` `javadoc` task excludes `**/impl/**` and passes the main source directories as `-sourcepath`, so the generated docs contain only the exported packages. There is no doclint gate in the build.

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
| `io.github.spair:imgui-java-app` | 1.92.7.1 (runtime: `[1.92.7.1,1.93)`, prefer 1.92.7.1) | imgui | compileOnly + runtimeOnly |
| `org.lwjgl:lwjgl-stb` (+ natives) | 3.4.1 | imgui | api / runtimeOnly |
| `org.lwjgl:lwjgl`, `org.lwjgl:lwjgl-sdl` | 3.4.1 | jme3 | compileOnly |
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
*Last Updated*: 2026-10-11
*Auto-detected*: languages, versions, modules, dependencies, build config, testing setup (from build files and sources)
*User-provided*: LTS-only Java policy, dependency-free `api` constraint, publishing goal
