package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class ComboBoxPopupStyleTest {
    @Test
    void shouldTreatNullAsUnset() {
        ComboBoxPopupStyle unset = ComboBoxPopupStyle.builder().build();
        ComboBoxPopupStyle.Builder builder = ComboBoxPopupStyle.builder();
        ComboBoxPopupStyle nullConstants = ComboBoxPopupStyle.builder()
                .backgroundColor((Color) null)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertNull(unset.getBackgroundColor()),
                () -> Assertions.assertNull(unset.getRounding()),
                () -> Assertions.assertNull(unset.getBorderThickness()),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.backgroundColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.rounding((Supplier<Float>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.borderThickness((Supplier<Float>) null)),
                () -> Assertions.assertNull(nullConstants.getBackgroundColor()));
    }
}
