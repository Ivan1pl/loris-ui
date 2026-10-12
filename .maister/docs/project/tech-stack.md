# Technology Stack

## Overview
This document describes the technology choices and rationale for LorisUI, a multi-module Java UI library. A toolkit-agnostic API is rendered through Dear ImGui and hosted in jMonkeyEngine 3 (OpenGL ES 3 through ANGLE, or OpenGL 3) or in an application's own LWJGL loop (SDL3 with OpenGL 3, SDL_GPU or Vulkan, or GLFW with OpenGL 3).

## Languages

### Java 25 (LTS)
- **Usage**: 100% of source code (main and test sources of every module)
- **Rationale**: modern language features and a JPMS module system. Policy: **track the latest LTS release** and never target non-LTS versions.
- **Key Features Used**:
  - Records (`TextureData` in `imgui.backend`, `ComboBox.Item`, `VulkanTextures.Texture` in the internal Vulkan port module, the private renderer-pair holder in `ImGuiJmeDisplay`)
  - Sealed class hierarchies (`ImageMode` and its final nested variants; the top-level `ImGuiRenderer` with its five final variants)
  - Pattern-matching `switch`, including unnamed patterns `_` (Java 22+), and exhaustive enum `switch` expressions (the jME renderer choice)
  - Flexible constructor bodies (final in Java 25): the Vulkan renderer and display check their arguments before `super(...)`
  - Instance/static `main()` without arguments (final in Java 25), used in `LorisJme3Example`, `LorisJme3Gl3Example`, `LorisLwjglSdlGl3Example`, `LorisLwjglGlfwGl3Example`, `LorisLwjglSdlGpuExample` and `LorisLwjglSdlVulkanExample`
  - `var`, lambdas, generics with self-typed builders (`BaseBuilder<PanelType, BuilderType>`)
  - JPMS `module-info.java` in `api` (`lorisui.api`), `imgui` (`lorisui.imgui`, `requires transitive lorisui.api`, `requires org.lwjgl.sdl`, plain `requires lorisui.imgui.gles3` and `requires lorisui.imgui.vulkan`, and plain `requires static org.lwjgl.opengles` and `requires static org.lwjgl.vulkan`), the two internal renderer-port modules `imgui-gles3` (`lorisui.imgui.gles3`) and `imgui-vulkan` (`lorisui.imgui.vulkan`), each with a qualified `exports ... to lorisui.imgui` under `@SuppressWarnings("module")`, `requires imgui.binding` and a plain `requires static` for its binding, and `lwjgl` (`lorisui.lwjgl`, `requires transitive lorisui.imgui`, `requires org.lwjgl.glfw` and `org.lwjgl.sdl`, and plain `requires static org.lwjgl.vulkan`); `jme3` is an automatic module (`Automatic-Module-Name: lorisui.jme3`). The `requires static` modules are never `transitive`, so GL-only modular consumers need neither binding and jME modular consumers need neither at compile time; because a `requires static` module is not resolved at runtime on its own, a modular application that uses the Vulkan variant declares `requires org.lwjgl.vulkan` itself, and a modular jME application on jME's default ANGLE renderer declares `requires org.lwjgl.opengles` or starts with `--add-modules org.lwjgl.opengles`; no application requires the port modules
- **Toolchain**: root `allprojects` sets a Java 25 Gradle toolchain for every module with the `java` plugin, so the JDK that runs Gradle does not matter. A JDK 25 must be installed locally: there is no foojay resolver, so Gradle does not download JDKs.
  - If Gradle selects a JDK it auto-provisioned earlier (under `~/.gradle/jdks`), it prints a Gradle 10 deprecation warning about auto-provisioning without toolchain repositories. Fix it by deleting that JDK from `~/.gradle/jdks`; Gradle then auto-detects the system JDK 25.

### Gradle Groovy DSL
- Build scripts: `build.gradle`, `settings.gradle`, `*/build.gradle`

## Frameworks

