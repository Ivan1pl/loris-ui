package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.gl3.ImGuiImplGl3;
import imgui.sdl3.ImGuiImplSdl3;

public class ImGuiOpenGlRenderer {
    private final long windowHandle;
    private final ImGuiBackend backend;
    private final ImGuiImplSdl3 implSdl3;
    private final ImGuiImplGl3 implGl3;

    public ImGuiOpenGlRenderer(ImGuiBackend backend, long windowHandle) {
        this.backend = backend;
        this.windowHandle = windowHandle;
        ImGui.createContext();
        ImGui.styleColorsDark();
        this.implSdl3 = new ImGuiImplSdl3();
        this.implGl3 = new ImGuiImplGl3();
    }

    public void initialize() {
        ImGui.getIO().setIniFilename(null);
        ImGui.getIO().setLogFilename(null);
        ImGui.getIO().setWantSaveIniSettings(false);
        ImGui.getIO().getFonts().build();
        this.implSdl3.init(windowHandle);
        this.implGl3.init();
        ImGui.getStyle().setWindowBorderSize(0f);
        ImGui.getStyle().setWindowPadding(0f, 0f);
        ImGui.getStyle().setAlpha(1f);
        ImGui.getStyle().setItemSpacing(0f, 0f);
        ImGui.getStyle().setColor(ImGuiCol.ModalWindowDimBg, 0f, 0f, 0f, 0.6f);
    }

    public void processSdlEvent(long eventPtr) {
        implSdl3.processEvent(eventPtr);
    }

    public void render(LorisScreen screen, int w, int h, BundleKeyResolver bundleKeyResolver) {
        implGl3.newFrame();
        implSdl3.newFrame();
        ImGui.getIO().setDisplaySize(w, h);
        ImGui.newFrame();

        ImGuiState.hoveringScrollableWindow = false;
        screen.render(backend, w, h, bundleKeyResolver);

        ImGui.render();
        implGl3.renderDrawData(ImGui.getDrawData());
    }
}
