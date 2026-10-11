# Renderers & Style Appliers

### Renderer Implementation Shape
Each renderer is `public class <Component>Renderer extends Renderer<Component>` in `imgui.renderer.impl`, overriding the single `render(component, w, h, rendererProvider, bundleKeyResolver, topLevel, enableInput)` method. Renderers have no-argument constructors and hold no backend reference: everything they need from the backend comes through the `RendererProvider` argument. Custom renderers outside the library follow the same shape and are registered with `registerRenderer` (see `architecture/component-model.md`).
- Render children via `rendererProvider.getRenderer(child).render(...)` with `topLevel = false`.
- ImGui window flags are `private static final int FLAGS_*` constants (one flag per line, combined with `|`); disable input with `enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs)`.
- Unique ImGui IDs derive from the component (`component + "_text"`, `"##" + component`).
- Font-bearing components wrap drawing in `((ImGuiFont) font).useFont(() -> doRender(...))`, falling back when the font is null. `ImGuiFont` lives in `com.ivan1pl.lorisui.imgui` (there is no `imgui.font` package); `useFont` pops the font in `finally` and throws `IllegalStateException` (without running the code) if the renderer the font was registered with is not initialized or another renderer's Dear ImGui context is current.
- Every ImGui push has a matching pop.
- Load textures with `rendererProvider.loadTexture(path)` or `rendererProvider.loadTexture(path, maskedR, maskedG, maskedB)` and draw them with `rendererProvider.renderTexture(...)`. The backend caches textures per instance, keyed by the normalized path (`"/x.png"` and `"x.png"` share one texture) and the masks, until its `unloadTextures()` runs. Wrap the `IOException` in a `RuntimeException`, since `render` can't throw it. Never unload textures from a renderer or a component callback: the backend rejects `unloadTextures()` while it is drawing.
- Renderer-shared global flags live in the internal `com.ivan1pl.lorisui.imgui.impl.ImGuiState`; hosts never read it directly but use the `ImGuiOpenGlRenderer` input-capture queries.

### Class-Path Resource Lookup
Fonts and textures are loaded by class-path resource name through the class loader (`imgui.impl.ClassPathResources.open(path)`), never through `Class.getResourceAsStream`. A single leading `/` is stripped, so `"/textures/x.png"` and `"textures/x.png"` name the same resource. A missing resource is an `IOException` whose message includes the path. Public Javadoc for resource-path parameters states the module-path rule (see `global/commenting.md`).

### Style Class Shape
Style classes (in `api.component.style`, implementing `Style`) store every attribute as `Supplier<Color>` / `Supplier<Float>`, default each builder field to `() -> null` (meaning "keep toolkit default"), and expose getters returning the unwrapped nullable value (`Color`, boxed `Float`). Each attribute has a constant overload alongside the Supplier overload.

```java
private Supplier<Float> rounding = () -> null;

public Float getRounding() {
    return rounding.get();
}
```

### StyleApplier Implementation Shape
Each applier implements `StyleApplier<XStyle>`: `applyStyles` returns an opaque `Styles` token, and `popStyles(Styles)` pops exactly what was pushed. The token counts pushed colours/vars (`popColors` / `popVars`); push only non-null values. Callers always pair `applyStyles` with `popStyles`, in `try/finally` where possible.

```java
@Override
public Styles applyStyles(ScrollbarStyle style, RendererProvider rendererProvider) {
    StylesImpl styles = new StylesImpl();
    applyColor(ImGuiCol.ScrollbarBg, style.getBackgroundColor(), styles);
    applyVar(ImGuiStyleVar.ScrollbarSize, style.getSize(), styles);
    return styles;
}
```
