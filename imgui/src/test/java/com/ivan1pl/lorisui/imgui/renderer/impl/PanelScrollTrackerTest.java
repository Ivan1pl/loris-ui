package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Panel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class PanelScrollTrackerTest {
    private static final int ID_A = 101;
    private static final int ID_B = 202;
    private static final int ID_C = 303;
    private static final float NO_TARGET = -1f;
    private static final float DELTA = 1e-6f;
    private static final long TIMEOUT_MILLIS = 5_000;

    @Test
    void shouldReportClampedRatioOrZeroWhenUpdated() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");

        tracker.update(panel, ID_A, 1, 25f, 100f);
        float inRange = panel.getScrollPosition();
        tracker.update(panel, ID_A, 2, 150f, 100f);
        float aboveRange = panel.getScrollPosition();
        tracker.update(panel, ID_A, 3, -5f, 100f);
        float belowRange = panel.getScrollPosition();
        tracker.update(panel, ID_A, 4, 30f, 0f);
        float contentFits = panel.getScrollPosition();

        Assertions.assertEquals(0.25f, inRange, DELTA);
        Assertions.assertEquals(1f, aboveRange, DELTA);
        Assertions.assertEquals(0f, belowRange, DELTA);
        Assertions.assertEquals(0f, contentFits, DELTA);
    }

    @Test
    void shouldReturnNoTargetUntilThirdConsecutiveFrame() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        panel.setScrollPosition(1f);

        float firstFrame = draw(tracker, panel, ID_A, 1, 0f, 0f);
        float secondFrame = draw(tracker, panel, ID_A, 2, 0f, 200f);
        float thirdFrame = tracker.scrollTarget(panel, ID_A, 3);

        Assertions.assertEquals(NO_TARGET, firstFrame, DELTA);
        Assertions.assertEquals(NO_TARGET, secondFrame, DELTA);
        Assertions.assertEquals(200f, thirdFrame, DELTA);
        Assertions.assertTrue(panel.requestsScrollPosition());
    }

    @Test
    void shouldKeepRangeKnownWhenSameIdDrawnTwiceInFrame() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        panel.setScrollPosition(1f);
        draw(tracker, panel, ID_A, 1, 0f, 100f);
        draw(tracker, panel, ID_A, 2, 0f, 200f);

        float firstDraw = draw(tracker, panel, ID_A, 3, 200f, 300f);
        float secondDraw = draw(tracker, panel, ID_A, 3, 0f, 50f);
        float reportedBySecondDraw = panel.getScrollPosition();
        float nextFrame = tracker.scrollTarget(panel, ID_A, 4);

        Assertions.assertEquals(200f, firstDraw, DELTA);
        Assertions.assertEquals(NO_TARGET, secondDraw, DELTA);
        Assertions.assertEquals(0f, reportedBySecondDraw, DELTA);
        Assertions.assertEquals(300f, nextFrame, DELTA);
    }

    @Test
    void shouldRestartWaitWhenFrameSkipped() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        panel.setScrollPosition(1f);
        draw(tracker, panel, ID_A, 1, 0f, 100f);
        draw(tracker, panel, ID_A, 2, 0f, 100f);

        float afterSkip = draw(tracker, panel, ID_A, 4, 0f, 100f);
        float secondAfterSkip = draw(tracker, panel, ID_A, 5, 0f, 150f);
        float thirdAfterSkip = tracker.scrollTarget(panel, ID_A, 6);

        Assertions.assertEquals(NO_TARGET, afterSkip, DELTA);
        Assertions.assertEquals(NO_TARGET, secondAfterSkip, DELTA);
        Assertions.assertEquals(150f, thirdAfterSkip, DELTA);
    }

    @Test
    void shouldRestartWaitWhenFrameNumberDecreases() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        Panel other = panel("b");
        draw(tracker, panel, ID_A, 1, 0f, 100f);
        draw(tracker, panel, ID_A, 2, 0f, 100f);
        draw(tracker, panel, ID_A, 3, 0f, 100f);
        draw(tracker, other, ID_B, 4, 0f, 100f);
        panel.setScrollPosition(1f);

        float firstNewFrame = draw(tracker, panel, ID_A, 3, 0f, 200f);
        float secondNewFrame = draw(tracker, panel, ID_A, 4, 0f, 300f);
        float thirdNewFrame = tracker.scrollTarget(panel, ID_A, 5);

        Assertions.assertEquals(NO_TARGET, firstNewFrame, DELTA);
        Assertions.assertEquals(NO_TARGET, secondNewFrame, DELTA);
        Assertions.assertEquals(300f, thirdNewFrame, DELTA);
    }

    @Test
    void shouldApplyNothingStaleWhenScrollTargetHasNoUpdate() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        Panel sameId = panel("b");
        draw(tracker, panel, ID_A, 1, 0f, 100f);
        draw(tracker, panel, ID_A, 2, 0f, 100f);
        panel.setScrollPosition(1f);

        float notOpened = tracker.scrollTarget(panel, ID_A, 3);
        float afterNotOpened = tracker.scrollTarget(panel, ID_A, 4);
        float withoutRequest = tracker.scrollTarget(sameId, ID_A, 4);
        tracker.update(sameId, ID_A, 4, 0f, 100f);
        float secondFrame = draw(tracker, panel, ID_A, 5, 100f, 100f);
        boolean pendingAfterSecondFrame = panel.requestsScrollPosition();
        float thirdFrame = tracker.scrollTarget(panel, ID_A, 6);

        Assertions.assertEquals(100f, notOpened, DELTA);
        Assertions.assertEquals(NO_TARGET, afterNotOpened, DELTA);
        Assertions.assertEquals(NO_TARGET, withoutRequest, DELTA);
        Assertions.assertEquals(NO_TARGET, secondFrame, DELTA);
        Assertions.assertTrue(pendingAfterSecondFrame);
        Assertions.assertEquals(100f, thirdFrame, DELTA);
    }

    @Test
    void shouldRestartWaitWhenIdNotDrawnInFrame() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel hidden = panel("a");
        Panel shown = panel("b");
        draw(tracker, hidden, ID_A, 1, 0f, 100f);
        draw(tracker, shown, ID_B, 1, 0f, 100f);
        draw(tracker, hidden, ID_A, 2, 0f, 100f);
        draw(tracker, shown, ID_B, 2, 0f, 100f);
        hidden.setScrollPosition(1f);
        shown.setScrollPosition(1f);

        float shownTarget = draw(tracker, shown, ID_B, 3, 100f, 100f);
        float firstAfterHidden = draw(tracker, hidden, ID_A, 4, 0f, 100f);
        float secondAfterHidden = draw(tracker, hidden, ID_A, 5, 0f, 100f);
        boolean pendingAfterSecond = hidden.requestsScrollPosition();
        float thirdAfterHidden = tracker.scrollTarget(hidden, ID_A, 6);

        Assertions.assertEquals(100f, shownTarget, DELTA);
        Assertions.assertEquals(NO_TARGET, firstAfterHidden, DELTA);
        Assertions.assertEquals(NO_TARGET, secondAfterHidden, DELTA);
        Assertions.assertTrue(pendingAfterSecond);
        Assertions.assertEquals(100f, thirdAfterHidden, DELTA);
    }

    @Test
    void shouldDiscardJudgmentWhenFrameChangesBeforeUpdate() {
        PanelScrollTracker newContext = new PanelScrollTracker();
        Panel restarted = panel("a");
        draw(newContext, restarted, ID_A, 1, 0f, 100f);
        draw(newContext, restarted, ID_A, 2, 0f, 100f);
        restarted.setScrollPosition(1f);
        float restartedTarget = newContext.scrollTarget(restarted, ID_A, 3);
        newContext.update(restarted, ID_A, 1, 40f, 100f);

        PanelScrollTracker nextFrame = new PanelScrollTracker();
        Panel advanced = panel("b");
        draw(nextFrame, advanced, ID_A, 1, 0f, 100f);
        draw(nextFrame, advanced, ID_A, 2, 0f, 100f);
        advanced.setScrollPosition(1f);
        float advancedTarget = nextFrame.scrollTarget(advanced, ID_A, 3);
        nextFrame.update(advanced, ID_A, 4, 40f, 100f);

        Assertions.assertEquals(100f, restartedTarget, DELTA);
        Assertions.assertTrue(restarted.requestsScrollPosition(), "a lower frame is not taken for a drag");
        Assertions.assertEquals(100f, advancedTarget, DELTA);
        Assertions.assertTrue(advanced.requestsScrollPosition(), "a later frame is not taken for a drag");
    }

    @Test
    void shouldClearWhenOffsetWithinHalfPixelOfTarget() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        panel.setScrollPosition(0.5f);
        draw(tracker, panel, ID_A, 1, 0f, 200f);
        draw(tracker, panel, ID_A, 2, 0f, 200f);

        float target = tracker.scrollTarget(panel, ID_A, 3);
        tracker.update(panel, ID_A, 3, 100.5f, 200f);

        Assertions.assertEquals(100f, target, DELTA);
        Assertions.assertFalse(panel.requestsScrollPosition());
    }

    @Test
    void shouldKeepPendingWhenOffsetOnePixelFromTarget() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        panel.setScrollPosition(0.5f);
        draw(tracker, panel, ID_A, 1, 0f, 200f);
        draw(tracker, panel, ID_A, 2, 0f, 200f);

        float target = tracker.scrollTarget(panel, ID_A, 3);
        tracker.update(panel, ID_A, 3, 100f, 202f);

        Assertions.assertEquals(100f, target, DELTA);
        Assertions.assertTrue(panel.requestsScrollPosition());
    }

    @Test
    void shouldNotClearInFirstFrameRequestIsSeen() {
        PanelScrollTracker atOldBottom = new PanelScrollTracker();
        Panel log = panel("log");
        draw(atOldBottom, log, ID_A, 1, 100f, 100f);
        draw(atOldBottom, log, ID_A, 2, 100f, 100f);
        log.setScrollPosition(1f);
        float oldBottomTarget = draw(atOldBottom, log, ID_A, 3, 100f, 100f);
        boolean pendingAtOldBottom = log.requestsScrollPosition();

        PanelScrollTracker overflowing = new PanelScrollTracker();
        Panel growing = panel("growing");
        draw(overflowing, growing, ID_B, 1, 0f, 0f);
        draw(overflowing, growing, ID_B, 2, 0f, 0f);
        growing.setScrollPosition(1f);
        float fitsTarget = draw(overflowing, growing, ID_B, 3, 0f, 0f);
        boolean pendingWhileFits = growing.requestsScrollPosition();
        float startsToOverflowTarget = draw(overflowing, growing, ID_B, 4, 0f, 150f);
        boolean pendingWhenOverflowStarts = growing.requestsScrollPosition();
        float appliedTarget = draw(overflowing, growing, ID_B, 5, 150f, 150f);

        Assertions.assertEquals(100f, oldBottomTarget, DELTA);
        Assertions.assertTrue(pendingAtOldBottom);
        Assertions.assertEquals(NO_TARGET, fitsTarget, DELTA);
        Assertions.assertTrue(pendingWhileFits);
        Assertions.assertEquals(NO_TARGET, startsToOverflowTarget, DELTA);
        Assertions.assertTrue(pendingWhenOverflowStarts);
        Assertions.assertEquals(150f, appliedTarget, DELTA);
        Assertions.assertFalse(growing.requestsScrollPosition());
    }

    @Test
    void shouldClearWithoutTargetWhenContentFitsAtEitherEnd() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel top = panel("top");
        Panel middle = panel("middle");
        Panel bottom = panel("bottom");
        top.setScrollPosition(0f);
        middle.setScrollPosition(0.5f);
        bottom.setScrollPosition(1f);

        float topFirst = draw(tracker, top, ID_A, 1, 0f, 0f);
        float middleFirst = draw(tracker, middle, ID_C, 1, 0f, 0f);
        float bottomFirst = draw(tracker, bottom, ID_B, 1, 0f, 0f);
        boolean topPendingAfterFirst = top.requestsScrollPosition();
        boolean middlePendingAfterFirst = middle.requestsScrollPosition();
        boolean bottomPendingAfterFirst = bottom.requestsScrollPosition();
        float topSecond = draw(tracker, top, ID_A, 2, 0f, 0f);
        float middleSecond = draw(tracker, middle, ID_C, 2, 0f, 0f);
        float bottomSecond = draw(tracker, bottom, ID_B, 2, 0f, 0f);

        Assertions.assertEquals(NO_TARGET, topFirst, DELTA);
        Assertions.assertEquals(NO_TARGET, middleFirst, DELTA);
        Assertions.assertEquals(NO_TARGET, bottomFirst, DELTA);
        Assertions.assertTrue(topPendingAfterFirst);
        Assertions.assertTrue(middlePendingAfterFirst);
        Assertions.assertTrue(bottomPendingAfterFirst);
        Assertions.assertEquals(NO_TARGET, topSecond, DELTA);
        Assertions.assertEquals(NO_TARGET, middleSecond, DELTA);
        Assertions.assertEquals(NO_TARGET, bottomSecond, DELTA);
        Assertions.assertFalse(top.requestsScrollPosition());
        Assertions.assertFalse(middle.requestsScrollPosition());
        Assertions.assertFalse(bottom.requestsScrollPosition());
    }

    @Test
    void shouldApplyZeroTargetWhenTopRequested() {
        PanelScrollTracker reached = new PanelScrollTracker();
        Panel panel = panel("a");
        draw(reached, panel, ID_A, 1, 100f, 100f);
        draw(reached, panel, ID_A, 2, 100f, 100f);
        panel.setScrollPosition(0f);
        float firstTarget = draw(reached, panel, ID_A, 3, 0f, 100f);
        boolean pendingAfterFirst = panel.requestsScrollPosition();
        float secondTarget = draw(reached, panel, ID_A, 4, 0f, 100f);

        PanelScrollTracker dragged = new PanelScrollTracker();
        Panel draggedPanel = panel("b");
        draw(dragged, draggedPanel, ID_B, 1, 100f, 100f);
        draw(dragged, draggedPanel, ID_B, 2, 100f, 100f);
        draggedPanel.setScrollPosition(0f);
        float draggedTarget = draw(dragged, draggedPanel, ID_B, 3, 40f, 100f);

        Assertions.assertEquals(0f, firstTarget, DELTA);
        Assertions.assertTrue(pendingAfterFirst);
        Assertions.assertEquals(0f, secondTarget, DELTA);
        Assertions.assertFalse(panel.requestsScrollPosition());
        Assertions.assertEquals(0f, draggedTarget, DELTA);
        Assertions.assertFalse(draggedPanel.requestsScrollPosition(), "a drag away from a zero target clears");
    }

    @Test
    void shouldNotClearInFirstFrameRenewedRequestIsSeen() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        draw(tracker, panel, ID_A, 1, 100f, 100f);
        draw(tracker, panel, ID_A, 2, 100f, 100f);
        panel.setScrollPosition(1f);
        draw(tracker, panel, ID_A, 3, 100f, 100f);
        boolean pendingAfterFirst = panel.requestsScrollPosition();

        renewRequest(panel, 1f);
        float renewedTarget = draw(tracker, panel, ID_A, 4, 100f, 100f);
        boolean pendingAfterRenewal = panel.requestsScrollPosition();
        float nextTarget = draw(tracker, panel, ID_A, 5, 100f, 100f);

        Assertions.assertTrue(pendingAfterFirst);
        Assertions.assertEquals(100f, renewedTarget, DELTA);
        Assertions.assertTrue(pendingAfterRenewal, "the renewed request was not pending at the previous draw");
        Assertions.assertEquals(100f, nextTarget, DELTA);
        Assertions.assertFalse(panel.requestsScrollPosition());
    }

    @Test
    void shouldReapplyWithNewerRangeWhenContentGrows() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        panel.setScrollPosition(1f);
        draw(tracker, panel, ID_A, 1, 0f, 100f);
        draw(tracker, panel, ID_A, 2, 0f, 100f);

        float shortTarget = draw(tracker, panel, ID_A, 3, 100f, 130f);
        boolean pendingAfterShortTarget = panel.requestsScrollPosition();
        float newerTarget = draw(tracker, panel, ID_A, 4, 130f, 130f);

        Assertions.assertEquals(100f, shortTarget, DELTA);
        Assertions.assertTrue(pendingAfterShortTarget);
        Assertions.assertEquals(130f, newerTarget, DELTA);
        Assertions.assertFalse(panel.requestsScrollPosition());
    }

    @Test
    void shouldClearWithoutReachingWhenUserDragsScrollbar() {
        PanelScrollTracker dragged = new PanelScrollTracker();
        Panel panel = panel("a");
        panel.setScrollPosition(1f);
        draw(dragged, panel, ID_A, 1, 0f, 100f);
        draw(dragged, panel, ID_A, 2, 0f, 100f);
        float draggedTarget = draw(dragged, panel, ID_A, 3, 40f, 100f);
        boolean pendingAfterDrag = panel.requestsScrollPosition();
        float afterDrag = dragged.scrollTarget(panel, ID_A, 4);

        PanelScrollTracker shrunk = new PanelScrollTracker();
        Panel shrinking = panel("b");
        shrinking.setScrollPosition(0.5f);
        draw(shrunk, shrinking, ID_B, 1, 0f, 200f);
        draw(shrunk, shrinking, ID_B, 2, 0f, 200f);
        float clampedTarget = draw(shrunk, shrinking, ID_B, 3, 80f, 80f);
        boolean pendingAfterClamp = shrinking.requestsScrollPosition();
        float afterClamp = draw(shrunk, shrinking, ID_B, 4, 40f, 80f);

        Assertions.assertEquals(100f, draggedTarget, DELTA);
        Assertions.assertFalse(pendingAfterDrag);
        Assertions.assertEquals(NO_TARGET, afterDrag, DELTA);
        Assertions.assertEquals(100f, clampedTarget, DELTA);
        Assertions.assertTrue(pendingAfterClamp);
        Assertions.assertEquals(40f, afterClamp, DELTA);
        Assertions.assertFalse(shrinking.requestsScrollPosition());
    }

    @Test
    void shouldKeepRequestRenewedBetweenScrollTargetAndUpdate() {
        PanelScrollTracker tracker = new PanelScrollTracker();
        Panel panel = panel("a");
        panel.setScrollPosition(1f);
        draw(tracker, panel, ID_A, 1, 0f, 100f);
        draw(tracker, panel, ID_A, 2, 0f, 100f);

        float target = tracker.scrollTarget(panel, ID_A, 3);
        renewRequest(panel, 0.5f);
        tracker.update(panel, ID_A, 3, 100f, 100f);

        Assertions.assertEquals(100f, target, DELTA);
        Assertions.assertTrue(panel.requestsScrollPosition());
        Assertions.assertEquals(0.5f, panel.getRequestedScrollPosition(), DELTA);
    }

    private static float draw(PanelScrollTracker tracker, Panel panel, int id, int frame, float scrollY,
                              float scrollMaxY) {
        float target = tracker.scrollTarget(panel, id, frame);
        tracker.update(panel, id, frame, scrollY, scrollMaxY);
        return target;
    }

    // Spins until the clock has moved past the pending stamp, so the renewed request cannot get an equal stamp
    private static void renewRequest(Panel panel, float position) {
        long firstStamp = panel.getScrollPositionRequestTime();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
        while (System.nanoTime() - firstStamp <= 0 && deadline - System.nanoTime() > 0) {
            Thread.onSpinWait();
        }
        panel.setScrollPosition(position);
        Assertions.assertNotEquals(firstStamp, panel.getScrollPositionRequestTime(),
                "the renewed request has a new stamp");
    }

    private static Panel panel(String id) {
        return Panel.panelBuilder(id)
                .size(10, 10)
                .build();
    }
}
