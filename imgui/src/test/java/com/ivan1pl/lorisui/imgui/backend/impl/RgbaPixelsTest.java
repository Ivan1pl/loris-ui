package com.ivan1pl.lorisui.imgui.backend.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

class RgbaPixelsTest {
    @Test
    void shouldWriteArgbAsRgbaBytesAtIndexLeavingNeighborsUntouched() {
        ByteBuffer pixels = ByteBuffer.allocate(12);
        for (int i = 0; i < 12; i++) {
            pixels.put(i, (byte) 0x5A);
        }

        RgbaPixels.put(pixels, 4, 0x80FF4010);

        assertBytes(pixels, 0, 0x5A, 0x5A, 0x5A, 0x5A);
        assertBytes(pixels, 4, 0xFF, 0x40, 0x10, 0x80);
        assertBytes(pixels, 8, 0x5A, 0x5A, 0x5A, 0x5A);
        Assertions.assertEquals(0, pixels.position());
    }

    @Test
    void shouldPackArgbArrayFromIndexZeroInRgbaOrder() {
        ByteBuffer pixels = ByteBuffer.allocateDirect(16);
        pixels.position(8);

        RgbaPixels.pack(new int[] {0xFF102030, 0x00000000, 0x7FFFFFFF}, pixels);

        assertBytes(pixels, 0, 0x10, 0x20, 0x30, 0xFF);
        assertBytes(pixels, 4, 0x00, 0x00, 0x00, 0x00);
        assertBytes(pixels, 8, 0xFF, 0xFF, 0xFF, 0x7F);
        assertBytes(pixels, 12, 0x00, 0x00, 0x00, 0x00);
        Assertions.assertEquals(8, pixels.position());
    }

    private static void assertBytes(ByteBuffer pixels, int index, int r, int g, int b, int a) {
        Assertions.assertEquals(r, Byte.toUnsignedInt(pixels.get(index)), "red at byte " + index);
        Assertions.assertEquals(g, Byte.toUnsignedInt(pixels.get(index + 1)), "green at byte " + index);
        Assertions.assertEquals(b, Byte.toUnsignedInt(pixels.get(index + 2)), "blue at byte " + index);
        Assertions.assertEquals(a, Byte.toUnsignedInt(pixels.get(index + 3)), "alpha at byte " + index);
    }
}
