package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class ButtonInputTrackerTest {
    private static final int ID_A = 101;
    private static final int ID_B = 202;

    @Test
    void shouldClickButtonWhenPressedAndReleasedOverIt() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger clicks = new AtomicInteger();
        Button a = button("a", clicks);

        tracker.update(a, ID_A, 1, true, true, true, false);
        tracker.update(a, ID_A, 2, true, false, true, false);
        tracker.update(a, ID_A, 3, true, false, false, true);

        Assertions.assertEquals(1, clicks.get());
    }

    @Test
    void shouldNotClickButtonReleasedOverWhenPressWasOnAnotherButton() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger clicksA = new AtomicInteger();
        AtomicInteger clicksB = new AtomicInteger();
        Button a = button("a", clicksA);
        Button b = button("b", clicksB);

        tracker.update(a, ID_A, 1, true, true, true, false);
        tracker.update(b, ID_B, 1, false, true, true, false);
        tracker.update(a, ID_A, 2, false, false, true, false);
        tracker.update(b, ID_B, 2, true, false, true, false);
        tracker.update(a, ID_A, 3, false, false, false, true);
        tracker.update(b, ID_B, 3, true, false, false, true);

        Assertions.assertEquals(0, clicksA.get());
        Assertions.assertEquals(0, clicksB.get());
    }

    @Test
    void shouldNotClickButtonWhenPressStartedOutsideAnyButton() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger clicks = new AtomicInteger();
        Button a = button("a", clicks);

        tracker.update(a, ID_A, 1, false, true, true, false);
        tracker.update(a, ID_A, 2, true, false, true, false);
        tracker.update(a, ID_A, 3, true, false, false, true);

        Assertions.assertEquals(0, clicks.get());
    }

    @Test
    void shouldNotShowActiveStateWhenDraggedOntoButtonPressedElsewhere() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        Button a = button("a", new AtomicInteger());

        tracker.update(a, ID_A, 1, false, true, true, false);

        Assertions.assertFalse(tracker.update(a, ID_A, 2, true, false, true, false));
    }

    @Test
    void shouldClickOnlyInnermostOfNestedButtons() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger outerClicks = new AtomicInteger();
        AtomicInteger innerClicks = new AtomicInteger();
        Button outer = button("outer", outerClicks);
        Button inner = button("inner", innerClicks);

        tracker.update(outer, ID_A, 1, true, true, true, false);
        tracker.update(inner, ID_B, 1, true, true, true, false);
        tracker.update(outer, ID_A, 2, true, false, false, true);
        tracker.update(inner, ID_B, 2, true, false, false, true);

        Assertions.assertEquals(0, outerClicks.get());
        Assertions.assertEquals(1, innerClicks.get());
    }

    @Test
    void shouldFireHoverOnceWhileButtonStaysHovered() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger hovers = new AtomicInteger();
        Button a = hoverButton("a", hovers, new AtomicInteger(), new AtomicBoolean(true));

        tracker.update(a, ID_A, 1, true, false, false, false);
        tracker.update(a, ID_A, 2, true, false, false, false);
        tracker.update(a, ID_A, 3, true, false, false, false);

        Assertions.assertEquals(1, hovers.get());
    }

    @Test
    void shouldFireHoverAgainWhenPointerLeavesAndReturns() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger hovers = new AtomicInteger();
        Button a = hoverButton("a", hovers, new AtomicInteger(), new AtomicBoolean(true));

        tracker.update(a, ID_A, 1, true, false, false, false);
        tracker.update(a, ID_A, 2, false, false, false, false);
        tracker.update(a, ID_A, 3, true, false, false, false);

        Assertions.assertEquals(2, hovers.get());
    }

    @Test
    void shouldFireHoverAgainAfterFrameInWhichButtonWasNotRendered() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger hovers = new AtomicInteger();
        Button a = hoverButton("a", hovers, new AtomicInteger(), new AtomicBoolean(true));
        Button b = button("b", new AtomicInteger());

        tracker.update(a, ID_A, 1, true, false, false, false);
        tracker.update(b, ID_B, 2, false, false, false, false);
        tracker.update(a, ID_A, 3, true, false, false, false);
        tracker.update(a, ID_A, 5, true, false, false, false);

        Assertions.assertEquals(3, hovers.get());
    }

    @Test
    void shouldKeepHoverAliveWhilePressIsHeld() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger hovers = new AtomicInteger();
        AtomicInteger clicks = new AtomicInteger();
        Button a = hoverButton("a", hovers, clicks, new AtomicBoolean(true));

        tracker.update(a, ID_A, 1, true, true, true, false);
        tracker.update(a, ID_A, 2, false, false, true, false);
        tracker.update(a, ID_A, 3, true, false, true, false);
        tracker.update(a, ID_A, 4, true, false, false, true);

        Assertions.assertEquals(1, hovers.get());
        Assertions.assertEquals(1, clicks.get());
    }

    @Test
    void shouldRunHoverActionOnlyWhenHoverStartsWhileEnabled() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger hovers = new AtomicInteger();
        AtomicBoolean enabled = new AtomicBoolean(false);
        Button a = hoverButton("a", hovers, new AtomicInteger(), enabled);

        tracker.update(a, ID_A, 1, true, false, false, false);
        int hoversWhileDisabled = hovers.get();
        tracker.update(a, ID_A, 2, false, false, false, false);
        enabled.set(true);
        tracker.update(a, ID_A, 3, true, false, false, false);

        Assertions.assertEquals(0, hoversWhileDisabled);
        Assertions.assertEquals(1, hovers.get());
    }

    @Test
    void shouldClickWhenDraggedOffAndBackOntoPressedButton() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger clicks = new AtomicInteger();
        Button a = hoverButton("a", new AtomicInteger(), clicks, new AtomicBoolean(true));

        boolean activeOnPress = tracker.update(a, ID_A, 1, true, true, true, false);
        boolean activeWhileOff = tracker.update(a, ID_A, 2, false, false, true, false);
        boolean activeWhenBack = tracker.update(a, ID_A, 3, true, false, true, false);
        boolean activeOnRelease = tracker.update(a, ID_A, 4, true, false, false, true);

        Assertions.assertTrue(activeOnPress);
        Assertions.assertFalse(activeWhileOff);
        Assertions.assertTrue(activeWhenBack);
        Assertions.assertFalse(activeOnRelease);
        Assertions.assertEquals(1, clicks.get());
    }

    @Test
    void shouldNotClickWhenPressWasRecordedBeforeSkippedFrame() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger clicks = new AtomicInteger();
        Button a = hoverButton("a", new AtomicInteger(), clicks, new AtomicBoolean(true));

        tracker.update(a, ID_A, 1, true, true, true, false);
        tracker.update(a, ID_A, 3, true, false, true, false);
        tracker.update(a, ID_A, 4, true, false, false, true);

        Assertions.assertEquals(0, clicks.get());
    }

    @Test
    void shouldClickOnlyOnceWhenTwoButtonsShareId() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger firstClicks = new AtomicInteger();
        AtomicInteger secondClicks = new AtomicInteger();
        Button first = button("first", firstClicks);
        Button second = button("second", secondClicks);

        tracker.update(first, ID_A, 1, true, true, true, false);
        tracker.update(second, ID_A, 1, true, true, true, false);
        tracker.update(first, ID_A, 2, true, false, false, true);
        tracker.update(second, ID_A, 2, true, false, false, true);

        Assertions.assertEquals(1, firstClicks.get() + secondClicks.get());
    }

    @Test
    void shouldSkipClickAndForgetPressWhenHoverActionThrows() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger clicks = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("hover failed");
        Button a = Button.buttonBuilder("a")
                .size(10, 10)
                .action(clicks::incrementAndGet)
                .hoverAction(() -> {
                    throw failure;
                })
                .build();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> tracker.update(a, ID_A, 1, true, true, false, true));
        tracker.update(a, ID_A, 2, true, false, false, true);

        Assertions.assertSame(failure, thrown);
        Assertions.assertEquals(0, clicks.get());
    }

    @Test
    void shouldKeepHoverAndPressAcrossFrameCounterWrap() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger hovers = new AtomicInteger();
        AtomicInteger clicks = new AtomicInteger();
        Button a = hoverButton("a", hovers, clicks, new AtomicBoolean(true));

        tracker.update(a, ID_A, Integer.MAX_VALUE, true, true, true, false);
        tracker.update(a, ID_A, Integer.MIN_VALUE, true, false, false, true);

        Assertions.assertEquals(1, hovers.get());
        Assertions.assertEquals(1, clicks.get());
    }

    @Test
    void shouldTrackHoverStartSeparatelyForNestedButtons() {
        ButtonInputTracker tracker = new ButtonInputTracker();
        AtomicInteger outerHovers = new AtomicInteger();
        AtomicInteger innerHovers = new AtomicInteger();
        Button outer = hoverButton("outer", outerHovers, new AtomicInteger(), new AtomicBoolean(true));
        Button inner = hoverButton("inner", innerHovers, new AtomicInteger(), new AtomicBoolean(true));

        tracker.update(outer, ID_A, 1, true, false, false, false);
        tracker.update(inner, ID_B, 1, true, false, false, false);
        tracker.update(outer, ID_A, 2, true, false, false, false);
        tracker.update(inner, ID_B, 2, false, false, false, false);
        tracker.update(outer, ID_A, 3, true, false, false, false);
        tracker.update(inner, ID_B, 3, true, false, false, false);

        Assertions.assertEquals(1, outerHovers.get());
        Assertions.assertEquals(2, innerHovers.get());
    }

    private static Button button(String id, AtomicInteger clicks) {
        return Button.buttonBuilder(id)
                .size(10, 10)
                .action(clicks::incrementAndGet)
                .build();
    }

    private static Button hoverButton(String id, AtomicInteger hovers, AtomicInteger clicks, AtomicBoolean enabled) {
        return Button.buttonBuilder(id)
                .size(10, 10)
                .enabled(enabled::get)
                .action(clicks::incrementAndGet)
                .hoverAction(hovers::incrementAndGet)
                .build();
    }
}
