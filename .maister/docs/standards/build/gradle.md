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
Versions used by more than one module (jMonkeyEngine, LWJGL, Mockito) are defined once in the root `ext` block. Add new shared versions there instead of hardcoding them in subprojects.

```groovy
ext {
    jmonkeyVersion = '3.10.0-SNAPSHOT'
    lwjglVersion = '3.4.1'
    mockitoVersion = '5.24.0'
}
```

Subprojects reference them explicitly as `rootProject.ext.<name>` inside double-quoted coordinates. The implicit form (`$jmonkeyVersion`, resolved through the parent project) is deprecated in Gradle 9.6 and fails in Gradle 10.

```groovy
implementation "org.jmonkeyengine:jme3-core:${rootProject.ext.jmonkeyVersion}"
testImplementation "org.mockito:mockito-core:${rootProject.ext.mockitoVersion}"
```

### jME3 Dependencies Non-Transitive in Library
The `jme3` library module declares jMonkeyEngine artifacts with `transitive = false` so the library does not impose engine transitive dependencies on consumers; the consuming application declares the full jME set (core, desktop, lwjgl3) itself.
