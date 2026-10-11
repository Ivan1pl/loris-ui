/**
 * Hosts LorisUI screens in an application's own LWJGL SDL3 window and OpenGL loop.
 *
 * <p>The module provides {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglDisplay ImGuiLwjglDisplay}, an embeddable display
 * that draws the current {@link com.ivan1pl.lorisui.api.screen.LorisScreen LorisScreen} with the Dear ImGui backend
 * of {@code lorisui.imgui}. The application keeps ownership of the SDL window, its OpenGL context and the main loop:
 * it forwards the window's events to the display, calls the display once per frame between clearing and swapping,
 * and closes the display before it destroys the context or the window.
 *
 * <p><b>Runtime requirements.</b> The module needs imgui-java (through {@code lorisui.imgui}) and LWJGL with its SDL
 * and OpenGL bindings and their native libraries at runtime. The library declares imgui-java as a runtime dependency,
 * which brings these LWJGL modules with it; an application that creates its window with LWJGL compiles against
 * LWJGL itself. On JDK 25, imgui-java and LWJGL load native code, so run the application with
 * {@code --enable-native-access} ({@code ALL-UNNAMED} on the class path, the names of those modules on the module
 * path) to avoid restricted-method warnings. On macOS, SDL windows must be created and their events processed on the
 * process's main thread, so the JVM may need {@code -XstartOnFirstThread}. The module reads {@code lorisui.imgui}
 * transitively, so the backend and API types are available to every module that requires this one.
 */
module lorisui.lwjgl {
    exports com.ivan1pl.lorisui.lwjgl;
    requires transitive lorisui.imgui;
    requires org.lwjgl.sdl;
}
