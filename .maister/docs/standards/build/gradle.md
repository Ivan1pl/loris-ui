# Gradle Build

### Gradle Wrapper and Groovy DSL
Build with the Gradle wrapper (`./gradlew`, pinned to 9.6.0). Build scripts use the Groovy DSL (`*.gradle`), not the Kotlin DSL.

### Module Naming and Registration
Subprojects live in short directories (`api`, `imgui`, `jme3`, `lwjgl`) and published ones are renamed to `loris-ui-<dir>` in `settings.gradle`. Inter-module dependencies reference the renamed path (`project(':loris-ui-api')`). Non-published modules (`examples`) keep their directory name.

```groovy
include 'foo'
project(":foo").name = "loris-ui-foo"
```

### Plugins per Module Type
Library modules apply `java-library` and `maven-publish`; the non-published demo module applies only `java`. The root project has no sources and applies no plugin (`checkstyle` is applied to every Java project from root `allprojects`).

### Shared Configuration in `allprojects`
Group (`com.ivan1pl.loris-ui`), version, repositories (`mavenLocal()` first — needed for the jME 3.10.0-SNAPSHOT — then `mavenCentral()`), the Java toolchain, Checkstyle and the JUnit Platform are set once in root `allprojects`. Subprojects must not redeclare them.

The toolchain, Checkstyle and test settings use lazy hooks, because `allprojects` runs before the subprojects apply their plugins. A plain `java { }` or `test { }` block there fails for any subproject, since its `java` extension and `test` task don't exist yet when the root script runs. Use `plugins.withId('java')` (it also fires for `java-library`) and `tasks.withType(Test).configureEach`:

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

    tasks.withType(Test).configureEach {
        useJUnitPlatform()
    }
}
```

The toolchain only selects an installed JDK 25 (there is no foojay resolver plugin, so nothing is downloaded), and the JDK running Gradle does not matter.

### Checkstyle Gate
Checkstyle is a failing build gate for the main and test sources of every Java project. The `checkstyleMain` and `checkstyleTest` tasks attach to `check`, so `./gradlew build` fails on any violation. The rules live once at the root in `config/checkstyle/checkstyle.xml` (`severity` `error`), with file-specific exceptions in `config/checkstyle/suppressions.xml`; subprojects have no Checkstyle configuration of their own. Keep `maxWarnings = 0` and the default `ignoreFailures = false`. Fix violations instead of suppressing them; add a suppression only for a deliberate, documented exception (the internal `ImGuiState` flag holder keeps its public static fields). The Checkstyle task runs on the project's Java 25 toolchain. A repository-root `.editorconfig` keeps editors in line with the same rules; there is no auto-formatter.

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

### jME3 Dependencies Non-Transitive in Library
The `jme3` library module declares jMonkeyEngine artifacts with `transitive = false` so the library does not impose engine transitive dependencies on consumers; the consuming application declares the full jME set (core, desktop, lwjgl3) itself.

### Dependency Scopes for Toolkit Libraries
A library module declares a dependency as `api` only when its types appear in the module's exported signatures (`api project(':loris-ui-api')` in `imgui`). Toolkit libraries used only inside the implementation are never `api`: compile against a pinned version with `compileOnly`, and declare the runtime requirement with `runtimeOnly` and a rich-version range that prefers the tested version, so consumers get the toolkit at runtime but not on their compile classpath. A toolkit library the module calls directly but never exposes is `implementation` (runtime scope for consumers), as `lwjgl-stb` is in `imgui`.

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

The range needs Gradle's version listing: the first build on a machine, and the first build after Gradle's dynamic-version cache expires, need network access; `--offline` works once the listing is cached.

Libraries that the consuming application (or another runtime dependency) already supplies at runtime are `compileOnly` only. Both host modules compile against LWJGL this way: `jme3` against `lwjgl` and `lwjgl-sdl` (jME's `SdlEventListener` signature uses `SDL_Event`), which the application's `jme3-lwjgl3` provides; `lwjgl` against `lwjgl` and `lwjgl-sdl`, which arrive at runtime with imgui-java. An application that calls LWJGL itself (the LWJGL example creates its window and GL context) declares the LWJGL modules it compiles against as `implementation`.

```groovy
compileOnly "org.lwjgl:lwjgl:${rootProject.ext.lwjglVersion}"
compileOnly "org.lwjgl:lwjgl-sdl:${rootProject.ext.lwjglVersion}"
```

Native libraries are `runtimeOnly` with one declaration per platform classifier: `natives-windows`, `natives-linux`, `natives-macos` and `natives-macos-arm64`.

### Javadoc Excludes `impl` Packages
Generated Javadoc documents only exported packages. A module with `impl` packages excludes them from the `javadoc` sources and passes the main source directories as `-sourcepath`, so javadoc still resolves the `impl` types without documenting them. Use this exact form (Gradle has no `options.sourcepath`):

```groovy
tasks.named('javadoc', Javadoc) {
    exclude '**/impl/**'
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
The `examples` module registers one `JavaExec` task per example (`runJme3Example`, `runLwjglExample`) instead of applying the `application` plugin. Each sets `group = 'application'`, a description, `classpath = sourceSets.main.runtimeClasspath` and the main class, and passes `--enable-native-access=ALL-UNNAMED` so imgui-java and LWJGL load native code without restricted-method warnings on JDK 25. The tasks use the Java 25 toolchain by convention.

```groovy
tasks.register('runLwjglExample', JavaExec) {
    group = 'application'
    description = 'Runs the LorisUI example in a plain LWJGL SDL3 window.'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.ivan1pl.lorisui.example.lwjgl.LorisLwjglExample'
    jvmArgs '--enable-native-access=ALL-UNNAMED'
}
```
