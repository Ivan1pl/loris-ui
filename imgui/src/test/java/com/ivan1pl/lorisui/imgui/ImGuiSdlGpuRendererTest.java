package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiSdlGpuBackend;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ImGuiSdlGpuRendererTest {
    private static final long WINDOW_HANDLE = 1L;
    private static final long GPU_DEVICE = 1L;
    private static final long COMMAND_BUFFER = 1L;
    private static final long TARGET_TEXTURE = 1L;

    private final ImGuiSdlGpuBackend backend = new ImGuiSdlGpuBackend(GPU_DEVICE);
    private final ImGuiSdlGpuRenderer renderer = new ImGuiSdlGpuRenderer(backend, WINDOW_HANDLE);
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
        Assertions.assertThrows(NullPointerException.class, () -> new ImGuiSdlGpuRenderer(null, WINDOW_HANDLE));
        Assertions.assertThrows(NullPointerException.class, () -> new ImGuiSdlGpuRenderer(null, 0L));
    }

    @Test
    void shouldRejectZeroWindowHandle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImGuiSdlGpuRenderer(backend, 0L));
    }

    @Test
    void shouldRejectZeroCommandBufferBeforeCheckingState() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> renderer.render(screen, 800, 600, resolver, 0L, TARGET_TEXTURE));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> renderer.render(screen, 800, 600, resolver, 0L, 0L));
    }

    @Test
    void shouldRejectFramesBeforeInitialize() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> renderer.render(screen, 800, 600, resolver, COMMAND_BUFFER, TARGET_TEXTURE));
        Assertions.assertThrows(IllegalStateException.class,
                () -> renderer.render(screen, 800, 600, resolver, COMMAND_BUFFER, 0L));
    }

    @Test
    void shouldRejectSdlEventsBeforeInitialize() {
        Assertions.assertThrows(IllegalStateException.class, () -> renderer.processSdlEvent(0L));
    }
}
