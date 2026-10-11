/**
 * Drives Dear ImGui frames for a host and provides fonts for the Dear ImGui backend.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer} is the entry point for host integrations: it owns a Dear
 * ImGui context for one window, forwards window events to it, renders one
 * {@link com.ivan1pl.lorisui.api.screen.LorisScreen LorisScreen} per frame with OpenGL, and reports whether the UI
 * wants mouse or keyboard input so the host can keep that input away from the game. A renderer is constructed without
 * touching native state, initialized once the window's OpenGL context exists, and disposed before that context goes
 * away; after disposal it can be initialized again.
 *
 * <p>Fonts are registered with the renderer, which returns an {@link com.ivan1pl.lorisui.imgui.ImGuiFont} for each
 * one. A font stays valid across disposal and re-initialization of its renderer and can be used only while that
 * renderer is initialized.
 */
package com.ivan1pl.lorisui.imgui;
