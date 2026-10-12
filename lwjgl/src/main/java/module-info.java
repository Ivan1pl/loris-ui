/**
 * Hosts LorisUI screens in an application's own LWJGL window and loop.
 *
 * <p>The module provides three embeddable displays that draw the current
 * {@link com.ivan1pl.lorisui.api.screen.LorisScreen LorisScreen} with the Dear ImGui backend of {@code lorisui.imgui}.
 * Choose the display that matches the application's window library and graphics API:
 * <table class="striped">
 *     <caption>Which display to use</caption>
 *     <thead>
 *         <tr>
 *             <th scope="col">Display</th>
 *             <th scope="col">Window and graphics</th>
 *             <th scope="col">Input and frame</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <th scope="row">{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGl3Display ImGuiLwjglSdlGl3Display}</th>
 *             <td>SDL3 window with an OpenGL context</td>
 *             <td>The application forwards the window's SDL events to the display; the display draws into the current
 *             OpenGL framebuffer.</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglGlfwGl3Display ImGuiLwjglGlfwGl3Display}</th>
 *             <td>GLFW window with an OpenGL context</td>
 *             <td>The display chains its GLFW input callbacks in front of the application's; the display draws into
 *             the current OpenGL framebuffer.</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGpuDisplay ImGuiLwjglSdlGpuDisplay}</th>
 *             <td>SDL3 window claimed for an SDL_GPU device</td>
 *             <td>The application forwards the window's SDL events to the display, and passes each frame's command
 *             buffer and target texture to it.</td>
 *         </tr>
 *     </tbody>
 * </table>
 * The application keeps ownership of the window, its OpenGL context or GPU device, and the main loop: it calls the
 * display once per frame after clearing and before presenting, and closes the display before it destroys the
 * context, the device or the window. The package documentation of {@code com.ivan1pl.lorisui.lwjgl} describes the
 * typical setup of each display.
 *
 * <p><b>sRGB output.</b> Images are uploaded as sRGB textures, so every display needs an sRGB framebuffer to show their
 * true colors. With the OpenGL displays, request an sRGB-capable default framebuffer when creating the window
 * ({@code SDL_GL_FRAMEBUFFER_SRGB_CAPABLE} or {@code GLFW_SRGB_CAPABLE}) and enable {@code GL_FRAMEBUFFER_SRGB}; with
 * the SDL_GPU display, set the window's swapchain to the {@code SDL_GPU_SWAPCHAINCOMPOSITION_SDR_LINEAR} composition
 * before initializing the display.
 *
 * <p><b>Runtime requirements.</b> The module needs imgui-java (through {@code lorisui.imgui}) and LWJGL with its SDL,
 * GLFW and OpenGL bindings and their native libraries at runtime. The library declares imgui-java as a runtime
 * dependency, which brings these LWJGL modules with it; an application that creates its window with LWJGL compiles
 * against LWJGL itself. The GLFW and SDL native libraries that come with imgui-java cover only the {@code linux},
 * {@code macos}, {@code macos-arm64} and {@code windows} platforms. On JDK 25, imgui-java and LWJGL load native code,
 * so run the application with {@code --enable-native-access} ({@code ALL-UNNAMED} on the class path, the names of those
 * modules on the module path) to avoid restricted-method warnings. On macOS, SDL and GLFW windows must be created and
 * their events processed on the process's main thread, so the JVM may need {@code -XstartOnFirstThread}. The module
 * reads {@code lorisui.imgui} transitively, so the backend and API types are available to every module that requires
 * this one.
 */
module lorisui.lwjgl {
    exports com.ivan1pl.lorisui.lwjgl;
    requires transitive lorisui.imgui;
    requires org.lwjgl.glfw;
    requires org.lwjgl.sdl;
}
