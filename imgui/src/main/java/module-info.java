/**
 * Renders LorisUI screens with Dear ImGui on OpenGL 3, OpenGL ES 3, SDL_GPU or Vulkan.
 *
 * <p>The module provides the backends, which turn the components of a
 * {@link com.ivan1pl.lorisui.api.screen.LorisScreen LorisScreen} into Dear ImGui widgets, and the frame drivers that a
 * host (a game engine integration or an application's own SDL3 or GLFW loop) calls each frame. The frame driver is an
 * {@link com.ivan1pl.lorisui.imgui.ImGuiRenderer ImGuiRenderer}, whose five variants each pair a windowing library
 * with a graphics API and are used with one of the four backends, the one for that API:
 * <ul>
 *     <li>{@link com.ivan1pl.lorisui.imgui.ImGuiSdlGl3Renderer ImGuiSdlGl3Renderer} (SDL3 with OpenGL 3) and
 *     {@link com.ivan1pl.lorisui.imgui.ImGuiGlfwGl3Renderer ImGuiGlfwGl3Renderer} (GLFW with OpenGL 3), with an
 *     {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend ImGuiOpenGlBackend};</li>
 *     <li>{@link com.ivan1pl.lorisui.imgui.ImGuiSdlGles3Renderer ImGuiSdlGles3Renderer} (SDL3 with OpenGL ES 3, such
 *     as jMonkeyEngine's ANGLE renderer), with an
 *     {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlEsBackend ImGuiOpenGlEsBackend};</li>
 *     <li>{@link com.ivan1pl.lorisui.imgui.ImGuiSdlGpuRenderer ImGuiSdlGpuRenderer} (SDL3 with SDL_GPU), with an
 *     {@link com.ivan1pl.lorisui.imgui.backend.ImGuiSdlGpuBackend ImGuiSdlGpuBackend};</li>
 *     <li>{@link com.ivan1pl.lorisui.imgui.ImGuiSdlVulkanRenderer ImGuiSdlVulkanRenderer} (SDL3 with Vulkan 1.3), with
 *     an {@link com.ivan1pl.lorisui.imgui.backend.ImGuiVulkanBackend ImGuiVulkanBackend}.</li>
 * </ul>
 *
 * <p><b>Renderer lifecycle and fonts.</b> An {@code ImGuiRenderer} owns its Dear ImGui context. It is constructed
 * without touching native state, {@linkplain com.ivan1pl.lorisui.imgui.ImGuiRenderer#initialize() initialized} once
 * the window's graphics context is ready, and {@linkplain com.ivan1pl.lorisui.imgui.ImGuiRenderer#dispose() disposed}
 * before that context goes away, which also releases every texture the backend has loaded. A disposed renderer can be
 * initialized again. Fonts are registered with the renderer (or through a host display) before it is initialized;
 * each registration returns an {@link com.ivan1pl.lorisui.imgui.ImGuiFont ImGuiFont} that stays valid across disposal
 * and re-initialization.
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
 * {@link com.ivan1pl.lorisui.imgui.renderer.RendererProvider RendererProvider} they receive. All four bundled
 * backends are final. A custom {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend ImGuiBackend} subclass, which
 * implements its protected {@code createTexture} and {@code deleteTexture} methods, for example to load textures
 * differently on OpenGL, pairs with the GL3 or GLES3 renderers, because its textures must be texture names of the
 * renderer's OpenGL or OpenGL ES context. Supporting another graphics API needs a new renderer variant inside this
 * module, so it is not an extension point.
 *
 * <p><b>OpenGL ES vertex limit.</b> The OpenGL ES 3 renderer limits the number of vertices of a single Dear ImGui
 * window, as described for {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGles3Renderer ImGuiSdlGles3Renderer}.
 *
 * <p><b>Runtime requirement.</b> The module needs imgui-java 1.92.x, version 1.92.7.1 or later within 1.92 (the
 * {@code imgui.binding} and {@code imgui.lwjgl3} modules), at runtime. The library declares it as a runtime
 * dependency, so applications that depend on the library through Gradle or Maven get it without declaring it
 * themselves. imgui-java is not part of the library's compile-time API: code that calls Dear ImGui directly, such as a
 * custom {@link com.ivan1pl.lorisui.imgui.renderer.Renderer Renderer}, must add imgui-java to its own build and, on
 * the module path, {@code requires imgui.binding}. On JDK 25, imgui-java and LWJGL load native code, so run the
 * application with {@code --enable-native-access} ({@code ALL-UNNAMED} on the class path, the names of those modules
 * on the module path) to avoid restricted-method warnings. The module also requires LWJGL's SDL3 binding
 * ({@code org.lwjgl.sdl}), which imgui-java supplies, so on the module path it must be present at runtime for every
 * host, even one that never uses SDL, such as a jME or GLFW host. The module reads {@code lorisui.api} transitively,
 * so the API types are available to every module that requires this one.
 *
 * <p><b>Ported renderer modules.</b> The OpenGL ES 3 and Vulkan variants draw with Java ports of Dear ImGui's renderer
 * backends, which live in two internal modules, {@code lorisui.imgui.gles3} and {@code lorisui.imgui.vulkan} (the
 * {@code loris-ui-imgui-gles3} and {@code loris-ui-imgui-vulkan} artifacts), distributed under the MIT License. They
 * export nothing to applications. The library declares them as runtime dependencies, and this module requires them,
 * so on the module path both must be present at runtime for every host, even one that uses neither variant.
 *
 * <p><b>OpenGL ES and Vulkan bindings.</b> LWJGL's OpenGL ES binding ({@code org.lwjgl.opengles}) and Vulkan binding
 * ({@code org.lwjgl.vulkan}) are optional: each is needed, at compile time and at runtime, only by the variant and
 * backend that use it, so applications that use only the OpenGL or SDL_GPU variants need neither. The application
 * supplies them; jMonkeyEngine brings the OpenGL ES binding for its ANGLE renderer. This module and the ported
 * renderer modules read them only optionally and neither pass them on nor add them to the module graph, so on the
 * module path an application that uses one of these variants makes sure that its binding is resolved at runtime: a
 * modular application that uses the Vulkan variant declares {@code requires org.lwjgl.vulkan}, and a modular
 * jMonkeyEngine application that runs with jMonkeyEngine's default ANGLE renderer declares
 * {@code requires org.lwjgl.opengles} or is started with {@code --add-modules org.lwjgl.opengles}, as jMonkeyEngine
 * itself needs under that renderer. Neither needs to require the ported renderer modules. At compile time an
 * application needs a binding only if its own code uses that binding's types.
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
    requires lorisui.imgui.gles3;
    requires lorisui.imgui.vulkan;
    requires org.lwjgl.opengl;
    requires static org.lwjgl.opengles;
    requires org.lwjgl.sdl;
    requires org.lwjgl.stb;
    requires static org.lwjgl.vulkan;
}
