package com.ivan1pl.lorisui.api.screen;

import com.ivan1pl.lorisui.api.component.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

// Wires a ScreenManager and a DeferredRelease together the way the LWJGL and jME displays do
class HostLifecycleCompositionTest {
    private final List<String> events = new ArrayList<>();
    private final Host host = new Host();

    @Test
    void shouldReleaseAtOnceWhenClosedOutsideAnOperation() {
        host.addScreen(new RecordingScreen("main", null, null));
        host.setCurrentScreen("main");
        host.initialize();

        host.close();
        host.close();

        Assertions.assertEquals(List.of("start:main", "end:main", "released"), events);
    }

    @Test
    void shouldReleaseAfterFrameAndOnlyRecordSwitchWhenClosedDuringFrame() {
        RecordingScreen other = new RecordingScreen("other", null, null);
        host.addScreen(new RecordingScreen("main", null, null));
        host.addScreen(other);
        host.setCurrentScreen("main");
        host.initialize();

        host.frame(() -> {
            host.close();
            events.add("pending:" + host.deferredRelease.isPending());
            host.setCurrentScreen("other");
            events.add("frame-done");
        });

        Assertions.assertEquals(List.of("start:main", "pending:true", "frame-done", "end:main", "released"), events);
        Assertions.assertFalse(host.deferredRelease.isPending());
        Assertions.assertSame(other, host.screenManager.getCurrentScreen());

        events.clear();
        host.initialize();
        host.close();

        Assertions.assertEquals(List.of("start:other", "end:other", "released"), events);
    }

    @Test
    void shouldReleaseAfterInitializeWhenClosedInOnStartScreen() {
        host.addScreen(new RecordingScreen("main", () -> {
            host.close();
            events.add("closed-in-hook");
        }, null));
        host.setCurrentScreen("main");

        host.initialize();

        Assertions.assertEquals(List.of("start:main", "closed-in-hook", "end:main", "released"), events);
        Assertions.assertFalse(host.deferredRelease.isPending());
    }

    @Test
    void shouldCompleteSwitchAndReleaseAfterOutermostFrameWhenOldScreenClosesHostInOnEndScreen() {
        host.addScreen(new RecordingScreen("main", null, host::close));
        host.addScreen(new RecordingScreen("other", null, null));
        host.setCurrentScreen("main");
        host.initialize();

        host.frame(() -> {
            host.setCurrentScreen("other");
            events.add("frame-done");
        });

        Assertions.assertEquals(
                List.of("start:main", "end:main", "start:other", "frame-done", "end:other", "released"), events);
        Assertions.assertFalse(host.deferredRelease.isPending());
    }

    // Mirrors the LWJGL displays and ImGuiJmeDisplay without the renderer and input handling: every operation that
    // can call application code runs through deferredRelease, and close() begins the deactivation before the request
    private final class Host {
        private final ScreenManager screenManager = new ScreenManager();
        private final DeferredRelease deferredRelease = new DeferredRelease(this::release);
        private boolean initialized = false;

        private void addScreen(RecordingScreen screen) {
            screenManager.addScreen(screen.name, screen);
        }

        private void setCurrentScreen(String name) {
            deferredRelease.run(() -> screenManager.setCurrentScreen(name));
        }

        private void initialize() {
            initialized = true;
            deferredRelease.run(screenManager::activate);
        }

        private void frame(Runnable componentCallbacks) {
            deferredRelease.run(componentCallbacks);
        }

        private void close() {
            if (!initialized) {
                return;
            }
            initialized = false;
            screenManager.beginDeactivation();
            deferredRelease.request();
        }

        private void release() {
            DeferredRelease.runAll(
                    screenManager::deactivate,
                    () -> events.add("released"));
        }
    }

    private final class RecordingScreen extends LorisScreen {
        private final String name;
        private final Runnable onStart;
        private final Runnable onEnd;

        private RecordingScreen(String name, Runnable onStart, Runnable onEnd) {
            this.name = name;
            this.onStart = onStart;
            this.onEnd = onEnd;
        }

        @Override
        public void onStartScreen() {
            events.add("start:" + name);
            if (onStart != null) {
                onStart.run();
            }
        }

        @Override
        public void onEndScreen() {
            events.add("end:" + name);
            if (onEnd != null) {
                onEnd.run();
            }
        }

        @Override
        protected List<Component> layout() {
            return List.of();
        }
    }
}
