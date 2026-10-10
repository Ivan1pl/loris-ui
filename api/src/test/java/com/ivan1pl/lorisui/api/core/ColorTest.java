package com.ivan1pl.lorisui.api.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ColorTest {
    @Test
    void shouldKeepTransparentPixelTransparentWhenMasked() {
        Color transparentRed = new Color(1f, 0f, 0f, 0f);

        Color masked = transparentRed.mask(Color.BLUE, Color.GREEN, Color.RED);

        Assertions.assertEquals(0f, masked.getA());
    }

    @Test
    void shouldMultiplySourceAlphaByMaskAlpha() {
        Color halfTransparentRed = new Color(1f, 0f, 0f, 0.5f);

        Color masked = halfTransparentRed.mask(Color.BLUE.withA(0.5f), null, null);

        Assertions.assertEquals(0.25f, masked.getA(), 1e-6f);
    }

    @Test
    void shouldPackArgbAndRoundTripThroughIntConstructor() {
        Color color = new Color(1f, 0.5f, 0f, 0.2f);

        Color roundTripped = new Color(color.getRGBA());

        Assertions.assertEquals(0x33FF8000, color.getRGBA());
        Assertions.assertEquals(0x33FF8000, roundTripped.getRGBA());
        Assertions.assertEquals(1f, roundTripped.getR(), 1e-6f);
        Assertions.assertEquals(128 / 255f, roundTripped.getG(), 1e-6f);
        Assertions.assertEquals(0f, roundTripped.getB(), 1e-6f);
        Assertions.assertEquals(51 / 255f, roundTripped.getA(), 1e-6f);
    }

    @Test
    void shouldCopyWithNewAlphaAndCompareByValue() {
        Color opaque = new Color(0.2f, 0.4f, 0.6f, 1f);

        Color translucent = opaque.withA(0.5f);

        Assertions.assertEquals(1f, opaque.getA());
        Assertions.assertEquals(new Color(0.2f, 0.4f, 0.6f, 0.5f), translucent);
        Assertions.assertEquals(new Color(0.2f, 0.4f, 0.6f, 0.5f).hashCode(), translucent.hashCode());
        Assertions.assertNotEquals(opaque, translucent);
    }

    @Test
    void shouldReturnSourceUnchangedWhenMasksNeutralOrSourceBlack() {
        Color source = new Color(0.2f, 0.4f, 0.6f, 0.8f);

        Color masked = source.mask(null, null, null);

        assertChannels(0.2f, 0.4f, 0.6f, 0.8f, masked);
        Assertions.assertSame(Color.BLACK, Color.BLACK.mask(Color.RED, Color.GREEN, Color.BLUE));
    }

    @Test
    void shouldRecolourWhenMasksOpaque() {
        Color red = new Color(1f, 0f, 0f, 1f);
        Color yellow = new Color(0.5f, 0.5f, 0f, 1f);

        Color redAsBlue = red.mask(Color.BLUE, null, null);
        Color yellowSwapped = yellow.mask(Color.BLUE, Color.RED, null);

        assertChannels(0f, 0f, 1f, 1f, redAsBlue);
        assertChannels(0.5f, 0f, 0.5f, 1f, yellowSwapped);
    }

    private static void assertChannels(float r, float g, float b, float a, Color actual) {
        Assertions.assertEquals(r, actual.getR(), 1e-6f);
        Assertions.assertEquals(g, actual.getG(), 1e-6f);
        Assertions.assertEquals(b, actual.getB(), 1e-6f);
        Assertions.assertEquals(a, actual.getA(), 1e-6f);
    }
}
