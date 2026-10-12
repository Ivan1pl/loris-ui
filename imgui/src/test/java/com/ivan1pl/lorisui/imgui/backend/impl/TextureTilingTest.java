package com.ivan1pl.lorisui.imgui.backend.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TextureTilingTest {
    @Test
    void shouldReturnFullTilesWhenLengthIsExactMultiple() {
        Assertions.assertArrayEquals(new int[] {32, 32}, TextureTiling.spans(64, 32));
    }

    @Test
    void shouldReturnSingleFullTileWhenLengthEqualsTileLength() {
        Assertions.assertArrayEquals(new int[] {32}, TextureTiling.spans(32, 32));
    }

    @Test
    void shouldEndWithPartialTileWhenLengthIsNotMultiple() {
        Assertions.assertArrayEquals(new int[] {32, 32, 32, 4}, TextureTiling.spans(100, 32));
    }

    @Test
    void shouldReturnSinglePartialTileWhenLengthIsShorterThanTile() {
        Assertions.assertArrayEquals(new int[] {20}, TextureTiling.spans(20, 32));
    }

    @Test
    void shouldReturnNoTilesWhenLengthIsNotPositive() {
        Assertions.assertArrayEquals(new int[0], TextureTiling.spans(0, 32));
        Assertions.assertArrayEquals(new int[0], TextureTiling.spans(-5, 32));
    }

    @Test
    void shouldRejectNonPositiveTileLengthNamingValue() {
        IllegalArgumentException zero = Assertions.assertThrows(IllegalArgumentException.class,
                () -> TextureTiling.spans(10, 0));
        IllegalArgumentException negative = Assertions.assertThrows(IllegalArgumentException.class,
                () -> TextureTiling.spans(10, -1));

        Assertions.assertTrue(zero.getMessage().contains("0"), zero.getMessage());
        Assertions.assertTrue(negative.getMessage().contains("-1"), negative.getMessage());
    }
}
