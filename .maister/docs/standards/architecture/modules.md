# Module Architecture

### Layered Modules with a Dependency-Free API
Modules form a strict layered chain with two hosts side by side: `examples` → `loris-ui-jme3` / `loris-ui-lwjgl` → `loris-ui-imgui` → `loris-ui-api`, with two internal renderer-port modules below the backend: `loris-ui-imgui` → `loris-ui-imgui-vulkan` / `loris-ui-imgui-gles3`, and `loris-ui-imgui-vulkan` → `loris-ui-api` (`loris-ui-imgui-gles3` depends on no LorisUI module). Dependencies only point toward `api`; each library depends on the layer directly below it, hosts never depend on each other, and only the module a helper serves depends on that helper.
`loris-ui-api` must have **zero** non-test dependencies and must not import any toolkit, engine or graphics library (ImGui, LWJGL, jME) — only `java.*` and its own packages. Components and styles in `api` are pure descriptions with no drawing code; rendering lives in a backend module (`imgui`) and engine or loop integration in a host module (`jme3`, `lwjgl`). Host-neutral screen, teardown and i18n logic that every host needs (`ScreenManager`, `DeferredRelease`, `ResourceBundleKeyResolver`) belongs in an exported `api` package, using only `java.*`: the automatic `lorisui.jme3` module can read only exported packages, so a shared host helper cannot live in an `impl` package or in a host module.
A new backend module depends only on `loris-ui-api`; a new host module depends only on `loris-ui-imgui` (which brings `loris-ui-api` transitively).
Code is split off into an internal helper module only when it must ship under another license (code derived from MIT-licensed projects, see License per Subproject in `build/gradle.md`); otherwise it stays in an `impl` package of the module that uses it. A helper depends on nothing above itself and serves exactly one module.

```java
// api:   public class ProgressBar implements Component { /* data + builder only */ }
// imgui: public class ProgressBarRenderer extends Renderer<ProgressBar> { /* ImGui calls */ }
```

### Package Structure
All code lives under `com.ivan1pl.lorisui.<module>.<area>` (e.g. `api.component`, `api.component.style`, `imgui.renderer`, `imgui.renderer.style`); the examples module uses `com.ivan1pl.lorisui.example`. Concrete implementations and internal helpers that are not public API go into an `impl` subpackage (`imgui.impl`, `backend.impl`, `renderer.impl`, `renderer.style.impl`). An internal helper module has one package of its own, `com.ivan1pl.lorisui.<module>.<part>` after its artifact `loris-ui-<module>-<part>` (`imgui.gles3`, `imgui.vulkan`), which no other module splits. The Maven group is `com.ivan1pl.loris-ui`.

### JPMS Module Naming
Library modules declare a `module-info.java` named `lorisui.<module>` — **no `.main` suffix** (`lorisui.api`, `lorisui.imgui`, `lorisui.lwjgl`); an internal helper module is named `lorisui.<module>.<part>` (`lorisui.imgui.gles3`, `lorisui.imgui.vulkan`). Dependent modules `requires` siblings by that name, with `requires transitive` when the sibling's types appear in their exported signatures (`lorisui.imgui` requires `lorisui.api` transitively, `lorisui.lwjgl` requires `lorisui.imgui` transitively). Toolkit modules used only internally are plain `requires` (`requires org.lwjgl.sdl;` in `lorisui.imgui` for SDL_GPU and in `lorisui.lwjgl`, `requires org.lwjgl.glfw;` in `lorisui.lwjgl`), and so are internal helper modules (`requires lorisui.imgui.gles3;` and `requires lorisui.imgui.vulkan;` in `lorisui.imgui`), which are never `transitive`.

**Internal helper modules use a qualified export.** A helper module exports its package only to the module it serves (`exports com.ivan1pl.lorisui.imgui.vulkan to lorisui.imgui;`), so applications cannot use it, and puts `@SuppressWarnings("module")` on its module declaration, with a comment naming the reasons: javac compiles the helper before the module it serves, so it never finds the export target, and `lorisui.imgui.gles3` ends in digits (to match its artifact). Its classes are public only for that module. The package is not public API: it gets a `package-info.java` and the module a doc comment (stating that it is internal, its license and its optional binding), but the generated Javadoc excludes the package (see `build/gradle.md`). On the module path a helper must be present at runtime whenever the module it serves is, which the dependency's runtime scope ensures; no application `requires` it.

