package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.function.Supplier;

class ImageTest {
    @Test
    void shouldRejectMissingImageAtSetterAndAtBuild() {
        ImageMode mode = ImageMode.fit();
        Image.Builder builder = Image.builder().size(10, 10);

        Assertions.assertAll(
                () -> assertNullPointer("image", () -> builder.image((String) null, mode)),
                () -> assertNullPointer("imageMode", () -> builder.image("image.png", null)),
                () -> assertNullPointer("image", () -> builder.image((Supplier<String>) null, () -> mode)),
                () -> assertNullPointer("imageMode", () -> builder.image(() -> "image.png", null)),
                () -> assertNullPointer("image", () -> Image.builder().size(10, 10).build()));
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
