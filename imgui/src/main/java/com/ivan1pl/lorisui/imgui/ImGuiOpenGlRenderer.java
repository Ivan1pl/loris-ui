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
        // A renderer exception can leave windows and style stacks open when render() closes the frame. Error recovery
        // unwinds them, but with its assert enabled imgui-java's default assert callback calls System.exit(1) and the
        // original exception is lost. The recovery tooltip is disabled too, as it offers to re-enable the assert.
        ImGui.getIO().setConfigErrorRecoveryEnableAssert(false);
        ImGui.getIO().setConfigErrorRecoveryEnableTooltip(false);
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

    /**
     * Renders one ImGui frame containing the given screen. Must be called on the render (GL) thread. The frame is
     * always closed and its draw data submitted, even if the screen throws. Unbalanced ImGui state left by a throwing
     * renderer (open windows, pushed styles) is recovered when the frame closes, and the original exception (for
     * example the {@link IllegalStateException} reporting a failed {@link LorisScreen} layout) then propagates.
     *
     * @param screen            screen to render; must not be null
     * @param w                 display width in pixels
     * @param h                 display height in pixels
     * @param bundleKeyResolver resolver for localised text; must not be null
     */
    public void render(LorisScreen screen, int w, int h, BundleKeyResolver bundleKeyResolver) {
        implGl3.newFrame();
        implSdl3.newFrame();
        ImGui.getIO().setDisplaySize(w, h);
        ImGui.newFrame();

        ImGuiState.hoveringScrollableWindow = false;
        try {
            screen.render(backend, w, h, bundleKeyResolver);
        } finally {
            ImGui.render();
            implGl3.renderDrawData(ImGui.getDrawData());
        }
    }
}
