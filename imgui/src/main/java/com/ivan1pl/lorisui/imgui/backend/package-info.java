/**
 * The Dear ImGui implementation of the LorisUI {@link com.ivan1pl.lorisui.api.backend.Backend Backend} port.
 *
 * <p>{@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend} draws components with the renderers and style appliers it
 * looks up by class, and offers texture loading and drawing to them.
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend} is the OpenGL implementation used by hosts, and
 * {@link com.ivan1pl.lorisui.imgui.backend.TextureData} describes a loaded texture. Custom components and styles need
 * a subclass of {@code ImGuiBackend} that adds their renderers or style appliers and implements texture loading;
 * {@code ImGuiOpenGlBackend} is final, and the bundled jMonkeyEngine host does not yet accept a custom backend (see
 * {@code ImGuiBackend}).
 */
package com.ivan1pl.lorisui.imgui.backend;
