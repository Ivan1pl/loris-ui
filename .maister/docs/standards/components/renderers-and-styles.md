# Renderers & Style Appliers

### Renderer Implementation Shape
Each renderer is `public class <Component>Renderer extends Renderer<Component>` in `imgui.renderer.impl`, overriding the single `render(component, w, h, rendererProvider, bundleKeyResolver, topLevel, enableInput)` method. Renderers have no-argument constructors and hold no backend reference: everything they need from the backend comes through the `RendererProvider` argument. Custom renderers outside the library follow the same shape and are registered with `registerRenderer` (see `architecture/component-model.md`).
- Render children via `rendererProvider.getRenderer(child).render(...)` with `topLevel = false`.
- ImGui window flags are `private static final int FLAGS_*` constants (one flag per line, combined with `|`); disable input with `enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs)`.
- Unique ImGui IDs derive from the component (`component + "_text"`, `"##" + component`), also for inputs (never a bare `"##"`, which gives two inputs in one window the same ID) and for combo items (derived inside the item's `pushID` scope).
- Font-bearing components wrap drawing in `((ImGuiFont) font).useFont(() -> doRender(...))`, falling back when the font is null. `ImGuiFont` lives in `com.ivan1pl.lorisui.imgui` (there is no `imgui.font` package).
- Every ImGui push has a matching pop.
- Load textures with `rendererProvider.loadTexture(path)` or `rendererProvider.loadTexture(path, maskedR, maskedG, maskedB)` and draw them with `rendererProvider.renderTexture(id, imageMode, ...)`, passing the `long` id from the returned `TextureData` unchanged. The id is opaque and backend-specific; never interpret it in a renderer. The backend draws every `ImageMode` itself (including `Repeat` tiling), so no renderer depends on the texture's wrap mode. Wrap the `IOException` in a `RuntimeException`, since `render` can't throw it. Never unload textures from a renderer or a component callback: the backend rejects `unloadTextures()` while it is drawing.
- Draw a `Canvas` image through `rendererProvider.loadTexture(canvas)`, which returns the canvas's own up-to-date texture in this backend, and draw the visible region with `renderTexture(id, ImageMode.subImage(...))`. Run the canvas's triggers (`click`, `drag`, `wheel`) before `loadTexture` and read `getVisibleRegion()` again after them, so changes a callback makes show in the same frame. The renderer never holds the canvas's lock: it calls only the canvas's own atomic methods, never `takePixelUpdate` (that belongs to the backend).
- Renderer-shared global flags live in the internal `com.ivan1pl.lorisui.imgui.impl.ImGuiState`; hosts never read it directly but use the input-capture queries of `ImGuiRenderer`.
- Respect `enableInput`: when it is false (combo labels and previews, tooltip content, `NoInputs` windows) the renderer takes no input and registers nothing for keyboard focus; children that must not take input are rendered with `enableInput = false`.
- **Enabled state and disabled style**: a renderer of a disableable component evaluates `isEnabled()` once per draw and uses that value for the keyboard-focus registration, `beginDisabled` and the style choice, so one draw never sees two states (the component's trigger methods keep their own `isEnabled()` gate). It applies the component's `disabledStyle` while disabled when it is set, otherwise its `style`. A disabled widget is drawn inside `ImGui.beginDisabled(!enabled)` with `endDisabled()` in `finally`, balanced per window: every window or child begun inside the block also ends inside it. A click on a disabled widget still sets the keyboard-focus anchor, so click hover checks add `ImGuiHoveredFlags.AllowWhenDisabled`. A disabled input that is still Dear ImGui's active ID is released with `imgui.internal.ImGui.clearActiveID()` before its widget, so it takes no keys in the frame it becomes disabled; an expanded combo box that is disabled closes its popup.

### Cross-Frame Input State
Renderers are stateless per frame except for input state that must span frames (button press identity, hover start, canvas gestures, keyboard focus). Such state lives in a native-free helper class owned by the one object that needs it and keyed by the component's ImGui ID:
- **Per renderer instance**: a package-private helper in `imgui.renderer.impl` owned by the renderer (e.g. `ButtonInputTracker` in `PanelRenderer`, `CanvasInputTracker` in `CanvasRenderer`). Stale entries are dropped by frame number, so state never survives a skipped frame. The renderer reads ImGui and passes the component plus plain values to the helper's `update`, which calls the component's trigger methods (keyboard focus starts and key activations are plain inputs like pointer events) and returns what the renderer needs. Because renderers belong to a backend, this state is per `ImGuiBackend` (see `architecture/component-model.md`, one backend per renderer).
- **Hit-testing without an active item**: such helpers are fed from a `dummy(width, height)` hit area, `isWindowHovered(ChildWindows)` and raw mouse state, never from `invisibleButton` or another widget that becomes active, so a pointer gesture does not set `ActiveId` and does not capture the keyboard. A canvas additionally calls `imgui.internal.ImGui.itemHoverable(...)` while hovered, so a press does not start a window move; it never activates the item. The accepted consequence is that other widgets may show hover during a drag.
- **Spanning several renderers**: keyboard focus involves the panel, input and combo renderers and the frame driver, so its helper, `KeyboardFocus`, is owned by `ImGuiRenderer` (one per renderer), lives in the non-exported `imgui.impl` and is reached by renderers only through the renderer's keyboard-focus slot during the frame, which is set in every frame (frame mechanics in `project/architecture.md`). Register through that slot, read once per `render` and only when `enableInput` is true; a `null` slot means the component takes no input, never "navigation off"; the navigation switch is applied inside `KeyboardFocus` and `wantsKeyboardInput()`, never by a renderer. A focusable renderer registers each element in draw order with its component-derived ImGui ID, its real enabled state (the value evaluated once for the draw) and the component as its `Focusable`, acts on the returned result bits, reports clicks and input activity, and pairs every scope push with exactly one `popScope()` in `finally`, so it also runs when the content throws.

Cross-frame helpers stay free of `imgui.ImGui` calls and are unit-tested directly (see `testing/test-writing.md`). A **stateless** helper that only wraps a shared sequence of Dear ImGui calls is a different kind: package-private and final in `imgui.renderer.impl`, with static methods and no fields, and untested like the renderers (e.g. `InputKeyboardFocus`, shared by the text and number input renderers).

### Canvas Wheel Take-Over
The `Canvas` takes the mouse wheel itself through a scrollable child window in `CanvasRenderer`. The mechanism is specific to the canvas and is not a general pattern for other renderers; it is described in `project/architecture.md` (`CanvasRenderer`, and the known concern "Canvas wheel take-over is best effort").

### Class-Path Resource Lookup
Fonts and textures are loaded by class-path resource name through the class loader (`imgui.impl.ClassPathResources`), never through `Class.getResourceAsStream`. A single leading `/` is stripped, so `"/textures/x.png"` and `"textures/x.png"` name the same resource. A missing resource is an `IOException` whose message includes the path. Public Javadoc for resource-path parameters states the module-path rule (see `global/commenting.md`).

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
