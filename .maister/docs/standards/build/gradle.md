# Gradle Build

### Gradle Wrapper and Groovy DSL
Build with the Gradle wrapper (`./gradlew`, pinned to 9.6.0). Build scripts use the Groovy DSL (`*.gradle`), not the Kotlin DSL.

### Module Naming and Registration
Subprojects live in short directories (`api`, `imgui`, `imgui-gles3`, `imgui-vulkan`, `jme3`, `lwjgl`) and published ones are renamed to `loris-ui-<dir>` in `settings.gradle`. Inter-module dependencies reference the renamed path (`project(':loris-ui-api')`). Non-published modules (`examples`) keep their directory name.

```groovy
include 'foo'
project(":foo").name = "loris-ui-foo"
```

### Plugins per Module Type
Library modules apply `java-library` and then `maven-publish`, in that order: the root publishing hook runs when `maven-publish` is applied and needs the `java` extension and `components.java`, which `java-library` creates. The non-published demo module applies only `java`. The root project has no sources and applies no plugin (`checkstyle` is applied to every Java project from root `allprojects`).

### Shared Configuration in `allprojects`
Group (`com.ivan1pl.loris-ui`), version, repositories (`mavenLocal()` first — needed for the jME 3.10.0-SNAPSHOT — then `mavenCentral()`), the Java toolchain, Checkstyle, the JUnit Platform, the publishing configuration (see Publishing) and the Javadoc gate (see Javadoc Gate) are set once in root `allprojects`. Subprojects must not redeclare them.

The toolchain, Checkstyle, test, publishing and Javadoc gate settings use lazy hooks, because `allprojects` runs before the subprojects apply their plugins. A plain `java { }` or `test { }` block there fails for any subproject, since its `java` extension and `test` task don't exist yet when the root script runs. Use `plugins.withId('java')` (it also fires for `java-library`), `plugins.withId('maven-publish')` and `tasks.withType(Test).configureEach`:

```groovy
allprojects {
    plugins.withId('java') {
        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }

        apply plugin: 'checkstyle'
        checkstyle {
            toolVersion = rootProject.ext.checkstyleVersion
            configDirectory = rootProject.layout.projectDirectory.dir('config/checkstyle')
            maxWarnings = 0
        }
    }

    plugins.withId('maven-publish') {
        // sources and Javadoc jars, the `maven` publication and the Javadoc gate; see the sections below
    }

    tasks.withType(Test).configureEach {
        useJUnitPlatform()
    }
}
```

The toolchain only selects an installed JDK 25 (there is no foojay resolver plugin, so nothing is downloaded), and the JDK running Gradle does not matter.

### Checkstyle Gate
Checkstyle is a failing build gate for the main and test sources of every Java project. The `checkstyleMain` and `checkstyleTest` tasks attach to `check`, so `./gradlew build` fails on any violation. The rules live once at the root in `config/checkstyle/checkstyle.xml` (`severity` `error`), with file-specific exceptions in `config/checkstyle/suppressions.xml`; subprojects have no Checkstyle configuration of their own. Keep `maxWarnings = 0` and the default `ignoreFailures = false`. Fix violations instead of suppressing them; add a suppression only for a deliberate, documented exception (the internal `ImGuiState` flag holder keeps its public static fields). The Checkstyle task runs on the project's Java 25 toolchain. A repository-root `.editorconfig` keeps editors in line with the same rules; there is no auto-formatter.

### Javadoc Gate
Javadoc is a failing build gate for every library module. The root `plugins.withId('maven-publish')` hook runs `javadoc` with strict doclint. The Javadoc jar (see Publishing) makes `assemble` run `javadoc`, so `./gradlew build` and `publishToMavenLocal` fail on any Javadoc warning; the hook also makes `check` depend on `javadoc`, so a standalone `./gradlew check` is gated too. Only modules that apply `maven-publish` get the gate, so the demo module `examples` is not linted. Fix warnings instead of relaxing the options; do not add `links` to external Javadoc. The gate needs no network access: Javadoc resolves the compile classpath, whose versions are pinned.

```groovy
tasks.named('javadoc', Javadoc) {
    options.addBooleanOption('Xdoclint:all', true)
    options.addBooleanOption('Werror', true)
}

tasks.named('check') {
    dependsOn 'javadoc'
}
```

