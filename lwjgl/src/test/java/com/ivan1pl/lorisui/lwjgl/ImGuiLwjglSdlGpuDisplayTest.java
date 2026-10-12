package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ImGuiLwjglSdlGpuDisplayTest {
    private static final long WINDOW_HANDLE = 1L;
    private static final long GPU_DEVICE_HANDLE = 2L;
    // Never dereferenced: every case returns or throws before the display reads an event or touches the GPU
    private static final long EVENT_POINTER = 8L;
    private static final long COMMAND_BUFFER = 16L;
    private static final long TARGET_TEXTURE = 32L;
    private static final String WINDOW_MESSAGE = "sdlWindow must be the handle of an SDL window, not 0";
    private static final String CLOSED_MESSAGE = "Display is closed; create a new display to show UI again";

    private final List<String> events = new ArrayList<>();
    private final ImGuiLwjglSdlGpuDisplay display = new ImGuiLwjglSdlGpuDisplay(WINDOW_HANDLE, GPU_DEVICE_HANDLE);

    @Test
    void shouldRejectNullWindowHandle() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiLwjglSdlGpuDisplay(0L, GPU_DEVICE_HANDLE));

        Assertions.assertEquals(WINDOW_MESSAGE, thrown.getMessage());
    }

    @Test
    void shouldRejectNullGpuDevice() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImGuiLwjglSdlGpuDisplay(WINDOW_HANDLE, 0L));
    }

    @Test
    void shouldReportWindowFirstWhenBothHandlesAreZero() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiLwjglSdlGpuDisplay(0L, 0L));

        Assertions.assertEquals(WINDOW_MESSAGE, thrown.getMessage());
    }

    @Test
    void shouldReportEventNotConsumedBeforeInitialize() {
        Assertions.assertFalse(display.processEvent(EVENT_POINTER));
    }

    @Test
    void shouldRejectNullCommandBufferBeforeStateChecks() {
        display.addScreen("main", new RecordingScreen("main"));
        display.setCurrentScreen("main");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> display.render(800, 600, 0L, TARGET_TEXTURE));
        display.close();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> display.render(800, 600, 0L, TARGET_TEXTURE));
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldRejectRenderBeforeInitialize() {
        display.addScreen("main", new RecordingScreen("main"));
        display.setCurrentScreen("main");

        Assertions.assertThrows(IllegalStateException.class,
                () -> display.render(800, 600, COMMAND_BUFFER, TARGET_TEXTURE));
        Assertions.assertThrows(IllegalStateException.class, () -> display.render(800, 600, COMMAND_BUFFER, 0L));
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldCloseIdempotentlyWithoutHooksBeforeInitialize() {
        display.addScreen("main", new RecordingScreen("main"));
        display.setCurrentScreen("main");

        display.close();
        display.close();

        Assertions.assertFalse(display.processEvent(EVENT_POINTER));
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> display.render(800, 600, COMMAND_BUFFER, TARGET_TEXTURE));
        Assertions.assertEquals(CLOSED_MESSAGE, thrown.getMessage());
        Assertions.assertThrows(IllegalStateException.class, display::initialize);
        Assertions.assertEquals(List.of(), events);
    }

    private final class RecordingScreen extends LorisScreen {
        private final String name;

        private RecordingScreen(String name) {
            this.name = name;
        }

        @Override
        public void onStartScreen() {
            events.add("start:" + name);
        }

        @Override
        public void onEndScreen() {
            events.add("end:" + name);
        }

        @Override
        protected List<Component> layout() {
            return List.of();
        }
    }
}
