package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.ComboBoxItemStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxPopupStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

class ComboBoxTest {
    @Test
    void shouldShowSelectedItemLabelOrEmptySelectionLabel() {
        Panel emptyLabel = Panel.panelBuilder("empty").size(10, 10).build();
        Panel firstLabel = Panel.panelBuilder("first").size(10, 10).build();
        Panel secondLabel = Panel.panelBuilder("second").size(10, 10).build();
        ComboBox<String> comboBox = ComboBox.builder(String.class)
                .items(new ComboBox.Item<>("first", firstLabel), new ComboBox.Item<>("second", secondLabel))
                .emptySelectionLabel(emptyLabel)
                .build();

        Assertions.assertSame(emptyLabel, comboBox.getSelectedItemLabel());
        comboBox.setSelectedItem("second");
        Assertions.assertSame(secondLabel, comboBox.getSelectedItemLabel());
        comboBox.setSelectedItem("missing");
        Assertions.assertSame(emptyLabel, comboBox.getSelectedItemLabel());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectNullItems() {
        ComboBox.Builder<String> builder = ComboBox.builder(String.class);

        Assertions.assertAll(
                () -> assertNullPointer("items", () -> builder.items((List<ComboBox.Item<String>>) null)),
                () -> assertNullPointer("items", () -> builder.items((ComboBox.Item<String>[]) null)),
                () -> assertNullPointer("emptySelectionLabel", () -> builder.emptySelectionLabel(null)),
                () -> assertNullPointer("style", () -> builder.style((ComboBoxStyle) null)),
                () -> assertNullPointer("popupStyle", () -> builder.popupStyle((ComboBoxPopupStyle) null)),
                () -> assertNullPointer("itemStyle", () -> builder.itemStyle((ComboBoxItemStyle) null)));
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
