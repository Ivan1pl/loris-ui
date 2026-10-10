package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class TextInputStyleTest {
    @Test
    void shouldTreatNullAsUnset() {
        TextInputStyle unset = TextInputStyle.builder().build();
        TextInputStyle.Builder builder = TextInputStyle.builder();
        TextInputStyle nullConstants = TextInputStyle.builder()
                .backgroundColor((Color) null)
                .textColor((Color) null)
                .selectedTextBackgroundColor((Color) null)
                .borderColor((Color) null)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertNull(unset.getBackgroundColor()),
                () -> Assertions.assertNull(unset.getTextColor()),
                () -> Assertions.assertNull(unset.getSelectedTextBackgroundColor()),
                () -> Assertions.assertNull(unset.getBorderColor()),
                () -> Assertions.assertNull(unset.getRounding()),
                () -> Assertions.assertNull(unset.getBorderThickness()),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.backgroundColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.textColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.selectedTextBackgroundColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.borderColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.rounding((Supplier<Float>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.borderThickness((Supplier<Float>) null)),
                () -> Assertions.assertNull(nullConstants.getBackgroundColor()),
                () -> Assertions.assertNull(nullConstants.getTextColor()),
                () -> Assertions.assertNull(nullConstants.getSelectedTextBackgroundColor()),
                () -> Assertions.assertNull(nullConstants.getBorderColor()));
    }
}
