package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class ProgressBarTest {
    @Test
    void shouldReturnPlainValuesAndDefaults() {
        ProgressBar unset = ProgressBar.builder().build();
        ProgressBar set = ProgressBar.builder()
                .progress(() -> 0.25f)
                .backgroundColor(Color.BLUE)
                .barColor(() -> Color.RED)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(0f, unset.getProgress()),
                () -> Assertions.assertNull(unset.getBackgroundColor()),
                () -> Assertions.assertNull(unset.getBarColor()),
                () -> Assertions.assertEquals(0.25f, set.getProgress()),
                () -> Assertions.assertSame(Color.BLUE, set.getBackgroundColor()),
                () -> Assertions.assertSame(Color.RED, set.getBarColor()),
                () -> Assertions.assertEquals("progress", Assertions.assertThrows(NullPointerException.class,
                        () -> ProgressBar.builder().progress((Supplier<Float>) null)).getMessage()));
    }

    @Test
    void shouldReturnNullRoundingWhenUnset() {
        Assertions.assertNull(ProgressBar.builder().build().getRounding());
    }

    @Test
    void shouldReturnRoundingFromConstantAndSupplierOverloads() {
        ProgressBar constant = ProgressBar.builder().rounding(5f).build();
        ProgressBar supplied = ProgressBar.builder().rounding(() -> 5f).build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(5f, constant.getRounding(), 1e-6f),
                () -> Assertions.assertEquals(5f, supplied.getRounding(), 1e-6f));
    }

    @Test
    void shouldKeepExplicitZeroRounding() {
        ProgressBar constant = ProgressBar.builder().rounding(0f).build();
        ProgressBar supplied = ProgressBar.builder().rounding(() -> 0f).build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(0f, constant.getRounding(), 1e-6f),
                () -> Assertions.assertEquals(0f, supplied.getRounding(), 1e-6f));
    }

    @Test
    void shouldRejectNullRoundingSupplier() {
        Assertions.assertEquals("rounding", Assertions.assertThrows(NullPointerException.class,
                () -> ProgressBar.builder().rounding((Supplier<Float>) null)).getMessage());
    }
}
