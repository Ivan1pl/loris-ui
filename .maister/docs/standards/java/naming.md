# Java Naming

### Type Naming Roles
Types are PascalCase with role suffixes: component styles `<Component>[Part]Style`, renderers `<Component>Renderer`, style appliers `<Style>Applier` (`ScrollbarStyle` → `ScrollbarStyleApplier`). Toolkit/engine-specific top-level classes are prefixed `ImGui` (`ImGuiBackend`, `ImGuiFont`, `ImGuiJmeDisplay`, `ImGuiLwjglDisplay`). Acronyms are camel-cased in type names (`OpenGl`, `Jme`, `Gl3`, `Lwjgl`), which Checkstyle checks for type declarations (`ImGuiLwjglDisplay`, not `ImGuiLWJGLDisplay`); member names are not checked.

### Full-Word Generic Type Parameters
Name generic type parameters with descriptive words ending in `Type` (`ComponentType`, `StyleType`, `ItemType`, `ResultType` in `IntBiFunction<ResultType>`, `ElementType` in the internal `ArrayStack<ElementType>`) rather than single letters. Checkstyle enforces this for class, interface, record and method type parameters (`^[A-Z][a-zA-Z0-9]*Type$`), so `<T>` or `<R>` fails the build.

```java
public abstract class Renderer<ComponentType extends Component> { ... }
<StyleType extends Style> StyleApplier<? super StyleType> getStyleApplier(StyleType style);
```
