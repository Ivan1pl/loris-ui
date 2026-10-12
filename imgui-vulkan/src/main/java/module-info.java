/**
 * Dear ImGui's Vulkan renderer backend, ported to Java for the LorisUI Dear ImGui backend.
 *
 * <p>The module is an internal part of {@code lorisui.imgui}, which draws the Vulkan variant with it: it exports its
 * package only to that module and has no API of its own. It is distributed under the MIT License, unlike the rest of
 * LorisUI; the license text is in {@code META-INF/LICENSE} of its jar.
 *
 * <p>LWJGL's Vulkan binding ({@code org.lwjgl.vulkan}) is optional here, as it is in {@code lorisui.imgui}: the
 * module's classes are loaded only when the Vulkan variant runs, and an application that runs it resolves the
 * binding itself.
 */
@SuppressWarnings("module")
module lorisui.imgui.vulkan {
    // The suppressed warning: javac never finds the export's target, lorisui.imgui, because it depends on this module
    exports com.ivan1pl.lorisui.imgui.vulkan to lorisui.imgui;
    requires imgui.binding;
    requires lorisui.api;
    requires org.lwjgl;
    requires static org.lwjgl.vulkan;
}
