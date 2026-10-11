package com.ivan1pl.lorisui.api.screen;

import com.ivan1pl.lorisui.api.component.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class ScreenManagerTest {
    private final ScreenManager manager = new ScreenManager();
    private final List<String> events = new ArrayList<>();

    @Test
    void shouldRejectSetCurrentScreenFromOnStartScreenDuringSwitch() {
        AtomicReference<RuntimeException> nested = new AtomicReference<>();
        manager.addScreen("a", new RecordingScreen("a", null, null));
        manager.addScreen("b", new RecordingScreen("b", () -> nested.set(trySetCurrentScreen("c")), null));
        manager.addScreen("c", new RecordingScreen("c", null, null));
        manager.setCurrentScreen("a");
        manager.activate();

        manager.setCurrentScreen("b");
        manager.deactivate();

        Assertions.assertInstanceOf(IllegalStateException.class, nested.get());
        Assertions.assertEquals(List.of("start:a", "end:a", "start:b", "end:b"), events);
    }

    @Test
    void shouldRejectSetCurrentScreenFromOnEndScreenDuringSwitch() {
        AtomicReference<RuntimeException> nested = new AtomicReference<>();
        manager.addScreen("a", new RecordingScreen("a", null, () -> nested.set(trySetCurrentScreen("c"))));
        manager.addScreen("b", new RecordingScreen("b", null, null));
        manager.addScreen("c", new RecordingScreen("c", null, null));
        manager.setCurrentScreen("a");
        manager.activate();

        manager.setCurrentScreen("b");
        manager.deactivate();

        Assertions.assertInstanceOf(IllegalStateException.class, nested.get());
        Assertions.assertEquals(List.of("start:a", "end:a", "start:b", "end:b"), events);
    }

    @Test
    void shouldRejectSetCurrentScreenFromOnStartScreenDuringActivate() {
        AtomicReference<RuntimeException> nested = new AtomicReference<>();
        manager.addScreen("a", new RecordingScreen("a", () -> nested.set(trySetCurrentScreen("b")), null));
        manager.addScreen("b", new RecordingScreen("b", null, null));
        manager.setCurrentScreen("a");

        manager.activate();
        manager.deactivate();

        Assertions.assertInstanceOf(IllegalStateException.class, nested.get());
        Assertions.assertEquals(List.of("start:a", "end:a"), events);
    }

    @Test
    void shouldRejectSetCurrentScreenFromOnEndScreenDuringDeactivate() {
        AtomicReference<RuntimeException> nested = new AtomicReference<>();
        manager.addScreen("a", new RecordingScreen("a", null, () -> nested.set(trySetCurrentScreen("b"))));
        manager.addScreen("b", new RecordingScreen("b", null, null));
        manager.setCurrentScreen("a");
        manager.activate();

        manager.deactivate();

        Assertions.assertInstanceOf(IllegalStateException.class, nested.get());
        Assertions.assertEquals(List.of("start:a", "end:a"), events);
    }

    @Test
    void shouldAllowSetCurrentScreenAgainAfterHooksReturn() {
        manager.addScreen("a", new RecordingScreen("a", null, null));
        manager.addScreen("b", new RecordingScreen("b", null, null));
        manager.setCurrentScreen("a");
        manager.activate();

        manager.setCurrentScreen("b");
        manager.setCurrentScreen("a");

        Assertions.assertEquals(List.of("start:a", "end:a", "start:b", "end:b", "start:a"), events);
    }

    @Test
    void shouldRejectActivateWithoutCurrentScreen() {
        manager.addScreen("a", new RecordingScreen("a", null, null));

        Assertions.assertThrows(IllegalStateException.class, manager::activate);

        Assertions.assertEquals(List.of(), events);
        manager.setCurrentScreen("a");
        Assertions.assertEquals(List.of(), events);
    }

    @Test
    void shouldOnlyRecordCurrentScreenWhileInactiveAndStartItOnActivate() {
        RecordingScreen screenB = new RecordingScreen("b", null, null);
        manager.addScreen("a", new RecordingScreen("a", null, null));
        manager.addScreen("b", screenB);

        manager.setCurrentScreen("a");
        manager.setCurrentScreen("b");

        Assertions.assertEquals(List.of(), events);
        Assertions.assertSame(screenB, manager.getCurrentScreen());

        manager.activate();

        Assertions.assertEquals(List.of("start:b"), events);
    }

    @Test
    void shouldCompleteSwitchWhenOnEndScreenThrows() {
        IllegalArgumentException endFailure = new IllegalArgumentException("end");
        UnsupportedOperationException startFailure = new UnsupportedOperationException("start");
        AtomicReference<LorisScreen> currentDuringEnd = new AtomicReference<>();
        RecordingScreen screenB = new RecordingScreen("b", () -> {
            throw startFailure;
        }, null);
        manager.addScreen("a", new RecordingScreen("a", null, () -> {
            currentDuringEnd.set(manager.getCurrentScreen());
            throw endFailure;
        }));
        manager.addScreen("b", screenB);
        manager.setCurrentScreen("a");
        manager.activate();

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> manager.setCurrentScreen("b"));

        Assertions.assertSame(endFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[] {startFailure}, thrown.getSuppressed());
        Assertions.assertSame(screenB, currentDuringEnd.get());
        Assertions.assertSame(screenB, manager.getCurrentScreen());
        Assertions.assertEquals(List.of("start:a", "end:a", "start:b"), events);
    }

    @Test
    void shouldNotEndScreenWhoseStartThrewOnDeactivate() {
        IllegalArgumentException startFailure = new IllegalArgumentException("start");
        manager.addScreen("a", new RecordingScreen("a", () -> {
            throw startFailure;
        }, null));
        manager.setCurrentScreen("a");

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, manager::activate);
        manager.deactivate();

        Assertions.assertSame(startFailure, thrown);
        Assertions.assertEquals(List.of("start:a"), events);
    }

    @Test
    void shouldRejectDeactivateFromHookAndStayActive() {
        AtomicReference<RuntimeException> nested = new AtomicReference<>();
        manager.addScreen("a", new RecordingScreen("a", () -> nested.set(tryRun(manager::deactivate)), null));
        manager.setCurrentScreen("a");

        manager.activate();
        manager.deactivate();
        manager.deactivate();

        Assertions.assertInstanceOf(IllegalStateException.class, nested.get());
        Assertions.assertEquals(List.of("start:a", "end:a"), events);
    }

    @Test
    void shouldRejectActivateFromHook() {
        AtomicReference<RuntimeException> nested = new AtomicReference<>();
        manager.addScreen("a", new RecordingScreen("a", null, () -> nested.set(tryRun(manager::activate))));
        manager.setCurrentScreen("a");
        manager.activate();

        manager.deactivate();

        Assertions.assertInstanceOf(IllegalStateException.class, nested.get());
        Assertions.assertEquals(List.of("start:a", "end:a"), events);
    }

    @Test
    void shouldRestartCurrentScreenAfterDeactivateAndActivate() {
        manager.addScreen("a", new RecordingScreen("a", null, null));
        manager.setCurrentScreen("a");
        manager.activate();

        manager.deactivate();
        manager.activate();

        Assertions.assertEquals(List.of("start:a", "end:a", "start:a"), events);
    }

    @Test
    void shouldRejectDuplicateAndNullScreenRegistrationAndKeepOriginalScreen() {
        RecordingScreen original = new RecordingScreen("a", null, null);
        manager.addScreen("a", original);

        IllegalStateException duplicate = Assertions.assertThrows(IllegalStateException.class,
                () -> manager.addScreen("a", new RecordingScreen("other", null, null)));
        Assertions.assertThrows(NullPointerException.class,
                () -> manager.addScreen(null, new RecordingScreen("b", null, null)));
        Assertions.assertThrows(NullPointerException.class, () -> manager.addScreen("b", null));

        Assertions.assertTrue(duplicate.getMessage().contains("a"), duplicate.getMessage());
        manager.setCurrentScreen("a");
        Assertions.assertSame(original, manager.getCurrentScreen());
        Assertions.assertThrows(IllegalStateException.class, () -> manager.setCurrentScreen("b"));
    }

    @Test
    void shouldRejectUnknownScreenBeforeChangingStateOrFiringHooks() {
        RecordingScreen screenA = new RecordingScreen("a", null, null);
        manager.addScreen("a", screenA);
        manager.setCurrentScreen("a");
        manager.activate();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> manager.setCurrentScreen("missing"));
        Assertions.assertThrows(NullPointerException.class, () -> manager.setCurrentScreen(null));

        Assertions.assertTrue(thrown.getMessage().contains("missing"), thrown.getMessage());
        Assertions.assertSame(screenA, manager.getCurrentScreen());
        manager.deactivate();
        Assertions.assertEquals(List.of("start:a", "end:a"), events);
    }

    @Test
    void shouldNotRestartScreenWhenSelectingCurrentScreenWhileActive() {
        manager.addScreen("a", new RecordingScreen("a", null, null));
        manager.setCurrentScreen("a");
        manager.activate();

        manager.setCurrentScreen("a");

        Assertions.assertEquals(List.of("start:a"), events);
    }

    @Test
    void shouldOnlyRecordSwitchAfterBeginDeactivationAndStartRecordedScreenOnNextActivate() {
        manager.addScreen("a", new RecordingScreen("a", null, null));
        manager.addScreen("b", new RecordingScreen("b", null, null));
        manager.setCurrentScreen("a");
        manager.activate();

        manager.beginDeactivation();
        manager.setCurrentScreen("b");
        Assertions.assertEquals(List.of("start:a"), events);

        manager.deactivate();
        manager.activate();
        Assertions.assertEquals(List.of("start:a", "end:a", "start:b"), events);
    }

    @Test
    void shouldValidateSwitchArgumentsAfterBeginDeactivation() {
        manager.addScreen("a", new RecordingScreen("a", null, null));
        manager.setCurrentScreen("a");
        manager.activate();
        manager.beginDeactivation();

        Assertions.assertThrows(NullPointerException.class, () -> manager.setCurrentScreen(null));
        Assertions.assertThrows(IllegalStateException.class, () -> manager.setCurrentScreen("missing"));

        manager.deactivate();
        Assertions.assertEquals(List.of("start:a", "end:a"), events);
    }

    @Test
    void shouldAllowBeginDeactivationFromHookAndRejectActivateUntilDeactivated() {
        AtomicReference<RuntimeException> nested = new AtomicReference<>();
        manager.addScreen("a", new RecordingScreen("a", () -> {
            manager.beginDeactivation();
            nested.set(trySetCurrentScreen("b"));
        }, null));
        RecordingScreen screenB = new RecordingScreen("b", null, null);
        manager.addScreen("b", screenB);
        manager.setCurrentScreen("a");

        manager.activate();
        RuntimeException earlyActivate = tryRun(manager::activate);
        manager.setCurrentScreen("b");
        manager.deactivate();

        Assertions.assertInstanceOf(IllegalStateException.class, nested.get());
        Assertions.assertInstanceOf(IllegalStateException.class, earlyActivate);
        Assertions.assertEquals(List.of("start:a", "end:a"), events);
        Assertions.assertSame(screenB, manager.getCurrentScreen());
    }

    private RuntimeException trySetCurrentScreen(String name) {
        return tryRun(() -> manager.setCurrentScreen(name));
    }

    private static RuntimeException tryRun(Runnable action) {
        try {
            action.run();
            return null;
        } catch (RuntimeException e) {
            return e;
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
