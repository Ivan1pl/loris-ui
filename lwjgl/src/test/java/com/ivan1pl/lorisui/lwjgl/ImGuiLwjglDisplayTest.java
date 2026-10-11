package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.Text;
import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ImGuiLwjglDisplayTest {
    private static final long WINDOW_HANDLE = 1L;
    // Never dereferenced: every case returns or throws before the display reads an event
    private static final long EVENT_POINTER = 8L;
    private static final String FONT_RESOURCE = "/fonts/missing-display-test-font.ttf";

    private final List<String> events = new ArrayList<>();
    private final ImGuiLwjglDisplay display = new ImGuiLwjglDisplay(WINDOW_HANDLE);

    @Test
    void shouldRejectNullWindowHandle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImGuiLwjglDisplay(0L));
    }

    @Test
    void shouldRejectInitializeWithoutCurrentScreen() {
        display.addScreen("main", new RecordingScreen("main"));

        Assertions.assertThrows(IllegalStateException.class, display::initialize);
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldReportEventNotConsumedBeforeInitialize() {
        Assertions.assertFalse(display.processEvent(EVENT_POINTER));
    }

    @Test
    void shouldRejectRenderBeforeInitialize() {
        display.addScreen("main", new RecordingScreen("main"));
        display.setCurrentScreen("main");

        Assertions.assertThrows(IllegalStateException.class, () -> display.render(800, 600));
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldCloseIdempotentlyWithoutHooksBeforeInitialize() {
        display.addScreen("main", new RecordingScreen("main"));
        display.setCurrentScreen("main");

        display.close();
        display.close();

        Assertions.assertFalse(display.processEvent(EVENT_POINTER));
        Assertions.assertThrows(IllegalStateException.class, () -> display.render(800, 600));
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldRejectRegistrationAndInitializeAfterClose() {
        display.addScreen("main", new RecordingScreen("main"));
        display.setCurrentScreen("main");
        display.close();

        Assertions.assertThrows(IllegalStateException.class,
                () -> display.registerFontFromResources(FONT_RESOURCE, 16));
        Assertions.assertThrows(IllegalStateException.class,
                () -> display.registerFontFromFile(FONT_RESOURCE, 16, new short[] { 0x0020, 0x007F, 0 }));
        Assertions.assertThrows(IllegalStateException.class,
                () -> display.registerRenderer(Text.class, new NoOpRenderer()));
        Assertions.assertThrows(IllegalStateException.class, display::initialize);

        display.addScreen("other", new RecordingScreen("other"));
        display.setCurrentScreen("other");
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldAcceptRegistrationBeforeInitializeAndReportMissingFontPath() {
        display.registerRenderer(Text.class, new NoOpRenderer());
        display.registerStyleApplier(GaugeStyle.class, new NoOpStyleApplier());

        IOException thrown = Assertions.assertThrows(IOException.class,
                () -> display.registerFontFromResources(FONT_RESOURCE, 16));

        Assertions.assertTrue(thrown.getMessage().contains(FONT_RESOURCE), thrown.getMessage());
        display.close();
        Assertions.assertThrows(IllegalStateException.class,
                () -> display.registerStyleApplier(GaugeStyle.class, new NoOpStyleApplier()));
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

    private static final class NoOpRenderer extends Renderer<Text> {
        @Override
        public void render(Text component, int w, int h, RendererProvider rendererProvider,
                           BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {}
    }

    private static final class GaugeStyle implements Style {}

    private static final class NoOpStyleApplier implements StyleApplier<GaugeStyle> {
        @Override
        public Styles applyStyles(GaugeStyle style, RendererProvider rendererProvider) {
            return new Styles() {};
        }

        @Override
        public void popStyles(Styles styles) {}
    }
}
