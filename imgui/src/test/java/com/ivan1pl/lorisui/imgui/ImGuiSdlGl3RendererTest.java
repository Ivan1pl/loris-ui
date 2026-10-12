package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.backend.TextureData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class ImGuiSdlGl3RendererTest {
    private static final long WINDOW_HANDLE = 1L;
    // 28 bytes, too short to be accepted as font data
    private static final String TEST_RESOURCE = "classpath-resources-test.txt";
    private static final int MAX_REJECTED_FONT_DATA_SIZE = 100;

    private final ImGuiBackend backend = new ImGuiBackend() {
        @Override
        protected TextureData createTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void deleteTexture(TextureData texture) {
            throw new UnsupportedOperationException();
        }
    };
    private final ImGuiSdlGl3Renderer renderer = new ImGuiSdlGl3Renderer(backend, WINDOW_HANDLE);

    @Test
    void shouldRejectNullBackend() {
        Assertions.assertThrows(NullPointerException.class, () -> new ImGuiSdlGl3Renderer(null, WINDOW_HANDLE));
    }

    @Test
    void shouldAcceptZeroWindowHandleAndIgnoreDispose() {
        ImGuiSdlGl3Renderer withoutWindow = new ImGuiSdlGl3Renderer(backend, 0L);

        Assertions.assertDoesNotThrow(withoutWindow::dispose);
    }

    @Test
    void shouldRejectInvalidFontRegistrationArguments() {
        Assertions.assertThrows(NullPointerException.class, () -> renderer.registerFontFromResources(null, 16));
        Assertions.assertThrows(NullPointerException.class,
                () -> renderer.registerFontFromResources(TEST_RESOURCE, 16, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> renderer.registerFontFromResources(TEST_RESOURCE, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> renderer.registerFontFromResources(TEST_RESOURCE, -1, new short[] { 0x0020, 0x007F, 0 }));
    }

    @Test
    void shouldThrowIoExceptionNamingPathWhenFontResourceIsMissing() {
        String missing = "/fonts/missing-renderer-test-font.ttf";

        IOException thrown = Assertions.assertThrows(IOException.class,
                () -> renderer.registerFontFromResources(missing, 16));

        Assertions.assertTrue(thrown.getMessage().contains(missing), thrown.getMessage());
    }

    @Test
    void shouldThrowIoExceptionNamingPathWhenFontFileIsMissing(@TempDir Path directory) {
        String missing = directory.resolve("missing-renderer-test-font.ttf").toString();

        IOException withDefaultRanges = Assertions.assertThrows(IOException.class,
                () -> renderer.registerFontFromFile(missing, 16));
        IOException withGlyphRanges = Assertions.assertThrows(IOException.class,
                () -> renderer.registerFontFromFile(missing, 16, new short[] { 0x0020, 0x007F, 0 }));

        Assertions.assertTrue(withDefaultRanges.getMessage().contains(missing), withDefaultRanges.getMessage());
        Assertions.assertTrue(withGlyphRanges.getMessage().contains(missing), withGlyphRanges.getMessage());
    }

    @Test
    void shouldRejectFontFileOfAtMostHundredBytesNamingPath(@TempDir Path directory) throws IOException {
        String empty = Files.write(directory.resolve("empty.ttf"), new byte[0]).toString();
        String truncated = Files.write(directory.resolve("truncated.ttf"), new byte[MAX_REJECTED_FONT_DATA_SIZE])
                .toString();

        IOException emptyThrown = Assertions.assertThrows(IOException.class,
                () -> renderer.registerFontFromFile(empty, 16));
        IOException truncatedThrown = Assertions.assertThrows(IOException.class,
                () -> renderer.registerFontFromFile(truncated, 16, new short[] { 0x0020, 0x007F, 0 }));

        Assertions.assertTrue(emptyThrown.getMessage().contains(empty), emptyThrown.getMessage());
        Assertions.assertTrue(truncatedThrown.getMessage().contains(truncated), truncatedThrown.getMessage());
    }

    @Test
    void shouldRejectTooShortFontResourceNamingPath() {
        IOException withDefaultRanges = Assertions.assertThrows(IOException.class,
                () -> renderer.registerFontFromResources(TEST_RESOURCE, 16));
        IOException withGlyphRanges = Assertions.assertThrows(IOException.class,
                () -> renderer.registerFontFromResources(TEST_RESOURCE, 16, new short[] { 0x0020, 0x007F, 0 }));

        Assertions.assertTrue(withDefaultRanges.getMessage().contains(TEST_RESOURCE), withDefaultRanges.getMessage());
        Assertions.assertTrue(withGlyphRanges.getMessage().contains(TEST_RESOURCE), withGlyphRanges.getMessage());
    }

    @Test
    void shouldRejectMalformedGlyphRangesBeforeReadingFont() {
        String missing = "/fonts/missing-renderer-test-font.ttf";

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> renderer.registerFontFromResources(missing, 16, new short[] { 0x0020 }));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> renderer.registerFontFromFile(missing, 16, new short[] { 0x007F, 0x0020, 0 }));
    }

    @Test
    void shouldIgnoreDisposeBeforeInitialize() {
        Assertions.assertDoesNotThrow(renderer::dispose);
        Assertions.assertDoesNotThrow(renderer::dispose);
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processSdlEvent(0L));
    }

    @Test
    void shouldReportNoInputCaptureBeforeInitialize() {
        Assertions.assertFalse(renderer.wantsMouseInput());
        Assertions.assertFalse(renderer.wantsKeyboardInput());
        Assertions.assertFalse(renderer.wantsTextInput());
        Assertions.assertFalse(renderer.isHoveringScrollablePanel());
    }

    @Test
    void shouldRejectEventsAndFramesBeforeInitialize() {
        LorisScreen screen = new LorisScreen() {
            @Override
            public void onStartScreen() {}

            @Override
            public void onEndScreen() {}

            @Override
            protected List<Component> layout() {
                return List.of();
            }
        };
        BundleKeyResolver resolver = (key, _) -> key;

        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processSdlEvent(0L));
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.render(screen, 800, 600, resolver));
    }

    @Test
    void shouldRejectUseOfFontWhoseRendererIsNotInitialized(@TempDir Path directory) throws IOException {
        // Registration checks only the length, so placeholder data just long enough is accepted
        Path file = Files.write(directory.resolve("placeholder.ttf"), new byte[MAX_REJECTED_FONT_DATA_SIZE + 1]);
        ImGuiFont font = renderer.registerFontFromFile(file.toString(), 16);
        AtomicBoolean ran = new AtomicBoolean();

        Assertions.assertThrows(IllegalStateException.class, () -> font.useFont(() -> ran.set(true)));
        Assertions.assertFalse(ran.get());
    }
}
