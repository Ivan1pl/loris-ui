/**
 * Hosts LorisUI screens in a jMonkeyEngine 3 application.
 * <p>
 * {@link com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay} is a jME {@link com.jme3.post.SceneProcessor} that draws the
 * current {@link com.ivan1pl.lorisui.api.screen.LorisScreen} with the Dear ImGui backend on top of a viewport,
 * usually the GUI viewport. It requires jME's LWJGL3 backend with its default {@code ANGLE_GLES3} renderer or any
 * {@code AppSettings.LWJGL_OPENGL*} renderer (jME itself accepts only the OpenGL 3.2 and later ones). A typical
 * setup, done on the jME application thread (for example in
 * {@code SimpleApplication.simpleInitApp()}):
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
 * The display supports the {@linkplain com.ivan1pl.lorisui.api.backend.Backend keyboard navigation} of the UI: Tab
 * and Shift+Tab move the keyboard focus, and the activation key and Escape act on the focused element. Choose the
 * activation key with {@link com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay#setActivationKey} and turn the navigation keys
 * off with {@link com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay#setKeyboardNavigationEnabled}, in any state of the
 * display; the focus reported and requested through {@link com.ivan1pl.lorisui.api.component.Focusable} keeps
 * working either way. Tab key events never reach Dear ImGui's own keyboard navigation, so Ctrl+Tab never moves or
 * highlights panels. While navigation is enabled, the display also consumes key events while an enabled element has
 * the keyboard focus or a combo box is expanded, as described for {@link com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay},
 * so a game key such as Escape reaches the application only once the focus is cleared.
 * <p>
 * <b>Runtime requirements.</b> The library does not bring jMonkeyEngine with it: the application declares
 * {@code jme3-core} and {@code jme3-lwjgl3}, and any other jME modules it uses, as its own dependencies. The library
 * brings the Dear ImGui backend and imgui-java at runtime; LWJGL and its SDL bindings come with jME's LWJGL3 backend.
 * The display needs jMonkeyEngine 3.10 or later, whose {@code jme3-lwjgl3} window is based on SDL3, running on
 * LWJGL 3.4.x, the LWJGL version the Dear ImGui backend uses at runtime. On JDK 25, imgui-java and LWJGL load
 * native code, so run the application with {@code --enable-native-access} ({@code ALL-UNNAMED} on the class path,
 * the names of those modules on the module path) to avoid restricted-method warnings. A failed Dear ImGui assertion
 * terminates the JVM; see {@link com.ivan1pl.lorisui.imgui.ImGuiRenderer}.
 */
package com.ivan1pl.lorisui.jme3;
