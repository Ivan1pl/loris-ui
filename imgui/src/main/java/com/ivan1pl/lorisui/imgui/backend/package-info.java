/**
 * The Dear ImGui implementation of the LorisUI {@link com.ivan1pl.lorisui.api.backend.Backend Backend} port.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend} draws components with the renderers and style appliers it
 * looks up by class, and offers texture loading and drawing to them. Its four final implementations match the
 * graphics APIs of the renderer variants: {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend} for the OpenGL
 * renderers, {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlEsBackend} for the OpenGL ES renderer,
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiSdlGpuBackend} for the SDL_GPU renderer and
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiVulkanBackend} for the Vulkan renderer.
 * {@link com.ivan1pl.lorisui.imgui.backend.TextureData} describes a loaded texture. Custom components and styles are
 * drawn by registering their renderers and style appliers on the backend, which also replaces built-in ones (see
 * {@code ImGuiBackend}).
 *
 * <p>Each backend owns the textures it loads: it returns the same texture for the same image resource and masks, and
 * releases all of them when {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#unloadTextures() unloadTextures} is
 * called, which is not allowed while a frame is being drawn. A custom subclass of {@code ImGuiBackend}, which
 * implements creating and deleting textures, for example to load textures differently on OpenGL, pairs with the GL3
 * or GLES3 renderers, because its textures must be texture names of the renderer's OpenGL or OpenGL ES context.
 * Supporting another graphics API needs a new renderer variant inside {@code lorisui.imgui}, so it is not an
 * extension point.
 */
package com.ivan1pl.lorisui.imgui.backend;
