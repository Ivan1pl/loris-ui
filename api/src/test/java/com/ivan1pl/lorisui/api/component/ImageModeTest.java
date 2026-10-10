package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImageModeTest {
    @Test
    void shouldReturnExpectedSubtypeWhenCreatedThroughEachFactory() {
        Assertions.assertTrue(ImageMode.repeat() instanceof ImageMode.Repeat);
        Assertions.assertTrue(ImageMode.fit() instanceof ImageMode.Fit);
        Assertions.assertTrue(ImageMode.subImage(0, 0, 1, 1) instanceof ImageMode.SubImage);
        Assertions.assertTrue(ImageMode.sprite(1, 1, 0) instanceof ImageMode.Sprite);
        Assertions.assertTrue(ImageMode.spriteResize(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                instanceof ImageMode.SpriteResize);
        Assertions.assertTrue(ImageMode.subImageResize(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                instanceof ImageMode.SubImageResize);
    }

    @Test
    void shouldExposeConstructorArgumentsThroughGettersInOrder() {
        if (!(ImageMode.subImage(1, 2, 3, 4) instanceof ImageMode.SubImage subImage)) {
            Assertions.fail("subImage() did not return a SubImage");
            return;
        }
        Assertions.assertEquals(1, subImage.getFromX());
        Assertions.assertEquals(2, subImage.getFromY());
        Assertions.assertEquals(3, subImage.getToX());
        Assertions.assertEquals(4, subImage.getToY());

        if (!(ImageMode.sprite(5, 6, 7) instanceof ImageMode.Sprite sprite)) {
            Assertions.fail("sprite() did not return a Sprite");
            return;
        }
        Assertions.assertEquals(5, sprite.getSpriteWidth());
        Assertions.assertEquals(6, sprite.getSpriteHeight());
        Assertions.assertEquals(7, sprite.getSpriteIndex());

        if (!(ImageMode.spriteResize(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
                instanceof ImageMode.SpriteResize spriteResize)) {
            Assertions.fail("spriteResize() did not return a SpriteResize");
            return;
        }
        Assertions.assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
                new int[] {spriteResize.getSpriteWidth(), spriteResize.getSpriteHeight(),
                        spriteResize.getSpriteIndex(), spriteResize.getX1(), spriteResize.getX2(),
                        spriteResize.getX3(), spriteResize.getY1(), spriteResize.getX4(), spriteResize.getX5(),
                        spriteResize.getX6(), spriteResize.getY2(), spriteResize.getX7(), spriteResize.getX8(),
                        spriteResize.getX9(), spriteResize.getY3()});

        if (!(ImageMode.subImageResize(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
                instanceof ImageMode.SubImageResize subImageResize)) {
            Assertions.fail("subImageResize() did not return a SubImageResize");
            return;
        }
        Assertions.assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
                new int[] {subImageResize.getFromX(), subImageResize.getFromY(), subImageResize.getX1(),
                        subImageResize.getX2(), subImageResize.getX3(), subImageResize.getY1(),
                        subImageResize.getX4(), subImageResize.getX5(), subImageResize.getX6(),
                        subImageResize.getY2(), subImageResize.getX7(), subImageResize.getX8(),
                        subImageResize.getX9(), subImageResize.getY3()});
    }
}
