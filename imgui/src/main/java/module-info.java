module lorisui.imgui.main {
    exports com.ivan1pl.lorisui.imgui;
    exports com.ivan1pl.lorisui.imgui.backend;
    exports com.ivan1pl.lorisui.imgui.font;
    exports com.ivan1pl.lorisui.imgui.renderer;
    exports com.ivan1pl.lorisui.imgui.renderer.style;
    requires lorisui.api.main;
    requires imgui.binding;
    requires imgui.lwjgl3;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
}
