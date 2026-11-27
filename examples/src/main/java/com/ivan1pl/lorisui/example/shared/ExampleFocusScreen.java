package com.ivan1pl.lorisui.example.shared;

import com.ivan1pl.lorisui.api.component.*;
import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.screen.LorisScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class ExampleFocusScreen extends LorisScreen {
    private static final long DELAY_NANOS = TimeUnit.SECONDS.toNanos(3);
    private static final TextInputStyle DISABLED_TEXT_INPUT_STYLE = TextInputStyle.builder()
            .backgroundColor(new Color(0.3f, 0.3f, 0.3f, 1f))
            .textColor(new Color(0.6f, 0.6f, 0.6f, 1f))
            .borderColor(new Color(0.5f, 0.5f, 0.5f, 1f))
            .borderThickness(1f)
            .rounding(5f)
            .build();
    private final TextInput textInput;
    private final NumberInput numberInput;
    private final TextInput modalInput;
    private final TextInput disabledTextInput;
    private final NumberInput disabledNumberInput;
    private final ComboBox<String> disabledComboBox;
    private final Button tab1Button;
    private final Button tab2Button;
    private final Button openModalButton;
    private final Button toggleButton;
    private final Button backButton;
    private final Button closeButton;
    private final Button focusNumberButton;
    private final Button focusNumberDelayedButton;
    private final Button focusModalButton;
    private final Button focusHiddenTabButton;
    private final List<FocusTarget> focusTargets;
    private final Panel panel;
    private final ModalPopup modalPopup;
    private String textInputContent = "Text input";
    private int numberInputContent = 1;
    private String modalInputContent = "Modal input";
    private String disabledTextInputContent = "Disabled input";
    private int disabledNumberInputContent = 2;
    private boolean modalOpen;
    private boolean widgetsEnabled;
    private boolean togglePending;
    private long toggleDeadline;
    private boolean numberRequestPending;
    private long numberRequestDeadline;

    public ExampleFocusScreen(Font font, Runnable backAction) {
        Objects.requireNonNull(backAction, "backAction");
        this.textInput = TextInput.builder()
                .width(180)
                .font(font)
                .style(ExampleScreen.TEXT_INPUT_STYLE)
                .maxLength(30)
                .value(() -> textInputContent, v -> textInputContent = v)
                .build();
        this.numberInput = NumberInput.builder()
                .width(180)
                .font(font)
                .style(ExampleScreen.TEXT_INPUT_STYLE)
                .minValue(0)
                .maxValue(99)
                .value(() -> numberInputContent, v -> numberInputContent = v)
                .build();
        this.modalInput = TextInput.builder()
                .width(270)
                .font(font)
                .style(ExampleScreen.TEXT_INPUT_STYLE)
                .maxLength(30)
                .value(() -> modalInputContent, v -> modalInputContent = v)
                .build();
        this.disabledTextInput = TextInput.builder()
                .width(180)
                .font(font)
                .style(ExampleScreen.TEXT_INPUT_STYLE)
                .disabledStyle(DISABLED_TEXT_INPUT_STYLE)
                .enabled(this::widgetsEnabled)
                .maxLength(30)
                .value(() -> disabledTextInputContent, v -> disabledTextInputContent = v)
                .build();
        // No disabled style, so the normal style is drawn dimmed
        this.disabledNumberInput = NumberInput.builder()
                .width(180)
                .font(font)
                .style(ExampleScreen.TEXT_INPUT_STYLE)
                .enabled(this::widgetsEnabled)
                .minValue(0)
                .maxValue(99)
                .value(() -> disabledNumberInputContent, v -> disabledNumberInputContent = v)
                .build();
        this.disabledComboBox = ComboBox.builder(String.class)
                .size(160, 30)
                .style(ExampleScreen.COMBO_BOX_STYLE)
                .popupStyle(ExampleScreen.COMBO_BOX_POPUP_STYLE)
                .itemStyle(ExampleScreen.COMBO_BOX_ITEM_STYLE)
                .enabled(this::widgetsEnabled)
                .items(Stream.of("item1", "item2", "item3")
                        .map(item -> ExampleScreen.createComboBoxItem(item, font))
                        .toList())
                .build();
        this.tab1Button = createTextButton("focus_tab1_button", 140, "global.button1", () -> {}, font);
        this.tab2Button = createTextButton("focus_tab2_button", 140, "global.button2", () -> {}, font);
        this.openModalButton = createTextButton("open_modal", 140, "global.openModal", () -> modalOpen = true,
                font);
        this.toggleButton = createTextButton("toggle_delayed", 140, "global.toggleDelayed", this::scheduleToggle,
                font);
        this.backButton = createTextButton("back", 90, "global.back", backAction, font);
        this.closeButton = createTextButton("close_modal", 90, "global.closeModal", () -> modalOpen = false, font);
        this.focusNumberButton = createTextButton("focus_number", 180, "global.focusNumber",
                () -> numberInput.setRequestsFocus(true), font);
        this.focusNumberDelayedButton = createTextButton("focus_number_delayed", 180, "global.focusNumberDelayed",
                this::scheduleNumberRequest, font);
        this.focusModalButton = createTextButton("focus_modal", 180, "global.focusModal", this::focusModal, font);
        this.focusHiddenTabButton = createTextButton("focus_hidden_tab", 180, "global.focusHiddenTab",
                () -> tab2Button.setRequestsFocus(true), font);
        this.focusTargets = List.of(
                new FocusTarget("text input", textInput),
                new FocusTarget("number input", numberInput),
                new FocusTarget("modal input", modalInput),
                new FocusTarget("disabled text input", disabledTextInput),
                new FocusTarget("disabled number input", disabledNumberInput),
                new FocusTarget("disabled combo box", disabledComboBox),
                new FocusTarget("button 1", tab1Button),
                new FocusTarget("button 2", tab2Button),
                new FocusTarget("open modal", openModalButton),
                new FocusTarget("toggle", toggleButton),
                new FocusTarget("back", backButton),
                new FocusTarget("close", closeButton),
                new FocusTarget("focus number", focusNumberButton),
                new FocusTarget("focus number delayed", focusNumberDelayedButton),
                new FocusTarget("focus modal", focusModalButton),
                new FocusTarget("focus hidden tab", focusHiddenTabButton));
        this.panel = Panel.panelBuilder("focus_panel")
                .size(600, 370)
                .position((w, _) -> w / 2 - 300, (_, h) -> h / 2 - 185)
                .layout(Layout.VERTICAL)
                .padding(15)
                .backgroundImage("/textures/textures.png", ExampleScreen.IMAGE_MODE_PANEL_BG)
                // Two inputs directly in one panel need distinct ImGui IDs to be focused and edited separately
                .addContent(Panel.panelBuilder("focus_inputs")
                        .size(570, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(textInput)
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(numberInput)
                        .build())
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                .addContent(Panel.panelBuilder("focus_disabled")
                        .size(570, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(disabledTextInput)
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(disabledNumberInput)
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(disabledComboBox)
                        .build())
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                .addContent(Panel.panelBuilder("focus_buttons")
                        .size(570, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(openModalButton)
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(toggleButton)
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(backButton)
                        .build())
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                .addContent(Panel.panelBuilder("focus_requests_number")
                        .size(570, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(focusNumberButton)
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(focusNumberDelayedButton)
                        .build())
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                .addContent(Panel.panelBuilder("focus_requests_other")
                        .size(570, 30)
                        .layout(Layout.HORIZONTAL)
                        .addContent(focusModalButton)
                        .addContent(Spacer.builder()
                                .width(10)
                                .build())
                        .addContent(focusHiddenTabButton)
                        .build())
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                .addContent(TabbedPanel.builder()
                        .size(570, 90)
                        .tabBarHeight(30)
                        .addTab(createTabLabel("global.tab1", font),
                                Background.image("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_LEFT),
                                Background.image("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_LEFT_H),
                                Background.image("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_LEFT_A),
                                Background.image("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_LEFT_S),
                                createTabContent("focus_tab1_content", tab1Button))
                        .addTab(createTabLabel("global.tab2", font),
                                Background.image("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_RIGHT),
                                Background.image("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_RIGHT_H),
                                Background.image("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_RIGHT_A),
                                Background.image("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_RIGHT_S),
                                createTabContent("focus_tab2_content", tab2Button))
                        .build())
                .addContent(Spacer.builder()
                        .height(10)
                        .build())
                .addContent(Text.builder()
                        .width(570)
                        .font(font)
                        .localizedText("global.focused")
                        .append(" ")
                        .append(this::focusedNames)
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
                .addContent(modalInput)
                .addContent(Spacer.builder()
                        .height(5)
                        .build())
                .addContent(closeButton)
                .build();
    }

    @Override
    public void onStartScreen() {
        textInput.setRequestsFocus(true);
    }

    @Override
    public void onEndScreen() {
    }

    @Override
    protected List<Component> layout() {
        return List.of(panel, modalPopup);
    }

    private void scheduleToggle() {
        togglePending = true;
        toggleDeadline = System.nanoTime() + DELAY_NANOS;
    }

    private void scheduleNumberRequest() {
        numberRequestPending = true;
        numberRequestDeadline = System.nanoTime() + DELAY_NANOS;
    }

    private void focusModal() {
        modalOpen = true;
        modalInput.setRequestsFocus(true);
    }

    // The renderers evaluate this supplier every frame, so the delayed toggle needs no thread or timer. A test-bed
    // shortcut: suppliers should only read state, so a real screen drives such changes from callbacks or its own logic
    private boolean widgetsEnabled() {
        if (togglePending && hasPassed(toggleDeadline)) {
            togglePending = false;
            widgetsEnabled = !widgetsEnabled;
        }
        return widgetsEnabled;
    }

    // The status text evaluates this supplier every frame, so the delayed request needs no thread or timer. A test-bed
    // shortcut, like widgetsEnabled(): a real screen requests the focus from callbacks or its own logic
    private String focusedNames() {
        if (numberRequestPending && hasPassed(numberRequestDeadline)) {
            numberRequestPending = false;
            numberInput.setRequestsFocus(true);
        }
        List<String> names = new ArrayList<>();
        for (FocusTarget target : focusTargets) {
            if (target.focusable().isFocused()) {
                names.add(target.name());
            }
        }
        return String.join(", ", names);
    }

    private static boolean hasPassed(long deadline) {
        return System.nanoTime() - deadline >= 0;
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

    private static BiFunction<Integer, Integer, Component> createTabLabel(String textKey, Font font) {
        return (tabWidth, _) -> Text.builder()
                .width(tabWidth)
                .align(Text.Align.CENTER)
                .font(font)
                .localizedText(textKey)
                .build();
    }

    private static Panel createTabContent(String id, Button button) {
        return Panel.panelBuilder(id)
                .size(570, 60)
                .backgroundImage("/textures/textures.png", ExampleScreen.IMAGE_MODE_TAB_CONTENT)
                .padding(15)
                .addContent(button)
                .build();
    }

    private record FocusTarget(String name, Focusable focusable) {
    }
}
