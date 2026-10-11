/**
 * Drives Dear ImGui frames for a host.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer} is the entry point for host integrations: it initializes
 * Dear ImGui for a window, forwards window events to it, renders one {@link com.ivan1pl.lorisui.api.screen.LorisScreen
 * LorisScreen} per frame with OpenGL, and reports whether the UI wants mouse or keyboard input so the host can keep
 * that input away from the game.
 */
package com.ivan1pl.lorisui.imgui;
