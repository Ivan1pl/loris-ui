/**
 * Renders LorisUI screens with Dear ImGui on OpenGL.
 *
 * <p>The module provides the {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend backend}, which turns the
 * components of a {@link com.ivan1pl.lorisui.api.screen.LorisScreen LorisScreen} into Dear ImGui widgets, and the
 * {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer frame driver} that a host (a game engine integration or an
 * application's own SDL3 loop) calls each frame.
 *
 * <p><b>Renderer lifecycle and fonts.</b> An {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer ImGuiOpenGlRenderer}
 * owns its Dear ImGui context. It is constructed without touching native state,
 * {@linkplain com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer#initialize() initialized} once the window's OpenGL context
 * exists, and {@linkplain com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer#dispose() disposed} before that context goes
 * away, which also releases every texture the backend has loaded. A disposed renderer can be initialized again. Fonts
 * are registered with the renderer (or through a host display) before it is initialized; each registration returns an
 * {@link com.ivan1pl.lorisui.imgui.ImGuiFont ImGuiFont} that stays valid across disposal and re-initialization.
 *
 * <p><b>Extension SPI.</b> Each component type is drawn by a
 * {@link com.ivan1pl.lorisui.imgui.renderer.Renderer Renderer} and each style type is applied by a
 * {@link com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier StyleApplier}. The backend looks them up by the
 * component's or style's class, walking up the superclass chain, and throws {@link IllegalStateException} when nothing
 * matches. Custom components and styles are supported by registering their renderers and style appliers with
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#registerRenderer(Class,
 * com.ivan1pl.lorisui.imgui.renderer.Renderer) registerRenderer} and
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#registerStyleApplier(Class,
 * com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier) registerStyleApplier}, on the backend or through a host
 * display; a registration also replaces a built-in one. Renderers load and draw textures through the
 * {@link com.ivan1pl.lorisui.imgui.renderer.RendererProvider RendererProvider} they receive. Supporting a graphics API
 * other than OpenGL means extending {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend ImGuiBackend} and
 * implementing its protected {@code createTexture} and {@code deleteTexture} methods;
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend ImGuiOpenGlBackend} is final.
 *
 * <p><b>Runtime requirement.</b> The module needs imgui-java 1.92.x, version 1.92.7.1 or later within 1.92 (the
 * {@code imgui.binding} and {@code imgui.lwjgl3} modules), at runtime. The library declares it as a runtime
 * dependency, so applications that depend on the library through Gradle or Maven get it without declaring it
 * themselves. imgui-java is not part of the library's compile-time API: code that calls Dear ImGui directly, such as a
 * custom {@link com.ivan1pl.lorisui.imgui.renderer.Renderer Renderer}, must add imgui-java to its own build and, on
 * the module path, {@code requires imgui.binding}. On JDK 25, imgui-java and LWJGL load native code, so run the
 * application with {@code --enable-native-access} ({@code ALL-UNNAMED} on the class path, the names of those modules
 * on the module path) to avoid restricted-method warnings. The module reads {@code lorisui.api} transitively, so the
 * API types are available to every module that requires this one.
 *
 * <p><b>Dear ImGui assertions.</b> A failed Dear ImGui assertion terminates the JVM: imgui-java reports it and then
 * calls {@code System.exit(1)}. The renderer disables the assertions of Dear ImGui's error recovery, so exceptions
 * thrown while a frame is drawn reach the host; other assertions still terminate the JVM. An application can install
 * its own handler with imgui-java's {@code ImGui.setAssertCallback}, for example to log the assertion, but cannot
 * prevent the termination.
 */
module lorisui.imgui {
    exports com.ivan1pl.lorisui.imgui;
    exports com.ivan1pl.lorisui.imgui.backend;
    exports com.ivan1pl.lorisui.imgui.renderer;
    exports com.ivan1pl.lorisui.imgui.renderer.style;
    requires transitive lorisui.api;
    requires imgui.binding;
    requires imgui.lwjgl3;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
}
