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
Group (`com.ivan1pl.loris-ui`), version, repositories (`mavenLocal()` first — needed for the jME 3.10.0-SNAPSHOT — then `mavenCentral()`) and `test { useJUnitPlatform() }` are set once in root `allprojects`. Subprojects must not redeclare them.

### Shared Versions in Root `ext`
Versions used by more than one module (jMonkeyEngine, LWJGL) are defined once in the root `ext` block and referenced with Groovy interpolation in double-quoted coordinates. Add new shared versions there instead of hardcoding them in subprojects.

```groovy
implementation "org.jmonkeyengine:jme3-core:$jmonkeyVersion"
```

### jME3 Dependencies Non-Transitive in Library
The `jme3` library module declares jMonkeyEngine artifacts with `transitive = false` so the library does not impose engine transitive dependencies on consumers; the consuming application declares the full jME set (core, desktop, lwjgl3) itself.
