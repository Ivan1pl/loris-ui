/**
 * Hosts LorisUI screens in a jMonkeyEngine 3 application.
 * <p>
 * {@link com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay} is a jME {@link com.jme3.post.SceneProcessor} that draws the
 * current {@link com.ivan1pl.lorisui.api.screen.LorisScreen} with the Dear ImGui backend on top of a viewport,
 * usually the GUI viewport. It requires jME's LWJGL3 renderer. A typical setup, done on the jME application thread
 * (for example in {@code SimpleApplication.simpleInitApp()}):
 * <ol>
 *     <li>create the display;</li>
 *     <li>register fonts, custom renderers and style appliers, and resource bundles;</li>
 *     <li>add the screens and select the current one;</li>
 *     <li>add the display to a viewport with {@link com.jme3.renderer.ViewPort#addProcessor}.</li>
 * </ol>
 * Remove the display from its viewport to stop displaying UI and release its resources, and add it again to show the
 * UI again. Removing it from a component callback or a screen hook is allowed: the release completes when the current
 * frame or screen switch has finished. Only one display may be attached at a time. jME does not remove the display on
 * application exit; see {@link com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay} for the lifecycle and the exit pattern.
 * <p>
 * On JDK 25, imgui-java and LWJGL load native code, so run the application with {@code --enable-native-access}
 * ({@code ALL-UNNAMED} on the class path, the names of those modules on the module path) to avoid restricted-method
 * warnings. A failed Dear ImGui assertion terminates the JVM; see
 * {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer}.
 */
package com.ivan1pl.lorisui.jme3;
