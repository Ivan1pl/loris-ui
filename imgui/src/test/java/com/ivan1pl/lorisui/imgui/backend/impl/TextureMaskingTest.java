package com.ivan1pl.lorisui.imgui.backend.impl;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

class TextureMaskingTest {
    @Test
    void shouldPreserveRgbaOrderAndFullIntensityChannels() {
        ByteBuffer pixels = pixels(
                255, 0, 0, 255,
                0, 255, 0, 128,
                0, 0, 255, 255);

        TextureMasking.apply(pixels, 3, 1, Color.RED, Color.GREEN, Color.BLUE);

        assertPixel(pixels, 0, 255, 0, 0, 255);
        assertPixel(pixels, 1, 0, 255, 0, 128);
        assertPixel(pixels, 2, 0, 0, 255, 255);
        Assertions.assertEquals(0, pixels.position());
    }

    @Test
    void shouldRecolourRedPixelToBlueKeepingSourceAlpha() {
        ByteBuffer pixels = pixels(255, 0, 0, 200);

        TextureMasking.apply(pixels, 1, 1, Color.BLUE, null, null);

        assertPixel(pixels, 0, 0, 0, 255, 200);
    }

    @Test
    void shouldKeepTransparentPixelTransparent() {
        ByteBuffer pixels = pixels(
                255, 0, 0, 0,
                0, 0, 0, 0);

        TextureMasking.apply(pixels, 1, 2, Color.BLUE, Color.GREEN, Color.RED);

        assertPixel(pixels, 0, 0, 0, 255, 0);
        assertPixel(pixels, 1, 0, 0, 0, 0);
    }

    @Test
    void shouldLeavePixelsUnchangedWhenAllMasksNull() {
        ByteBuffer pixels = pixels(
                10, 20, 30, 40,
                200, 100, 50, 255,
                0, 0, 0, 255,
                255, 255, 255, 180);

        TextureMasking.apply(pixels, 2, 2, null, null, null);

        assertPixel(pixels, 0, 10, 20, 30, 40);
        assertPixel(pixels, 1, 200, 100, 50, 255);
        assertPixel(pixels, 2, 0, 0, 0, 255);
        assertPixel(pixels, 3, 255, 255, 255, 180);
    }

    private static ByteBuffer pixels(int... channels) {
        ByteBuffer buffer = ByteBuffer.allocate(channels.length);
        for (int channel : channels) {
            buffer.put((byte) channel);
        }
        return buffer.flip();
    }

    private static void assertPixel(ByteBuffer pixels, int pixel, int r, int g, int b, int a) {
        int offset = pixel * 4;
        Assertions.assertEquals(r, Byte.toUnsignedInt(pixels.get(offset)), 1, "red of pixel " + pixel);
        Assertions.assertEquals(g, Byte.toUnsignedInt(pixels.get(offset + 1)), 1, "green of pixel " + pixel);
        Assertions.assertEquals(b, Byte.toUnsignedInt(pixels.get(offset + 2)), 1, "blue of pixel " + pixel);
        Assertions.assertEquals(a, Byte.toUnsignedInt(pixels.get(offset + 3)), 1, "alpha of pixel " + pixel);
    }
}
