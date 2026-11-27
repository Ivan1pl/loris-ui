package com.ivan1pl.lorisui.example.shared;

import com.ivan1pl.lorisui.api.component.*;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.screen.LorisScreen;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ExampleScrollScreen extends LorisScreen {
    private static final int LINE_COUNT = 30;
    private final Panel scrollPanel;
    private final ModalPopup modalPopup;
    private final Panel panel;
    private boolean modalOpen;

    public ExampleScrollScreen(Font font, Runnable backAction) {
        Objects.requireNonNull(backAction, "backAction");
        Panel.Builder scrollPanelBuilder = Panel.panelBuilder("scroll_content")
                .size(570, 280)
                .backgroundImage("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_CONTENT)
                .padding(10)
                .paddingRight(0)
                .addStyle(ExampleScreen.SCROLLBAR_STYLE);
        for (int line = 1; line <= LINE_COUNT; line++) {
            scrollPanelBuilder.addContent(Text.builder()
                    .width(560)
                    .font(font)
                    .text("Line " + line)
                    .build());
        }
        this.scrollPanel = scrollPanelBuilder.build();
        // Unpadded, so it shows that the background stays in place while the content scrolls
        ModalPopup.Builder modalPopupBuilder = ModalPopup.popupBuilder("scroll_modal")
                .size(300, 200)
                .position((w, _) -> w / 2 - 150, (_, h) -> h / 2 - 100)
                .layout(Layout.VERTICAL)
                .backgroundImage("/textures/textures.png", ExampleScreen.IMAGE_MODE_PANEL_BG)
                .addStyle(ExampleScreen.SCROLLBAR_STYLE)
                .open(() -> modalOpen);
        for (int line = 1; line <= LINE_COUNT; line++) {
            modalPopupBuilder.addContent(Text.builder()
                    .width(285)
                    .font(font)
                    .text("Modal line " + line)
                    .build());
        }
        this.modalPopup = modalPopupBuilder
                .addContent(ExampleFocusScreen.createTextButton("scroll_modal_close", 90, "global.closeModal",
                        () -> modalOpen = false, font))
                .build();
        this.panel = Panel.panelBuilder("scroll_panel")
                .size(600, 400)
                .position((w, _) -> w / 2 - 300, (_, h) -> h / 2 - 200)
                .layout(Layout.VERTICAL)
                .padding(15)
                .backgroundImage("/textures/textures.png", ExampleScreen.IMAGE_MODE_PANEL_BG)
                .addContent(Panel.panelBuilder("scroll_buttons")
                        .size(570, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(ExampleFocusScreen.createTextButton("scroll_top", 90, "global.scrollTop",
                                () -> scrollPanel.setScrollPosition(0f), font))
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(ExampleFocusScreen.createTextButton("scroll_middle", 90, "global.scrollMiddle",
                                () -> scrollPanel.setScrollPosition(0.5f), font))
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(ExampleFocusScreen.createTextButton("scroll_bottom", 90, "global.scrollBottom",
                                () -> scrollPanel.setScrollPosition(1f), font))
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(ExampleFocusScreen.createTextButton("scroll_bottom_delayed", 140,
                                "global.scrollBottomDelayed", this::scheduleBottomRequest, font))
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(ExampleFocusScreen.createTextButton("scroll_back", 90, "global.back", backAction,
                                font))
                        .build())
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                .addContent(scrollPanel)
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                // Below the scroll panel, so its position is the one reported in this frame. The modal is drawn after
                // this panel, so its position lags one frame
                .addContent(Panel.panelBuilder("scroll_status")
                        .size(570, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(Text.builder()
                                .width(470)
                                .font(font)
                                .localizedText("global.scrollPosition")
                                .append(" ")
                                .append(() -> formatPosition(scrollPanel))
                                .append("   ")
                                .appendLocalized("global.scrollModalPosition")
                                .append(" ")
                                .append(() -> formatPosition(modalPopup))
                                .build())
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(ExampleFocusScreen.createTextButton("scroll_modal_open", 90, "global.scrollModal",
                                this::openModal, font))
                        .build())
                .build();
    }

    @Override
    public void onStartScreen() {
        scrollPanel.setScrollPosition(1f);
    }

    @Override
    public void onEndScreen() {
    }

    @Override
    protected List<Component> layout() {
        return List.of(panel, modalPopup);
    }

    private void openModal() {
        modalOpen = true;
        modalPopup.setScrollPosition(1f);
    }

    private static String formatPosition(Panel panel) {
        return String.format(Locale.ROOT, "%.2f", panel.getScrollPosition());
    }

    // Sent from a timer thread rather than the UI thread, to exercise the any-thread contract of the request
    private void scheduleBottomRequest() {
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> scrollPanel.setScrollPosition(1f));
    }
}
