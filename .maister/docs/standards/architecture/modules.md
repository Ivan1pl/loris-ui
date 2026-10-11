# Module Architecture

### Layered Modules with a Dependency-Free API
Modules form a strict chain: `examples` → `loris-ui-jme3` → `loris-ui-imgui` → `loris-ui-api`. Dependencies only point toward `api`; each library depends on the layer directly below it.
`loris-ui-api` must have **zero** non-test dependencies and must not import any toolkit, engine or graphics library (ImGui, LWJGL, jME) — only `java.*` and its own packages. Components and styles in `api` are pure descriptions with no drawing code; rendering lives in a backend module (`imgui`) and engine integration in a host module (`jme3`).
A new backend module depends only on `loris-ui-api`.

```java
// api:   public class ProgressBar implements Component { /* data + builder only */ }
// imgui: public class ProgressBarRenderer extends Renderer<ProgressBar> { /* ImGui calls */ }
```

### Package Structure
All code lives under `com.ivan1pl.lorisui.<module>.<area>` (e.g. `api.component`, `api.component.style`, `imgui.renderer`, `imgui.renderer.style`); the examples module uses `com.ivan1pl.lorisui.example`. Concrete implementations and internal helpers that are not public API go into an `impl` subpackage (`imgui.impl`, `backend.impl`, `renderer.impl`, `renderer.style.impl`). The Maven group is `com.ivan1pl.loris-ui`.

### JPMS Module Naming
Library modules declare a `module-info.java` named `lorisui.<module>` — **no `.main` suffix** (`lorisui.api`, `lorisui.imgui`). Dependent modules `requires` siblings by that name, with `requires transitive` when the sibling's types appear in their exported signatures (`lorisui.imgui` requires `lorisui.api` transitively).
A module that can't be a named module because its dependencies split packages (`jme3`, against the jME3 jars) has no `module-info.java`; its jar declares `Automatic-Module-Name: lorisui.<module>` instead (see `build/gradle.md`).

### Internal `impl` Packages Are Not Exported
`module-info.java` exports only public contract packages, each listed explicitly. `impl` subpackages are never exported. When adding a new public package, add an `exports` line (and a `package-info.java`, see `global/commenting.md`); keep implementation classes in `impl`.

```java
module lorisui.imgui {
    exports com.ivan1pl.lorisui.imgui.renderer;
    exports com.ivan1pl.lorisui.imgui.renderer.style;
    // com.ivan1pl.lorisui.imgui.impl and com.ivan1pl.lorisui.imgui.renderer.impl are NOT exported
    requires transitive lorisui.api;
}
```
