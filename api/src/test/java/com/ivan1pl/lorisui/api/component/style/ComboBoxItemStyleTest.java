package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class ComboBoxItemStyleTest {
    @Test
    void shouldTreatNullAsUnset() {
        ComboBoxItemStyle unset = ComboBoxItemStyle.builder().build();
        ComboBoxItemStyle.Builder builder = ComboBoxItemStyle.builder();
        ComboBoxItemStyle nullConstants = ComboBoxItemStyle.builder()
                .selectedColor((Color) null)
                .hoveredColor((Color) null)
                .activeColor((Color) null)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertNull(unset.getSelectedColor()),
                () -> Assertions.assertNull(unset.getHoveredColor()),
                () -> Assertions.assertNull(unset.getActiveColor()),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.selectedColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.hoveredColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.activeColor((Supplier<Color>) null)),
                () -> Assertions.assertNull(nullConstants.getSelectedColor()),
                () -> Assertions.assertNull(nullConstants.getHoveredColor()),
                () -> Assertions.assertNull(nullConstants.getActiveColor()));
    }
}
