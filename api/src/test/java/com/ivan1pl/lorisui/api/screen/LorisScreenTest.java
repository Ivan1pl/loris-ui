package com.ivan1pl.lorisui.api.screen;

import com.ivan1pl.lorisui.api.backend.Backend;
import com.ivan1pl.lorisui.api.backend.TextureData;
import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.ImageMode;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class LorisScreenTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final BundleKeyResolver RESOLVER = (key, _) -> key;

    @Test
    void shouldRethrowLayoutFailureOnRender() {
        RuntimeException failure = new RuntimeException("layout failed");
        TestScreen screen = new TestScreen(() -> {
            throw failure;
        });

        IllegalStateException thrown = renderUntilThrows(screen);

        Assertions.assertSame(failure, thrown.getCause());
    }

    @Test
    void shouldRunLayoutOnDaemonThread() throws Exception {
        CompletableFuture<Boolean> daemon = new CompletableFuture<>();
        TestScreen screen = new TestScreen(() -> {
            daemon.complete(Thread.currentThread().isDaemon());
            return List.of();
        });

        screen.render(new RecordingBackend(), 100, 100, RESOLVER);

        Assertions.assertTrue(daemon.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    }

    @Test
    void shouldRenderNewContentWhenAsyncLayoutCompletes() {
        List<Component> content = List.of(new Component() {});
        TestScreen screen = new TestScreen(() -> content);
        RecordingBackend backend = new RecordingBackend();

        renderUntilRendered(screen, backend, content);

        Assertions.assertEquals(List.of(), backend.rendered.getFirst());
        Assertions.assertEquals(content, backend.lastRendered());
    }

    @Test
    void shouldRethrowLayoutFailureExactlyOnceAndKeepPreviousContent() {
        List<Component> content = List.of(new Component() {});
        RuntimeException failure = new RuntimeException("second layout failed");
        AtomicInteger layoutCalls = new AtomicInteger();
        TestScreen screen = new TestScreen(() -> {
            if (layoutCalls.getAndIncrement() == 0) {
                return content;
            }
            throw failure;
        });
        RecordingBackend backend = new RecordingBackend();
        renderUntilRendered(screen, backend, content);

        screen.setLayoutNeedsUpdate(true);
        IllegalStateException thrown = renderUntilThrows(screen, backend);
        backend.rendered.clear();
        screen.render(backend, 100, 100, RESOLVER);

        Assertions.assertSame(failure, thrown.getCause());
        Assertions.assertEquals(List.of(content), backend.rendered);
        Assertions.assertEquals(2, layoutCalls.get());
    }

    @Test
    void shouldThrowWithNullPointerCauseWhenLayoutReturnsNull() {
        TestScreen screen = new TestScreen(() -> null);

        IllegalStateException thrown = renderUntilThrows(screen);

        Assertions.assertInstanceOf(NullPointerException.class, thrown.getCause());
    }

    @Test
    void shouldRunLayoutOnThreadNamedLorisuiLayout() throws Exception {
        CompletableFuture<String> threadName = new CompletableFuture<>();
        TestScreen screen = new TestScreen(() -> {
            threadName.complete(Thread.currentThread().getName());
            return List.of();
        });

        screen.render(new RecordingBackend(), 100, 100, RESOLVER);

        Assertions.assertEquals("lorisui-layout", threadName.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    }

    private static IllegalStateException renderUntilThrows(TestScreen screen) {
        return renderUntilThrows(screen, new RecordingBackend());
    }

    private static IllegalStateException renderUntilThrows(TestScreen screen, RecordingBackend backend) {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                screen.render(backend, 100, 100, RESOLVER);
            } catch (IllegalStateException e) {
                return e;
            }
            Thread.onSpinWait();
        }
        return Assertions.fail("render() did not surface the layout failure within " + TIMEOUT);
    }

    private static void renderUntilRendered(TestScreen screen, RecordingBackend backend, List<Component> expected) {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            screen.render(backend, 100, 100, RESOLVER);
            if (expected.equals(backend.lastRendered())) {
                return;
            }
            Thread.onSpinWait();
        }
        Assertions.fail("render() did not render the layout result within " + TIMEOUT);
    }

    private static final class TestScreen extends LorisScreen {
        private final Supplier<List<Component>> layout;

        private TestScreen(Supplier<List<Component>> layout) {
            this.layout = layout;
        }

        @Override
        public void onStartScreen() {}

        @Override
        public void onEndScreen() {}

        @Override
        protected List<Component> layout() {
            return layout.get();
        }
    }

    private static final class RecordingBackend implements Backend {
        private final List<List<Component>> rendered = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void render(int w, int h, List<Component> components, BundleKeyResolver bundleKeyResolver) {
            rendered.add(components);
        }

        private List<Component> lastRendered() {
            return rendered.getLast();
        }

        @Override
        public void renderTexture(int id, ImageMode imageMode, int width, int height, int textureWidth,
                                  int textureHeight) {}

        @Override
        public TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) {
            throw new UnsupportedOperationException();
        }
    }
}
