/**
 * Dear ImGui's OpenGL ES 3 renderer backend, ported to Java for the LorisUI Dear ImGui backend.
 *
 * <p>The module is an internal part of {@code lorisui.imgui}, which draws the OpenGL ES 3 variant with it: it exports
 * its package only to that module and has no API of its own. It is distributed under the MIT License, unlike the rest
 * of LorisUI; the license text is in {@code META-INF/LICENSE} of its jar.
 *
 * <p>LWJGL's OpenGL ES binding ({@code org.lwjgl.opengles}) is optional here, as it is in {@code lorisui.imgui}: the
 * module's classes are loaded only when the OpenGL ES 3 variant runs, and an application that runs it resolves the
 * binding itself.
 */
@SuppressWarnings("module")
module lorisui.imgui.gles3 {
    // The suppressed warnings: javac never finds the export's target, lorisui.imgui, because it depends on this
    // module, and the name ends in digits to match the artifact name
    exports com.ivan1pl.lorisui.imgui.gles3 to lorisui.imgui;
    requires imgui.binding;
    requires static org.lwjgl.opengles;
}
