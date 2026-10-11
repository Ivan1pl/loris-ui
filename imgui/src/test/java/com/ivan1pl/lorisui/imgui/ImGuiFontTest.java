package com.ivan1pl.lorisui.imgui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImGuiFontTest {
    @Test
    void shouldKeepRangesAboveU7fff() {
        short[] cjk = { 0x4E00, (short) 0x9FAF, 0 };
        short[] top = { (short) 0xFF00, (short) 0xFFFF, 0 };

        Assertions.assertArrayEquals(cjk, ImGuiFont.buildGlyphRanges(cjk));
        Assertions.assertArrayEquals(top, ImGuiFont.buildGlyphRanges(top));
    }

    @Test
    void shouldKeepRangeCrossingU8000AndMergeAdjacentRanges() {
        short[] ranges = { 0x0020, 0x007F, 0x7F00, (short) 0x80FF, (short) 0x8100, (short) 0x81FF, 0 };

        Assertions.assertArrayEquals(new short[] { 0x0020, 0x007F, 0x7F00, (short) 0x81FF, 0 },
                ImGuiFont.buildGlyphRanges(ranges));
    }

    @Test
    void shouldRejectRangesWithoutLastCodePointOrTerminator() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ImGuiFont.buildGlyphRanges(new short[] { 0x0020 }));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ImGuiFont.buildGlyphRanges(new short[] { 0x0020, 0x007F, 0x0100 }));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ImGuiFont.buildGlyphRanges(new short[] { 0x0020, 0x007F }));
    }

    @Test
    void shouldRejectEmptyAndReversedRanges() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ImGuiFont.buildGlyphRanges(new short[] {}));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ImGuiFont.buildGlyphRanges(new short[] { 0 }));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ImGuiFont.buildGlyphRanges(new short[] { 0x007F, 0x0020, 0 }));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ImGuiFont.buildGlyphRanges(new short[] { (short) 0x9FAF, 0x4E00, 0 }));
    }
}
