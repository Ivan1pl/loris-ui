package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class ComboBoxStyleTest {
    @Test
    void shouldTreatNullAsUnset() {
        ComboBoxStyle unset = ComboBoxStyle.builder().build();
        ComboBoxStyle.Builder builder = ComboBoxStyle.builder();
        ComboBoxStyle nullConstants = ComboBoxStyle.builder()
                .backgroundColor((Color) null)
                .backgroundColorHovered((Color) null)
                .borderColor((Color) null)
                .arrowButtonColor((Color) null)
                .arrowButtonColorHovered((Color) null)
                .arrowColor((Color) null)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertNull(unset.getBackgroundColor()),
                () -> Assertions.assertNull(unset.getBackgroundColorHovered()),
                () -> Assertions.assertNull(unset.getBorderColor()),
                () -> Assertions.assertNull(unset.getArrowButtonColor()),
                () -> Assertions.assertNull(unset.getArrowButtonColorHovered()),
                () -> Assertions.assertNull(unset.getArrowColor()),
                () -> Assertions.assertNull(unset.getRounding()),
                () -> Assertions.assertNull(unset.getBorderThickness()),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.backgroundColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.backgroundColorHovered((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.borderColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.arrowButtonColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.arrowButtonColorHovered((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.arrowColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.rounding((Supplier<Float>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.borderThickness((Supplier<Float>) null)),
                () -> Assertions.assertNull(nullConstants.getBackgroundColor()),
                () -> Assertions.assertNull(nullConstants.getBackgroundColorHovered()),
                () -> Assertions.assertNull(nullConstants.getBorderColor()),
                () -> Assertions.assertNull(nullConstants.getArrowButtonColor()),
                () -> Assertions.assertNull(nullConstants.getArrowButtonColorHovered()),
                () -> Assertions.assertNull(nullConstants.getArrowColor()));
    }
}
