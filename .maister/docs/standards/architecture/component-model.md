# Component Model & Backend Extension

### Marker Interfaces and Abstract Base Classes
Toolkit-neutral abstractions in `api` are empty marker interfaces (`Component`, `Style`, `Font`, `StyleApplier.Styles`); backends downcast to their concrete type (e.g. `(ImGuiFont) component.getFont()`).
Use abstract classes for shared behaviour / template methods (`Renderer`, `LorisScreen`, `ImGuiBackend`, `Panel.BaseBuilder`); use interfaces for stateless contracts (`Backend`, `RendererProvider`, `StyleApplier`, `BundleKeyResolver`).

### Class-Keyed Renderer and StyleApplier Registry
Each `api` Component has exactly one `Renderer` and each `Style` exactly one `StyleApplier`, registered by class in the `ImGuiBackend` constructor. Lookup walks the superclass chain, so subclasses (Button, Popup, ModalPopup) reuse their parent's renderer unless registered explicitly. A missing registration fails fast with `IllegalStateException`.
- New component: class with Builder in `api/component` + `imgui/renderer/impl/<Name>Renderer` + one `renderers.put(...)` line.
- New style: class in `api/component/style` + `imgui/renderer/style/impl/<Name>Applier` + one `styleAppliers.put(...)` line.

```java
renderers.put(ProgressBar.class, new ProgressBarRenderer());
styleAppliers.put(TextInputStyle.class, new TextInputStyleApplier());
```