**Exception: optional toolkit modules are a plain `requires static`.** A toolkit module that only one optional variant uses, and that the consuming application already supplies, is declared `requires static` — never `requires transitive` or `requires static transitive` — even when its types appear in exported signatures: `requires static org.lwjgl.opengles;` and `requires static org.lwjgl.vulkan;` in `lorisui.imgui` (the GLES3 and Vulkan variants and backends), `requires static org.lwjgl.opengles;` in `lorisui.imgui.gles3` and `requires static org.lwjgl.vulkan;` in `lorisui.imgui.vulkan` (the renderer ports), `requires static org.lwjgl.vulkan;` in `lorisui.lwjgl` (the Vulkan display). LWJGL sets the precedent: its own `org.lwjgl.sdl` declares a plain `requires static org.lwjgl.vulkan` although `SDL_Vulkan_CreateSurface` exposes `VkInstance`.
- Modular consumers that never run a variant need its binding neither at compile time nor at runtime, and no consumer needs it at compile time unless it calls the variant's API itself. A `requires static` module is not added to the module graph at runtime, though, so a modular application in which the variant runs makes sure the binding is resolved: it `requires` the module itself (a Vulkan application already needs `org.lwjgl.vulkan` to create its device) or adds it with `--add-modules` (a modular jME application on jME's default ANGLE renderer, which runs the GLES3 variant, resolves `org.lwjgl.opengles` either way). The module descriptor's doc comment says so.
- `static transitive` is not used: it would make the binding mandatory on the compile module path of every modular consumer.
- The accepted cost is a `javac -Xlint:exports` (or `-Xlint:all`) warning that the Vulkan types in exported signatures are "not indirectly exported using 'requires transitive'". The build passes no `-Xlint` or `-Werror` to `javac`, and the Javadoc gate's doclint does not check exports, so the build stays green; do not add `-Xlint:all` with `-Werror` to `javac` without suppressing `exports` for these modules.
- Classes that touch such a module must be loaded only when the variant is used: keep the binding's types out of the shared base classes, constructors and static initializers of code every host runs (for example `ImGuiRenderer`, `ImGuiBackend`, `ImGuiLwjglDisplayBase`), and reference them only from the variant's own classes.

A module that can't be a named module because its dependencies split packages (`jme3`, against the jME3 jars) has no `module-info.java`; its jar declares `Automatic-Module-Name: lorisui.<module>` instead (see `build/gradle.md`).

### Internal `impl` Packages Are Not Exported
`module-info.java` exports only public contract packages, each listed explicitly. `impl` subpackages are never exported, not even to a sibling module; the only qualified exports are those of internal helper modules to the module they serve (see JPMS Module Naming). When adding a new public package, add an `exports` line (and a `package-info.java`, see `global/commenting.md`); keep implementation classes in `impl`. Host-internal logic shared by the classes of one exported package (the LWJGL displays' `ImGuiLwjglDisplayBase` and `ImGuiLwjglSdlInput`) may instead be package-private in that package, which keeps it out of the API without an `impl` package.

```java
module lorisui.imgui {
    exports com.ivan1pl.lorisui.imgui;
    exports com.ivan1pl.lorisui.imgui.backend;
    exports com.ivan1pl.lorisui.imgui.renderer;
    exports com.ivan1pl.lorisui.imgui.renderer.style;
    // com.ivan1pl.lorisui.imgui.impl and com.ivan1pl.lorisui.imgui.renderer.impl are NOT exported
    requires transitive lorisui.api;
    requires lorisui.imgui.gles3;       // internal helper modules: plain requires
    requires lorisui.imgui.vulkan;
    requires org.lwjgl.sdl;
    requires static org.lwjgl.opengles; // GLES3 variant only; supplied by jME
    requires static org.lwjgl.vulkan;   // Vulkan variant only; the application requires it itself
}

module lorisui.lwjgl {
    exports com.ivan1pl.lorisui.lwjgl;
    requires transitive lorisui.imgui;
    requires org.lwjgl.glfw;
    requires org.lwjgl.sdl;
    requires static org.lwjgl.vulkan;
}

@SuppressWarnings("module")
module lorisui.imgui.vulkan {
    // The suppressed warning: javac never finds the export's target, lorisui.imgui, because it depends on this module
    exports com.ivan1pl.lorisui.imgui.vulkan to lorisui.imgui;
    requires imgui.binding;
    requires lorisui.api;
    requires org.lwjgl;
    requires static org.lwjgl.vulkan;
}
```
