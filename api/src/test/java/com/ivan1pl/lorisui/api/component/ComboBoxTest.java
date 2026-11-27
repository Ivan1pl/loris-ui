package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.ComboBoxItemStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxPopupStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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

    @Test
    void shouldUseDefaultEmptySelectionLabelIdWithoutHash() {
        ComboBox<String> comboBox = ComboBox.builder(String.class).build();

        Assertions.assertFalse(comboBox.getEmptySelectionLabel().getId().contains("#"));
    }

    @Test
    void shouldDeriveZeroSizeFromEmptySelectionLabelAndKeepExplicitSize() {
        Panel label = Panel.panelBuilder("label").size(40, 12).build();
        ComboBox<String> derived = ComboBox.builder(String.class)
                .emptySelectionLabel(label)
                .size(0, 0)
                .build();
        ComboBox<String> explicit = ComboBox.builder(String.class)
                .emptySelectionLabel(label)
                .size(100, 20)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(12, derived.getHeight()),
                () -> Assertions.assertEquals(40 + 12, derived.getWidth()),
                () -> Assertions.assertEquals(20, explicit.getHeight()),
                () -> Assertions.assertEquals(100, explicit.getWidth()));
    }

    @Test
    void shouldEvaluateEnabledOnEachCallAndRejectNullSupplier() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        ComboBox<String> byDefault = ComboBox.builder(String.class).build();
        ComboBox<String> constant = ComboBox.builder(String.class)
                .enabled(false)
                .build();
        ComboBox<String> supplied = ComboBox.builder(String.class)
                .enabled(enabled::get)
                .build();
        boolean suppliedBefore = supplied.isEnabled();
        enabled.set(false);

        Assertions.assertAll(
                () -> Assertions.assertTrue(byDefault.isEnabled()),
                () -> Assertions.assertFalse(constant.isEnabled()),
                () -> Assertions.assertTrue(suppliedBefore),
                () -> Assertions.assertFalse(supplied.isEnabled()),
                () -> assertNullPointer("enabled",
                        () -> ComboBox.builder(String.class).enabled((Supplier<Boolean>) null)));
    }

    @Test
    void shouldKeepDisabledStyleAndAcceptNull() {
        ComboBoxStyle disabledStyle = ComboBoxStyle.builder().build();
        ComboBox<String> byDefault = ComboBox.builder(String.class).build();
        ComboBox<String> styled = ComboBox.builder(String.class)
                .disabledStyle(disabledStyle)
                .build();
        ComboBox<String> unset = ComboBox.builder(String.class)
                .disabledStyle(disabledStyle)
                .disabledStyle(null)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertNull(byDefault.getDisabledStyle()),
                () -> Assertions.assertSame(disabledStyle, styled.getDisabledStyle()),
                () -> Assertions.assertNull(unset.getDisabledStyle()));
    }

    @Test
    void shouldDelegateFocusStateToItsHolder() {
        Focusable comboBox = ComboBox.builder(String.class).build();

        comboBox.setFocused(true);
        long before = System.nanoTime();
        comboBox.setRequestsFocus(true);
        long after = System.nanoTime();
        long stamp = comboBox.getFocusRequestTime();
        boolean requested = comboBox.requestsFocus();
        comboBox.setRequestsFocus(false);

        Assertions.assertAll(
                () -> Assertions.assertTrue(comboBox.isFocused()),
                () -> Assertions.assertTrue(requested),
                () -> Assertions.assertTrue(stamp - before >= 0 && after - stamp >= 0),
                () -> Assertions.assertFalse(comboBox.requestsFocus()),
                () -> Assertions.assertEquals(stamp, comboBox.getFocusRequestTime()));
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
