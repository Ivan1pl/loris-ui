package com.ivan1pl.lorisui.imgui.impl;

public final class ImGuiState {
    public static volatile boolean hoveringScrollableWindow = false;
    // Pointer of the Dear ImGui context the renderer last made current, so font checks need no native call
    public static volatile long currentContextPtr = 0L;

    private ImGuiState() {}
}
