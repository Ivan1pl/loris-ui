package com.ivan1pl.lorisui.imgui.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ImGuiOpenGlEsBackendTest {
    @Test
    void shouldThrowIoExceptionNamingPathBeforeAnyGlCallWhenResourceIsMissing() {
        ImGuiOpenGlEsBackend backend = new ImGuiOpenGlEsBackend();

        IOException thrown = Assertions.assertThrows(IOException.class,
                () -> backend.loadTexture("/missing.png"));

        Assertions.assertTrue(thrown.getMessage().contains("missing.png"), thrown.getMessage());
    }
}
