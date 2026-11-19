/**
 * Drives Dear ImGui frames for a host and provides fonts for the Dear ImGui backend.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.ImGuiRenderer} is the entry point for host integrations: it owns a Dear ImGui
 * context for one window, forwards window events to it, renders one
 * {@link com.ivan1pl.lorisui.api.screen.LorisScreen LorisScreen} per frame, and reports whether the UI wants mouse or
 * keyboard input so the host can keep that input away from the game. It is the common base of five variants, each
 * pairing a windowing library with a graphics API and used with the backend for that API:
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGl3Renderer} (SDL3 with OpenGL) and
 * {@link com.ivan1pl.lorisui.imgui.ImGuiGlfwGl3Renderer} (GLFW with OpenGL) with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend ImGuiOpenGlBackend},
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGles3Renderer} (SDL3 with OpenGL ES 3) with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlEsBackend ImGuiOpenGlEsBackend},
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlGpuRenderer} (SDL3 with SDL_GPU) with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiSdlGpuBackend ImGuiSdlGpuBackend}, and
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlVulkanRenderer} (SDL3 with Vulkan) with an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiVulkanBackend ImGuiVulkanBackend}. A renderer is constructed without
 * touching native state, initialized once the window's graphics context exists, and disposed before that context goes
 * away; after disposal it can be initialized again.
 *
 * <p>The renderer also owns the UI's keyboard focus, which follows the
 * {@linkplain com.ivan1pl.lorisui.api.backend.Backend keyboard navigation} settings of its backend; they can be
 * changed through {@link com.ivan1pl.lorisui.imgui.ImGuiRenderer#setActivationKey ImGuiRenderer.setActivationKey}
 * and {@link com.ivan1pl.lorisui.imgui.ImGuiRenderer#setKeyboardNavigationEnabled
 * ImGuiRenderer.setKeyboardNavigationEnabled} at any time. The variants never pass Tab key events to Dear ImGui, so
 * Ctrl+Tab never moves or highlights panels; while navigation is enabled, a Tab press moves the keyboard focus
 * instead. {@link com.ivan1pl.lorisui.imgui.ImGuiRenderer#wantsKeyboardInput() wantsKeyboardInput} then also reports
 * keyboard input while an enabled button, an input or a combo box has the keyboard focus or a combo box is expanded.
 *
 * <p>Fonts are registered with the renderer, which returns an {@link com.ivan1pl.lorisui.imgui.ImGuiFont} for each
 * one. A font stays valid across disposal and re-initialization of its renderer and can be used only while that
 * renderer is initialized.
 */
package com.ivan1pl.lorisui.imgui;
