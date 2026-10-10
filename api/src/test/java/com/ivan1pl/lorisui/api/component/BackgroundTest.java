package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BackgroundTest {
    @Test
    void shouldRejectNullImagePathModeOrColor() {
        ImageMode mode = ImageMode.fit();

        Assertions.assertAll(
                () -> Assertions.assertEquals("image", Assertions.assertThrows(NullPointerException.class,
                        () -> Background.image(null, mode)).getMessage()),
                () -> Assertions.assertEquals("imageMode", Assertions.assertThrows(NullPointerException.class,
                        () -> Background.image("bg.png", null)).getMessage()),
                () -> Assertions.assertEquals("image", Assertions.assertThrows(NullPointerException.class,
                        () -> Background.maskedImage(null, mode, null, null, null)).getMessage()),
                () -> Assertions.assertEquals("imageMode", Assertions.assertThrows(NullPointerException.class,
                        () -> Background.maskedImage("bg.png", null, null, null, null)).getMessage()),
                () -> Assertions.assertEquals("color", Assertions.assertThrows(NullPointerException.class,
                        () -> Background.color(null)).getMessage()));
    }

    @Test
    void shouldAcceptNullMaskColorsWhenMaskedImageCreated() {
        ImageMode mode = ImageMode.fit();

        if (!(Background.maskedImage("bg.png", mode, null, null, null) instanceof Background.MaskedImage masked)) {
            Assertions.fail("maskedImage() did not return a MaskedImage");
            return;
        }
        Assertions.assertAll(
                () -> Assertions.assertEquals("bg.png", masked.getImage()),
                () -> Assertions.assertSame(mode, masked.getImageMode()),
                () -> Assertions.assertNull(masked.getRedColor()),
                () -> Assertions.assertNull(masked.getGreenColor()),
                () -> Assertions.assertNull(masked.getBlueColor()));
    }
}
