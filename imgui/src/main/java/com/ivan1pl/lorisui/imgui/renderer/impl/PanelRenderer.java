package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.*;
import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.backend.TextureData;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;
import imgui.ImGui;
import imgui.flag.*;
import imgui.type.ImBoolean;

import java.io.IOException;
import java.util.NoSuchElementException;

public class PanelRenderer extends Renderer<Panel> {
    private static final int FLAGS_WINDOW =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoBackground |
            ImGuiWindowFlags.NoBringToFrontOnFocus;
    private static final int FLAGS_POPUP =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoBackground;
    private static final int FLAGS_CHILD =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoBackground;
    private static final int FLAGS_CHILD_WITH_BACKGROUND =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize;
    private static final ImBoolean TRUE = new ImBoolean(true);

    private final StyleStack styleStack = new StyleStack();
    private final ImBoolean modalPopupOpen = new ImBoolean();
    private final ImGuiBackend backend;

    public PanelRenderer(ImGuiBackend backend) {
        this.backend = backend;
    }

    @Override
    public void render(Panel component, int w, int h, RendererProvider rendererProvider,
                       BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
        if (!component.isOpen() && !topLevel) {
            return;
        }
        int startDepth = styleStack.depth();
        for (Style style : component.getStyles()) {
            styleStack.pushStyle(style, rendererProvider);
        }
        try {
            String id = component.getId() + "_panel";
            if (component instanceof ModalPopup || component instanceof Popup) {
                if (component instanceof ModalPopup modalPopup && modalPopup.isOpen() && !ImGui.isPopupOpen(id)) {
                    ImGui.openPopup(id, ImGuiPopupFlags.MouseButtonLeft);
                } else if (component instanceof Popup popup && popup.isShouldOpen() && !ImGui.isPopupOpen(id)) {
                    ImGui.openPopup(id, ImGuiPopupFlags.MouseButtonLeft);
                    popup.setShouldOpen(false);
                }
                ImGui.setNextWindowPos(component.getX(w, h), component.getY(w, h), ImGuiCond.Always, 0f, 0f);
                ImGui.setNextWindowSize(component.getWidth(w, h), component.getHeight(w, h), ImGuiCond.Always);
                boolean open;
                int flags = enableInput ? FLAGS_POPUP : (FLAGS_POPUP | ImGuiWindowFlags.NoInputs);
                if (component instanceof ModalPopup) {
                    modalPopupOpen.set(component.isOpen());
                    open = ImGui.beginPopupModal(id, modalPopupOpen, flags);
                } else {
                    open = ImGui.beginPopup(id, flags);
                    ((Popup) component).getOpenSetter().accept(open);
                }
                if (!open) {
                    return;
                }/* else {
                    imgui.getCurrentWindow().setPos(new Vec2(component.getX(), component.getY()));
                }*/
            } else if (topLevel) {
                int flags = enableInput ? FLAGS_WINDOW : (FLAGS_WINDOW | ImGuiWindowFlags.NoInputs);
                if (component.isOpen()) {
                    ImGui.setNextWindowPos(component.getX(w, h), component.getY(w, h), ImGuiCond.Always, 0f, 0f);
                    ImGui.setNextWindowSize(component.getWidth(w, h), component.getHeight(w, h), ImGuiCond.Always);
                    ImGui.begin(id, TRUE, flags);
                } else {
                    ImGui.setNextWindowPos(Integer.MAX_VALUE, Integer.MAX_VALUE, ImGuiCond.Always, 0f, 0f);
                    ImGui.setNextWindowSize(1, 1, ImGuiCond.Always);
                    ImGui.begin(id, TRUE, flags);
                    ImGui.end();
                    return;
                }
            } else {
                int flags = enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs);
                ImGui.beginChild(id, component.getWidth(w, h), component.getHeight(w, h), false, flags);
            }
            Panel tooltip = component.getTooltip();
            if (ImGui.isWindowHovered(ImGuiHoveredFlags.ChildWindows)) {
                if (topLevel && component.isScrollable()) {
                    ImGuiState.hoveringScrollableWindow = true;
                }
                if (tooltip != null) {
                    ImGui.pushStyleColor(ImGuiCol.PopupBg, 0, 0, 0, 0);
                    ImGui.pushStyleColor(ImGuiCol.Border, 0, 0, 0, 0);
                    ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);

                    ImGui.beginTooltip();
                    render(tooltip, w, h, rendererProvider, bundleKeyResolver, false, enableInput);
                    ImGui.endTooltip();

                    ImGui.popStyleVar(1);
                    ImGui.popStyleColor(2);
                }
            }
            float posX = ImGui.getCursorPosX();
            float posY = ImGui.getCursorPosY();
            Background background;
            if (component instanceof Button button) {
                ImGui.dummy(component.getWidth(w, h), component.getHeight(w, h));
                ImGui.setCursorPos(posX, posY);
                boolean hovered = false;
                boolean active = false;
                if (ImGui.isWindowHovered(ImGuiHoveredFlags.ChildWindows)) {
                    hovered = true;
                    if (ImGui.isKeyDown(ImGuiKey.MouseLeft)) {
                        active = true;
                    } else if (ImGui.isKeyReleased(ImGuiKey.MouseLeft)) {
                        //TODO check if this is the same button that received down
                        button.click();
                    }
                }
                background = button.getBackground(hovered, active);
            } else {
                background = component.getBackground();
            }
            if (background != null) {
                switch (background) {
                    case Background.MaskedImage maskedImage -> {
                        int flags = enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs);
                        TextureData texture = loadTexture(backend, maskedImage.getImage(),
                                maskedImage.getRedColor(), maskedImage.getGreenColor(), maskedImage.getBlueColor());
                        ImageMode imageMode = maskedImage.getImageMode();
                        ImGui.beginChild(id + "_bg", component.getWidth(w, h), component.getHeight(w, h), false,
                                flags);
                        backend.renderTexture(texture.id(), imageMode, component.getWidth(w, h),
                                component.getHeight(w, h), texture.width(), texture.height());
                        ImGui.endChild();
                    }
                    case Background.Image image -> {
                        int flags = enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs);
                        TextureData texture = loadTexture(backend, image.getImage());
                        ImageMode imageMode = image.getImageMode();
                        ImGui.beginChild(id + "_bg", component.getWidth(w, h), component.getHeight(w, h), false,
                                flags);
                        backend.renderTexture(texture.id(), imageMode, component.getWidth(w, h),
                                component.getHeight(w, h), texture.width(), texture.height());
                        ImGui.endChild();
                    }
                    case Background.Color color -> {
                        int flags = enableInput ?
                                FLAGS_CHILD_WITH_BACKGROUND : (FLAGS_CHILD_WITH_BACKGROUND | ImGuiWindowFlags.NoInputs);
                        Color c = color.getColor();
                        ImGui.pushStyleColor(ImGuiCol.ChildBg, c.getR(), c.getG(), c.getB(), c.getA());
                        ImGui.beginChild(id + "_bg", component.getWidth(w, h), component.getHeight(w, h), false,
                                flags);
                        ImGui.dummy(component.getWidth(w, h), component.getHeight(w, h));
                        ImGui.popStyleColor(1);
                        ImGui.endChild();
                    }
                }
                ImGui.setCursorPos(posX, posY);
            }
            if (!component.getContent().isEmpty()) {
                Component first = component.getContent().getFirst().apply(w, h);
                rendererProvider.getRenderer(first).render(
                        first, w, h, rendererProvider, bundleKeyResolver, false, enableInput);
                float offsetX = ImGui.getItemRectSizeX();
                for (int i = 1; i < component.getContent().size(); ++i) {
                    if (component.getLayout() == Layout.HORIZONTAL) {
                        ImGui.sameLine(offsetX, 0);
                    }
                    Component c = component.getContent().get(i).apply(w, h);
                    rendererProvider.getRenderer(c).render(
                            c, w, h, rendererProvider, bundleKeyResolver, false, enableInput);
                    offsetX += ImGui.getItemRectSize().x;
                }
            }
            ImGui.dummy(0f, 0f);
            if (component instanceof ModalPopup) {
                ImGui.endPopup();
            } else if (topLevel) {
                ImGui.end();
            } else {
                ImGui.endChild();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            styleStack.popStyles(startDepth);
        }
    }

    private static class StyleStack {
        private final ArrayStack<StyleApplier<?>> styleAppliers = new ArrayStack<>();
        private final ArrayStack<StyleApplier.Styles> styles = new ArrayStack<>();

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void pushStyle(Style style, RendererProvider rendererProvider) {
            StyleApplier applier = rendererProvider.getStyleApplier(style);
            StyleApplier.Styles styles = applier.applyStyles(style, rendererProvider);

            styleAppliers.push(applier);
            this.styles.push(styles);
        }

        private void popStyles(int startDepth) {
            while (styleAppliers.size() > startDepth) {
                styleAppliers.pop().popStyles(styles.pop());
            }
        }

        private int depth() {
            return styleAppliers.size();
        }
    }

    private static class ArrayStack<Type> {
        private static final int INITIAL_SIZE = 256;
        private Object[] data = new Object[INITIAL_SIZE];
        private int size = 0;

        public void push(Type object) {
            Object[] data = this.data;
            if (size == data.length) {
                Object[] newData = new Object[data.length << 1];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            this.data = data;
            data[size++] = object;
        }

        @SuppressWarnings("unchecked")
        public Type pop() {
            if (size == 0) {
                throw new NoSuchElementException();
            }
            Type element = (Type) data[--size];
            data[size] = null;
            return element;
        }

        public int size() {
            return size;
        }
    }
}
