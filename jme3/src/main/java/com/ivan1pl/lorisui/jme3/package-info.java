/**
 * Hosts LorisUI screens in a jMonkeyEngine 3 application.
 * <p>
 * {@link com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay} is a jME {@link com.jme3.post.SceneProcessor} that draws the
 * current {@link com.ivan1pl.lorisui.api.screen.LorisScreen} with the Dear ImGui backend on top of a viewport,
 * usually the GUI viewport. It requires jME's LWJGL3 renderer. A typical setup, done on the jME application thread
 * (for example in {@code SimpleApplication.simpleInitApp()}):
 * <ol>
 *     <li>create the display;</li>
 *     <li>register fonts and resource bundles;</li>
 *     <li>add the screens and select the current one;</li>
 *     <li>add the display to a viewport with {@link com.jme3.renderer.ViewPort#addProcessor}.</li>
 * </ol>
 * Remove the display from its viewport to stop displaying UI. jME does not do this on application exit; see
 * {@link com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay} for the lifecycle.
 */
package com.ivan1pl.lorisui.jme3;
