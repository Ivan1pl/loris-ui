# Gradle Build

### Gradle Wrapper and Groovy DSL
Build with the Gradle wrapper (`./gradlew`, pinned to 9.6.0). Build scripts use the Groovy DSL (`*.gradle`), not the Kotlin DSL.

### Module Naming and Registration
Subprojects live in short directories (`api`, `imgui`, `jme3`, ...) and published ones are renamed to `loris-ui-<dir>` in `settings.gradle`. Inter-module dependencies reference the renamed path (`project(':loris-ui-api')`). Non-published modules (`examples`) keep their directory name.

```groovy
include 'foo'
project(":foo").name = "loris-ui-foo"
```

### Plugins per Module Type
Library modules apply `java-library` and then `maven-publish`, in that order: the root publishing hook runs when `maven-publish` is applied and needs the `java` extension and `components.java`, which `java-library` creates. The non-published demo module applies only `java`. The root project has no sources and applies no plugin (`checkstyle` is applied to every Java project from root `allprojects`).

### Shared Configuration in `allprojects`
Group (`com.ivan1pl.loris-ui`), version, repositories (`mavenCentral()` only), the Java toolchain, Checkstyle, the JUnit Platform, the publishing configuration (see Publishing) and the Javadoc gate (see Javadoc Gate) are set once in root `allprojects`. Subprojects must not redeclare them.

These settings use lazy hooks, because `allprojects` runs before the subprojects apply their plugins: a plain `java { }` or `test { }` block there fails, since the `java` extension and `test` task don't exist yet. Use `plugins.withId('java')` (it also fires for `java-library`), `plugins.withId('maven-publish')` and `tasks.withType(Test).configureEach`:

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

The toolchain only selects an installed JDK 25 (no toolchain auto-provisioning plugin, so nothing is downloaded).

### Checkstyle Gate
Checkstyle is a failing build gate for the main and test sources of every Java project (`checkstyleMain` and `checkstyleTest` attach to `check`). The rules live once at the root in `config/checkstyle/checkstyle.xml` (`severity` `error`), with file-specific exceptions in `config/checkstyle/suppressions.xml`; subprojects have no Checkstyle configuration of their own. Keep `maxWarnings = 0` and the default `ignoreFailures = false`. Fix violations instead of suppressing them; add a suppression only for a deliberate, documented exception (e.g. the internal `ImGuiState` flag holder keeps its public static fields). A repository-root `.editorconfig` keeps editors in line with the same rules; there is no auto-formatter.

### Javadoc Gate
Javadoc is a failing build gate for every library module. The root `plugins.withId('maven-publish')` hook runs `javadoc` with strict doclint and makes `check` depend on `javadoc`, so `./gradlew build`, `check` and `publishToMavenLocal` fail on any Javadoc warning. Only modules that apply `maven-publish` get the gate, so `examples` is not linted. Fix warnings instead of relaxing the options; do not add `links` to external Javadoc, so the gate needs no network access.

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
The root `plugins.withId('maven-publish')` hook configures publishing for every library module: sources and Javadoc jars (`java { withSourcesJar(); withJavadocJar() }`), one `maven` publication `from components.java`, the shared POM metadata (url, developer, scm) and each module's single license (see License per Subproject). No `repositories` are configured, so artifacts are published only with `./gradlew publishToMavenLocal`; `examples` is not published.

Each library module sets its own POM name and description in its `build.gradle`, after the `plugins` block. Both are required; a module missing either fails POM generation with a `GradleException` naming the module. A string wrapped to stay within 120 columns ends the line with the `+` and continues with an 8-space indent, because a line starting with `+` is parsed as a new statement with a unary plus.

```groovy
description = 'Toolkit-agnostic LorisUI API: a declarative component model for game menus and HUDs, screens, ' +
        'and the backend port that renders them.'
ext.pomName = 'LorisUI API'
```

The hook runs when `maven-publish` is applied, before the rest of the module's script has set these values, so per-module values are read through providers evaluated when the POM is generated. Inside `pom {}`, qualify every project property with `project.` (`project.provider`, `project.ext.pomName`, `project.description`): a bare `ext` or `description` there binds to the POM object, not the project.

### Shared Versions in Root `ext`
Versions used by more than one module or declaration (jMonkeyEngine, LWJGL, Mockito, imgui-java, Checkstyle) are defined once in the root `ext` block, at the top of the root `build.gradle`, before `allprojects`. Add new shared versions there instead of hardcoding them in subprojects.

Subprojects reference them explicitly as `rootProject.ext.<name>` inside double-quoted coordinates. The implicit form (`$jmonkeyVersion`, resolved through the parent project) is deprecated in Gradle 9.6 and fails in Gradle 10.

```groovy
implementation "org.jmonkeyengine:jme3-core:${rootProject.ext.jmonkeyVersion}"
testImplementation "org.mockito:mockito-core:${rootProject.ext.mockitoVersion}"
```

Mockito is a test dependency of `api` only (see `testing/test-writing.md`); other modules don't declare it until a test uses it.

