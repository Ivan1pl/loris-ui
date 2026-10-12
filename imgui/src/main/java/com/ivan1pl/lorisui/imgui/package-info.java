/**
 * Drives Dear ImGui frames for a host and provides fonts for the Dear ImGui backend.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.ImGuiRenderer} is the entry point for host integrations: it owns a Dear ImGui
 * context for one window, forwards window events to it, renders one
 * {@link com.ivan1pl.lorisui.api.screen.LorisScreen LorisScreen} per frame, and reports whether the UI wants mouse or
 * keyboard input so the host can keep that input away from the game. It is the common base of three variants, each
 * pairing a windowing library with a graphics API and used with the backend for that API:
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGl3Renderer} (SDL3 with OpenGL) and
 * {@link com.ivan1pl.lorisui.imgui.ImGuiGlfwGl3Renderer} (GLFW with OpenGL) with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend ImGuiOpenGlBackend}, and
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGpuRenderer} (SDL3 with SDL_GPU) with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiSdlGpuBackend ImGuiSdlGpuBackend}. A renderer is constructed without
 * touching native state, initialized once the window's graphics context exists, and disposed before that context goes
 * away; after disposal it can be initialized again.
 *
 * <p>Fonts are registered with the renderer, which returns an {@link com.ivan1pl.lorisui.imgui.ImGuiFont} for each
 * one. A font stays valid across disposal and re-initialization of its renderer and can be used only while that
 * renderer is initialized.
 */
package com.ivan1pl.lorisui.imgui;
