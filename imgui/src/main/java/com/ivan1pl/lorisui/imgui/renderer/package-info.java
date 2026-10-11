/**
 * The renderer SPI of the Dear ImGui backend.
 *
 * <p>A {@link com.ivan1pl.lorisui.imgui.renderer.Renderer Renderer} draws one kind of component with Dear ImGui, and a
 * {@link com.ivan1pl.lorisui.imgui.renderer.RendererProvider RendererProvider} finds the renderer for a component and
 * the style applier for a style. Implement a renderer to draw a custom component, and return it from an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend ImGuiBackend} subclass.
 *
 * <p>imgui-java is a runtime dependency of this module, not part of its compile-time API: code that calls Dear ImGui
 * directly, such as a custom renderer, must add imgui-java to its own build and, on the module path,
 * {@code requires imgui.binding}.
 */
package com.ivan1pl.lorisui.imgui.renderer;
