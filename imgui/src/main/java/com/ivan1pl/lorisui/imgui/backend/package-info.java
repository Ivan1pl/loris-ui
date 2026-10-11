/**
 * The Dear ImGui implementation of the LorisUI {@link com.ivan1pl.lorisui.api.backend.Backend Backend} port.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend} draws components with the renderers and style appliers it
 * looks up by class, and offers texture loading and drawing to them.
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend} is the OpenGL implementation used by hosts, and
 * {@link com.ivan1pl.lorisui.imgui.backend.TextureData} describes a loaded texture. Custom components and styles are
 * drawn by registering their renderers and style appliers on the backend, which also replaces built-in ones (see
 * {@code ImGuiBackend}).
 *
 * <p>Each backend owns the textures it loads: it returns the same texture for the same image resource and masks, and
 * releases all of them when {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend#unloadTextures() unloadTextures} is
 * called, which is not allowed while a frame is being drawn. Only a backend for another graphics API subclasses
 * {@code ImGuiBackend}, to implement creating and deleting textures; {@code ImGuiOpenGlBackend} is final.
 */
package com.ivan1pl.lorisui.imgui.backend;
