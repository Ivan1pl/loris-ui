# Java Naming

### Type Naming Roles
Types are PascalCase with role suffixes: component styles `<Component>[Part]Style`, renderers `<Component>Renderer`, style appliers `<Style>Applier` (`ScrollbarStyle` → `ScrollbarStyleApplier`). Toolkit/engine-specific top-level classes are prefixed `ImGui` (`ImGuiBackend`, `ImGuiFont`, `ImGuiJmeDisplay`). Acronyms are camel-cased in type names (`OpenGl`, `Jme`, `Gl3`).

### Full-Word Generic Type Parameters
Name generic type parameters with descriptive words ending in `Type` (`ComponentType`, `StyleType`, `ItemType`) rather than single letters.

```java
public abstract class Renderer<ComponentType extends Component> { ... }
<StyleType extends Style> StyleApplier<? super StyleType> getStyleApplier(StyleType style);
```
