/**
 * Renders LorisUI screens with Dear ImGui on OpenGL.
 *
 * <p>The module provides the {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend backend}, which turns the
 * components of a {@link com.ivan1pl.lorisui.api.screen.LorisScreen LorisScreen} into Dear ImGui widgets, and the
 * {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer frame driver} that a host (a game engine integration) calls each
 * frame. Fonts for this backend come from {@link com.ivan1pl.lorisui.imgui.font.ImGuiFont}.
 *
 * <p><b>Extension SPI.</b> Each component type is drawn by a
 * {@link com.ivan1pl.lorisui.imgui.renderer.Renderer Renderer} and each style type is applied by a
 * {@link com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier StyleApplier}. The backend looks them up by the
 * component's or style's class, walking up the superclass chain, and throws {@link IllegalStateException} when nothing
 * matches. Custom components or styles need a subclass of
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend ImGuiBackend} that overrides
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#getRenderer(com.ivan1pl.lorisui.api.component.Component)
 * getRenderer} and
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#getStyleApplier(com.ivan1pl.lorisui.api.component.style.Style)
 * getStyleApplier} to return your own implementations for your types, delegating to {@code super} for the built-in
 * ones, and that implements
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#loadTexture(String, com.ivan1pl.lorisui.api.core.Color,
 * com.ivan1pl.lorisui.api.core.Color, com.ivan1pl.lorisui.api.core.Color) loadTexture} for its graphics API.
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend ImGuiOpenGlBackend} is final and cannot be extended.
 * A custom backend is passed to {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer ImGuiOpenGlRenderer} by the
 * host; the bundled jMonkeyEngine host ({@code ImGuiJmeDisplay}) always uses {@code ImGuiOpenGlBackend} and does not
 * yet accept a custom backend. Renderers draw textures through
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#renderTexture(int, com.ivan1pl.lorisui.api.component.ImageMode,
 * int, int, int, int) renderTexture}.
 *
 * <p><b>Runtime requirement.</b> The module needs imgui-java 1.92.x, version 1.92.7.1 or later within 1.92 (the
 * {@code imgui.binding} and {@code imgui.lwjgl3} modules), at runtime. The library declares it as a runtime
 * dependency, so applications that depend on the library through Gradle or Maven get it without declaring it
 * themselves. imgui-java is not part of the library's compile-time API: code that calls Dear ImGui directly, such as a
 * custom {@link com.ivan1pl.lorisui.imgui.renderer.Renderer Renderer}, must add imgui-java to its own build and, on
 * the module path, {@code requires imgui.binding}. The module reads {@code lorisui.api} transitively, so the API types
 * are available to every module that requires this one.
 */
module lorisui.imgui {
    exports com.ivan1pl.lorisui.imgui;
    exports com.ivan1pl.lorisui.imgui.backend;
    exports com.ivan1pl.lorisui.imgui.font;
    exports com.ivan1pl.lorisui.imgui.renderer;
    exports com.ivan1pl.lorisui.imgui.renderer.style;
    requires transitive lorisui.api;
    requires imgui.binding;
    requires imgui.lwjgl3;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
}