### Publishing (Local Only)
The root `plugins.withId('maven-publish')` hook configures publishing for every library module: it adds sources and Javadoc jars (`java { withSourcesJar(); withJavadocJar() }`) and one `maven` publication `from components.java`. The POM metadata shared by all modules (url, developer, scm) lives once in that hook, which also sets each module's single POM license and packages its license file as `META-INF/LICENSE` into the module's jar and sources jar (Apache-2.0 and the root `LICENSE` unless the module declares otherwise; see License per Subproject). No `repositories` are configured, so artifacts are published only with `./gradlew publishToMavenLocal`; the demo module `examples` is not published.

Each library module sets its own POM name and description in its `build.gradle`, after the `plugins` block. Both are required: the hook reads them lazily through providers, and a module missing either fails POM generation with a `GradleException` naming the module. A string wrapped to stay within 120 columns ends the line with the `+` and continues with an 8-space indent, because a line starting with `+` is parsed as a new statement with a unary plus.

```groovy
description = 'Toolkit-agnostic LorisUI API: a declarative component model for game menus and HUDs, screens, ' +
        'and the backend port that renders them.'
ext.pomName = 'LorisUI API'
```

The hook runs when `maven-publish` is applied, before the rest of the module's script has set these values, so the POM `name` and `description` must be providers evaluated when the POM is generated. Inside `pom {}`, qualify every project property with `project.` (`project.provider`, `project.ext.pomName`, `project.description`): a bare `ext` or `description` there binds to the POM object, not the project. Each provider guards its value with a `GradleException` naming the module (see the root `build.gradle`):

```groovy
name = project.provider {
    if (!project.ext.has('pomName')) {
        throw new GradleException("Project ${project.path} must set ext.pomName in its " +
                'build.gradle; the published POM requires it')
    }
    project.ext.pomName
}
```

### Shared Versions in Root `ext`
Versions used by more than one module or declaration (jMonkeyEngine, LWJGL, Mockito, imgui-java, Checkstyle) are defined once in the root `ext` block, at the top of the root `build.gradle`, before `allprojects` (which reads `checkstyleVersion`). Add new shared versions there instead of hardcoding them in subprojects.

```groovy
ext {
    jmonkeyVersion = '3.10.0-SNAPSHOT'
    lwjglVersion = '3.4.1'
    mockitoVersion = '5.24.0'
    imguiVersion = '1.92.7.1'
    checkstyleVersion = '14.3.0'
}
```

Subprojects reference them explicitly as `rootProject.ext.<name>` inside double-quoted coordinates. The implicit form (`$jmonkeyVersion`, resolved through the parent project) is deprecated in Gradle 9.6 and fails in Gradle 10.

```groovy
implementation "org.jmonkeyengine:jme3-core:${rootProject.ext.jmonkeyVersion}"
testImplementation "org.mockito:mockito-core:${rootProject.ext.mockitoVersion}"
```

Mockito is a test dependency of `api` only, declared twice there (test dependency and `mockitoAgent` javaagent, see `testing/test-writing.md`); other modules don't declare it until a test uses it.

### Dependency Scopes for Toolkit Libraries
A library module declares a dependency as `api` only when its types appear in the module's exported signatures (`api project(':loris-ui-api')` in `imgui`). Toolkit libraries used only inside the implementation are never `api`: compile against a pinned version with `compileOnly`, and declare the runtime requirement with `runtimeOnly` and a rich-version range that prefers the tested version, so consumers get the toolkit at runtime but not on their compile classpath. A toolkit library the module calls directly but never exposes is `implementation` (runtime scope for consumers), as `lwjgl-stb` is in `imgui`. So is an internal helper subproject whose types never appear in exported signatures: `imgui` depends on `loris-ui-imgui-gles3` and `loris-ui-imgui-vulkan` with `implementation`, and `loris-ui-imgui-vulkan` on `loris-ui-api`.

```groovy
compileOnly "io.github.spair:imgui-java-app:${rootProject.ext.imguiVersion}"

runtimeOnly('io.github.spair:imgui-java-app') {
    version {
        require "[${rootProject.ext.imguiVersion},1.93)"
        prefer rootProject.ext.imguiVersion
    }
}
```

Cap toolkit ranges below the next minor (`[1.92.7.1,1.93)`), never open-ended: toolkits such as imgui-java are not semver, and a published POM carries only the range (Maven ignores `prefer`), so an open range lets Maven consumers resolve an untested release. Raise the cap when moving to a new minor.

