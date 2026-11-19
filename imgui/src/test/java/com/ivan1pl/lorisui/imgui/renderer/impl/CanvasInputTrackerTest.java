package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Canvas;
import com.ivan1pl.lorisui.api.component.MouseButton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class CanvasInputTrackerTest {
    private static final int ID = 101;
    private static final float MIN_X = 100f;
    private static final float MIN_Y = 50f;
    private static final int NONE = 0;
    private static final int LEFT = CanvasInputTracker.bit(MouseButton.LEFT);
    private static final int RIGHT = CanvasInputTracker.bit(MouseButton.RIGHT);
    private static final int MIDDLE = CanvasInputTracker.bit(MouseButton.MIDDLE);

    @Test
    void shouldClickWithMappedCoordinatesOnlyWhenPressedWhileHovered() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        Canvas canvas = canvas(200, 100, 400, 200, events);
        canvas.setVisibleRegion(40, 20, 100, 50);

        pointer(tracker, canvas, 1, false, 31f, 11f, LEFT, LEFT, NONE);
        pointer(tracker, canvas, 2, false, 51f, 11f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 3, false, 51f, 11f, NONE, NONE, LEFT);
        pointer(tracker, canvas, 4, true, 31f, 11f, LEFT, LEFT, NONE);

        Assertions.assertEquals(List.of("click 55 25 LEFT"), events);
    }

    @Test
    void shouldGiveGestureToFirstPressedButtonAndIgnoreLaterPresses() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        Canvas canvas = canvas(100, 100, 100, 100, events);

        pointer(tracker, canvas, 1, true, 10f, 10f, RIGHT | MIDDLE, RIGHT | MIDDLE, NONE);
        pointer(tracker, canvas, 2, true, 12f, 10f, LEFT, LEFT | RIGHT | MIDDLE, NONE);
        pointer(tracker, canvas, 3, true, 12f, 13f, NONE, LEFT | RIGHT, MIDDLE);

        Assertions.assertEquals(List.of("click 10 10 RIGHT", "drag 12 10 2 0 RIGHT", "drag 12 13 0 3 RIGHT"), events);
    }

    @Test
    void shouldDragInWholeTruncatedPixelsAndCarryRemainder() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        Canvas canvas = canvas(100, 100, 25, 25, events);

        pointer(tracker, canvas, 1, true, 40f, 40f, LEFT, LEFT, NONE);
        pointer(tracker, canvas, 2, true, 43f, 40f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 3, true, 46f, 40f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 4, true, 39f, 40f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 5, true, 36f, 40f, NONE, LEFT, NONE);

        Assertions.assertEquals(List.of(
                "click 10 10 LEFT",
                "drag 11 10 1 0 LEFT",
                "drag 9 10 -1 0 LEFT",
                "drag 9 10 -1 0 LEFT"), events);
    }

    @Test
    void shouldReportOneDragForThreeSmallStepsOnNonIntegerScale() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        Canvas canvas = canvas(360, 100, 120, 100, events);

        pointer(tracker, canvas, 1, true, 0f, 0f, LEFT, LEFT, NONE);
        pointer(tracker, canvas, 2, true, 1f, 0f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 3, true, 2f, 0f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 4, true, 3f, 0f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 5, true, 4f, 0f, NONE, LEFT, NONE);

        Assertions.assertEquals(List.of("click 0 0 LEFT", "drag 1 0 1 0 LEFT"), events);
    }

    @Test
    void shouldKeepDraggingOutsideCanvasWithUnclampedCoordinates() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        Canvas canvas = canvas(100, 100, 50, 50, events);

        pointer(tracker, canvas, 1, true, 10f, 10f, LEFT, LEFT, NONE);
        pointer(tracker, canvas, 2, false, -30f, 250f, NONE, LEFT, NONE);

        Assertions.assertEquals(List.of("click 5 5 LEFT", "drag -15 125 -20 120 LEFT"), events);
    }

    @Test
    void shouldEndGestureWithoutEventOnReleaseOrClearedDownBit() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        Canvas canvas = canvas(100, 100, 25, 25, events);

        pointer(tracker, canvas, 1, true, 0f, 0f, LEFT, LEFT, NONE);
        pointer(tracker, canvas, 2, true, 3f, 0f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 3, true, 4f, 0f, NONE, LEFT, LEFT);
        pointer(tracker, canvas, 4, true, 12f, 0f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 5, true, 0f, 0f, LEFT, LEFT, NONE);
        pointer(tracker, canvas, 6, true, 8f, 0f, NONE, NONE, NONE);
        pointer(tracker, canvas, 7, true, 16f, 0f, NONE, LEFT, NONE);

        Assertions.assertEquals(List.of("click 0 0 LEFT", "click 0 0 LEFT"), events);
    }

    @Test
    void shouldDropGestureOnSkippedOrDisabledFrameAndIgnoreRepeatedUpdate() {
        List<String> skipped = new ArrayList<>();
        Canvas skippedCanvas = canvas(100, 100, 100, 100, skipped);
        CanvasInputTracker skippedTracker = new CanvasInputTracker();
        pointer(skippedTracker, skippedCanvas, 1, true, 0f, 0f, LEFT, LEFT, NONE);
        pointer(skippedTracker, skippedCanvas, 3, true, 10f, 0f, NONE, LEFT, NONE);
        pointer(skippedTracker, skippedCanvas, 4, true, 20f, 0f, NONE, LEFT, NONE);

        List<String> repeated = new ArrayList<>();
        Canvas repeatedCanvas = canvas(100, 100, 100, 100, repeated);
        CanvasInputTracker repeatedTracker = new CanvasInputTracker();
        pointer(repeatedTracker, repeatedCanvas, 1, true, 0f, 0f, LEFT, LEFT, NONE);
        pointer(repeatedTracker, repeatedCanvas, 1, true, 0f, 0f, LEFT, LEFT, NONE);
        pointer(repeatedTracker, repeatedCanvas, 2, true, 5f, 0f, NONE, LEFT, NONE);
        pointer(repeatedTracker, repeatedCanvas, 2, true, 15f, 0f, NONE, LEFT, NONE);

        List<String> disabled = new ArrayList<>();
        Canvas disabledCanvas = canvas(100, 100, 100, 100, disabled);
        CanvasInputTracker disabledTracker = new CanvasInputTracker();
        pointer(disabledTracker, disabledCanvas, 1, true, 0f, 0f, LEFT, LEFT, NONE);
        update(disabledTracker, disabledCanvas, 2, false, true, true, 5f, 0f, NONE, LEFT, NONE, 1f);
        pointer(disabledTracker, disabledCanvas, 3, true, 10f, 0f, NONE, LEFT, NONE);
        update(disabledTracker, disabledCanvas, 4, false, true, true, 10f, 0f, RIGHT, LEFT | RIGHT, NONE, 0f);
        pointer(disabledTracker, disabledCanvas, 5, true, 20f, 0f, NONE, LEFT | RIGHT, NONE);

        Assertions.assertEquals(List.of("click 0 0 LEFT"), skipped);
        Assertions.assertEquals(List.of("click 0 0 LEFT", "drag 5 0 5 0 LEFT"), repeated);
        Assertions.assertEquals(List.of("click 0 0 LEFT"), disabled);
    }

    @Test
    void shouldNotHandGestureToHeldButtonWhenOwnerIsReleased() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        Canvas canvas = canvas(100, 100, 100, 100, events);

        pointer(tracker, canvas, 1, true, 0f, 0f, LEFT, LEFT, NONE);
        pointer(tracker, canvas, 2, true, 5f, 0f, RIGHT, LEFT | RIGHT, NONE);
        pointer(tracker, canvas, 3, true, 10f, 0f, NONE, RIGHT, LEFT);
        pointer(tracker, canvas, 4, true, 20f, 0f, NONE, RIGHT, NONE);
        pointer(tracker, canvas, 5, true, 20f, 0f, MIDDLE, MIDDLE, RIGHT);
        pointer(tracker, canvas, 6, true, 23f, 0f, NONE, MIDDLE, NONE);

        Assertions.assertEquals(List.of(
                "click 0 0 LEFT",
                "drag 5 0 5 0 LEFT",
                "click 20 0 MIDDLE",
                "drag 23 0 3 0 MIDDLE"), events);
    }

    @Test
    void shouldMapWithRegionGivenToEachUpdateSoCallbackChangesApplyFromNextUpdate() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        AtomicReference<Canvas> zoomed = new AtomicReference<>();
        Canvas canvas = Canvas.builder()
                .size(100, 100)
                .clickAction((x, y, button) -> {
                    events.add("click " + x + " " + y + " " + button);
                    zoomed.get().setVisibleRegion(10, 20, 50, 50);
                })
                .dragAction((x, y, dx, dy, button) -> events.add(
                        "drag " + x + " " + y + " " + dx + " " + dy + " " + button))
                .wheelAction((x, y, delta) -> events.add("wheel " + x + " " + y + " " + delta))
                .build();
        zoomed.set(canvas);

        update(tracker, canvas, 1, true, true, true, 40f, 40f, LEFT, LEFT, NONE, 1f);
        pointer(tracker, canvas, 2, true, 44f, 40f, NONE, LEFT, NONE);

        Assertions.assertEquals(List.of("click 40 40 LEFT", "wheel 40 40 1.0", "drag 32 40 2 0 LEFT"), events);
    }

    @Test
    void shouldTrackCanvasesSeparatelyByIdWithinOneFrame() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> pressed = new ArrayList<>();
        List<String> other = new ArrayList<>();
        Canvas pressedCanvas = canvas(100, 100, 100, 100, pressed);
        Canvas otherCanvas = canvas(100, 100, 100, 100, other);

        updateWithId(tracker, pressedCanvas, 1, 1, true, 10f, LEFT, LEFT);
        updateWithId(tracker, otherCanvas, 2, 1, false, 10f, LEFT, LEFT);
        updateWithId(tracker, otherCanvas, 2, 2, false, 15f, NONE, LEFT);
        updateWithId(tracker, pressedCanvas, 1, 2, true, 15f, NONE, LEFT);
        updateWithId(tracker, otherCanvas, 2, 3, false, 20f, NONE, LEFT);
        updateWithId(tracker, pressedCanvas, 1, 4, true, 25f, NONE, LEFT);

        Assertions.assertEquals(List.of("click 10 10 LEFT", "drag 15 10 5 0 LEFT"), pressed);
        Assertions.assertEquals(List.of(), other);
    }

    @Test
    void shouldWheelOnlyWhileHoveredAndReanchorAfterInvalidMousePosition() {
        CanvasInputTracker tracker = new CanvasInputTracker();
        List<String> events = new ArrayList<>();
        Canvas canvas = canvas(100, 100, 100, 100, events);

        update(tracker, canvas, 1, true, true, true, 7f, 8f, NONE, NONE, NONE, 0f);
        update(tracker, canvas, 2, true, false, true, 7f, 8f, NONE, NONE, NONE, 1f);
        update(tracker, canvas, 3, true, true, true, 7f, 8f, NONE, NONE, NONE, -1.5f);
        pointer(tracker, canvas, 4, true, 0f, 0f, LEFT, LEFT, NONE);
        update(tracker, canvas, 5, true, false, false, -1000f, -1000f, NONE, LEFT, NONE, 0f);
        pointer(tracker, canvas, 6, true, 50f, 0f, NONE, LEFT, NONE);
        pointer(tracker, canvas, 7, true, 52f, 0f, NONE, LEFT, NONE);

        Assertions.assertEquals(List.of("wheel 7 8 -1.5", "click 0 0 LEFT", "drag 52 0 2 0 LEFT"), events);
    }

    private static void pointer(CanvasInputTracker tracker, Canvas canvas, int frame, boolean hovered, float offsetX,
                                float offsetY, int clicked, int down, int released) {
        update(tracker, canvas, frame, true, hovered, true, offsetX, offsetY, clicked, down, released, 0f);
    }

    private static void update(CanvasInputTracker tracker, Canvas canvas, int frame, boolean enabled, boolean hovered,
                               boolean mouseValid, float offsetX, float offsetY, int clicked, int down, int released,
                               float wheelDelta) {
        tracker.update(canvas, ID, frame, enabled, hovered, MIN_X + offsetX, MIN_Y + offsetY, mouseValid, clicked,
                down, released, wheelDelta, MIN_X, MIN_Y, canvas.getVisibleRegion());
    }

    private static void updateWithId(CanvasInputTracker tracker, Canvas canvas, int id, int frame, boolean hovered,
                                     float offsetX, int clicked, int down) {
        tracker.update(canvas, id, frame, true, hovered, MIN_X + offsetX, MIN_Y + 10f, true, clicked, down, NONE, 0f,
                MIN_X, MIN_Y, canvas.getVisibleRegion());
    }

    private static Canvas canvas(int width, int height, int resolutionWidth, int resolutionHeight,
                                 List<String> events) {
        return Canvas.builder()
                .size(width, height)
                .resolution(resolutionWidth, resolutionHeight)
                .clickAction((x, y, button) -> events.add("click " + x + " " + y + " " + button))
                .dragAction((x, y, dx, dy, button) -> events.add(
                        "drag " + x + " " + y + " " + dx + " " + dy + " " + button))
                .wheelAction((x, y, delta) -> events.add("wheel " + x + " " + y + " " + delta))
                .build();
    }
}
