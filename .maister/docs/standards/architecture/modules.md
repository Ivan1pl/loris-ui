# Module Architecture

### Layered Modules with a Dependency-Free API
Modules form a strict layered chain with two hosts side by side: `examples` → `loris-ui-jme3` / `loris-ui-lwjgl` → `loris-ui-imgui` → `loris-ui-api`. Dependencies only point toward `api`; each library depends on the layer directly below it, and hosts never depend on each other.
`loris-ui-api` must have **zero** non-test dependencies and must not import any toolkit, engine or graphics library (ImGui, LWJGL, jME) — only `java.*` and its own packages. Components and styles in `api` are pure descriptions with no drawing code; rendering lives in a backend module (`imgui`) and engine or loop integration in a host module (`jme3`, `lwjgl`). Host-neutral screen, teardown and i18n logic that every host needs (`ScreenManager`, `DeferredRelease`, `ResourceBundleKeyResolver`) belongs in an exported `api` package, using only `java.*`: the automatic `lorisui.jme3` module can read only exported packages, so a shared host helper cannot live in an `impl` package or in a host module.
A new backend module depends only on `loris-ui-api`; a new host module depends only on `loris-ui-imgui` (which brings `loris-ui-api` transitively).

```java
// api:   public class ProgressBar implements Component { /* data + builder only */ }
// imgui: public class ProgressBarRenderer extends Renderer<ProgressBar> { /* ImGui calls */ }
```

### Package Structure
All code lives under `com.ivan1pl.lorisui.<module>.<area>` (e.g. `api.component`, `api.component.style`, `imgui.renderer`, `imgui.renderer.style`); the examples module uses `com.ivan1pl.lorisui.example`. Concrete implementations and internal helpers that are not public API go into an `impl` subpackage (`imgui.impl`, `backend.impl`, `renderer.impl`, `renderer.style.impl`). The Maven group is `com.ivan1pl.loris-ui`.

### JPMS Module Naming
Library modules declare a `module-info.java` named `lorisui.<module>` — **no `.main` suffix** (`lorisui.api`, `lorisui.imgui`, `lorisui.lwjgl`). Dependent modules `requires` siblings by that name, with `requires transitive` when the sibling's types appear in their exported signatures (`lorisui.imgui` requires `lorisui.api` transitively, `lorisui.lwjgl` requires `lorisui.imgui` transitively). Toolkit modules used only internally are plain `requires` (`requires org.lwjgl.sdl;` in `lorisui.imgui` for SDL_GPU and in `lorisui.lwjgl`, `requires org.lwjgl.glfw;` in `lorisui.lwjgl`).
A module that can't be a named module because its dependencies split packages (`jme3`, against the jME3 jars) has no `module-info.java`; its jar declares `Automatic-Module-Name: lorisui.<module>` instead (see `build/gradle.md`).

### Internal `impl` Packages Are Not Exported
`module-info.java` exports only public contract packages, each listed explicitly. `impl` subpackages are never exported. When adding a new public package, add an `exports` line (and a `package-info.java`, see `global/commenting.md`); keep implementation classes in `impl`. Host-internal logic shared by the classes of one exported package (the LWJGL displays' `ImGuiLwjglDisplayBase` and `ImGuiLwjglSdlInput`) may instead be package-private in that package, which keeps it out of the API without an `impl` package.

```java
module lorisui.imgui {
    exports com.ivan1pl.lorisui.imgui;
    exports com.ivan1pl.lorisui.imgui.backend;
    exports com.ivan1pl.lorisui.imgui.renderer;
    exports com.ivan1pl.lorisui.imgui.renderer.style;
    // com.ivan1pl.lorisui.imgui.impl and com.ivan1pl.lorisui.imgui.renderer.impl are NOT exported
    requires transitive lorisui.api;
    requires org.lwjgl.sdl;
}

module lorisui.lwjgl {
    exports com.ivan1pl.lorisui.lwjgl;
    requires transitive lorisui.imgui;
    requires org.lwjgl.glfw;
    requires org.lwjgl.sdl;
}
```
