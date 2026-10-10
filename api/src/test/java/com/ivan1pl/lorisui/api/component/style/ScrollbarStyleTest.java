package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class ScrollbarStyleTest {
    @Test
    void shouldReturnNullForUnsetProperties() {
        ScrollbarStyle style = ScrollbarStyle.builder().build();

        Assertions.assertNull(style.getBackgroundColor());
        Assertions.assertNull(style.getGrabColor());
        Assertions.assertNull(style.getHoveredGrabColor());
        Assertions.assertNull(style.getActiveGrabColor());
        Assertions.assertNull(style.getRounding());
        Assertions.assertNull(style.getSize());
        Assertions.assertNull(style.getPadding());
    }

    @Test
    void shouldRejectNullSupplierAndAcceptNullConstant() {
        ScrollbarStyle.Builder builder = ScrollbarStyle.builder();

        Assertions.assertAll(
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.backgroundColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.grabColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.hoveredGrabColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.activeGrabColor((Supplier<Color>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.rounding((Supplier<Float>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.size((Supplier<Float>) null)),
                () -> Assertions.assertThrows(NullPointerException.class,
                        () -> builder.padding((Supplier<Float>) null)));

        ScrollbarStyle style = builder.backgroundColor((Color) null)
                .grabColor((Color) null)
                .hoveredGrabColor((Color) null)
                .activeGrabColor((Color) null)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertNull(style.getBackgroundColor()),
                () -> Assertions.assertNull(style.getGrabColor()),
                () -> Assertions.assertNull(style.getHoveredGrabColor()),
                () -> Assertions.assertNull(style.getActiveGrabColor()));
    }
}
