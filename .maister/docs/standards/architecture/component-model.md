# Component Model & Backend Extension

### Marker Interfaces and Abstract Base Classes
Toolkit-neutral abstractions in `api` are empty marker interfaces (`Component`, `Style`, `Font`), as is the opaque `StyleApplier.Styles` token in `imgui`; backends downcast to their concrete type (e.g. `(ImGuiFont) component.getFont()`).
Use abstract classes for shared behaviour / template methods (`Renderer`, `LorisScreen`, `ImGuiBackend`, `Panel.BaseBuilder`); use interfaces for stateless contracts (`Backend`, `RendererProvider`, `StyleApplier`, `BundleKeyResolver`).

### Toolkit-Neutral `Backend` Port
`Backend` declares only `render(int w, int h, List<Component> components, BundleKeyResolver bundleKeyResolver)`. Toolkit concerns stay in the backend module: textures are `ImGuiBackend` methods (`public abstract TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) throws IOException`, implemented by `ImGuiOpenGlBackend`, and the concrete `public void renderTexture(int id, ImageMode imageMode, int width, int height, int textureWidth, int textureHeight)`), with the `TextureData` record in `imgui.backend`. Input capture is exposed to hosts by `ImGuiOpenGlRenderer` (`wantsMouseInput()`, `wantsKeyboardInput()`, `isHoveringScrollablePanel()`), never by the API. Do not add toolkit-specific methods or types to `Backend`.

### Class-Keyed Renderer and StyleApplier Registry
Each `api` Component has exactly one `Renderer` and each `Style` exactly one `StyleApplier`, registered by class in the `ImGuiBackend` constructor. Lookup walks the superclass chain, so subclasses (Button, Popup, ModalPopup) reuse their parent's renderer unless registered explicitly. A missing registration fails fast with `IllegalStateException`.
- New component: class with Builder in `api/component` + `imgui/renderer/impl/<Name>Renderer` + one `renderers.put(...)` line.
- New style: class in `api/component/style` + `imgui/renderer/style/impl/<Name>Applier` + one `styleAppliers.put(...)` line.

```java
renderers.put(ProgressBar.class, new ProgressBarRenderer());
styleAppliers.put(TextInputStyle.class, new TextInputStyleApplier());
```

### Custom Components and Styles Outside the Library
Library users extend the backend by subclassing `ImGuiBackend` and overriding `getRenderer` / `getStyleApplier`: return their own `Renderer` / `StyleApplier` for their types and delegate to `super` for everything else, which keeps the built-in registry, the superclass-chain lookup and the `IllegalStateException` for unmatched types. There is no registration API.
- The subclass also implements `loadTexture` for its graphics API; `ImGuiOpenGlBackend` is final and can't be extended.
- The host passes the custom backend to `ImGuiOpenGlRenderer`. The jME host (`ImGuiJmeDisplay`) always uses `ImGuiOpenGlBackend` and does not accept a custom backend yet.
- imgui-java is only a runtime dependency of `loris-ui-imgui`, so code with a custom `Renderer` adds imgui-java to its own build and, on the module path, `requires imgui.binding`.

```java
@SuppressWarnings("unchecked")
@Override
public <ComponentType extends Component> Renderer<? super ComponentType> getRenderer(ComponentType component) {
    if (component instanceof Gauge) {
        return (Renderer<? super ComponentType>) gaugeRenderer;
    }
    return super.getRenderer(component);
}
```