### Dependency Scopes for Toolkit Libraries
- A library module declares a dependency as `api` only when its types appear in the module's exported signatures (`api project(':loris-ui-api')` in `imgui`).
- Toolkit libraries used only inside the implementation are never `api`: compile against a pinned version with `compileOnly`, and declare the runtime requirement with `runtimeOnly` and a rich-version range that prefers the tested version, so consumers get the toolkit at runtime but not on their compile classpath.
- A toolkit library the module calls directly but never exposes is `implementation` (e.g. `lwjgl-stb` in `imgui`), and so is an internal helper subproject whose types never appear in exported signatures.

```groovy
compileOnly "io.github.spair:imgui-java-app:${rootProject.ext.imguiVersion}"

runtimeOnly('io.github.spair:imgui-java-app') {
    version {
        require "[${rootProject.ext.imguiVersion},1.93)"
        prefer rootProject.ext.imguiVersion
    }
}
```

Cap toolkit ranges below the next minor (`[1.92.7.1,1.93)`), never open-ended: toolkits such as imgui-java are not semver, and a published POM carries only the range (Maven ignores `prefer`), so an open range lets Maven consumers resolve an untested release. Raise the cap when moving to a new minor. Only runtime classpaths resolve the range, so their first resolution (and the first after Gradle's dynamic-version cache expires) needs network access.

Libraries that the consuming application (or another runtime dependency) already supplies at runtime are `compileOnly` only: jMonkeyEngine in `jme3`, declared non-transitive (`transitive = false`) so jME's own dependencies stay off the compile and Javadoc classpath and the LWJGL modules the host compiles against are declared explicitly; and the LWJGL modules a host or backend compiles against that arrive at runtime with jME or imgui-java (e.g. `lwjgl-sdl`, `lwjgl-glfw`). An application that calls a library itself, such as `examples`, declares it as `implementation`.

```groovy
compileOnly("org.jmonkeyengine:jme3-core:${rootProject.ext.jmonkeyVersion}") { transitive = false }
compileOnly "org.lwjgl:lwjgl-sdl:${rootProject.ext.lwjglVersion}"
```

Bindings that only an optional variant uses, and that the application supplies (`lwjgl-opengles`, `lwjgl-vulkan`), are `compileOnly` with no runtime scope; their JPMS counterpart is the plain `requires static` exception in `architecture/modules.md`. They are absent from the test classpath too, except where a test needs them (Vulkan parameter types in tested signatures: `testCompileOnly` + `testRuntimeOnly`; classes under test that must link: `testRuntimeOnly`; see `testing/test-writing.md`).

Native libraries are `runtimeOnly` with one declaration per platform classifier: `natives-windows`, `natives-linux`, `natives-macos` and `natives-macos-arm64`. The exception is `lwjgl-vulkan` in `examples`, which declares only the macOS classifiers (they provide MoltenVK); Linux and Windows use the system Vulkan loader.

### License per Subproject
Every published artifact carries exactly one license: its POM lists one `<license>`, and its jar and sources jar contain that license's text as `META-INF/LICENSE`. The root publishing hook reads both from a lazy per-project `ext.license` map (`name`, `url`, `file`), which defaults to Apache-2.0 with the root `LICENSE` and fails the build when the declared file does not exist. Do not add a second `<license>` to a POM or extra notice files to `META-INF/`.

Code derived from MIT-licensed projects (Dear ImGui, imgui-java) is never mixed into an Apache-2.0 module. It lives in its own MIT-only subproject with its own `LICENSE` file (the upstream copyright lines plus the maintainer's), and each ported source file keeps its MIT header. That subproject sets `ext.license` after its `plugins` block, next to `description` and `ext.pomName`:

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

Any library module that gains an `impl` package adds the same block to its `build.gradle`; the root Javadoc gate does not exclude `impl` packages by itself. A package exported only to another LorisUI module (a qualified export) is not public API either, so its module excludes it the same way, by its path (e.g. `exclude 'com/ivan1pl/lorisui/imgui/vulkan/**'`), and the generated Javadoc is the module page only.

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
The `examples` module registers one `JavaExec` task per example entry point instead of applying the `application` plugin. The tasks are registered in a loop over one shared list of examples, which `runAllExamples` reads too, so an example is added in one place. Each task sets `group = 'application'`, a description (for jME examples, naming the jME renderer), `classpath = sourceSets.main.runtimeClasspath` and the main class, and passes `--enable-native-access=ALL-UNNAMED` so imgui-java and LWJGL load native code without restricted-method warnings. A system property that an example reads is passed through explicitly with `systemProperty` and `providers.systemProperty(...)`, because `JavaExec` does not inherit the Gradle JVM's properties (e.g. `lorisui.example.gpuDebug`).

`runAllExamples` (group `application`) starts all examples at once, each in its own JVM:
- It is not a `dependsOn` aggregate (a `JavaExec` task blocks until its window closes and one project's tasks run one after another), and it does not use `project.javaexec`, which was removed in Gradle 9. Its task class is declared in `examples/build.gradle` (its single source) with injected `ExecOperations` and `FileSystemOperations` and every property annotated, because Gradle rejects unannotated task properties.
- It passes the toolchain launcher explicitly, because `ExecOperations.javaexec` does not apply the toolchain convention; Java 25 stays defined only in the root build.
- Each JVM gets its own LWJGL natives folder (`org.lwjgl.system.SharedLibraryExtractPath`) under the task's temporary directory, deleted when that example exits, because JVMs starting together race on LWJGL's shared extraction directory.
- One failing example does not stop the others; after all have exited the task throws a `GradleException` naming every failed example.