### UI Toolkit
- **Dear ImGui** via `io.github.spair:imgui-java-app` **1.92.7.1**, which bundles Dear ImGui **1.92.7** (the version `ImGui.getVersion()` reports): immediate-mode GUI used by the `imgui` backend. Each platform/renderer pair is wrapped in a final variant of the sealed `ImGuiRenderer`. Three use imgui-java's backends: SDL3 + OpenGL 3 (`imgui.sdl3.ImGuiImplSdl3`, `imgui.gl3.ImGuiImplGl3`) in `ImGuiSdlGl3Renderer`, GLFW + OpenGL 3 (`imgui.glfw.ImGuiImplGlfw`, `ImGuiImplGl3`) in `ImGuiGlfwGl3Renderer`, and SDL3 + SDL_GPU (`ImGuiImplSdl3`, `imgui.sdl3.ImGuiImplSdlGpu3`) in `ImGuiSdlGpuRenderer`. Two pair `ImGuiImplSdl3` with LorisUI's own renderer ports, because imgui-java ships no GLES or Vulkan renderer; the ports live in two internal MIT-licensed subprojects, `loris-ui-imgui-gles3` (package `com.ivan1pl.lorisui.imgui.gles3`) and `loris-ui-imgui-vulkan` (package `com.ivan1pl.lorisui.imgui.vulkan`): SDL3 + OpenGL ES 3 (`ImGuiImplGles3`, an ES 3.0 port of imgui-java's `ImGuiImplGl3` on `GLES30`) in `ImGuiSdlGles3Renderer`, and SDL3 + Vulkan (`ImGuiImplSdl3.initForVulkan`, `ImGuiImplVulkan` with `ImGuiImplVulkanShaders`, a dynamic-rendering port of Dear ImGui 1.92.7's `imgui_impl_vulkan.cpp` with its SPIR-V) in `ImGuiSdlVulkanRenderer`.
  - **Licensing**: the ported code is MIT-licensed and lives only in the two port subprojects, each MIT-only: its own `LICENSE` file (`imgui-vulkan`: Dear ImGui, Copyright (c) 2014-2026 Omar Cornut, and Mikołaj Nowak; `imgui-gles3`: also imgui-java, Copyright (c) 2019-present, Ilya "SpaiR" Prymshyts) is packaged as `META-INF/LICENSE` into its jar and sources jar, its POM lists only the MIT license, and the ported files (`ImGuiImplGles3`, `ImGuiImplVulkan`, `ImGuiImplVulkanShaders`, `VulkanTextures`) keep their MIT headers. Every other library module is Apache-2.0 only, with the root `LICENSE` as `META-INF/LICENSE`. The root publishing hook picks the file and the POM license from a per-project `ext.license` map that defaults to Apache-2.0 (see "License per Subproject" in `standards/build/gradle.md`).
  - No ImGui type appears in an exported signature, so `loris-ui-imgui` compiles against the pinned version (`compileOnly`) and declares a `runtimeOnly` rich-version range (`require "[1.92.7.1,1.93)"`, `prefer "1.92.7.1"`). The range is capped below the next minor because imgui-java is not semver and a published POM carries only the range (Maven ignores `prefer`); raise the cap when moving to a new imgui-java minor. Consumers get imgui-java at runtime but not on their compile classpath; the version is `ext.imguiVersion`.
  - Code that calls Dear ImGui directly (a custom `Renderer`) adds imgui-java to its own build and, on the module path, `requires imgui.binding`.
  - On JDK 25, imgui-java and LWJGL load native code, so applications pass `--enable-native-access` to avoid restricted-method warnings.
  - **Offline builds**: the range needs network access the first time runtime classpaths resolve it; see "Dependency Scopes for Toolkit Libraries" in `standards/build/gradle.md`.

