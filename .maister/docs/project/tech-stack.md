# Technology Stack

## Overview
This document describes the technology choices and rationale for LorisUI, a multi-module Java UI library. A toolkit-agnostic API is rendered through Dear ImGui and hosted in jMonkeyEngine 3 or in an application's own LWJGL/SDL3 loop.

## Languages

### Java 25 (LTS)
- **Usage**: 100% of source code (main and test sources of every module)
- **Rationale**: modern language features and a JPMS module system. Policy: **track the latest LTS release** and never target non-LTS versions.
- **Key Features Used**:
  - Records (`TextureData` in `imgui.backend`, `ComboBox.Item`)
  - Sealed class hierarchies (`ImageMode` and its final nested variants)
  - Pattern-matching `switch`, including unnamed patterns `_` (Java 22+)
  - Instance/static `main()` without arguments (final in Java 25), used in `LorisJme3Example` and `LorisLwjglExample`
  - `var`, lambdas, generics with self-typed builders (`BaseBuilder<PanelType, BuilderType>`)
  - JPMS `module-info.java` in `api` (`lorisui.api`), `imgui` (`lorisui.imgui`, `requires transitive lorisui.api`) and `lwjgl` (`lorisui.lwjgl`, `requires transitive lorisui.imgui`); `jme3` is an automatic module (`Automatic-Module-Name: lorisui.jme3`)
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
  - **Offline builds**: the range needs network access the first time runtime classpaths resolve it; see "Dependency Scopes for Toolkit Libraries" in `standards/build/gradle.md`.

### Graphics / Native
- **LWJGL** **3.4.1**: `lwjgl-stb` (an `implementation` dependency of `loris-ui-imgui`, so consumers get it at runtime only) for image decoding when loading textures, plus the OpenGL and SDL bindings (through imgui-java). `lwjgl-stb` native classifiers for Windows, Linux, macOS and macOS arm64 (`natives-windows`, `natives-linux`, `natives-macos`, `natives-macos-arm64`) are declared as `runtimeOnly`.
  - `loris-ui-jme3` compiles against `lwjgl` and `lwjgl-sdl` as `compileOnly`, because jME's `SdlEventListener` signature uses `SDL_Event`; at runtime the application's `jme3-lwjgl3` supplies them.
  - `loris-ui-lwjgl` compiles against `lwjgl` and `lwjgl-sdl` as `compileOnly` (it reads SDL event types); at runtime they come with imgui-java, whose POM brings `lwjgl`, `lwjgl-sdl` and `lwjgl-opengl` with their natives.
  - `examples` declares `lwjgl`, `lwjgl-sdl` and `lwjgl-opengl` as `implementation`, because `LorisLwjglExample` creates its own SDL window and OpenGL context.

### Game Engine Host
- **jMonkeyEngine** **3.10.0-SNAPSHOT**: `jme3-core` and `jme3-lwjgl3`, non-transitive `compileOnly` in `jme3`, so the published `loris-ui-jme3` carries no jME dependency and the application declares the jME modules it uses; `examples` declares `jme3-core`, `jme3-lwjgl3` and `jme3-desktop` as `implementation`. LorisUI integrates as a `SceneProcessor` (`ImGuiJmeDisplay`).
  - The snapshot isn't on Maven Central. It's resolved from `mavenLocal()`, so jME must be installed locally first.
  - jME3 jars split packages, so `loris-ui-jme3` stays on the classpath (no `module-info.java`) and sets `Automatic-Module-Name: lorisui.jme3` in its jar manifest.

### Standalone Host
- **LWJGL SDL3** (no engine): `loris-ui-lwjgl` (`ImGuiLwjglDisplay`) is embedded in an application that owns the SDL window, its OpenGL context and the loop. It is a named module (`lorisui.lwjgl`) and depends only on `loris-ui-imgui`.

### Testing
- **JUnit 6.0.0** (Jupiter) through `junit-bom`, with `junit-platform-launcher`, declared in every Java module. The JUnit Platform is enabled for all `Test` tasks by the lazy `tasks.withType(Test).configureEach { useJUnitPlatform() }` hook in root `allprojects`.
- **Mockito 5.24.0** (`mockito-core`, version in root `ext`) declared only in `api`, whose tests use it for callbacks. `api` loads Mockito as a `-javaagent` from a non-transitive `mockitoAgent` configuration, avoiding the JDK 21+ dynamic-agent warning. `imgui` and `lwjgl` use JUnit only, with hand-written fakes.
- **Status**: unit tests exist in `api` (builders, styles, `Color`, `ImageMode`, `LorisScreen`, `ScreenManager` including two-phase deactivation, `DeferredRelease`, `ResourceBundleKeyResolver`), `imgui` (`ImGuiBackend` lookup and registration, the per-backend texture cache and unload, texture masking, class-path resource lookup, native-free `ImGuiOpenGlRenderer` gating, `ImGuiFont` glyph-range validation) and `lwjgl` (native-free `ImGuiLwjglDisplay` gating). They don't need a GL context or imgui-java on the test compile classpath. `jme3` and `examples` have no tests; the native lifecycle is verified manually by running the examples.

## Database
None. LorisUI is a client-side UI library.

