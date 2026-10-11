package com.ivan1pl.lorisui.imgui.backend;

/**
 * A texture returned by {@link com.ivan1pl.lorisui.imgui.renderer.RendererProvider#loadTexture(String)
 * RendererProvider.loadTexture}, ready to be drawn with
 * {@link com.ivan1pl.lorisui.imgui.renderer.RendererProvider#renderTexture RendererProvider.renderTexture}. The texture
 * belongs to the backend that loaded it and stays valid until {@link ImGuiBackend#unloadTextures} releases it.
 *
 * @param id     texture ID used to draw the texture
 * @param width  width of the texture in pixels
 * @param height height of the texture in pixels
 */
public record TextureData(int id, int width, int height) {
}
