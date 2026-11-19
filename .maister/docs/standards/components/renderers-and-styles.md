# Renderers & Style Appliers

### Renderer Implementation Shape
Each renderer is `public class <Component>Renderer extends Renderer<Component>` in `imgui.renderer.impl`, overriding the single `render(component, w, h, rendererProvider, bundleKeyResolver, topLevel, enableInput)` method. Renderers have no-argument constructors and hold no backend reference: everything they need from the backend comes through the `RendererProvider` argument. Custom renderers outside the library follow the same shape and are registered with `registerRenderer` (see `architecture/component-model.md`).
- Render children via `rendererProvider.getRenderer(child).render(...)` with `topLevel = false`.
- ImGui window flags are `private static final int FLAGS_*` constants (one flag per line, combined with `|`); disable input with `enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs)`.
- Unique ImGui IDs derive from the component (`component + "_text"`, `"##" + component`), also for inputs (never a bare `"##"`, which made two inputs in one window share an ID) and for combo items (`ImGui.getID("")` inside the item's `pushID(item.toString())` scope).
- Font-bearing components wrap drawing in `((ImGuiFont) font).useFont(() -> doRender(...))`, falling back when the font is null. `ImGuiFont` lives in `com.ivan1pl.lorisui.imgui` (there is no `imgui.font` package); `useFont` pops the font in `finally` and throws `IllegalStateException` (without running the code) if the renderer the font was registered with is not initialized or another renderer's Dear ImGui context is current.
- Every ImGui push has a matching pop.
- Load textures with `rendererProvider.loadTexture(path)` or `rendererProvider.loadTexture(path, maskedR, maskedG, maskedB)` and draw them with `rendererProvider.renderTexture(id, imageMode, ...)`, passing the `long` id from the returned `TextureData` unchanged. The id is opaque and backend-specific: an OpenGL or OpenGL ES texture name, an `SDL_GPUTexture*` or a `VkDescriptorSet`, depending on the backend; never interpret it in a renderer. The backend draws every `ImageMode` itself; `Repeat` is tiled as a grid of image items (`imgui.backend.impl.TextureTiling`), so no renderer and no backend depends on the texture's wrap mode (GL and GLES textures use `GL_CLAMP_TO_EDGE`, and the Vulkan sampler `CLAMP_TO_EDGE`). The backend caches textures per instance, keyed by the normalized path (`"/x.png"` and `"x.png"` share one texture) and the masks, until its `unloadTextures()` runs. Wrap the `IOException` in a `RuntimeException`, since `render` can't throw it. Never unload textures from a renderer or a component callback: the backend rejects `unloadTextures()` while it is drawing.
- Renderer-shared global flags live in the internal `com.ivan1pl.lorisui.imgui.impl.ImGuiState`; hosts never read it directly but use the input-capture queries of `ImGuiRenderer` (inherited by every variant).
- Respect `enableInput`: when it is false (combo labels and previews, tooltip content, `NoInputs` windows) the renderer takes no input and registers nothing for keyboard focus; children that must not take input are rendered with `enableInput = false`.

### Cross-Frame Input State
Renderers are stateless per frame except for input state that must span frames (button press identity, hover start, keyboard focus). Such state lives in a native-free helper class owned by the one object that needs it and keyed by the component's ImGui ID:
- **Per renderer instance**: a package-private helper in `imgui.renderer.impl` owned by the renderer (`ButtonInputTracker`, owned by `PanelRenderer`). Stale entries are dropped by frame number: hover entries not refreshed in the previous frame (all of them after a skipped frame), and a recorded press after a frame with a release or a skipped frame. The renderer reads ImGui (IDs, hovered/clicked/down/released flags, frame count, and the keyboard-focus result bits) and passes the component plus plain ints and booleans to the helper's `update` (including `keyboardFocusStarted` and `keyActivated`), which calls the component's trigger methods (`hover()`, `click()`, each at most once per update, so a keyboard focus start is a hover start and a key activation is a click) and returns the active state. Because renderers belong to a backend, this state is per `ImGuiBackend` (see `architecture/component-model.md`, one backend per renderer).
- **Spanning several renderers**: keyboard focus involves the panel, input and combo renderers and the frame driver, so its helper, `KeyboardFocus`, is owned by `ImGuiRenderer` (one per renderer), lives in the non-exported `imgui.impl` (public so `renderer.impl` reaches it) and is published only during `screen.render` through the `ImGuiState.keyboardFocus` slot. The renderer begins it before the screen draws, from plain key and click booleans read from Dear ImGui, and commits it (`endFrame`) right after, or discards a failed frame. A renderer reads the slot once per `render`, only when `enableInput` is true, and treats `null` as "navigation off" with no other behavior change. A focusable renderer registers each element in draw order with its component-derived ImGui ID, its kind and its enabled state (`register`, `registerComboItem`), acts on the returned result bits (focus look with existing hovered colors, activation through the component's trigger methods or the widget, scope closing, input focus and release), reports clicks and input activity, and pairs every `pushPopupScope()`/`pushComboScope(ownerId)` with exactly one `popScope()` in `finally`, so it also runs when the content throws: a popup's scope is pushed after a successful begin and popped after the end call on the normal path, and a combo's scope is popped before `endCombo`.

Cross-frame helpers stay free of `imgui.ImGui` calls and are unit-tested directly (see `testing/test-writing.md`). A **stateless** helper that only wraps a shared sequence of Dear ImGui calls is a different kind: package-private and final in `imgui.renderer.impl`, with static methods and no fields, and untested like the renderers (`InputKeyboardFocus.inputText`, shared by the text and number input renderers: register, `setKeyboardFocusHere()` right before the widget, report activity and clicks after it, `clearActiveID()` after the widget on a release).

### Class-Path Resource Lookup
Fonts and textures are loaded by class-path resource name through the class loader (`imgui.impl.ClassPathResources.open(path)`; all four texture backends go through `imgui.backend.impl.DecodedImage`, which uses it), never through `Class.getResourceAsStream`. A single leading `/` is stripped, so `"/textures/x.png"` and `"textures/x.png"` name the same resource. A missing resource is an `IOException` whose message includes the path. Public Javadoc for resource-path parameters states the module-path rule (see `global/commenting.md`).

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
