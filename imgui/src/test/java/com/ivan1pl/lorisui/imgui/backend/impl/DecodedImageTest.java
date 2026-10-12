package com.ivan1pl.lorisui.imgui.backend.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class DecodedImageTest {
    @Test
    void shouldThrowIoExceptionNamingPathWhenResourceIsMissing() {
        IOException thrown = Assertions.assertThrows(IOException.class,
                () -> DecodedImage.load("/missing.png", null, null, null));

        Assertions.assertTrue(thrown.getMessage().contains("Image not found: "), thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("/missing.png"), thrown.getMessage());
    }
}