### Graphics / Native
- **LWJGL** **3.4.1**: `lwjgl-stb` (an `implementation` dependency of `loris-ui-imgui`, so consumers get it at runtime only) for image decoding when loading textures, plus the OpenGL, SDL and GLFW bindings (through imgui-java). `loris-ui-imgui` also compiles against `lwjgl-sdl` as `compileOnly`, for the SDL_GPU calls of `ImGuiSdlGpuRenderer` and `ImGuiSdlGpuBackend`; at runtime it comes with imgui-java. `lwjgl-stb` native classifiers for Windows, Linux, macOS and macOS arm64 (`natives-windows`, `natives-linux`, `natives-macos`, `natives-macos-arm64`) are declared as `runtimeOnly`.
  - `loris-ui-imgui` compiles against `lwjgl-opengles` and `lwjgl-vulkan` as `compileOnly`, with no runtime scope, and so do the port subprojects (`imgui-gles3` against `lwjgl-opengles`, `imgui-vulkan` against `lwjgl-vulkan`, both also against imgui-java): jME supplies the OpenGL ES binding for its ANGLE renderer, and a Vulkan application supplies the Vulkan binding. For tests, `lwjgl-vulkan` is also `testCompileOnly` + `testRuntimeOnly` (`ImGuiVulkanBackend`'s constructor has Vulkan parameter types, so its test cannot compile without it; they pass Vulkan objects only as `null` literals), and `lwjgl-opengles` is `testRuntimeOnly`, so the GLES classes under test link; `imgui-vulkan` has `lwjgl-vulkan` as `testCompileOnly` + `testRuntimeOnly` for `VulkanTexturesTest`.
  - `loris-ui-jme3` compiles against `lwjgl` and `lwjgl-sdl` as `compileOnly`, because jME's `SdlEventListener` signature uses `SDL_Event`; at runtime the application's `jme3-lwjgl3` supplies them, together with the OpenGL ES binding that jME's `ANGLE_GLES3` renderer uses.
  - `loris-ui-lwjgl` compiles against `lwjgl`, `lwjgl-sdl`, `lwjgl-glfw` and `lwjgl-vulkan` as `compileOnly` (it reads SDL event types, chains GLFW callbacks and takes Vulkan objects); at runtime the first three come with imgui-java, whose POM brings `lwjgl`, `lwjgl-sdl`, `lwjgl-glfw` and `lwjgl-opengl` with their natives (GLFW and SDL natives only for Linux, macOS, macOS arm64 and Windows), and a Vulkan application adds `lwjgl-vulkan` itself. Its tests have `lwjgl-vulkan` as `testCompileOnly` + `testRuntimeOnly`, for the same reason as in `imgui`.
  - `examples` declares `lwjgl`, `lwjgl-sdl`, `lwjgl-glfw`, `lwjgl-opengl` and `lwjgl-vulkan` as `implementation`, because the LWJGL examples create their own SDL or GLFW window and OpenGL context, SDL_GPU device or Vulkan instance, device and swapchain, plus `lwjgl-vulkan`'s `natives-macos` and `natives-macos-arm64` classifiers as `runtimeOnly`, which provide MoltenVK on macOS.

### Vulkan
- **Vulkan 1.3** with the `dynamicRendering` feature, used only by the SDL3 + Vulkan variant (`ImGuiSdlVulkanRenderer`, `ImGuiVulkanBackend`, `ImGuiLwjglSdlVulkanDisplay`). The renderer draws with dynamic rendering into the application's image view and needs no render pass; its `initialize()` fails with `IllegalStateException` when the device does not expose Vulkan 1.3 functions. The instance and physical device are not parameters: LWJGL's `VkDevice` carries them.
- **Loader**: LWJGL's Vulkan binding loads the system Vulkan loader, which comes with the graphics driver, on Linux and Windows; on macOS it uses MoltenVK from LWJGL's Vulkan natives (Vulkan 1.3 support in the MoltenVK build shipped with LWJGL 3.4.1 is unverified). SDL and LWJGL must use the same library: the example passes LWJGL's loaded library path to `SDL_Vulkan_LoadLibrary`.
- **Validation**: `-Dlorisui.example.gpuDebug=true` on the Vulkan example enables `VK_LAYER_KHRONOS_validation`, which comes with the Vulkan SDK (on macOS run with `-Dorg.lwjgl.vulkan.libname=libvulkan.1.dylib` from the SDK to use its loader and layers).

### Game Engine Host
- **jMonkeyEngine** **3.10.0-SNAPSHOT**: `jme3-core` and `jme3-lwjgl3`, non-transitive `compileOnly` in `jme3`, so the published `loris-ui-jme3` carries no jME dependency and the application declares the jME modules it uses; `examples` declares `jme3-core`, `jme3-lwjgl3` and `jme3-desktop` as `implementation`. LorisUI integrates as a `SceneProcessor` (`ImGuiJmeDisplay`), which selects its renderer variant from jME's `AppSettings` renderer: OpenGL ES 3 (`ImGuiSdlGles3Renderer`) for jME 3.10's default `ANGLE_GLES3`, OpenGL 3 (`ImGuiSdlGl3Renderer`) for the `LWJGL_OPENGL*` renderers, and `IllegalStateException` for any other renderer.
  - The snapshot isn't on Maven Central. It's resolved from `mavenLocal()`, so jME must be installed locally first.
  - jME3 jars split packages, so `loris-ui-jme3` stays on the classpath (no `module-info.java`) and sets `Automatic-Module-Name: lorisui.jme3` in its jar manifest.

### Standalone Host
- **LWJGL** (no engine): `loris-ui-lwjgl` is embedded in an application that owns the window, its OpenGL context, GPU device or Vulkan device, and the loop. It offers one display per platform/renderer pair: `ImGuiLwjglSdlGl3Display` (SDL3 + OpenGL 3), `ImGuiLwjglGlfwGl3Display` (GLFW + OpenGL 3, chaining the window's GLFW input callbacks), `ImGuiLwjglSdlGpuDisplay` (SDL3 + SDL_GPU, drawing into the application's command buffer and target texture) and `ImGuiLwjglSdlVulkanDisplay` (SDL3 + Vulkan 1.3, recording into the application's command buffer and swapchain image view). It is a named module (`lorisui.lwjgl`) and depends only on `loris-ui-imgui`.

### Testing
- **JUnit 6.0.0** (Jupiter) through `junit-bom`, with `junit-platform-launcher`, declared in every Java module except `imgui-gles3`, which has no tests. The JUnit Platform is enabled for all `Test` tasks by the lazy `tasks.withType(Test).configureEach { useJUnitPlatform() }` hook in root `allprojects`.
- **Mockito 5.24.0** (`mockito-core`, version in root `ext`) declared only in `api`, whose tests use it for callbacks. `api` loads Mockito as a `-javaagent` from a non-transitive `mockitoAgent` configuration, avoiding the JDK 21+ dynamic-agent warning. The other modules use JUnit only, with hand-written fakes.
- **Status**: unit tests exist in `api` (builders, styles, `Color`, `ImageMode`, `LorisScreen`, `ScreenManager` including two-phase deactivation, `DeferredRelease`, `ResourceBundleKeyResolver`), `imgui` (`ImGuiBackend` lookup and registration, the per-backend texture cache and unload, texture masking, Repeat tile spans (`TextureTiling`), missing-resource `DecodedImage`, class-path resource lookup, native-free gating of the five renderer variants and of `ImGuiSdlGpuBackend` and `ImGuiVulkanBackend`, the GL and GLES backends' decode-before-GL order, `ImGuiFont` glyph-range validation), `imgui-vulkan` (the embedded SPIR-V words and `VulkanTextures`' upload size guard), `lwjgl` (native-free gating of the four displays, and the GLFW display's callback-dispatch rules through a package-private seam) and `jme3` (the renderer choice of `ImGuiJmeGraphicsApi`, which runs without jME on the test classpath). They don't need a GL context, a Vulkan device or imgui-java on the test compile classpath; Vulkan objects appear only as `null` literals. `imgui-gles3` and `examples` have no tests; the native lifecycle, including the GLES draw path under ANGLE and the Vulkan pipeline, uploads, swapchain and teardown, is verified manually by running the examples.

## Database
None. LorisUI is a client-side UI library.

## Build Tools & Package Management
- **Gradle 9.6.0** (wrapper committed)
- Multi-project build: `:api` → `loris-ui-api`, `:imgui` → `loris-ui-imgui`, `:imgui-gles3` → `loris-ui-imgui-gles3`, `:imgui-vulkan` → `loris-ui-imgui-vulkan`, `:jme3` → `loris-ui-jme3`, `:lwjgl` → `loris-ui-lwjgl`, `:examples`
- Plugins: `java-library` then `maven-publish` (library modules, in that order), `java` (examples), `checkstyle` (every Java project, applied from root `allprojects`); the root project applies no plugin and has no sources
- Repositories: `mavenLocal()` then `mavenCentral()`
- Group `com.ivan1pl.loris-ui`, version `1.0-SNAPSHOT`
- Shared versions are set as root `ext` properties (`jmonkeyVersion`, `lwjglVersion`, `mockitoVersion`, `imguiVersion`, `checkstyleVersion`) and referenced as `rootProject.ext.<name>`. There is no version catalog.
- Root `allprojects` also sets the Java 25 toolchain, Checkstyle, the JUnit Platform, publishing and the Javadoc gate through lazy hooks (`plugins.withId('java')`, `plugins.withId('maven-publish')`, `tasks.withType(Test).configureEach`)
- Running the examples: six `JavaExec` tasks in the `application` group, on the Java 25 toolchain, passing `--enable-native-access=ALL-UNNAMED`: `./gradlew :examples:runJme3Example` (jME's default renderer, OpenGL ES 3 through ANGLE), `:examples:runJme3Gl3Example` (jME's OpenGL 3.3 renderer), `:examples:runLwjglSdlGl3Example`, `:examples:runLwjglGlfwGl3Example`, `:examples:runLwjglSdlGpuExample` and `:examples:runLwjglSdlVulkanExample`. `runLwjglSdlGpuExample` and `runLwjglSdlVulkanExample` pass the `lorisui.example.gpuDebug` system property through, so `-Dlorisui.example.gpuDebug=true` creates the SDL_GPU device in debug mode or enables the Vulkan validation layer (Vulkan SDK). On macOS the SDL- and GLFW-based examples may need `-XstartOnFirstThread`, which the tasks don't pass.

## Infrastructure

### Containerization
None.

### CI/CD
None yet (see the roadmap, Phase 1).

### Distribution
`api`, `imgui`, `imgui-gles3`, `imgui-vulkan`, `jme3` and `lwjgl` apply `maven-publish`, configured once by a root `plugins.withId('maven-publish')` hook: one `maven` publication from `components.java` with sources and Javadoc jars, and a POM with the module's name and description (each library sets `description` and `ext.pomName`, both required; see `standards/build/gradle.md`) plus the shared url (`https://github.com/Ivan1pl/loris-ui`), developer and scm, and exactly one license from the module's `ext.license` (Apache-2.0 by default, MIT for `imgui-gles3` and `imgui-vulkan`); the same hook packages that license's file as `META-INF/LICENSE` into the module's jar and sources jar. `loris-ui-imgui` depends on the two port artifacts with `implementation`, so they are runtime-scope dependencies in its POM. No repository is configured, so `./gradlew publishToMavenLocal` publishes to the local Maven repository only; `examples` is not published. Remote publishing (Maven Central or GitHub Packages) is planned (see the roadmap, Phase 3).

### Javadoc
Every exported package of `lorisui.api`, `lorisui.imgui` and `lorisui.lwjgl`, and the `jme3` package, has user-facing Javadoc, a `package-info.java`, and (for the three named modules) a module doc comment. The port modules `lorisui.imgui.gles3` and `lorisui.imgui.vulkan` have a module doc comment and a `package-info.java` for their qualified-exported package. The `imgui` `javadoc` task excludes `**/impl/**`, and each port module's excludes its package, all passing the main source directories as `-sourcepath`, so the generated docs contain only the exported packages (for a port module, the module page only). The `javadoc` task of every library runs with `-Xdoclint:all -Werror`. `assemble` runs it through the Javadoc jar, so `./gradlew build` and `publishToMavenLocal` fail on any Javadoc warning, and `check` depends on it, so a standalone `./gradlew check` does too; `examples` has no Javadoc gate.

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
| `loris-ui-imgui-gles3`, `loris-ui-imgui-vulkan` (project; MIT) | 1.0-SNAPSHOT | imgui | implementation |
| `loris-ui-api` (project) | 1.0-SNAPSHOT | imgui-vulkan | implementation |
| `io.github.spair:imgui-java-app` | 1.92.7.1 (runtime: `[1.92.7.1,1.93)`, prefer 1.92.7.1) | imgui / imgui-gles3, imgui-vulkan | compileOnly + runtimeOnly / compileOnly |
| `org.lwjgl:lwjgl-stb` (+ natives) | 3.4.1 | imgui | implementation / runtimeOnly |
| `org.lwjgl:lwjgl-sdl` | 3.4.1 | imgui | compileOnly |
| `org.lwjgl:lwjgl-opengles` | 3.4.1 | imgui / imgui-gles3 | compileOnly + testRuntimeOnly / compileOnly |
| `org.lwjgl:lwjgl-vulkan` | 3.4.1 | imgui, imgui-vulkan, lwjgl / examples | compileOnly + testCompileOnly + testRuntimeOnly / implementation |
| `org.lwjgl:lwjgl-vulkan` natives (`natives-macos`, `natives-macos-arm64`; MoltenVK) | 3.4.1 | examples | runtimeOnly |
| `org.lwjgl:lwjgl`, `org.lwjgl:lwjgl-sdl` | 3.4.1 | jme3, lwjgl | compileOnly |
| `org.lwjgl:lwjgl-glfw` | 3.4.1 | lwjgl / examples | compileOnly / implementation |
| `org.lwjgl:lwjgl`, `org.lwjgl:lwjgl-sdl`, `org.lwjgl:lwjgl-opengl` | 3.4.1 | examples | implementation |
| `org.jmonkeyengine:jme3-core` | 3.10.0-SNAPSHOT | jme3 / examples | compileOnly / implementation |
| `org.jmonkeyengine:jme3-lwjgl3` | 3.10.0-SNAPSHOT | jme3 / examples | compileOnly / implementation |
| `org.jmonkeyengine:jme3-desktop` | 3.10.0-SNAPSHOT | examples | implementation |
| `org.junit:junit-bom` | 6.0.0 | all but imgui-gles3 | test |
| `com.puppycrawl.tools:checkstyle` | 14.3.0 | all (Gradle `checkstyle` plugin) | build tool |
| `org.mockito:mockito-core` | 5.24.0 | api | test (+ `mockitoAgent` javaagent) |

**`loris-ui-api` has no external dependencies, and this is a firm architectural constraint.**

## Version Management
- Library versions: root `build.gradle` `ext` block plus inline coordinates
- Project version: `allprojects { version = '1.0-SNAPSHOT' }`
- Java: latest LTS (currently 25). Upgrade when the next LTS ships (expected: Java 29).

---
*Last Updated*: 2026-10-12
*Auto-detected*: languages, versions, modules, dependencies, build config, testing setup (from build files and sources); updated for the jME ANGLE/GLES3 and SDL3 + Vulkan variants and the MIT-only renderer-port subprojects
*User-provided*: LTS-only Java policy, dependency-free `api` constraint, publishing goal
