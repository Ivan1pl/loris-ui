/**
 * The style SPI of the Dear ImGui backend.
 *
 * <p>A {@link com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier StyleApplier} applies one kind of
 * {@link com.ivan1pl.lorisui.api.component.style.Style Style} to the Dear ImGui widgets a renderer draws next, and
 * restores the previous style afterwards. Implement a style applier for a custom style, and return it from an
 * {@link com.ivan1pl.lorisui.imgui.backend.ImGuiBackend ImGuiBackend} subclass.
 */
package com.ivan1pl.lorisui.imgui.renderer.style;
