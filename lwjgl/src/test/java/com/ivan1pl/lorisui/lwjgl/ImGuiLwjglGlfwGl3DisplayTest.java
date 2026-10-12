package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.Text;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ImGuiLwjglGlfwGl3DisplayTest {
    private static final long WINDOW_HANDLE = 1L;
    private static final String FONT_RESOURCE = "/fonts/missing-display-test-font.ttf";
    private static final String CLOSED_MESSAGE = "Display is closed; create a new display to show UI again";
    private static final String NOT_INITIALIZED_MESSAGE = "Display is not initialized; call initialize before render";
    private static final String CLOSE_IN_CALLBACK_MESSAGE = "Cannot close the display from inside a GLFW callback; "
            + "request the close there and call close() after glfwPollEvents returns";

    private final List<String> events = new ArrayList<>();
    private final ImGuiLwjglGlfwGl3Display display = new ImGuiLwjglGlfwGl3Display(WINDOW_HANDLE);

    @Test
    void shouldRejectNullWindowHandle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImGuiLwjglGlfwGl3Display(0L));
    }

    @Test
    void shouldRejectInitializeWithoutCurrentScreen() {
        display.addScreen("main", new RecordingScreen("main"));

        Assertions.assertThrows(IllegalStateException.class, display::initialize);
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldRejectRenderBeforeInitialize() {
        display.addScreen("main", new RecordingScreen("main"));
        display.setCurrentScreen("main");

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> display.render(800, 600));

        Assertions.assertEquals(NOT_INITIALIZED_MESSAGE, thrown.getMessage());
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldCloseIdempotentlyWithoutHooksBeforeInitialize() {
        display.addScreen("main", new RecordingScreen("main"));
        display.setCurrentScreen("main");

        display.close();
        display.close();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> display.render(800, 600));
        Assertions.assertEquals(CLOSED_MESSAGE, thrown.getMessage());
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
                () -> display.registerRenderer(Text.class, new NoOpRenderer()));
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, display::initialize);

        Assertions.assertEquals(CLOSED_MESSAGE, thrown.getMessage());
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldRejectCloseInsideDispatchAndRethrowItFromNextClose() {
        display.dispatch(display::close);

        display.registerRenderer(Text.class, new NoOpRenderer());
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, display::close);

        Assertions.assertEquals(CLOSE_IN_CALLBACK_MESSAGE, thrown.getMessage());
        IllegalStateException afterClose = Assertions.assertThrows(IllegalStateException.class,
                () -> display.registerRenderer(Text.class, new NoOpRenderer()));
        Assertions.assertEquals(CLOSED_MESSAGE, afterClose.getMessage());
        Assertions.assertDoesNotThrow(display::close);
    }

    @Test
    void shouldDropEventsAfterDispatchFailureAndRethrowItOnceFromRender() {
        RuntimeException failure = new RuntimeException("callback failed");

        display.dispatch(() -> {
            throw failure;
        });
        display.dispatch(() -> events.add("dropped"));
        RuntimeException rethrown = Assertions.assertThrows(RuntimeException.class, () -> display.render(800, 600));

        Assertions.assertSame(failure, rethrown);
        Assertions.assertEquals(List.of(), events);
        display.dispatch(() -> events.add("forwarded"));
        Assertions.assertEquals(List.of("forwarded"), events);
        IllegalStateException afterRethrow = Assertions.assertThrows(IllegalStateException.class,
                () -> display.render(800, 600));
        Assertions.assertEquals(NOT_INITIALIZED_MESSAGE, afterRethrow.getMessage());
    }

    @Test
    void shouldWrapCheckedDispatchFailureWhenRethrowing() {
        IOException failure = new IOException("checked callback failure");

        display.dispatch(() -> ImGuiLwjglGlfwGl3DisplayTest.<RuntimeException>throwUnchecked(failure));
        RuntimeException rethrown = Assertions.assertThrows(RuntimeException.class, () -> display.render(800, 600));

        Assertions.assertEquals(RuntimeException.class, rethrown.getClass());
        Assertions.assertSame(failure, rethrown.getCause());
    }

    @Test
    void shouldRethrowErrorFromDispatchWithoutWrapping() {
        Error failure = new Error("callback error");

        display.dispatch(() -> {
            throw failure;
        });
        Error rethrown = Assertions.assertThrows(Error.class, () -> display.render(800, 600));

        Assertions.assertSame(failure, rethrown);
    }

    @Test
    void shouldThrowDispatchFailureOnceFromCloseOfClosedDisplay() {
        RuntimeException failure = new RuntimeException("callback failed after close");
        display.close();

        display.dispatch(() -> {
            throw failure;
        });
        RuntimeException rethrown = Assertions.assertThrows(RuntimeException.class, display::close);

        Assertions.assertSame(failure, rethrown);
        Assertions.assertEquals(0, rethrown.getSuppressed().length);
        Assertions.assertDoesNotThrow(display::close);
        IllegalStateException afterRethrow = Assertions.assertThrows(IllegalStateException.class,
                () -> display.render(800, 600));
        Assertions.assertEquals(CLOSED_MESSAGE, afterRethrow.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static <ThrowableType extends Throwable> void throwUnchecked(Throwable failure) throws ThrowableType {
        throw (ThrowableType) failure;
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
}
