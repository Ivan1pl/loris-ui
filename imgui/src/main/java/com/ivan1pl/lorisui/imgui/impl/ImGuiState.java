package com.ivan1pl.lorisui.imgui.impl;

public final class ImGuiState {
    // Set by scrollable top-level panels and interactive canvases while hovered
    public static volatile boolean hoveringScrollableWindow = false;
    // Pointer of the Dear ImGui context the renderer last made current, so font checks need no native call
    public static volatile long currentContextPtr = 0L;
    // Set only while a frame is rendered with keyboard navigation enabled; null otherwise. The slot is shared by the
    // whole JVM, so it is valid only for the renderer whose frame is running, one frame at a time
    public static volatile KeyboardFocus keyboardFocus = null;

    private ImGuiState() {}
}
