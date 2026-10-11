package com.ivan1pl.lorisui.example.shared;

import com.ivan1pl.lorisui.api.component.*;
import com.ivan1pl.lorisui.api.component.style.*;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.screen.LorisScreen;

import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ExampleScreen extends LorisScreen {
    private static final ImageMode IMAGE_MODE_PANEL_BG =
            ImageMode.spriteResize(40, 40, 1, 5, 30, 5, 5, 5, 30, 5, 30, 5, 30, 5, 5);
    private static final ImageMode IMAGE_MODE_BUTTON =
            ImageMode.spriteResize(40, 40, 2, 5, 30, 5, 5, 5, 30, 5, 30, 5, 30, 5, 5);
    private static final ImageMode IMAGE_MODE_BUTTON_H =
            ImageMode.spriteResize(40, 40, 3, 5, 30, 5, 5, 5, 30, 5, 30, 5, 30, 5, 5);
    private static final ImageMode IMAGE_MODE_BUTTON_A =
            ImageMode.spriteResize(40, 40, 4, 5, 30, 5, 5, 5, 30, 5, 30, 5, 30, 5, 5);
    private static final ImageMode IMAGE_MODE_BUTTON_D =
            ImageMode.spriteResize(40, 40, 5, 5, 30, 5, 5, 5, 30, 5, 30, 5, 30, 5, 5);
    private static final ImageMode IMAGE_MODE_TAB_LEFT =
            ImageMode.spriteResize(40, 40, 6, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_LEFT_H =
            ImageMode.spriteResize(40, 40, 7, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_LEFT_A =
            ImageMode.spriteResize(40, 40, 8, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_LEFT_S =
            ImageMode.spriteResize(40, 40, 9, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_MID =
            ImageMode.spriteResize(40, 40, 10, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_MID_H =
            ImageMode.spriteResize(40, 40, 11, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_MID_A =
            ImageMode.spriteResize(40, 40, 12, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_MID_S =
            ImageMode.spriteResize(40, 40, 13, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_RIGHT =
            ImageMode.spriteResize(40, 40, 14, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_RIGHT_H =
            ImageMode.spriteResize(40, 40, 15, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_RIGHT_A =
            ImageMode.spriteResize(40, 40, 16, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_RIGHT_S =
            ImageMode.spriteResize(40, 40, 17, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final ImageMode IMAGE_MODE_TAB_CONTENT =
            ImageMode.spriteResize(40, 40, 18, 19, 2, 19, 20, 19, 2, 19, 15, 19, 2, 19, 5);
    private static final TextInputStyle TEXT_INPUT_STYLE = TextInputStyle.builder()
            .backgroundColor(Color.BLUE)
            .borderColor(Color.YELLOW)
            .borderThickness(1f)
            .rounding(5f)
            .build();
    private static final ComboBoxStyle COMBO_BOX_STYLE = ComboBoxStyle.builder()
            .backgroundColor(Color.BLUE)
            .backgroundColorHovered(new Color(0.2f, 0.2f, 1f, 1f))
            .borderColor(Color.YELLOW)
            .arrowButtonColor(new Color(0f, 0f, 0.5f, 1f))
            .arrowButtonColorHovered(new Color(0.2f, 0.2f, 0.7f, 1f))
            .arrowColor(Color.WHITE)
            .borderThickness(1f)
            .rounding(5f)
            .build();
    private static final ComboBoxItemStyle COMBO_BOX_ITEM_STYLE = ComboBoxItemStyle.builder()
            .hoveredColor(new Color(0.2f, 0.2f, 1f, 1f))
            .activeColor(new Color(0.4f, 0.4f, 1f, 1f))
            .selectedColor(new Color(0.6f, 0.6f, 1f, 1f))
            .build();
    private static final ComboBoxPopupStyle COMBO_BOX_POPUP_STYLE = ComboBoxPopupStyle.builder()
            .backgroundColor(Color.BLUE)
            .borderThickness(1f)
            .rounding(5f)
            .build();
    private static final ScrollbarStyle SCROLLBAR_STYLE = ScrollbarStyle.builder()
            .backgroundColor(Color.BLACK.withA(0.2f))
            .grabColor(Color.BLUE)
            .hoveredGrabColor(new Color(0.2f, 0.2f, 1f, 1f))
            .activeGrabColor(new Color(0.4f, 0.4f, 1f, 1f))
            .padding(2f)
            .rounding(10f)
            .size(15f)
            .build();
    private final ComboBox<String> comboBox;
    private final Panel panel;
    private float progress = 0f;
    private String textInputContent = "Text input";
    private int numberInputContent = 1;

    public ExampleScreen(Font font, Runnable hideUiAction) {
        Objects.requireNonNull(hideUiAction, "hideUiAction");
        List<ComboBox.Item<String>> comboBoxItems = Stream.of("item1", "item2", "item3", "item4")
                .map(item -> createComboBoxItem(item, font))
                .toList();
        this.comboBox = ComboBox.builder(String.class)
                .size(160, 30)
                .style(COMBO_BOX_STYLE)
                .popupStyle(COMBO_BOX_POPUP_STYLE)
                .itemStyle(COMBO_BOX_ITEM_STYLE)
                .items(comboBoxItems)
                .build();
        this.panel = Panel.panelBuilder("example_panel")
                .size(600, 400)
                .position((w, _) -> w / 2 - 300, (_, h) -> h / 2 - 200)
                .layout(Layout.VERTICAL)
                .backgroundImage("/textures/textures.png", IMAGE_MODE_PANEL_BG)
                .addStyle(SCROLLBAR_STYLE)
                .addContent(Spacer.builder()
                        .height(15)
                        .build())
                .addContent(Panel.panelBuilder("example_pannel_inner")
                        .size(600, 370)
                        .layout(Layout.HORIZONTAL)
                        .addContent(Spacer.builder()
                                .width(15)
                                .build())
                        .addContent(Panel.panelBuilder("example_pannel_inner_inner")
                                .size(585, 370)
                                .layout(Layout.VERTICAL)
                                .addContent(Panel.panelBuilder("row1")
                                        .size(570, 30)
                                        .layout(Layout.HORIZONTAL)
                                        .addContent(Text.builder()
                                                .width(200)
                                                .font(font)
                                                .localizedText("global.label1")
                                                .build())
                                        .addContent(Button.buttonBuilder("progress_minus")
                                                .size(30, 30)
                                                .backgroundImage("/textures/textures.png", IMAGE_MODE_BUTTON)
                                                .hoveredBackgroundImage("/textures/textures.png", IMAGE_MODE_BUTTON_H)
                                                .activeBackgroundImage("/textures/textures.png", IMAGE_MODE_BUTTON_A)
                                                .disabledBackgroundImage("/textures/textures.png", IMAGE_MODE_BUTTON_D)
                                                .enabled(() -> progress > 0f)
                                                .action(() -> progress = Math.max(0f, progress - 0.1f))
                                                .addContent(Text.builder()
                                                        .width(30)
                                                        .align(Text.Align.CENTER)
                                                        .font(font)
                                                        .text("-")
                                                        .build())
                                                .build())
                                        .addContent(ProgressBar.builder()
                                                .size(100, 30)
                                                .backgroundColor(Color.BLUE)
                                                .barColor(new Color(.2f, .2f, 1f, 1f))
                                                .rounding(5f)
                                                .progress(() -> progress)
                                                .build())
                                        .addContent(Button.buttonBuilder("progress_plus")
                                                .size(30, 30)
                                                .backgroundImage("/textures/textures.png", IMAGE_MODE_BUTTON)
                                                .hoveredBackgroundImage("/textures/textures.png", IMAGE_MODE_BUTTON_H)
                                                .activeBackgroundImage("/textures/textures.png", IMAGE_MODE_BUTTON_A)
                                                .disabledBackgroundImage("/textures/textures.png", IMAGE_MODE_BUTTON_D)
                                                .enabled(() -> progress < 1f)
                                                .action(() -> progress = Math.min(1f, progress + 0.1f))
                                                .addContent(Text.builder()
                                                        .width(30)
                                                        .align(Text.Align.CENTER)
                                                        .font(font)
                                                        .text("+")
                                                        .build())
                                                .build())
                                        .addContent(Spacer.builder()
                                                .width(10)
                                                .build())
                                        .addContent(Text.builder()
                                                .width(100)
                                                .font(font)
                                                .color(Color.GREEN)
                                                .text(() -> NumberFormat.getPercentInstance().format(progress))
                                                .build())
                                        .addContent(Button.buttonBuilder("progress_reset")
                                                .size(90, 30)
                                                .action(() -> progress = 0f)
                                                .addContent(Text.builder()
                                                        .width(90)
                                                        .align(Text.Align.CENTER)
                                                        .font(font)
                                                        .localizedText("global.reset")
                                                        .build())
                                                .build())
                                        .build())
                                .addContent(Spacer.builder()
                                        .height(5)
                                        .build())
                                .addContent(Panel.panelBuilder("row2")
                                        .size(570, 30)
                                        .layout(Layout.HORIZONTAL)
                                        .addContent(Text.builder()
                                                .width(200)
                                                .font(font)
                                                .localizedText("global.label2")
                                                .build())
                                        .addContent(TextInput.builder()
                                                .width(160)
                                                .font(font)
                                                .style(TEXT_INPUT_STYLE)
                                                .maxLength(30)
                                                .value(() -> textInputContent, v -> textInputContent = v)
                                                .build())
                                        .addContent(Spacer.builder()
                                                .width(10)
                                                .build())
                                        .addContent(Text.builder()
                                                .width(100)
                                                .font(font)
                                                .color(Color.GREEN)
                                                .text(() -> textInputContent.length() + "/30")
                                                .build())
                                        .addContent(Button.buttonBuilder("hide_ui")
                                                .size(90, 30)
                                                .action(hideUiAction)
                                                .addContent(Text.builder()
                                                        .width(90)
                                                        .align(Text.Align.CENTER)
                                                        .font(font)
                                                        .localizedText("global.hideUi")
                                                        .build())
                                                .build())
                                        .build())
                                .addContent(Spacer.builder()
                                        .height(5)
                                        .build())
                                .addContent(Panel.panelBuilder("row3")
                                        .size(570, 30)
                                        .layout(Layout.HORIZONTAL)
                                        .addContent(Text.builder()
                                                .width(200)
                                                .font(font)
                                                .localizedText("global.label3")
                                                .build())
                                        .addContent(NumberInput.builder()
                                                .width(160)
                                                .font(font)
                                                .style(TEXT_INPUT_STYLE)
                                                .minValue(0)
                                                .maxValue(99)
                                                .value(() -> numberInputContent, v -> numberInputContent = v)
                                                .build())
                                        .addContent(Spacer.builder()
                                                .width(10)
                                                .build())
                                        .addContent(Text.builder()
                                                .width(100)
                                                .font(font)
                                                .color(Color.GREEN)
                                                .text(() -> numberInputContent + "/99")
                                                .build())
                                        .build())
                                .addContent(Spacer.builder()
                                        .height(5)
                                        .build())
                                .addContent(Panel.panelBuilder("row4")
                                        .size(570, 30)
                                        .layout(Layout.HORIZONTAL)
                                        .addContent(Text.builder()
                                                .width(200)
                                                .font(font)
                                                .localizedText("global.label4")
                                                .build())
                                        .addContent(Panel.panelBuilder("row4_combo")
                                                .size(160, 30)
                                                .addContent(comboBox)
                                                .build())
                                        .addContent(Spacer.builder()
                                                .width(10)
                                                .build())
                                        .addContent(Text.builder()
                                                .width(100)
                                                .font(font)
                                                .color(Color.GREEN)
                                                .text("")
                                                .append(resolver -> Optional.ofNullable(comboBox.getSelectedItem())
                                                        .map(item -> resolver.resolveResourceKey("global." + item))
                                                        .orElse("=========="))
                                                .build())
                                        .build())
                                .addContent(Spacer.builder()
                                        .height(5)
                                        .build())
                                .addContent(TabbedPanel.builder()
                                        .size(570, 230)
                                        .tabBarHeight(30)
                                        .addTab((_, _) -> Text.builder()
                                                .width(190)
                                                .align(Text.Align.CENTER)
                                                .font(font)
                                                .localizedText("global.tab1")
                                                .build(),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_LEFT),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_LEFT_H),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_LEFT_A),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_LEFT_S),
                                                Panel.panelBuilder("tab1_content")
                                                        .size(570, 200)
                                                        .backgroundImage("/textures/textures.png",
                                                                IMAGE_MODE_TAB_CONTENT)
                                                        .layout(Layout.HORIZONTAL)
                                                        .addContent(Spacer.builder()
                                                                .width(10)
                                                                .build())
                                                        .addContent(Panel.panelBuilder("tab1_content_inner")
                                                                .size(560, 190)
                                                                .addContent(Text.builder()
                                                                        .width(560)
                                                                        .font(font)
                                                                        .localizedText("global.tab1.content")
                                                                        .build())
                                                                .build())
                                                        .build())
                                        .addTab((_, _) -> Text.builder()
                                                        .width(190)
                                                        .align(Text.Align.CENTER)
                                                        .font(font)
                                                        .localizedText("global.tab2")
                                                        .build(),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_MID),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_MID_H),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_MID_A),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_MID_S),
                                                Panel.panelBuilder("tab2_content")
                                                        .size(570, 200)
                                                        .backgroundImage("/textures/textures.png",
                                                                IMAGE_MODE_TAB_CONTENT)
                                                        .layout(Layout.HORIZONTAL)
                                                        .addContent(Spacer.builder()
                                                                .width(10)
                                                                .build())
                                                        .addContent(Panel.panelBuilder("tab2_content_inner")
                                                                .size(560, 190)
                                                                .addContent(Text.builder()
                                                                        .width(560)
                                                                        .font(font)
                                                                        .localizedText("global.tab2.content")
                                                                        .build())
                                                                .build())
                                                        .build())
                                        .addTab((_, _) -> Text.builder()
                                                        .width(190)
                                                        .align(Text.Align.CENTER)
                                                        .font(font)
                                                        .localizedText("global.tab3")
                                                        .build(),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_RIGHT),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_RIGHT_H),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_RIGHT_A),
                                                Background.image("/textures/textures.png", IMAGE_MODE_TAB_RIGHT_S),
                                                Panel.panelBuilder("tab3_content")
                                                        .size(570, 200)
                                                        .backgroundImage("/textures/textures.png",
                                                                IMAGE_MODE_TAB_CONTENT)
                                                        .layout(Layout.HORIZONTAL)
                                                        .addContent(Spacer.builder()
                                                                .width(10)
                                                                .build())
                                                        .addContent(Panel.panelBuilder("tab3_content_inner")
                                                                .size(560, 190)
                                                                .addContent(Text.builder()
                                                                        .width(560)
                                                                        .font(font)
                                                                        .localizedText("global.tab3.content")
                                                                        .build())
                                                                .build())
                                                        .build())
                                        .build())
                                .build())
                        .build())
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
        return List.of(panel);
    }

    private static ComboBox.Item<String> createComboBoxItem(String label, Font font) {
        return new ComboBox.Item<>(label, Panel.panelBuilder("comboBoxItem_" + label)
                .size(130, 30)
                .layout(Layout.HORIZONTAL)
                .addContent(Spacer.builder()
                        .width(5)
                        .build())
                .addContent(Text.builder()
                        .width(120)
                        .font(font)
                        .localizedText("global." + label)
                        .color(Color.WHITE)
                        .build())
                .build());
    }
}
