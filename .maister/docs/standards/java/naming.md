# Java Naming

### Type Naming Roles
Types are PascalCase with role suffixes: component styles `<Component>[Part]Style`, renderers `<Component>Renderer`, style appliers `<Style>Applier` (`ScrollbarStyle` → `ScrollbarStyleApplier`). Toolkit/engine-specific top-level classes are prefixed `ImGui` (`ImGuiBackend`, `ImGuiSdlGpuBackend`, `ImGuiOpenGlEsBackend`, `ImGuiFont`, `ImGuiJmeDisplay`, `ImGuiLwjglSdlGl3Display`, `ImGuiLwjglSdlVulkanDisplay`). Acronyms are camel-cased in type names (`OpenGl`, `OpenGlEs`, `Jme`, `Gl3`, `Gles3`, `Lwjgl`, `Sdl`, `Glfw`, `SdlGpu`, `SdlVulkan`), which Checkstyle checks for type declarations (`ImGuiLwjglSdlGl3Display`, not `ImGuiLWJGLSDLGL3Display`; `ImGuiSdlGpuRenderer`, not `ImGuiSDLGPURenderer`; `ImGuiSdlGles3Renderer`, not `ImGuiSDLGLES3Renderer`; `ImGuiOpenGlEsBackend`, not `ImGuiOpenGLESBackend`); member names are not checked. Classes for one platform/renderer pair name the platform before the graphics API (`ImGuiGlfwGl3Renderer`, `ImGuiSdlGles3Renderer`, `ImGuiLwjglSdlGpuDisplay`, `ImGuiLwjglSdlVulkanDisplay`, `LorisLwjglGlfwGl3Example`).

### Full-Word Generic Type Parameters
Name generic type parameters with descriptive words ending in `Type` (`ComponentType`, `StyleType`, `ItemType`, `ResultType` in `IntBiFunction<ResultType>`, `ElementType` in the internal `ArrayStack<ElementType>`, `RendererType` in the package-private `ImGuiLwjglDisplayBase<RendererType extends ImGuiRenderer>`) rather than single letters. Checkstyle enforces this for class, interface, record and method type parameters (`^[A-Z][a-zA-Z0-9]*Type$`), so `<T>` or `<R>` fails the build.

```java
public abstract class Renderer<ComponentType extends Component> { ... }
<StyleType extends Style> StyleApplier<? super StyleType> getStyleApplier(StyleType style);
```
