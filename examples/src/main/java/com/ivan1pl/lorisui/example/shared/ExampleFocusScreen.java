package com.ivan1pl.lorisui.example.shared;

import com.ivan1pl.lorisui.api.component.*;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.screen.LorisScreen;

import java.util.List;
import java.util.Objects;

public class ExampleFocusScreen extends LorisScreen {
    private final Panel panel;
    private final ModalPopup modalPopup;
    private String textInputContent = "Text input";
    private int numberInputContent = 1;
    private String modalInputContent = "Modal input";
    private boolean modalOpen;

    public ExampleFocusScreen(Font font, Runnable backAction) {
        Objects.requireNonNull(backAction, "backAction");
        this.panel = Panel.panelBuilder("focus_panel")
                .size(400, 100)
                .position((w, _) -> w / 2 - 200, (_, h) -> h / 2 - 50)
                .layout(Layout.VERTICAL)
                .padding(15)
                .backgroundImage("/textures/textures.png", ExampleScreen.IMAGE_MODE_PANEL_BG)
                // Two inputs directly in one panel need distinct ImGui IDs to be focused and edited separately
                .addContent(Panel.panelBuilder("focus_inputs")
                        .size(370, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(TextInput.builder()
                                .width(180)
                                .font(font)
                                .style(ExampleScreen.TEXT_INPUT_STYLE)
                                .maxLength(30)
                                .value(() -> textInputContent, v -> textInputContent = v)
                                .build())
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(NumberInput.builder()
                                .width(180)
                                .font(font)
                                .style(ExampleScreen.TEXT_INPUT_STYLE)
                                .minValue(0)
                                .maxValue(99)
                                .value(() -> numberInputContent, v -> numberInputContent = v)
                                .build())
                        .build())
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                .addContent(Panel.panelBuilder("focus_buttons")
                        .size(370, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(createTextButton("open_modal", 140, "global.openModal", () -> modalOpen = true,
                                font))
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(createTextButton("back", 90, "global.back", backAction, font))
                        .build())
                .build();
        this.modalPopup = ModalPopup.popupBuilder("focus_modal")
                .size(300, 140)
                .position((w, _) -> w / 2 - 150, (_, h) -> h / 2 - 70)
                .layout(Layout.VERTICAL)
                .padding(15)
                .backgroundImage("/textures/textures.png", ExampleScreen.IMAGE_MODE_PANEL_BG)
                .open(() -> modalOpen)
                .addContent(Text.builder()
                        .width(270)
                        .font(font)
                        .localizedText("global.modal")
                        .build())
                .addContent(TextInput.builder()
                        .width(270)
                        .font(font)
                        .style(ExampleScreen.TEXT_INPUT_STYLE)
                        .maxLength(30)
                        .value(() -> modalInputContent, v -> modalInputContent = v)
                        .build())
                .addContent(Spacer.builder()
                        .height(5)
                        .build())
                .addContent(createTextButton("close_modal", 90, "global.closeModal", () -> modalOpen = false, font))
                .build();
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }

    @Override
    protected List<Component> layout() {
        return List.of(panel, modalPopup);
    }

    private static Button createTextButton(String id, int width, String textKey, Runnable action, Font font) {
        return Button.buttonBuilder(id)
                .size(width, 30)
                .action(action)
                .addContent(Text.builder()
                        .width(width)
                        .align(Text.Align.CENTER)
                        .font(font)
                        .localizedText(textKey)
                        .build())
                .build();
    }
}
