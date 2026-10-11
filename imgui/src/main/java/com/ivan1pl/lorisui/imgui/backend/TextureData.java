package com.ivan1pl.lorisui.imgui.backend;

/**
 * A texture loaded by {@link ImGuiBackend#loadTexture}, ready to be drawn with {@link ImGuiBackend#renderTexture}.
 *
 * @param id     texture ID used to draw the texture
 * @param width  width of the texture in pixels
 * @param height height of the texture in pixels
 */
public record TextureData(int id, int width, int height) {
}
