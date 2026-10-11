/**
 * Hosts LorisUI screens in an application's own LWJGL SDL3 window.
 * <p>
 * {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglDisplay} draws the current
 * {@link com.ivan1pl.lorisui.api.screen.LorisScreen} with the Dear ImGui backend into an SDL window whose OpenGL
 * context the application has created. A typical setup, done on the thread that owns the window and its context:
 * <ol>
 *     <li>create the display for the window;</li>
 *     <li>register fonts, custom renderers and style appliers, and resource bundles;</li>
 *     <li>add the screens and select the current one;</li>
 *     <li>call {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglDisplay#initialize()}.</li>
 * </ol>
 * Then, in the application's loop, forward every SDL event of the window to
 * {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglDisplay#processEvent(long)} and skip the application's own handling of
 * the events it consumes, and call {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglDisplay#render(int, int)} once per
 * frame before swapping the window. Close the display before destroying the OpenGL context or the window; a closed
 * display cannot be used again.
 */
package com.ivan1pl.lorisui.lwjgl;
