package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.backend.TextureData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ImGuiSdlGles3RendererTest {
    private static final long WINDOW_HANDLE = 1L;

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
    private final ImGuiSdlGles3Renderer renderer = new ImGuiSdlGles3Renderer(backend, WINDOW_HANDLE);
    private final LorisScreen screen = new LorisScreen() {
        @Override
        public void onStartScreen() {}

        @Override
        public void onEndScreen() {}

        @Override
        protected List<Component> layout() {
            return List.of();
        }
    };
    private final BundleKeyResolver resolver = (key, _) -> key;

    @Test
    void shouldRejectNullBackendBeforeCheckingWindowHandle() {
        Assertions.assertThrows(NullPointerException.class, () -> new ImGuiSdlGles3Renderer(null, WINDOW_HANDLE));
        Assertions.assertThrows(NullPointerException.class, () -> new ImGuiSdlGles3Renderer(null, 0L));
    }

    @Test
    void shouldRejectZeroWindowHandle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImGuiSdlGles3Renderer(backend, 0L));
    }

    @Test
    void shouldRejectFramesBeforeInitialize() {
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.render(screen, 800, 600, resolver));
    }

    @Test
    void shouldRejectSdlEventsBeforeInitialize() {
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processSdlEvent(0L));
    }
}
