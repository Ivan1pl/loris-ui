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

class ImGuiGlfwGl3RendererTest {
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
    private final ImGuiGlfwGl3Renderer renderer = new ImGuiGlfwGl3Renderer(backend, WINDOW_HANDLE);

    @Test
    void shouldRejectNullBackendBeforeCheckingWindowHandle() {
        Assertions.assertThrows(NullPointerException.class, () -> new ImGuiGlfwGl3Renderer(null, WINDOW_HANDLE));
        Assertions.assertThrows(NullPointerException.class, () -> new ImGuiGlfwGl3Renderer(null, 0L));
    }

    @Test
    void shouldRejectZeroWindowHandle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImGuiGlfwGl3Renderer(backend, 0L));
    }

    @Test
    void shouldRejectGlfwEventsBeforeInitialize() {
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processGlfwWindowFocus(true));
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processGlfwCursorEnter(true));
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processGlfwCursorPos(10.0, 20.0));
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processGlfwMouseButton(0, 1, 0));
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processGlfwScroll(0.0, 1.0));
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processGlfwKey(65, 38, 1, 0));
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processGlfwChar('a'));
    }

    @Test
    void shouldRejectFramesBeforeInitialize() {
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

        Assertions.assertThrows(IllegalStateException.class, () -> renderer.render(screen, 800, 600, resolver));
    }
}