The range needs Gradle's version listing, and only runtime classpaths resolve it: the tests run by `check` and the example run tasks. Their first resolution on a machine, and the first after Gradle's dynamic-version cache expires, need network access; `--offline` works once the listing is cached.

Libraries that the consuming application (or another runtime dependency) already supplies at runtime are `compileOnly` only. The `jme3` host compiles against jMonkeyEngine this way (`jme3-core` and `jme3-lwjgl3`), so its published POM and Gradle module metadata carry no jME dependency. Both are non-transitive (`transitive = false`), so jME's own dependencies stay off the host's compile and Javadoc classpath, and the LWJGL modules the host compiles against are declared explicitly; the application declares the jME modules it uses (`jme3-core`, `jme3-lwjgl3`, `jme3-desktop`) itself, as `examples` does with `implementation`.

Both host modules compile against LWJGL the same way: `jme3` against `lwjgl` and `lwjgl-sdl` (jME's `SdlEventListener` signature uses `SDL_Event`), which the application's `jme3-lwjgl3` provides; `lwjgl` against `lwjgl`, `lwjgl-sdl` and `lwjgl-glfw` (SDL events and GLFW callbacks), which arrive at runtime with imgui-java. `imgui` compiles against `lwjgl-sdl` as `compileOnly` too, for its SDL_GPU renderer and texture backend; it also arrives with imgui-java. An application that calls LWJGL itself (the LWJGL examples create their SDL or GLFW window and their GL context, GPU device or Vulkan device) declares the LWJGL modules it compiles against as `implementation` (`lwjgl`, `lwjgl-sdl`, `lwjgl-glfw`, `lwjgl-opengl` and `lwjgl-vulkan` in `examples`).

```groovy
compileOnly("org.jmonkeyengine:jme3-core:${rootProject.ext.jmonkeyVersion}") { transitive = false }
compileOnly("org.jmonkeyengine:jme3-lwjgl3:${rootProject.ext.jmonkeyVersion}") { transitive = false }
compileOnly "org.lwjgl:lwjgl:${rootProject.ext.lwjglVersion}"
compileOnly "org.lwjgl:lwjgl-sdl:${rootProject.ext.lwjglVersion}"
// lwjgl only: compileOnly "org.lwjgl:lwjgl-glfw:${rootProject.ext.lwjglVersion}"
```

Bindings that only an optional variant uses, and that the application supplies, are `compileOnly` with no runtime scope: `lwjgl-opengles` (supplied by jME for its ANGLE renderer) and `lwjgl-vulkan` (supplied by the Vulkan application) in `imgui`, `lwjgl-opengles` in `imgui-gles3`, and `lwjgl-vulkan` in `imgui-vulkan` and `lwjgl`. The port subprojects also compile against imgui-java with `compileOnly` only; `imgui` declares its runtime range. Their JPMS counterpart is the plain `requires static` exception in `architecture/modules.md`. They are absent from the test classpath too, except where a test needs them: `lwjgl-vulkan` is `testCompileOnly` + `testRuntimeOnly` in `imgui`, `imgui-vulkan` and `lwjgl`, because the tested Vulkan constructors and methods have Vulkan parameter types (tests pass Vulkan objects only as `null` literals; see `testing/test-writing.md`), and `lwjgl-opengles` is `testRuntimeOnly` in `imgui`, so the GLES classes under test link.

```groovy
compileOnly "org.lwjgl:lwjgl-opengles:${rootProject.ext.lwjglVersion}"
compileOnly "org.lwjgl:lwjgl-vulkan:${rootProject.ext.lwjglVersion}"
testCompileOnly "org.lwjgl:lwjgl-vulkan:${rootProject.ext.lwjglVersion}"
testRuntimeOnly "org.lwjgl:lwjgl-opengles:${rootProject.ext.lwjglVersion}" // imgui only
testRuntimeOnly "org.lwjgl:lwjgl-vulkan:${rootProject.ext.lwjglVersion}"
```

Native libraries are `runtimeOnly` with one declaration per platform classifier: `natives-windows`, `natives-linux`, `natives-macos` and `natives-macos-arm64`. The exception is `lwjgl-vulkan` in `examples`, which declares only `natives-macos` and `natives-macos-arm64` (they provide MoltenVK); Linux and Windows use the system Vulkan loader that comes with the graphics driver.

### License per Subproject
Every published artifact carries exactly one license: its POM lists one `<license>`, and its jar and sources jar contain that license's text as `META-INF/LICENSE`. The root publishing hook reads both from a lazy per-project `ext.license` map (`name`, `url`, `file`), which defaults to Apache-2.0 with the root `LICENSE`, and fails the build with a `GradleException` naming the module when the declared file does not exist. Do not add a second `<license>` to a POM or extra notice files to `META-INF/`.

Code derived from MIT-licensed projects (Dear ImGui, imgui-java) is never mixed into an Apache-2.0 module. It lives in its own MIT-only subproject with its own `LICENSE` file (the upstream copyright lines plus the maintainer's), and each ported source file keeps its MIT header. That subproject sets `ext.license` after its `plugins` block, next to `description` and `ext.pomName` (`imgui-gles3` and `imgui-vulkan` are the current cases):

```groovy
ext.license = [
        name: 'The MIT License',
        url : 'https://opensource.org/license/mit',
        file: file('LICENSE'),
]
```

Such a subproject is an internal helper of the module that uses it: it exports its package only to that module (see `architecture/modules.md`), and the using module depends on it with `implementation`.

### Javadoc Excludes `impl` and Qualified-Exported Packages
Generated Javadoc documents only packages exported to every module. A module with `impl` packages excludes them from the `javadoc` sources and passes the main source directories as `-sourcepath`, so javadoc still resolves the `impl` types without documenting them. Use this exact form (Gradle has no `options.sourcepath`):

```groovy
tasks.named('javadoc', Javadoc) {
    exclude '**/impl/**'
    options.addPathOption('sourcepath').value = sourceSets.main.java.srcDirs.toList()
}
```

Only `imgui` has `impl` packages. Any library module that gains one, including `jme3` or `lwjgl`, adds the same block to its `build.gradle`; the root Javadoc gate does not exclude `impl` packages by itself.

A package exported only to another LorisUI module (a qualified export, as in the renderer-port modules `imgui-gles3` and `imgui-vulkan`) is not public API either, so its module excludes it the same way, by its path, and the generated Javadoc is the module page only:

```groovy
tasks.named('javadoc', Javadoc) {
    exclude 'com/ivan1pl/lorisui/imgui/vulkan/**'
    options.addPathOption('sourcepath').value = sourceSets.main.java.srcDirs.toList()
}
```

### Automatic Module Name for Non-Modular Libraries
A library module without a `module-info.java` (`jme3`) sets a stable automatic module name in its jar manifest, following the `lorisui.<module>` naming in `architecture/modules.md`. No compiler module-patching arguments are used.

```groovy
tasks.named('jar', Jar) {
    manifest {
        attributes('Automatic-Module-Name': 'lorisui.jme3')
    }
}
```

### Example Run Tasks
The `examples` module registers one `JavaExec` task per example entry point, six in all (`runJme3Example`, `runJme3Gl3Example`, `runLwjglSdlGl3Example`, `runLwjglGlfwGl3Example`, `runLwjglSdlGpuExample`, `runLwjglSdlVulkanExample`), instead of applying the `application` plugin. The tasks are registered in a loop over one shared list, `exampleRunTasks` (entries with `name`, `mainClass`, `description` and a `gpuDebug` flag), which `runAllExamples` reads too, so an example is added in one place. Each sets `group = 'application'`, a description, `classpath = sourceSets.main.runtimeClasspath` and the main class, and passes `--enable-native-access=ALL-UNNAMED` so imgui-java and LWJGL load native code without restricted-method warnings on JDK 25. The tasks use the Java 25 toolchain by convention. A system property that an example reads is passed through explicitly with `systemProperty` and `providers.systemProperty(...)`, because `JavaExec` does not inherit the Gradle JVM's properties: `runLwjglSdlGpuExample` and `runLwjglSdlVulkanExample` (the entries with `gpuDebug: true`) pass `lorisui.example.gpuDebug` (default `false`), so `./gradlew :examples:runLwjglSdlGpuExample -Dlorisui.example.gpuDebug=true` creates the SDL_GPU device in debug mode, with validation, and the same property on `runLwjglSdlVulkanExample` enables `VK_LAYER_KHRONOS_validation` (which needs the Vulkan SDK). Each jME task's description names the jME renderer it runs with (`runJme3Example`: jME's default renderer, OpenGL ES 3 through ANGLE; `runJme3Gl3Example`: jME's OpenGL 3.3 renderer).

`runAllExamples` (group `application`) starts all six examples at once, each in its own JVM. It is not a `dependsOn` aggregate, because a `JavaExec` task blocks until its window closes and the tasks of one project run one after another, and it cannot use `project.javaexec`, which was removed in Gradle 9. Its type `RunExamplesInParallel` is an abstract `DefaultTask` declared in `examples/build.gradle`. Every property is annotated, because Gradle rejects unannotated task properties:
- `@Inject` `ExecOperations` and `FileSystemOperations`, with `import javax.inject.Inject` and `import java.nio.file.Files` at the top of the script (neither is a default import; `org.gradle.process.*`, `org.gradle.api.file.*` and `org.gradle.jvm.toolchain.*` are);
- `@Classpath` classpath from `sourceSets.main.runtimeClasspath`, which carries the build dependency, so `examples` compiles first;
- `@Nested` `Property<JavaLauncher>` from `javaToolchains.launcherFor(java.toolchain)`, set explicitly because `ExecOperations.javaexec` does not apply the toolchain convention (Java 25 stays defined only in the root build);
- `@Input` example list (the shared list) and gpuDebug value (the same provider as the individual tasks);
- `@Internal` working directory, the project directory, as `JavaExec` uses.

The task declares no outputs, so it always runs. Its action runs one `javaexec` per example on its own daemon thread, with the launcher's executable, the classpath, the main class, the working directory, `--enable-native-access=ALL-UNNAMED`, `org.lwjgl.system.SharedLibraryExtractPath` and, for the `gpuDebug` entries, the gpuDebug property. The extract path is a fresh `Files.createTempDirectory` under the task's `temporaryDir` (`build/tmp/runAllExamples`), one per example, deleted with `FileSystemOperations.delete` in a `finally` once the example exits: LWJGL's shared extraction directory (`/tmp/lwjgl_<user>`) is probed through one fixed test file, so JVMs that start together make each other's probe fail and fall back to `.lwjgl` in the working directory. `File.deleteOnExit` is not used, because it removes only empty directories and runs when the long-lived Gradle daemon exits. Each thread records its example's failure (a non-zero exit, a launch error or any other `Throwable`), so one failing example does not stop the others; the task joins all threads and then throws a `GradleException` listing every failed example. Interrupting the task restores the interrupt flag and fails, but does not end the children: `ExecOperations` returns no process handle, so running examples keep their windows open until closed. Child output goes to the Gradle console, interleaved. The task class is not repeated here; `examples/build.gradle` is its single source.

```groovy
import javax.inject.Inject

def exampleRunTasks = [
        [name       : 'runLwjglSdlGl3Example',
         mainClass  : 'com.ivan1pl.lorisui.example.lwjgl.LorisLwjglSdlGl3Example',
         description: 'Runs the LorisUI example in a plain LWJGL SDL3 window with OpenGL 3.',
         gpuDebug   : false],
        [name       : 'runLwjglSdlGpuExample',
         mainClass  : 'com.ivan1pl.lorisui.example.lwjgl.LorisLwjglSdlGpuExample',
         description: 'Runs the LorisUI example in a plain LWJGL SDL3 window with SDL_GPU.',
         gpuDebug   : true],
        // ... one entry per example, six in all
]

def gpuDebugProperty = providers.systemProperty('lorisui.example.gpuDebug').orElse('false')

exampleRunTasks.each { example ->
    tasks.register(example.name, JavaExec) {
        group = 'application'
        description = example.description
        classpath = sourceSets.main.runtimeClasspath
        mainClass = example.mainClass
        jvmArgs '--enable-native-access=ALL-UNNAMED'
        if (example.gpuDebug) {
            systemProperty 'lorisui.example.gpuDebug', gpuDebugProperty.get()
        }
    }
}

abstract class RunExamplesInParallel extends DefaultTask {
    // annotated properties as listed above; the full task action is in examples/build.gradle
}

tasks.register('runAllExamples', RunExamplesInParallel) {
    group = 'application'
    description = 'Runs all six LorisUI examples at the same time, each in its own JVM.'
    classpath.from(sourceSets.main.runtimeClasspath)
    launcher = javaToolchains.launcherFor(java.toolchain)
    examples = exampleRunTasks
    gpuDebug = gpuDebugProperty
    workingDir = layout.projectDirectory
}
```