## Build Tools & Package Management
- **Gradle 9.6.0** (wrapper committed)
- Multi-project build: `:api` → `loris-ui-api`, `:imgui` → `loris-ui-imgui`, `:jme3` → `loris-ui-jme3`, `:lwjgl` → `loris-ui-lwjgl`, `:examples`
- Plugins: `java-library` then `maven-publish` (library modules, in that order), `java` (examples), `checkstyle` (every Java project, applied from root `allprojects`); the root project applies no plugin and has no sources
- Repositories: `mavenLocal()` then `mavenCentral()`
- Group `com.ivan1pl.loris-ui`, version `1.0-SNAPSHOT`
- Shared versions are set as root `ext` properties (`jmonkeyVersion`, `lwjglVersion`, `mockitoVersion`, `imguiVersion`, `checkstyleVersion`) and referenced as `rootProject.ext.<name>`. There is no version catalog.
- Root `allprojects` also sets the Java 25 toolchain, Checkstyle, the JUnit Platform, publishing and the Javadoc gate through lazy hooks (`plugins.withId('java')`, `plugins.withId('maven-publish')`, `tasks.withType(Test).configureEach`)
- Running the examples: `./gradlew :examples:runJme3Example` and `./gradlew :examples:runLwjglExample` (`JavaExec` tasks in the `application` group, on the Java 25 toolchain, passing `--enable-native-access=ALL-UNNAMED`). On macOS the SDL-based examples may need `-XstartOnFirstThread`, which the tasks don't pass.

## Infrastructure

### Containerization
None.

### CI/CD
None yet (see the roadmap, Phase 1).

### Distribution
`api`, `imgui`, `jme3` and `lwjgl` apply `maven-publish`, configured once by a root `plugins.withId('maven-publish')` hook: one `maven` publication from `components.java` with sources and Javadoc jars, and a POM with the module's name and description (each library sets `description` and `ext.pomName`, both required; see `standards/build/gradle.md`) plus the shared url (`https://github.com/Ivan1pl/loris-ui`), Apache-2.0 license, developer and scm. No repository is configured, so `./gradlew publishToMavenLocal` publishes to the local Maven repository only; `examples` is not published. Remote publishing (Maven Central or GitHub Packages) is planned (see the roadmap, Phase 3).

### Javadoc
Every exported package of `lorisui.api`, `lorisui.imgui` and `lorisui.lwjgl`, and the `jme3` package, has user-facing Javadoc, a `package-info.java`, and (for the three named modules) a module doc comment. The `imgui` `javadoc` task excludes `**/impl/**` and passes the main source directories as `-sourcepath`, so the generated docs contain only the exported packages. The `javadoc` task of every library runs with `-Xdoclint:all -Werror`. `assemble` runs it through the Javadoc jar, so `./gradlew build` and `publishToMavenLocal` fail on any Javadoc warning, and `check` depends on it, so a standalone `./gradlew check` does too; `examples` has no Javadoc gate.

## Development Tools

### IDE
IntelliJ IDEA (`.idea/`, git-ignored), with the project language level set to JDK 25.

### Linting & Formatting
- **Checkstyle 14.3.0** (`checkstyleVersion` in root `ext`) checks the main and test sources of every Java project through `checkstyleMain`/`checkstyleTest`, which `check` (and so `./gradlew build`) runs. The rules live in `config/checkstyle/checkstyle.xml` (severity `error`, `maxWarnings = 0`), with one suppression in `config/checkstyle/suppressions.xml` (`VisibilityModifier` for the internal `ImGuiState`). It enforces the formatting and naming standards: no tabs, 120 columns, 4-space indentation, K&R braces, whitespace, no static imports, ASCII-sorted import groups, declaration order, `*Type` type parameters and camel-cased acronyms in type names. Wildcard imports are allowed. The first build after a fresh checkout downloads Checkstyle, so it needs network access.
- **`.editorconfig`** at the repository root: UTF-8, LF line endings (CRLF for `*.bat`), final newline, trimmed trailing whitespace, and 4-space indentation with a 120-column limit for Java and Gradle files.
- There is no auto-formatter.

### Type Checking
Provided by the Java compiler. No nullability annotations or static analysers are used.

## Key Dependencies

| Dependency | Version | Module | Scope |
|---|---|---|---|
| `io.github.spair:imgui-java-app` | 1.92.7.1 (runtime: `[1.92.7.1,1.93)`, prefer 1.92.7.1) | imgui | compileOnly + runtimeOnly |
| `org.lwjgl:lwjgl-stb` (+ natives) | 3.4.1 | imgui | implementation / runtimeOnly |
| `org.lwjgl:lwjgl`, `org.lwjgl:lwjgl-sdl` | 3.4.1 | jme3, lwjgl | compileOnly |
| `org.lwjgl:lwjgl`, `org.lwjgl:lwjgl-sdl`, `org.lwjgl:lwjgl-opengl` | 3.4.1 | examples | implementation |
| `org.jmonkeyengine:jme3-core` | 3.10.0-SNAPSHOT | jme3 / examples | compileOnly / implementation |
| `org.jmonkeyengine:jme3-lwjgl3` | 3.10.0-SNAPSHOT | jme3 / examples | compileOnly / implementation |
| `org.jmonkeyengine:jme3-desktop` | 3.10.0-SNAPSHOT | examples | implementation |
| `org.junit:junit-bom` | 6.0.0 | all | test |
| `com.puppycrawl.tools:checkstyle` | 14.3.0 | all (Gradle `checkstyle` plugin) | build tool |
| `org.mockito:mockito-core` | 5.24.0 | api | test (+ `mockitoAgent` javaagent) |

**`loris-ui-api` has no external dependencies, and this is a firm architectural constraint.**

## Version Management
- Library versions: root `build.gradle` `ext` block plus inline coordinates
- Project version: `allprojects { version = '1.0-SNAPSHOT' }`
- Java: latest LTS (currently 25). Upgrade when the next LTS ships (expected: Java 29).

---
*Last Updated*: 2026-10-11
*Auto-detected*: languages, versions, modules, dependencies, build config, testing setup (from build files and sources)
*User-provided*: LTS-only Java policy, dependency-free `api` constraint, publishing goal
