package com.ivan1pl.lorisui.imgui.backend;

/**
 * A texture returned by {@link com.ivan1pl.lorisui.imgui.renderer.RendererProvider#loadTexture(String)
 * RendererProvider.loadTexture}, ready to be drawn with
 * {@link com.ivan1pl.lorisui.imgui.renderer.RendererProvider#renderTexture RendererProvider.renderTexture}. The texture
 * belongs to the backend that loaded it and stays valid until {@link ImGuiBackend#unloadTextures} releases it.
 *
 * @param id     texture ID used to draw the texture: a backend-specific handle, such as an OpenGL or OpenGL ES texture
 *               name, an {@code SDL_GPUTexture*} pointer or a {@code VkDescriptorSet} handle
 * @param width  width of the texture in pixels
 * @param height height of the texture in pixels
 */
public record TextureData(long id, int width, int height) {
}
