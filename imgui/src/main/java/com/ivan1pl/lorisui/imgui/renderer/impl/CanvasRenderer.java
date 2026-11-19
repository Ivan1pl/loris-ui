package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Canvas;
import com.ivan1pl.lorisui.api.component.ImageMode;
import com.ivan1pl.lorisui.api.component.MouseButton;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.backend.TextureData;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;

public class CanvasRenderer extends Renderer<Canvas> {
    private static final int FLAGS_CHILD =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoScrollbar |
            ImGuiWindowFlags.NoBackground;
    private static final int FLAGS_CHILD_WITH_BACKGROUND =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoScrollbar;

    private final CanvasInputTracker inputTracker = new CanvasInputTracker();

    @Override
    public void render(Canvas component, int w, int h, RendererProvider rendererProvider,
                       BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
        Color background = component.getBackgroundColor();
        boolean interactive = enableInput && component.isEnabled();
        int flags = background != null ? FLAGS_CHILD_WITH_BACKGROUND : FLAGS_CHILD;
        // Without NoScrollWithMouse the child, which overflows by one pixel, takes the wheel instead of its parent
        if (!interactive) {
            flags |= ImGuiWindowFlags.NoScrollWithMouse;
        }
        if (!enableInput) {
            flags |= ImGuiWindowFlags.NoInputs;
        }
        int width = component.getWidth();
        int height = component.getHeight();
        if (background != null) {
            ImGui.pushStyleColor(ImGuiCol.ChildBg, background.getR(), background.getG(), background.getB(),
                    background.getA());
        }
        // Holding the scroll at 0 keeps the image in place while the child consumes the wheel
        ImGui.setNextWindowScroll(0, 0);
        ImGui.beginChild(component + "_canvas", width, height, false, flags);
        try {
            if (enableInput) {
                updateInput(component, interactive, width, height);
            }
            TextureData texture = rendererProvider.loadTexture(component);
            Canvas.Region region = component.getVisibleRegion();
            ImageMode imageMode = ImageMode.subImage(region.x(), region.y(), region.x() + region.width() - 1,
                    region.y() + region.height() - 1);
            rendererProvider.renderTexture(texture.id(), imageMode, width, height, texture.width(),
                    texture.height());
            // Nothing that can throw may sit between these two calls, or ending the child fails an assert
            ImGui.setCursorPos(width, height);
            ImGui.dummy(1, 1);
        } finally {
            ImGui.endChild();
            if (background != null) {
                ImGui.popStyleColor(1);
            }
        }
    }

    private void updateInput(Canvas component, boolean enabled, int width, int height) {
        float posX = ImGui.getCursorPosX();
        float posY = ImGui.getCursorPosY();
        ImGui.dummy(width, height);
        float minX = ImGui.getItemRectMinX();
        float minY = ImGui.getItemRectMinY();
        float maxX = ImGui.getItemRectMaxX();
        float maxY = ImGui.getItemRectMaxY();
        ImGui.setCursorPos(posX, posY);

        boolean hovered = ImGui.isWindowHovered(ImGuiHoveredFlags.ChildWindows);
        int id = ImGui.getID("##" + component);
        float mouseX = ImGui.getMousePosX();
        float mouseY = ImGui.getMousePosY();
        boolean mouseValid = ImGui.isMousePosValid();
        int clicked = mouseBit(ImGui.isMouseClicked(ImGuiMouseButton.Left), MouseButton.LEFT)
                | mouseBit(ImGui.isMouseClicked(ImGuiMouseButton.Right), MouseButton.RIGHT)
                | mouseBit(ImGui.isMouseClicked(ImGuiMouseButton.Middle), MouseButton.MIDDLE);
        int down = mouseBit(ImGui.isKeyDown(ImGuiKey.MouseLeft), MouseButton.LEFT)
                | mouseBit(ImGui.isKeyDown(ImGuiKey.MouseRight), MouseButton.RIGHT)
                | mouseBit(ImGui.isKeyDown(ImGuiKey.MouseMiddle), MouseButton.MIDDLE);
        int released = mouseBit(ImGui.isKeyReleased(ImGuiKey.MouseLeft), MouseButton.LEFT)
                | mouseBit(ImGui.isKeyReleased(ImGuiKey.MouseRight), MouseButton.RIGHT)
                | mouseBit(ImGui.isKeyReleased(ImGuiKey.MouseMiddle), MouseButton.MIDDLE);
        float wheel = ImGui.getIO().getMouseWheel();
        int frame = ImGui.getFrameCount();
        Canvas.Region region = component.getVisibleRegion();

        if (hovered) {
            // A hovered item keeps a left press off Dear ImGui's window-move path, which would make the window's move
            // ID active and capture the keyboard; the item itself is never made active
            imgui.internal.ImGui.itemHoverable(minX, minY, maxX, maxY, id, 0);
        }
        inputTracker.update(component, id, frame, enabled, hovered, mouseX, mouseY, mouseValid, clicked, down,
                released, wheel, minX, minY, region);
        if (enabled && hovered) {
            ImGuiState.hoveringScrollableWindow = true;
        }
    }

    private static int mouseBit(boolean set, MouseButton button) {
        return set ? CanvasInputTracker.bit(button) : 0;
    }
}
