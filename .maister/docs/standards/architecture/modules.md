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
All code lives under `com.ivan1pl.lorisui.<module>.<area>` (e.g. `api.component`, `api.component.style`, `imgui.renderer`, `imgui.renderer.style`); the examples module uses `com.ivan1pl.lorisui.example`. Concrete implementations that are not public API go into an `impl` subpackage (`renderer.impl`, `renderer.style.impl`). The Maven group is `com.ivan1pl.loris-ui`.

### JPMS Module Naming
Library modules declare a `module-info.java` named `lorisui.<module>` — **no `.main` suffix** (e.g. `lorisui.api`, `lorisui.imgui`). Dependent modules `requires` siblings by that name.
Note: the existing descriptors still use `lorisui.api.main` / `lorisui.imgui.main` and are pending rename (roadmap Phase 2). Do not introduce new `.main` names.

### Internal `impl` Packages Are Not Exported
`module-info.java` exports only public contract packages, each listed explicitly. `impl` subpackages are never exported. When adding a new public package, add an `exports` line; keep implementation classes in `impl`.

```java
module lorisui.imgui {
    exports com.ivan1pl.lorisui.imgui.renderer;
    exports com.ivan1pl.lorisui.imgui.renderer.style;
    // com.ivan1pl.lorisui.imgui.renderer.impl is NOT exported
    requires lorisui.api;
}
```
