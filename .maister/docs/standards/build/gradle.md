# Gradle Build

### Gradle Wrapper and Groovy DSL
Build with the Gradle wrapper (`./gradlew`, pinned to 9.6.0). Build scripts use the Groovy DSL (`*.gradle`), not the Kotlin DSL.

### Module Naming and Registration
Subprojects live in short directories (`api`, `imgui`, `jme3`) and published ones are renamed to `loris-ui-<dir>` in `settings.gradle`. Inter-module dependencies reference the renamed path (`project(':loris-ui-api')`). Non-published modules (`examples`) keep their directory name.

```groovy
include 'foo'
project(":foo").name = "loris-ui-foo"
```

### Plugins per Module Type
Library modules apply `java-library` and `maven-publish`; the non-published demo module applies only `java`.

### Shared Configuration in `allprojects`
Group (`com.ivan1pl.loris-ui`), version, repositories (`mavenLocal()` first — needed for the jME 3.10.0-SNAPSHOT — then `mavenCentral()`), the Java toolchain and the JUnit Platform are set once in root `allprojects`. Subprojects must not redeclare them.

The toolchain and test settings use lazy hooks, because `allprojects` runs before the subprojects apply their plugins. A plain `java { }` or `test { }` block there fails for any subproject, since its `java` extension and `test` task don't exist yet when the root script runs. Use `plugins.withId('java')` (it also fires for `java-library`) and `tasks.withType(Test).configureEach`:

```groovy
allprojects {
    plugins.withId('java') {
        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }
    }

    tasks.withType(Test).configureEach {
        useJUnitPlatform()
    }
}
```

The toolchain only selects an installed JDK 25 (there is no foojay resolver plugin, so nothing is downloaded), and the JDK running Gradle does not matter.

### Shared Versions in Root `ext`
Versions used by more than one module or declaration (jMonkeyEngine, LWJGL, Mockito, imgui-java) are defined once in the root `ext` block. Add new shared versions there instead of hardcoding them in subprojects.

```groovy
ext {
    jmonkeyVersion = '3.10.0-SNAPSHOT'
    lwjglVersion = '3.4.1'
    mockitoVersion = '5.24.0'
    imguiVersion = '1.92.7.1'
}
```

Subprojects reference them explicitly as `rootProject.ext.<name>` inside double-quoted coordinates. The implicit form (`$jmonkeyVersion`, resolved through the parent project) is deprecated in Gradle 9.6 and fails in Gradle 10.

```groovy
implementation "org.jmonkeyengine:jme3-core:${rootProject.ext.jmonkeyVersion}"
testImplementation "org.mockito:mockito-core:${rootProject.ext.mockitoVersion}"
```

### jME3 Dependencies Non-Transitive in Library
The `jme3` library module declares jMonkeyEngine artifacts with `transitive = false` so the library does not impose engine transitive dependencies on consumers; the consuming application declares the full jME set (core, desktop, lwjgl3) itself.

### Dependency Scopes for Toolkit Libraries
A library module declares a dependency as `api` only when its types appear in the module's exported signatures (`api project(':loris-ui-api')` in `imgui`). Toolkit libraries used only inside the implementation are never `api`: compile against a pinned version with `compileOnly`, and declare the runtime requirement with `runtimeOnly` and a rich-version range that prefers the tested version, so consumers get the toolkit at runtime but not on their compile classpath. `lwjgl-stb` in `imgui` is the one remaining `api` toolkit dependency (roadmap, Phase 3).

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

Libraries that the consuming application already supplies at runtime are `compileOnly` only. `jme3` compiles against `lwjgl` and `lwjgl-sdl` (jME's `SdlEventListener` signature uses `SDL_Event`), which the application's `jme3-lwjgl3` provides.

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
