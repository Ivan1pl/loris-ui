package com.ivan1pl.lorisui.imgui.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class KeyboardFocusTest {
    private static final int ID_A = 101;
    private static final int ID_B = 202;
    private static final int ID_C = 303;
    private static final int ID_D = 404;
    private static final int ID_E = 505;
    private static final int ID_COMBO = 606;
    private static final int ID_ITEM_1 = 711;
    private static final int ID_ITEM_2 = 722;
    private static final int ID_ITEM_3 = 733;
    private static final int ID_INPUT = 808;
    private static final int NO_ELEMENT = 0;
    // Scope tokens for frame(...), which registers every other token as an enabled button
    private static final int POPUP = -1;
    private static final int END_SCOPE = -2;
    private static final int FOCUS_STARTED = KeyboardFocus.FOCUS_LOOK | KeyboardFocus.KEYBOARD_FOCUS_STARTED;
    private static final long FRAME_DURATION = 10L;

    // One fake per ID, so every occurrence of an ID registers the same Focusable
    private final Map<Integer, RecordingFocusable> focusables = new HashMap<>();
    private long frameStart;

    @Test
    void shouldFocusFirstElementOnFirstTabAndLastOnFirstShiftTab() {
        KeyboardFocus forward = new KeyboardFocus();
        KeyboardFocus back = new KeyboardFocus();
        frame(forward, ID_A, ID_B, ID_C);
        frame(back, ID_A, ID_B, ID_C);

        forward.queueMove(1);
        back.queueMove(-1);

        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, frame(forward, ID_A, ID_B, ID_C));
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, frame(back, ID_A, ID_B, ID_C));
    }

    @Test
    void shouldWrapAroundInBothDirections() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(-1);
        frame(focus, ID_A, ID_B, ID_C);

        focus.queueMove(1);
        int[] afterForwardWrap = frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(-1);
        int[] afterBackWrap = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, afterForwardWrap);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterBackWrap);
    }

    @Test
    void shouldSkipDisabledButtons() {
        KeyboardFocus focus = new KeyboardFocus();
        frameWithDisabledB(focus);
        focus.queueMove(1);
        frameWithDisabledB(focus);

        focus.queueMove(1);
        int[] afterTab = frameWithDisabledB(focus);
        focus.queueMove(-1);
        int[] afterShiftTab = frameWithDisabledB(focus);

        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, afterShiftTab);
    }

    @Test
    void shouldKeepAnchorWithoutLookWhenFocusedButtonIsDisabled() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);

        int[] whileDisabled = frameWithDisabledB(focus);
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, whileDisabled);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldStartFocusAgainWhenFocusedButtonIsEnabledAgain() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A);
        focus.queueMove(1);
        int onArrival = frame(focus, ID_A)[0];
        int whileStillFocused = frame(focus, ID_A)[0];

        begin(focus);
        int whileDisabled = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, false, focusable(ID_A));
        focus.endFrame();
        int whenEnabledAgain = frame(focus, ID_A)[0];
        int afterEnabledAgain = frame(focus, ID_A)[0];

        Assertions.assertEquals(FOCUS_STARTED, onArrival);
        Assertions.assertEquals(KeyboardFocus.FOCUS_LOOK, whileStillFocused);
        Assertions.assertEquals(0, whileDisabled);
        Assertions.assertEquals(FOCUS_STARTED, whenEnabledAgain);
        Assertions.assertEquals(KeyboardFocus.FOCUS_LOOK, afterEnabledAgain);
    }

    @Test
    void shouldApplySeveralQueuedMovesAsOneNetStep() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C, ID_D);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C, ID_D);

        focus.queueMove(1);
        focus.queueMove(1);
        focus.queueMove(1);
        focus.queueMove(-1);
        int[] afterNetTwo = frame(focus, ID_A, ID_B, ID_C, ID_D);
        int[] nextFrame = frame(focus, ID_A, ID_B, ID_C, ID_D);

        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED, 0}, afterNetTwo);
        Assertions.assertArrayEquals(new int[] {0, 0, KeyboardFocus.FOCUS_LOOK, 0}, nextFrame);
    }

    @Test
    void shouldDropTabWhenOrderIsEmpty() {
        KeyboardFocus focus = new KeyboardFocus();

        focus.queueMove(1);
        int[] firstFrame = frame(focus, ID_A, ID_B);
        int[] secondFrame = frame(focus, ID_A, ID_B);

        Assertions.assertArrayEquals(new int[] {0, 0}, firstFrame);
        Assertions.assertArrayEquals(new int[] {0, 0}, secondFrame);
    }

    @Test
    void shouldClearStaleFocusWhenElementIsNotRegistered() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);

        frame(focus, ID_A, ID_C);
        int[] whenDrawnAgain = frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, whenDrawnAgain);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, afterTab);
    }

    @Test
    void shouldSetAnchorWithoutLookOrCaptureOnClick() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);

        clickFrame(focus, ID_B, ID_A, ID_B, ID_C);
        boolean captureAfterClick = focus.wantsCapture();
        int[] afterClick = frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertFalse(captureAfterClick);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, afterClick);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldSetAnchorOnClickOnDisabledButton() {
        KeyboardFocus focus = new KeyboardFocus();
        frameWithDisabledB(focus);

        begin(focus, Input.CLICK);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, false, focusable(ID_B));
        focus.reportClick(ID_B);
        focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true, focusable(ID_C));
        focus.endFrame();
        focus.queueMove(1);
        int[] afterTab = frameWithDisabledB(focus);

        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldClearFocusAndAnchorOnClickElsewhere() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);

        clickFrame(focus, NO_ELEMENT, ID_A, ID_B, ID_C);
        boolean captureAfterClick = focus.wantsCapture();
        int[] afterClick = frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertFalse(captureAfterClick);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, afterClick);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, afterTab);
    }

    @Test
    void shouldKeepLastCompletedStateAndDropConsumedMovesWhenFrameIsDiscarded() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);

        focus.queueMove(1);
        begin(focus, Input.CLICK);
        int[] inFailedFrame = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B)),
            focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true, focusable(ID_C))
        };
        focus.discardFrame();
        boolean captureAfterFailure = focus.wantsCapture();
        int[] afterFailure = frame(focus, ID_A, ID_B, ID_C);
        int[] nextFrame = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED, 0}, inFailedFrame);
        Assertions.assertTrue(captureAfterFailure);
        Assertions.assertArrayEquals(new int[] {KeyboardFocus.FOCUS_LOOK, 0, 0}, afterFailure);
        Assertions.assertArrayEquals(new int[] {KeyboardFocus.FOCUS_LOOK, 0, 0}, nextFrame);
    }

    @Test
    void shouldClearEverythingAndDropQueuedMovesOnReset() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);

        focus.queueMove(1);
        focus.reset();
        boolean captureAfterReset = focus.wantsCapture();
        int[] afterReset = frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertFalse(captureAfterReset);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, afterReset);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, afterTab);
    }

    @Test
    void shouldCaptureForKeyboardFocusedEnabledButtonOrComboButNotWhileDisabled() {
        KeyboardFocus buttons = new KeyboardFocus();
        frame(buttons, ID_A, ID_B, ID_C);
        buttons.queueMove(1);
        buttons.queueMove(1);
        frame(buttons, ID_A, ID_B, ID_C);
        boolean captureOnButton = buttons.wantsCapture();
        frameWithDisabledB(buttons);
        boolean captureWhileDisabled = buttons.wantsCapture();

        KeyboardFocus combo = new KeyboardFocus();
        comboFrame(combo);
        combo.queueMove(1);
        begin(combo);
        combo.register(ID_A, KeyboardFocus.Kind.COMBO, true, focusable(ID_A));
        boolean captureBeforeFrameEnds = combo.wantsCapture();
        combo.endFrame();
        boolean captureOnCombo = combo.wantsCapture();

        Assertions.assertTrue(captureOnButton);
        Assertions.assertFalse(captureWhileDisabled);
        Assertions.assertFalse(captureBeforeFrameEnds);
        Assertions.assertTrue(captureOnCombo);
    }

    @Test
    void shouldNotCaptureForAnchorOnly() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B);

        clickFrame(focus, ID_A, ID_A, ID_B);
        boolean captureAfterClick = focus.wantsCapture();
        frame(focus, ID_A, ID_B);
        boolean captureNextFrame = focus.wantsCapture();
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, ID_B);

        Assertions.assertFalse(captureAfterClick);
        Assertions.assertFalse(captureNextFrame);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldTrapInInnermostScopeAndPreferLastDrawnAmongEqualDepth() {
        KeyboardFocus nested = new KeyboardFocus();
        frame(nested, ID_A, POPUP, ID_B, POPUP, ID_C, ID_D, END_SCOPE, END_SCOPE);
        nested.queueMove(1);
        int[] nestedFirstTab = frame(nested, ID_A, POPUP, ID_B, POPUP, ID_C, ID_D, END_SCOPE, END_SCOPE);
        nested.queueMove(-1);
        int[] nestedAfterWrap = frame(nested, ID_A, POPUP, ID_B, POPUP, ID_C, ID_D, END_SCOPE, END_SCOPE);

        KeyboardFocus siblings = new KeyboardFocus();
        frame(siblings, ID_A, POPUP, ID_B, ID_C, END_SCOPE, POPUP, ID_D, ID_E, END_SCOPE);
        siblings.queueMove(1);
        int[] siblingsFirstTab = frame(siblings, ID_A, POPUP, ID_B, ID_C, END_SCOPE, POPUP, ID_D, ID_E, END_SCOPE);
        siblings.queueMove(-1);
        int[] siblingsAfterWrap = frame(siblings, ID_A, POPUP, ID_B, ID_C, END_SCOPE, POPUP, ID_D, ID_E, END_SCOPE);

        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED, 0}, nestedFirstTab);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, FOCUS_STARTED}, nestedAfterWrap);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, FOCUS_STARTED, 0}, siblingsFirstTab);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, 0, FOCUS_STARTED}, siblingsAfterWrap);
    }

    @Test
    void shouldClearFocusOutsideActiveScope() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B);

        frame(focus, ID_A, ID_B, POPUP, ID_C, ID_D, END_SCOPE);
        boolean captureWithPopupOpen = focus.wantsCapture();
        int[] whilePopupOpen = inputFrame(focus, Input.ACTIVATE, ID_A, ID_B, POPUP, ID_C, ID_D, END_SCOPE);
        int[] afterPopupClosed = frame(focus, ID_A, ID_B);
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, ID_B);

        Assertions.assertFalse(captureWithPopupOpen);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, 0}, whilePopupOpen);
        Assertions.assertArrayEquals(new int[] {0, 0}, afterPopupClosed);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0}, afterTab);
    }

    @Test
    void shouldKeepOwnerAnchorWhenComboIsOpenedWithMouse() {
        KeyboardFocus focus = new KeyboardFocus();
        closedComboFrame(focus);

        openComboFrame(focus, NO_ELEMENT, ID_COMBO, Input.CLICK);
        int[] whileOpen = openComboFrame(focus, NO_ELEMENT, NO_ELEMENT);
        closedComboFrame(focus);
        focus.queueMove(1);
        int[] afterTab = closedComboFrame(focus);

        Assertions.assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0}, whileOpen);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldFocusSelectedItemOrFirstItemWhenComboIsOpenedFromKeyboard() {
        KeyboardFocus withSelection = new KeyboardFocus();
        int[] openingFrame = openComboFromKeyboard(withSelection, ID_ITEM_2);
        int[] withSelectionNextFrame = openComboFrame(withSelection, ID_ITEM_2, NO_ELEMENT);

        KeyboardFocus withoutSelection = new KeyboardFocus();
        openComboFromKeyboard(withoutSelection, NO_ELEMENT);
        int[] withoutSelectionNextFrame = openComboFrame(withoutSelection, NO_ELEMENT, NO_ELEMENT);

        Assertions.assertArrayEquals(
                new int[] {0, KeyboardFocus.FOCUS_LOOK | KeyboardFocus.ACTIVATE, 0, 0, 0, 0}, openingFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, KeyboardFocus.FOCUS_LOOK, 0, 0}, withSelectionNextFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, KeyboardFocus.FOCUS_LOOK, 0, 0, 0}, withoutSelectionNextFrame);
    }

    @Test
    void shouldReturnFocusToOwnerOnItemActivationWithoutClearingItInClosingFrame() {
        KeyboardFocus focus = new KeyboardFocus();
        openComboFromKeyboard(focus, ID_ITEM_2);

        int[] activationFrame = openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.ACTIVATE);
        boolean captureAfterActivation = focus.wantsCapture();
        int[] afterClose = closedComboFrame(focus);
        focus.queueMove(1);
        int[] afterTab = closedComboFrame(focus);

        Assertions.assertArrayEquals(
                new int[] {0, 0, 0, KeyboardFocus.FOCUS_LOOK | KeyboardFocus.ACTIVATE, 0, 0}, activationFrame);
        Assertions.assertTrue(captureAfterActivation);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.FOCUS_LOOK, 0}, afterClose);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldReturnFocusToOwnerOnEscapeInComboAndClearFocusOnEscapeOutside() {
        KeyboardFocus focus = new KeyboardFocus();
        openComboFromKeyboard(focus, ID_ITEM_2);

        int[] escapeFrame = openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.ESCAPE, Input.ACTIVATE);
        int[] afterClose = closedComboFrame(focus);
        int[] escapeOutside = closedComboFrame(focus, Input.ESCAPE);
        focus.queueMove(1);
        int[] afterTab = closedComboFrame(focus);

        Assertions.assertArrayEquals(
                new int[] {0, FOCUS_STARTED | KeyboardFocus.CLOSE_COMBO_SCOPE, 0, 0, 0, 0}, escapeFrame);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.FOCUS_LOOK, 0}, afterClose);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, escapeOutside);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, afterTab);
    }

    @Test
    void shouldAnchorOwnerWithoutLookOnMouseItemClick() {
        KeyboardFocus focus = new KeyboardFocus();
        openComboFromKeyboard(focus, ID_ITEM_2);

        openComboFrame(focus, ID_ITEM_2, ID_ITEM_3, Input.CLICK);
        boolean captureAfterClick = focus.wantsCapture();
        int[] afterClose = closedComboFrame(focus);
        focus.queueMove(1);
        int[] afterTab = closedComboFrame(focus);

        Assertions.assertFalse(captureAfterClick);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, afterClose);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldMoveWithUpAndDownOnlyInComboScope() {
        KeyboardFocus buttons = new KeyboardFocus();
        frame(buttons, ID_A, ID_B, ID_C);
        buttons.queueMove(1);
        frame(buttons, ID_A, ID_B, ID_C);
        int[] afterDownOutsideCombo = inputFrame(buttons, Input.DOWN, ID_A, ID_B, ID_C);
        int[] afterUpOutsideCombo = inputFrame(buttons, Input.UP, ID_A, ID_B, ID_C);

        KeyboardFocus combo = new KeyboardFocus();
        openComboFromKeyboard(combo, NO_ELEMENT);
        int[] afterDown = openComboFrame(combo, NO_ELEMENT, NO_ELEMENT, Input.DOWN);
        int[] afterUp = openComboFrame(combo, NO_ELEMENT, NO_ELEMENT, Input.UP);
        int[] afterUpWrap = openComboFrame(combo, NO_ELEMENT, NO_ELEMENT, Input.UP);

        Assertions.assertArrayEquals(new int[] {KeyboardFocus.FOCUS_LOOK, 0, 0}, afterDownOutsideCombo);
        Assertions.assertArrayEquals(new int[] {KeyboardFocus.FOCUS_LOOK, 0, 0}, afterUpOutsideCombo);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, FOCUS_STARTED, 0, 0}, afterDown);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED, 0, 0, 0}, afterUp);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, 0, FOCUS_STARTED, 0}, afterUpWrap);
    }

    @Test
    void shouldEmitTakeInputFocusOnce() {
        KeyboardFocus focus = new KeyboardFocus();

        int[] arrivalFrame = tabToInput(focus);
        int[] activationFrame = inputPanelFrame(focus, true);
        int[] nextFrame = inputPanelFrame(focus, true);

        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.TAKE_INPUT_FOCUS, 0}, arrivalFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, activationFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, nextFrame);
    }

    @Test
    void shouldKeepPendingInputInItsArrivalFrame() {
        KeyboardFocus focus = new KeyboardFocus();

        tabToInput(focus);
        boolean captureAfterArrival = focus.wantsCapture();
        inputPanelFrame(focus, true);
        boolean captureAfterActivation = focus.wantsCapture();
        inputPanelFrame(focus, true);
        boolean captureWhileActive = focus.wantsCapture();

        Assertions.assertTrue(captureAfterArrival);
        Assertions.assertTrue(captureAfterActivation);
        Assertions.assertTrue(captureWhileActive);
    }

    @Test
    void shouldDropPendingInputToAnchorWhenNotActiveByNextFrame() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);

        inputPanelFrame(focus, false);
        boolean captureAfterDrop = focus.wantsCapture();
        int[] afterDrop = inputPanelFrame(focus, false);
        focus.queueMove(1);
        int[] afterTab = inputPanelFrame(focus, false);

        Assertions.assertFalse(captureAfterDrop);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, afterDrop);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.RELEASE_INPUT, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldReleasePendingInputOvertakenByTab() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);

        focus.queueMove(1);
        int[] overtakingFrame = inputPanelFrame(focus, true);
        int[] nextFrame = inputPanelFrame(focus, false);

        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.RELEASE_INPUT, FOCUS_STARTED}, overtakingFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, KeyboardFocus.FOCUS_LOOK}, nextFrame);
    }

    @Test
    void shouldReleaseActiveInputOnTab() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);
        inputPanelFrame(focus, true);

        focus.queueMove(-1);
        int[] shiftTabFrame = inputPanelFrame(focus, true);
        boolean captureAfterShiftTab = focus.wantsCapture();

        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, KeyboardFocus.RELEASE_INPUT, 0}, shiftTabFrame);
        Assertions.assertTrue(captureAfterShiftTab);
    }

    @Test
    void shouldClearFocusWithoutReleaseOnEscapeInActiveInput() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);
        inputPanelFrame(focus, true);

        int[] escapeFrame = inputPanelFrame(focus, true, Input.ESCAPE);
        boolean captureAfterEscape = focus.wantsCapture();
        focus.queueMove(1);
        int[] afterTab = inputPanelFrame(focus, false);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, escapeFrame);
        Assertions.assertFalse(captureAfterEscape);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, afterTab);
    }

    @Test
    void shouldDropInputToAnchorWhenEnterCommitsIt() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);
        inputPanelFrame(focus, true);

        int[] commitFrame = inputPanelFrame(focus, false, Input.ACTIVATE);
        boolean captureAfterCommit = focus.wantsCapture();
        focus.queueMove(1);
        int[] afterTab = inputPanelFrame(focus, false);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, commitFrame);
        Assertions.assertFalse(captureAfterCommit);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.RELEASE_INPUT, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldCaptureForExpandedComboAndPendingInput() {
        KeyboardFocus combo = new KeyboardFocus();
        closedComboFrame(combo);
        openComboFrame(combo, NO_ELEMENT, ID_COMBO, Input.CLICK);
        boolean captureWhenExpandedWithMouse = combo.wantsCapture();
        openComboFrame(combo, NO_ELEMENT, NO_ELEMENT);
        boolean captureWhileExpanded = combo.wantsCapture();
        closedComboFrame(combo);
        boolean captureAfterCollapse = combo.wantsCapture();

        KeyboardFocus input = new KeyboardFocus();
        tabToInput(input);
        boolean captureForPendingInput = input.wantsCapture();

        Assertions.assertTrue(captureWhenExpandedWithMouse);
        Assertions.assertTrue(captureWhileExpanded);
        Assertions.assertFalse(captureAfterCollapse);
        Assertions.assertTrue(captureForPendingInput);
    }

    @Test
    void shouldTakeInputFocusAgainWithoutReleaseWhenTabWrapsOntoSameInput() {
        KeyboardFocus focus = new KeyboardFocus();
        singleInputFrame(focus, false);
        focus.queueMove(1);
        singleInputFrame(focus, false);
        singleInputFrame(focus, true);

        focus.queueMove(1);
        int wrapFrame = singleInputFrame(focus, true);
        boolean captureAfterWrap = focus.wantsCapture();
        int nextFrame = singleInputFrame(focus, true);

        Assertions.assertEquals(KeyboardFocus.TAKE_INPUT_FOCUS, wrapFrame);
        Assertions.assertTrue(captureAfterWrap);
        Assertions.assertEquals(0, nextFrame);
    }

    @Test
    void shouldReleaseClickedInputOnTab() {
        KeyboardFocus focus = new KeyboardFocus();
        inputPanelFrame(focus, false);

        begin(focus, Input.CLICK);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true, focusable(ID_INPUT));
        focus.reportInput(ID_INPUT, true);
        focus.reportClick(ID_INPUT);
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.endFrame();
        boolean captureAfterClick = focus.wantsCapture();
        int[] whileClickedActive = inputPanelFrame(focus, true);
        focus.queueMove(1);
        int[] tabFrame = inputPanelFrame(focus, true);

        Assertions.assertFalse(captureAfterClick);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, whileClickedActive);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.RELEASE_INPUT, FOCUS_STARTED}, tabFrame);
    }

    @Test
    void shouldStartFocusAgainWhenFocusedButtonInPopupIsEnabledAgain() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE);
        focus.queueMove(1);
        frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE);

        begin(focus);
        int[] whileDisabled = new int[3];
        whileDisabled[0] = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focus.pushPopupScope();
        whileDisabled[1] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, false, focusable(ID_B));
        whileDisabled[2] = focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true, focusable(ID_C));
        focus.popScope();
        focus.endFrame();
        boolean captureWhileDisabled = focus.wantsCapture();
        int[] whenEnabledAgain = frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE);
        boolean captureWhenEnabledAgain = focus.wantsCapture();

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, whileDisabled);
        Assertions.assertFalse(captureWhileDisabled);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED, 0}, whenEnabledAgain);
        Assertions.assertTrue(captureWhenEnabledAgain);
    }

    @Test
    void shouldEnterItemsOfMouseOpenedComboFromOwnerAnchor() {
        KeyboardFocus forward = new KeyboardFocus();
        closedComboFrame(forward);
        openComboFrame(forward, NO_ELEMENT, ID_COMBO, Input.CLICK);
        KeyboardFocus back = new KeyboardFocus();
        closedComboFrame(back);
        openComboFrame(back, NO_ELEMENT, ID_COMBO, Input.CLICK);

        forward.queueMove(1);
        int[] afterTab = openComboFrame(forward, NO_ELEMENT, NO_ELEMENT);
        back.queueMove(-1);
        int[] afterShiftTab = openComboFrame(back, NO_ELEMENT, NO_ELEMENT);

        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED, 0, 0, 0}, afterTab);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, 0, FOCUS_STARTED, 0}, afterShiftTab);
    }

    @Test
    void shouldReturnToComboInsidePopupAndKeepPopupTrapWhenComboScopeCloses() {
        KeyboardFocus focus = new KeyboardFocus();
        comboInPopupFrame(focus, false);
        focus.queueMove(2);
        comboInPopupFrame(focus, false);
        comboInPopupFrame(focus, true, Input.ACTIVATE);

        int[] escapeFrame = comboInPopupFrame(focus, true, Input.ESCAPE);
        int[] afterClose = comboInPopupFrame(focus, false);
        focus.queueMove(1);
        int[] afterTab = comboInPopupFrame(focus, false);
        focus.queueMove(1);
        int[] afterWrap = comboInPopupFrame(focus, false);

        Assertions.assertArrayEquals(
                new int[] {0, 0, FOCUS_STARTED | KeyboardFocus.CLOSE_COMBO_SCOPE, 0, 0, 0, 0}, escapeFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, KeyboardFocus.FOCUS_LOOK, 0}, afterClose);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, FOCUS_STARTED}, afterTab);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED, 0, 0}, afterWrap);
    }

    @Test
    void shouldKeepPopupTrapWhenEscapeClearsFocusInsidePopup() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE);
        focus.queueMove(-1);
        frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE);

        int[] escapeFrame = inputFrame(focus, Input.ESCAPE, ID_A, POPUP, ID_B, ID_C, END_SCOPE);
        boolean captureAfterEscape = focus.wantsCapture();
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, escapeFrame);
        Assertions.assertFalse(captureAfterEscape);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED, 0}, afterTab);
    }

    @Test
    void shouldSkipDisabledInputAndDisabledComboInBothDirections() {
        KeyboardFocus focus = new KeyboardFocus();
        frameWithDisabledInputAndCombo(focus);
        focus.queueMove(1);
        int[] firstTab = frameWithDisabledInputAndCombo(focus);

        focus.queueMove(1);
        int[] afterTab = frameWithDisabledInputAndCombo(focus);
        focus.queueMove(-1);
        int[] afterShiftTab = frameWithDisabledInputAndCombo(focus);

        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0, 0}, firstTab);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, FOCUS_STARTED}, afterTab);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0, 0}, afterShiftTab);
    }

    @Test
    void shouldKeepAnchorWithoutCaptureOrNewInputFocusWhenFocusedInputIsDisabled() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);
        inputPanelFrame(focus, true);

        int[] whileDisabled = disabledInputPanelFrame(focus);
        boolean captureWhileDisabled = focus.wantsCapture();
        int[] whenEnabledAgain = inputPanelFrame(focus, false);
        boolean captureWhenEnabledAgain = focus.wantsCapture();
        focus.queueMove(1);
        int[] afterTab = inputPanelFrame(focus, false);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, whileDisabled);
        Assertions.assertFalse(captureWhileDisabled);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, whenEnabledAgain);
        Assertions.assertFalse(captureWhenEnabledAgain);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.RELEASE_INPUT, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldCloseExpandedComboAndMoveItemAnchorToComboWhenComboIsDisabled() {
        KeyboardFocus focus = new KeyboardFocus();
        openComboFromKeyboard(focus, ID_ITEM_2);

        begin(focus);
        int[] disablingFrame = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, false, focusable(ID_COMBO)),
            0, 0, 0, 0
        };
        focus.pushComboScope(ID_COMBO);
        disablingFrame[2] = focus.registerComboItem(ID_ITEM_1, false);
        disablingFrame[3] = focus.registerComboItem(ID_ITEM_2, true);
        disablingFrame[4] = focus.registerComboItem(ID_ITEM_3, false);
        focus.popScope();
        disablingFrame[5] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.endFrame();
        boolean captureWhileDisabled = focus.wantsCapture();
        int[] whenEnabledAgain = closedComboFrame(focus);
        boolean captureWhenEnabledAgain = focus.wantsCapture();
        focus.queueMove(1);
        int[] afterTab = closedComboFrame(focus);

        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.CLOSE_COMBO_SCOPE, 0, 0, 0, 0}, disablingFrame);
        Assertions.assertFalse(captureWhileDisabled);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED, 0}, whenEnabledAgain);
        Assertions.assertTrue(captureWhenEnabledAgain);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
    }

    @Test
    void shouldDropQueuedMovesWhileNavigationIsOff() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);

        focus.queueMove(1);
        int[] navigationOffFrame = inputFrame(focus, Input.NAVIGATION_OFF, ID_A, ID_B, ID_C);
        int[] navigationOnFrame = frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, navigationOffFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, navigationOnFrame);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0, 0}, afterTab);
    }

    @Test
    void shouldIgnoreActivationEscapeAndArrowsWhileNavigationIsOff() {
        KeyboardFocus focus = new KeyboardFocus();
        openComboFromKeyboard(focus, ID_ITEM_2);
        int[] itemFocused = {0, 0, 0, KeyboardFocus.FOCUS_LOOK, 0, 0};

        int[] afterDown = openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.NAVIGATION_OFF, Input.DOWN);
        int[] afterUp = openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.NAVIGATION_OFF, Input.UP);
        int[] afterEscape = openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.NAVIGATION_OFF, Input.ESCAPE);
        int[] afterActivation = openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.NAVIGATION_OFF, Input.ACTIVATE);
        int[] withNavigationOn = openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.ACTIVATE);

        Assertions.assertArrayEquals(itemFocused, afterDown);
        Assertions.assertArrayEquals(itemFocused, afterUp);
        Assertions.assertArrayEquals(itemFocused, afterEscape);
        Assertions.assertArrayEquals(itemFocused, afterActivation);
        Assertions.assertArrayEquals(
                new int[] {0, 0, 0, KeyboardFocus.FOCUS_LOOK | KeyboardFocus.ACTIVATE, 0, 0}, withNavigationOn);
    }

    @Test
    void shouldReportCaptureFromFocusStateWhileNavigationIsOff() {
        KeyboardFocus buttons = new KeyboardFocus();
        frame(buttons, ID_A, ID_B);
        buttons.queueMove(1);
        frame(buttons, ID_A, ID_B);
        int[] navigationOffFrame = inputFrame(buttons, Input.NAVIGATION_OFF, ID_A, ID_B);
        boolean captureOnButton = buttons.wantsCapture();

        KeyboardFocus combo = new KeyboardFocus();
        closedComboFrame(combo, Input.NAVIGATION_OFF);
        boolean captureWhileCollapsed = combo.wantsCapture();
        openComboFrame(combo, NO_ELEMENT, ID_COMBO, Input.NAVIGATION_OFF, Input.CLICK);
        boolean captureWhileExpanded = combo.wantsCapture();

        Assertions.assertArrayEquals(new int[] {KeyboardFocus.FOCUS_LOOK, 0}, navigationOffFrame);
        Assertions.assertTrue(captureOnButton);
        Assertions.assertFalse(captureWhileCollapsed);
        Assertions.assertTrue(captureWhileExpanded);
    }

    @Test
    void shouldWriteFocusedOnceAtEndFrameForTabFocusedButton() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);

        begin(focus);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        int writesBeforeEndFrame = focusable(ID_A).getFocusedTrueCount();
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true, focusable(ID_C));
        focus.endFrame();
        int writesAfterEndFrame = focusable(ID_A).getFocusedTrueCount();
        frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertEquals(0, writesBeforeEndFrame);
        Assertions.assertEquals(1, writesAfterEndFrame);
        Assertions.assertEquals(1, focusable(ID_A).getFocusedTrueCount());
        Assertions.assertEquals(0, focusable(ID_A).getFocusedFalseCount());
        Assertions.assertEquals(0, focusable(ID_B).getFocusedTrueCount());
        Assertions.assertEquals(0, focusable(ID_C).getFocusedTrueCount());
    }

    @Test
    void shouldWriteNotFocusedWhenFocusedElementIsNotDrawnButNotInDiscardedFrame() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);

        begin(focus);
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.discardFrame();
        int clearsAfterDiscardedFrame = focusable(ID_A).getFocusedFalseCount();
        frame(focus, ID_B, ID_C);
        int clearsWhenNotDrawn = focusable(ID_A).getFocusedFalseCount();
        frame(focus, ID_B, ID_C);

        Assertions.assertEquals(0, clearsAfterDiscardedFrame);
        Assertions.assertEquals(1, clearsWhenNotDrawn);
        Assertions.assertEquals(1, focusable(ID_A).getFocusedFalseCount());
        Assertions.assertEquals(1, focusable(ID_A).getFocusedTrueCount());
    }

    @Test
    void shouldWriteNotFocusedOnResetOnlyForFocusedElements() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(1);
        frame(focus, ID_A, ID_B, ID_C);

        focus.reset();
        int clearsAfterReset = focusable(ID_A).getFocusedFalseCount();
        frame(focus, ID_A, ID_B, ID_C);
        focus.reset();

        Assertions.assertEquals(1, clearsAfterReset);
        Assertions.assertEquals(1, focusable(ID_A).getFocusedFalseCount());
        Assertions.assertEquals(1, focusable(ID_A).getFocusedTrueCount());
        Assertions.assertEquals(0, focusable(ID_B).getFocusedFalseCount());
        Assertions.assertEquals(0, focusable(ID_C).getFocusedFalseCount());
    }

    @Test
    void shouldWriteClickedInputFocusedWhileActiveWithNavigationOff() {
        KeyboardFocus focus = new KeyboardFocus();
        inputPanelFrame(focus, false, Input.NAVIGATION_OFF);

        begin(focus, Input.CLICK, Input.NAVIGATION_OFF);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true, focusable(ID_INPUT));
        focus.reportInput(ID_INPUT, true);
        focus.reportClick(ID_INPUT);
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.endFrame();
        int writesAfterClick = focusable(ID_INPUT).getFocusedTrueCount();
        inputPanelFrame(focus, true, Input.NAVIGATION_OFF);
        int clearsWhileActive = focusable(ID_INPUT).getFocusedFalseCount();
        inputPanelFrame(focus, false, Input.NAVIGATION_OFF);

        Assertions.assertEquals(1, writesAfterClick);
        Assertions.assertEquals(0, clearsWhileActive);
        Assertions.assertEquals(1, focusable(ID_INPUT).getFocusedTrueCount());
        Assertions.assertEquals(1, focusable(ID_INPUT).getFocusedFalseCount());
        Assertions.assertEquals(0, focusable(ID_A).getFocusedTrueCount());
        Assertions.assertEquals(0, focusable(ID_B).getFocusedTrueCount());
    }

    @Test
    void shouldWriteComboFocusedWhileFocusIsOnItsItems() {
        KeyboardFocus focus = new KeyboardFocus();
        closedComboFrame(focus);
        focus.queueMove(2);
        closedComboFrame(focus);
        int writesOnCombo = focusable(ID_COMBO).getFocusedTrueCount();

        openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.ACTIVATE);
        openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.DOWN);
        openComboFrame(focus, ID_ITEM_2, NO_ELEMENT, Input.ESCAPE);
        int clearsWhileOnItemsAndBack = focusable(ID_COMBO).getFocusedFalseCount();
        focus.queueMove(1);
        closedComboFrame(focus);

        Assertions.assertEquals(1, writesOnCombo);
        Assertions.assertEquals(0, clearsWhileOnItemsAndBack);
        Assertions.assertEquals(1, focusable(ID_COMBO).getFocusedTrueCount());
        Assertions.assertEquals(1, focusable(ID_COMBO).getFocusedFalseCount());
        Assertions.assertEquals(1, focusable(ID_B).getFocusedTrueCount());
    }

    @Test
    void shouldWriteFocusedWhileAnyOccurrenceOfFocusableIsFocused() {
        KeyboardFocus focus = new KeyboardFocus();
        RecordingFocusable shared = focusable(ID_A);
        int[] focusedTrueCounts = new int[4];
        int[] focusedFalseCounts = new int[4];
        sharedFocusableFrame(focus, shared);

        for (int tab = 0; tab < focusedTrueCounts.length; tab++) {
            focus.queueMove(1);
            sharedFocusableFrame(focus, shared);
            focusedTrueCounts[tab] = shared.getFocusedTrueCount();
            focusedFalseCounts[tab] = shared.getFocusedFalseCount();
        }

        // Tabs land on A, B, the second occurrence D, then wrap to A
        Assertions.assertArrayEquals(new int[] {1, 1, 2, 2}, focusedTrueCounts);
        Assertions.assertArrayEquals(new int[] {0, 1, 1, 1}, focusedFalseCounts);
    }

    @Test
    void shouldWriteNotFocusedWhileFocusedButtonIsDisabled() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(2);
        frame(focus, ID_A, ID_B, ID_C);

        frameWithDisabledB(focus);
        int clearsWhileDisabled = focusable(ID_B).getFocusedFalseCount();
        int writesWhileDisabled = focusable(ID_B).getFocusedTrueCount();
        frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertEquals(1, clearsWhileDisabled);
        Assertions.assertEquals(1, writesWhileDisabled);
        Assertions.assertEquals(2, focusable(ID_B).getFocusedTrueCount());
        Assertions.assertEquals(1, focusable(ID_B).getFocusedFalseCount());
    }

    @Test
    void shouldGrantRequestPerKindAtNextFrameAndClearIt() {
        KeyboardFocus focus = new KeyboardFocus();
        kindsFrame(focus);

        requestFocus(ID_A);
        int buttonGrant = kindsFrame(focus)[0];
        requestFocus(ID_INPUT);
        int inputGrant = kindsFrame(focus)[1];
        requestFocus(ID_COMBO);
        int comboGrant = kindsFrame(focus)[2];

        Assertions.assertEquals(FOCUS_STARTED, buttonGrant);
        Assertions.assertEquals(KeyboardFocus.TAKE_INPUT_FOCUS, inputGrant);
        Assertions.assertEquals(FOCUS_STARTED, comboGrant);
        for (int id : new int[] {ID_A, ID_INPUT, ID_COMBO}) {
            Assertions.assertFalse(focusable(id).requestsFocus());
            Assertions.assertEquals(1, focusable(id).getRequestClearCount());
        }
    }

    @Test
    void shouldGrantRequestOfComponentFirstDrawnInFrameAfterRequest() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B);
        frame(focus, ID_A, ID_B);

        requestFocus(ID_C);
        int[] firstDraw = frame(focus, ID_A, ID_B, ID_C);
        int[] nextFrame = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, firstDraw);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, nextFrame);
        Assertions.assertEquals(1, focusable(ID_C).getRequestClearCount());
    }

    @Test
    void shouldGrantRequestMadeAfterDrawWithinFrameInNextFrame() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B);
        frame(focus, ID_A, ID_B);

        begin(focus);
        int requestFrame = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        requestFocus(ID_A);
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.endFrame();
        int clearsAfterRequestFrame = focusable(ID_A).getRequestClearCount();
        int[] nextFrame = frame(focus, ID_A, ID_B);

        Assertions.assertEquals(0, requestFrame);
        Assertions.assertEquals(0, clearsAfterRequestFrame);
        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, 0}, nextFrame);
        Assertions.assertEquals(1, focusable(ID_A).getRequestClearCount());
    }

    @Test
    void shouldDropAndClearRequestOfComponentHiddenThroughOneMoreDrawnFrame() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B);

        requestFocus(ID_C);
        frame(focus, ID_A, ID_B);
        frame(focus, ID_A, ID_B, ID_C);
        int[] judgedFrame = frame(focus, ID_A, ID_B, ID_C);
        int[] laterFrame = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, judgedFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, laterFrame);
        Assertions.assertFalse(focusable(ID_C).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_C).getRequestClearCount());
    }

    @Test
    void shouldNotAgeRequestInEmptyCompletedFrames() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A);

        requestFocus(ID_C);
        frame(focus);
        frame(focus);
        frame(focus, ID_C);
        int afterFirstDraw = frame(focus, ID_C)[0];

        Assertions.assertEquals(FOCUS_STARTED, afterFirstDraw);
        Assertions.assertEquals(1, focusable(ID_C).getRequestClearCount());
    }

    @Test
    void shouldAcceptOlderRequestInFirstDrawnFrameAfterConstructionOrReset() {
        KeyboardFocus focus = new KeyboardFocus();
        focusable(ID_A).request(frameStart - 1000);
        frame(focus, ID_A);
        int afterConstruction = frame(focus, ID_A)[0];

        frame(focus, ID_B);
        focusable(ID_B).request(frameStart - 1000);
        focus.reset();
        frame(focus, ID_B);
        int afterReset = frame(focus, ID_B)[0];

        Assertions.assertEquals(FOCUS_STARTED, afterConstruction);
        Assertions.assertEquals(FOCUS_STARTED, afterReset);
    }

    @Test
    void shouldWriteNoFocusAndDropConsumedRequestWhenFrameIsDiscarded() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B);

        requestFocus(ID_B);
        begin(focus);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        int discardedGrant = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.discardFrame();
        boolean requestedAfterDiscard = focusable(ID_B).requestsFocus();
        int[] retry = frame(focus, ID_A, ID_B);

        Assertions.assertEquals(FOCUS_STARTED, discardedGrant);
        Assertions.assertFalse(requestedAfterDiscard);
        Assertions.assertEquals(1, focusable(ID_B).getRequestClearCount());
        Assertions.assertArrayEquals(new int[] {0, 0}, retry);
        Assertions.assertEquals(0, focusable(ID_B).getFocusedTrueCount());
    }

    @Test
    void shouldNotSettleCompletedFrameRequestsAgainWhenFrameIsDiscardedBeforeBeginFrame() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B);

        requestFocus(ID_B);
        long stamp = focusable(ID_B).getFocusRequestTime();
        frame(focus, ID_A, ID_B);
        focusable(ID_B).request(stamp);
        focus.discardFrame();

        Assertions.assertTrue(focusable(ID_B).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_B).getRequestClearCount());
    }

    @Test
    void shouldKeepRequestRenewedBetweenBeginFrameAndEndFrame() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A);

        requestFocus(ID_A);
        begin(focus);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focusable(ID_A).request(frameStart + 2);
        focus.endFrame();
        boolean requestedAfterGrantFrame = focusable(ID_A).requestsFocus();
        int clearsAfterGrantFrame = focusable(ID_A).getRequestClearCount();
        frame(focus, ID_A);

        Assertions.assertTrue(requestedAfterGrantFrame);
        Assertions.assertEquals(0, clearsAfterGrantFrame);
        Assertions.assertFalse(focusable(ID_A).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_A).getRequestClearCount());
    }

    @Test
    void shouldDropAndClearRequestOnDisabledTargetTargetOutsidePopupAndWhileComboIsExpanded() {
        KeyboardFocus disabled = new KeyboardFocus();
        frameWithDisabledB(disabled);
        requestFocus(ID_B);
        int[] disabledJudged = frameWithDisabledB(disabled);
        int[] disabledEnabledAgain = frame(disabled, ID_A, ID_B, ID_C);
        boolean disabledTargetRequested = focusable(ID_B).requestsFocus();
        int disabledTargetClears = focusable(ID_B).getRequestClearCount();

        KeyboardFocus popup = new KeyboardFocus();
        frame(popup, ID_D, POPUP, ID_E, END_SCOPE);
        requestFocus(ID_D);
        int[] popupJudged = frame(popup, ID_D, POPUP, ID_E, END_SCOPE);
        int[] popupClosed = frame(popup, ID_D);

        KeyboardFocus combo = new KeyboardFocus();
        closedComboFrame(combo);
        openComboFrame(combo, NO_ELEMENT, ID_COMBO, Input.CLICK);
        requestFocus(ID_COMBO);
        int[] comboJudged = openComboFrame(combo, NO_ELEMENT, NO_ELEMENT);
        int[] comboCollapsed = closedComboFrame(combo);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, disabledJudged);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, disabledEnabledAgain);
        Assertions.assertFalse(disabledTargetRequested);
        Assertions.assertEquals(1, disabledTargetClears);
        Assertions.assertArrayEquals(new int[] {0, 0}, popupJudged);
        Assertions.assertArrayEquals(new int[] {0}, popupClosed);
        Assertions.assertFalse(focusable(ID_D).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_D).getRequestClearCount());
        Assertions.assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0}, comboJudged);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, comboCollapsed);
        Assertions.assertFalse(focusable(ID_COMBO).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_COMBO).getRequestClearCount());
    }

    @Test
    void shouldGrantLastGrantableRequestInDrawOrderAndClearOthers() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE, ID_D);

        for (int id : new int[] {ID_A, ID_B, ID_C, ID_D}) {
            requestFocus(id);
        }
        int[] judgedFrame = frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE, ID_D);
        int[] nextFrame = frame(focus, ID_A, POPUP, ID_B, ID_C, END_SCOPE, ID_D);

        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED, 0}, judgedFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, KeyboardFocus.FOCUS_LOOK, 0}, nextFrame);
        for (int id : new int[] {ID_A, ID_B, ID_C, ID_D}) {
            Assertions.assertFalse(focusable(id).requestsFocus());
            Assertions.assertEquals(1, focusable(id).getRequestClearCount());
        }
    }

    @Test
    void shouldGrantNoRequestAndClearItInFrameWithMouseClick() {
        KeyboardFocus focus = new KeyboardFocus();
        kindsFrame(focus);

        requestFocus(ID_INPUT);
        int[] inputClickFrame = kindsFrame(focus, Input.CLICK);
        requestFocus(ID_A);
        int[] buttonClickFrame = kindsFrame(focus, Input.CLICK);
        int[] nextFrame = kindsFrame(focus);
        boolean captureAfterwards = focus.wantsCapture();

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, inputClickFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, buttonClickFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, nextFrame);
        Assertions.assertFalse(captureAfterwards);
        for (int id : new int[] {ID_A, ID_INPUT}) {
            Assertions.assertFalse(focusable(id).requestsFocus());
            Assertions.assertEquals(1, focusable(id).getRequestClearCount());
            Assertions.assertEquals(0, focusable(id).getFocusedTrueCount());
        }
    }

    @Test
    void shouldReleaseActiveInputWhenRequestFocusesAnotherElement() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);
        inputPanelFrame(focus, true);

        requestFocus(ID_A);
        int[] requestFrame = inputPanelFrame(focus, true);
        boolean captureAfterRequest = focus.wantsCapture();
        int[] nextFrame = inputPanelFrame(focus, false);

        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, KeyboardFocus.RELEASE_INPUT, 0}, requestFrame);
        Assertions.assertTrue(captureAfterRequest);
        Assertions.assertArrayEquals(new int[] {KeyboardFocus.FOCUS_LOOK, 0, 0}, nextFrame);
    }

    @Test
    void shouldNeitherReleaseNorRetakeCommittedInputWhenRequestUndoesSameFrameTab() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);
        inputPanelFrame(focus, true);

        focus.queueMove(1);
        requestFocus(ID_INPUT);
        int[] requestFrame = inputPanelFrame(focus, true);
        boolean captureAfterRequest = focus.wantsCapture();
        int[] nextFrame = inputPanelFrame(focus, true);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, requestFrame);
        Assertions.assertTrue(captureAfterRequest);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, nextFrame);
        Assertions.assertEquals(0, focusable(ID_INPUT).getFocusedFalseCount());
        Assertions.assertEquals(0, focusable(ID_B).getFocusedTrueCount());
        Assertions.assertEquals(1, focusable(ID_INPUT).getRequestClearCount());
    }

    @Test
    void shouldStartNoFocusAgainForFocusedButtonOrMouseEditedInputOnRequest() {
        KeyboardFocus button = new KeyboardFocus();
        frame(button, ID_A, ID_B);
        button.queueMove(1);
        frame(button, ID_A, ID_B);
        requestFocus(ID_A);
        int[] buttonRequestFrame = frame(button, ID_A, ID_B);
        int buttonFocusWrites = focusable(ID_A).getFocusedTrueCount();
        int buttonRequestClears = focusable(ID_A).getRequestClearCount();

        KeyboardFocus input = new KeyboardFocus();
        inputPanelFrame(input, false);
        begin(input, Input.CLICK);
        input.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        input.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true, focusable(ID_INPUT));
        input.reportInput(ID_INPUT, true);
        input.reportClick(ID_INPUT);
        input.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        input.endFrame();
        requestFocus(ID_INPUT);
        int[] inputRequestFrame = inputPanelFrame(input, true);
        int[] inputNextFrame = inputPanelFrame(input, true);

        Assertions.assertArrayEquals(new int[] {KeyboardFocus.FOCUS_LOOK, 0}, buttonRequestFrame);
        Assertions.assertEquals(1, buttonFocusWrites);
        Assertions.assertEquals(1, buttonRequestClears);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, inputRequestFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, inputNextFrame);
        Assertions.assertEquals(1, focusable(ID_INPUT).getFocusedTrueCount());
        Assertions.assertEquals(0, focusable(ID_INPUT).getFocusedFalseCount());
        Assertions.assertEquals(1, focusable(ID_INPUT).getRequestClearCount());
    }

    @Test
    void shouldKeepFocusWithoutNewStartWhenRequestUndoesSameFrameTabAway() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B, ID_C);
        focus.queueMove(2);
        frame(focus, ID_A, ID_B, ID_C);

        focus.queueMove(1);
        requestFocus(ID_B);
        int[] requestFrame = frame(focus, ID_A, ID_B, ID_C);
        int[] nextFrame = frame(focus, ID_A, ID_B, ID_C);

        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.FOCUS_LOOK, 0}, requestFrame);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.FOCUS_LOOK, 0}, nextFrame);
        Assertions.assertEquals(1, focusable(ID_B).getFocusedTrueCount());
        Assertions.assertEquals(0, focusable(ID_B).getFocusedFalseCount());
        Assertions.assertEquals(0, focusable(ID_C).getFocusedTrueCount());
    }

    @Test
    void shouldGrantAndWriteBackRequestWhileNavigationIsOff() {
        KeyboardFocus focus = new KeyboardFocus();
        inputFrame(focus, Input.NAVIGATION_OFF, ID_A, ID_B);

        requestFocus(ID_B);
        int[] grantFrame = inputFrame(focus, Input.NAVIGATION_OFF, ID_A, ID_B);
        boolean captureAfterGrant = focus.wantsCapture();
        int[] nextFrame = inputFrame(focus, Input.NAVIGATION_OFF, ID_A, ID_B);

        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED}, grantFrame);
        Assertions.assertTrue(captureAfterGrant);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.FOCUS_LOOK}, nextFrame);
        Assertions.assertEquals(1, focusable(ID_B).getFocusedTrueCount());
        Assertions.assertEquals(0, focusable(ID_B).getFocusedFalseCount());
        Assertions.assertFalse(focusable(ID_B).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_B).getRequestClearCount());
    }

    @Test
    void shouldNeitherCaptureNorRetakeInputFocusWhenPendingInputIsDisabled() {
        KeyboardFocus focus = new KeyboardFocus();
        tabToInput(focus);
        boolean captureWhilePending = focus.wantsCapture();

        int[] whileDisabled = disabledInputPanelFrame(focus);
        boolean captureWhileDisabled = focus.wantsCapture();
        int[] whenEnabledAgain = inputPanelFrame(focus, false);
        boolean captureWhenEnabledAgain = focus.wantsCapture();

        Assertions.assertTrue(captureWhilePending);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, whileDisabled);
        Assertions.assertFalse(captureWhileDisabled);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, whenEnabledAgain);
        Assertions.assertFalse(captureWhenEnabledAgain);
        Assertions.assertEquals(1, focusable(ID_INPUT).getFocusedTrueCount());
        Assertions.assertEquals(1, focusable(ID_INPUT).getFocusedFalseCount());
    }

    @Test
    void shouldNeitherTakeInputFocusNorCaptureWhenInputIsDisabledInFrameTabOrRequestLandsOnIt() {
        KeyboardFocus tabbed = new KeyboardFocus();
        inputPanelFrame(tabbed, false);
        tabbed.queueMove(2);
        int[] tabFrame = disabledInputPanelFrame(tabbed);
        boolean captureAfterTab = tabbed.wantsCapture();
        int[] tabEnabledAgain = inputPanelFrame(tabbed, false);

        KeyboardFocus requested = new KeyboardFocus();
        inputPanelFrame(requested, false);
        requestFocus(ID_INPUT);
        int[] requestFrame = disabledInputPanelFrame(requested);
        boolean captureAfterRequest = requested.wantsCapture();
        int[] requestEnabledAgain = inputPanelFrame(requested, false);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, tabFrame);
        Assertions.assertFalse(captureAfterTab);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, tabEnabledAgain);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, requestFrame);
        Assertions.assertFalse(captureAfterRequest);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, requestEnabledAgain);
        Assertions.assertFalse(focusable(ID_INPUT).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_INPUT).getRequestClearCount());
        Assertions.assertEquals(0, focusable(ID_INPUT).getFocusedTrueCount());
    }

    @Test
    void shouldNotActivateComboDisabledInFrameOfActivationKey() {
        KeyboardFocus focus = new KeyboardFocus();
        closedComboFrame(focus);
        focus.queueMove(2);
        closedComboFrame(focus);

        begin(focus, Input.ACTIVATE);
        int[] disablingFrame = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, false, focusable(ID_COMBO)),
            focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B))
        };
        focus.endFrame();
        boolean captureWhileDisabled = focus.wantsCapture();
        int[] whenEnabledAgain = closedComboFrame(focus);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, disablingFrame);
        Assertions.assertFalse(captureWhileDisabled);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED, 0}, whenEnabledAgain);
        Assertions.assertTrue(focus.wantsCapture());
    }

    @Test
    void shouldDropRequestOnDisabledComboInsidePopupAndKeepPopupTrap() {
        KeyboardFocus focus = new KeyboardFocus();
        comboInPopupFrame(focus, false);
        focus.queueMove(1);
        comboInPopupFrame(focus, false);
        disabledComboInPopupFrame(focus);

        requestFocus(ID_COMBO);
        int[] judgedFrame = disabledComboInPopupFrame(focus);
        focus.queueMove(1);
        int[] afterTab = disabledComboInPopupFrame(focus);
        int[] whenEnabledAgain = comboInPopupFrame(focus, false);

        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.FOCUS_LOOK, 0, 0}, judgedFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, FOCUS_STARTED}, afterTab);
        Assertions.assertArrayEquals(new int[] {0, 0, 0, KeyboardFocus.FOCUS_LOOK}, whenEnabledAgain);
        Assertions.assertFalse(focusable(ID_COMBO).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_COMBO).getRequestClearCount());
        Assertions.assertEquals(0, focusable(ID_COMBO).getFocusedTrueCount());
    }

    @Test
    void shouldReleaseMouseEditedInputWhenRequestFocusesAnotherElement() {
        KeyboardFocus focus = new KeyboardFocus();
        inputPanelFrame(focus, false);
        begin(focus, Input.CLICK);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true, focusable(ID_INPUT));
        focus.reportInput(ID_INPUT, true);
        focus.reportClick(ID_INPUT);
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.endFrame();
        inputPanelFrame(focus, true);

        requestFocus(ID_A);
        // The input still reports active in the frame its renderer releases it
        int[] requestFrame = inputPanelFrame(focus, true);
        int clearsInRequestFrame = focusable(ID_INPUT).getFocusedFalseCount();
        int[] nextFrame = inputPanelFrame(focus, false);

        Assertions.assertArrayEquals(new int[] {FOCUS_STARTED, KeyboardFocus.RELEASE_INPUT, 0}, requestFrame);
        Assertions.assertEquals(0, clearsInRequestFrame);
        Assertions.assertArrayEquals(new int[] {KeyboardFocus.FOCUS_LOOK, 0, 0}, nextFrame);
        Assertions.assertTrue(focus.wantsCapture());
        Assertions.assertEquals(1, focusable(ID_INPUT).getFocusedFalseCount());
        Assertions.assertEquals(1, focusable(ID_A).getFocusedTrueCount());
    }

    @Test
    void shouldGrantRequestOnInputAndKeepItFocusedWhileActiveWithNavigationOff() {
        KeyboardFocus focus = new KeyboardFocus();
        inputPanelFrame(focus, false, Input.NAVIGATION_OFF);

        requestFocus(ID_INPUT);
        int[] grantFrame = inputPanelFrame(focus, false, Input.NAVIGATION_OFF);
        boolean captureWhilePending = focus.wantsCapture();
        int[] activeFrame = inputPanelFrame(focus, true, Input.NAVIGATION_OFF);
        boolean captureWhileActive = focus.wantsCapture();

        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.TAKE_INPUT_FOCUS, 0}, grantFrame);
        Assertions.assertTrue(captureWhilePending);
        Assertions.assertArrayEquals(new int[] {0, 0, 0}, activeFrame);
        Assertions.assertTrue(captureWhileActive);
        Assertions.assertEquals(1, focusable(ID_INPUT).getFocusedTrueCount());
        Assertions.assertEquals(0, focusable(ID_INPUT).getFocusedFalseCount());
        Assertions.assertEquals(1, focusable(ID_INPUT).getRequestClearCount());
    }

    @Test
    void shouldKeepRequestRenewedDuringDiscardedFrameAndGrantItOnceInRetry() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A, ID_B);

        requestFocus(ID_B);
        begin(focus);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focusable(ID_B).request(frameStart + 2);
        focus.discardFrame();
        boolean requestedAfterDiscard = focusable(ID_B).requestsFocus();
        int clearsAfterDiscard = focusable(ID_B).getRequestClearCount();
        int[] retry = frame(focus, ID_A, ID_B);
        int[] afterRetry = frame(focus, ID_A, ID_B);

        Assertions.assertTrue(requestedAfterDiscard);
        Assertions.assertEquals(0, clearsAfterDiscard);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED}, retry);
        Assertions.assertArrayEquals(new int[] {0, KeyboardFocus.FOCUS_LOOK}, afterRetry);
        Assertions.assertFalse(focusable(ID_B).requestsFocus());
        Assertions.assertEquals(1, focusable(ID_B).getRequestClearCount());
    }

    @Test
    void shouldGrantRequestMadeWhenPopupOpensAfterPopupsFirstDraw() {
        KeyboardFocus focus = new KeyboardFocus();
        frame(focus, ID_A);
        frame(focus, ID_A);

        requestFocus(ID_D);
        int[] firstPopupDraw = frame(focus, ID_A, POPUP, ID_D, ID_E, END_SCOPE);
        int[] nextFrame = frame(focus, ID_A, POPUP, ID_D, ID_E, END_SCOPE);
        focus.queueMove(1);
        int[] afterTab = frame(focus, ID_A, POPUP, ID_D, ID_E, END_SCOPE);
        focus.queueMove(1);
        int[] afterWrap = frame(focus, ID_A, POPUP, ID_D, ID_E, END_SCOPE);

        Assertions.assertArrayEquals(new int[] {0, 0, 0}, firstPopupDraw);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED, 0}, nextFrame);
        Assertions.assertArrayEquals(new int[] {0, 0, FOCUS_STARTED}, afterTab);
        Assertions.assertArrayEquals(new int[] {0, FOCUS_STARTED, 0}, afterWrap);
        Assertions.assertEquals(1, focusable(ID_D).getRequestClearCount());
    }

    // Every frame starts later than the previous one; navigation is enabled unless NAVIGATION_OFF is passed
    private void begin(KeyboardFocus focus, Input... inputs) {
        List<Input> pressed = List.of(inputs);
        frameStart += FRAME_DURATION;
        focus.beginFrame(
                frameStart,
                !pressed.contains(Input.NAVIGATION_OFF),
                pressed.contains(Input.ACTIVATE),
                pressed.contains(Input.ESCAPE),
                pressed.contains(Input.UP),
                pressed.contains(Input.DOWN),
                pressed.contains(Input.CLICK));
    }

    private RecordingFocusable focusable(int id) {
        return focusables.computeIfAbsent(id, _ -> new RecordingFocusable());
    }

    // A request made after the start of the last begun frame, as a request from a callback of that frame
    private void requestFocus(int id) {
        focusable(id).request(frameStart + 1);
    }

    private int[] frame(KeyboardFocus focus, int... tokens) {
        begin(focus);
        return registerAndEnd(focus, tokens);
    }

    private int[] inputFrame(KeyboardFocus focus, Input input, int... tokens) {
        begin(focus, input);
        return registerAndEnd(focus, tokens);
    }

    // Returns the results of the registered buttons only, in draw order
    private int[] registerAndEnd(KeyboardFocus focus, int... tokens) {
        List<Integer> results = new ArrayList<>();
        for (int token : tokens) {
            switch (token) {
                case POPUP -> focus.pushPopupScope();
                case END_SCOPE -> focus.popScope();
                default -> results.add(focus.register(token, KeyboardFocus.Kind.BUTTON, true, focusable(token)));
            }
        }
        focus.endFrame();
        int[] resultArray = new int[results.size()];
        for (int i = 0; i < resultArray.length; i++) {
            resultArray[i] = results.get(i);
        }
        return resultArray;
    }

    private int[] clickFrame(KeyboardFocus focus, int clickedId, int... enabledButtonIds) {
        begin(focus, Input.CLICK);
        int[] results = new int[enabledButtonIds.length];
        for (int i = 0; i < enabledButtonIds.length; i++) {
            int id = enabledButtonIds[i];
            results[i] = focus.register(id, KeyboardFocus.Kind.BUTTON, true, focusable(id));
            if (id == clickedId) {
                focus.reportClick(clickedId);
            }
        }
        focus.endFrame();
        return results;
    }

    private int[] frameWithDisabledB(KeyboardFocus focus) {
        begin(focus);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_B, KeyboardFocus.Kind.BUTTON, false, focusable(ID_B)),
            focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true, focusable(ID_C))
        };
        focus.endFrame();
        return results;
    }

    // Button A, a disabled input, a disabled combo, button B
    private int[] frameWithDisabledInputAndCombo(KeyboardFocus focus) {
        begin(focus);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, false, focusable(ID_INPUT)),
            focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, false, focusable(ID_COMBO)),
            focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B))
        };
        focus.endFrame();
        return results;
    }

    // Button A, the input (never reported active), the collapsed combo
    private int[] kindsFrame(KeyboardFocus focus, Input... inputs) {
        begin(focus, inputs);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true, focusable(ID_INPUT)),
            focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, true, focusable(ID_COMBO))
        };
        focus.reportInput(ID_INPUT, false);
        focus.endFrame();
        return results;
    }

    // Buttons A, B and D, of which A and D register the same Focusable
    private void sharedFocusableFrame(KeyboardFocus focus, RecordingFocusable shared) {
        begin(focus);
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, shared);
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.register(ID_D, KeyboardFocus.Kind.BUTTON, true, shared);
        focus.endFrame();
    }

    private void comboFrame(KeyboardFocus focus) {
        begin(focus);
        focus.register(ID_A, KeyboardFocus.Kind.COMBO, true, focusable(ID_A));
        focus.endFrame();
    }

    // Button A, the collapsed combo, button B
    private int[] closedComboFrame(KeyboardFocus focus, Input... inputs) {
        begin(focus, inputs);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, true, focusable(ID_COMBO)),
            focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B))
        };
        focus.endFrame();
        return results;
    }

    // Button A, the expanded combo with three items, button B; a click on the combo frame is reported after the
    // items, as the combo renderer reports it after the expanded combo ends
    private int[] openComboFrame(KeyboardFocus focus, int selectedId, int clickedId, Input... inputs) {
        begin(focus, inputs);
        int[] items = {ID_ITEM_1, ID_ITEM_2, ID_ITEM_3};
        int[] results = new int[items.length + 3];
        results[0] = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        results[1] = focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, true, focusable(ID_COMBO));
        focus.pushComboScope(ID_COMBO);
        for (int i = 0; i < items.length; i++) {
            results[i + 2] = focus.registerComboItem(items[i], items[i] == selectedId);
            if (items[i] == clickedId) {
                focus.reportClick(clickedId);
            }
        }
        focus.popScope();
        if (clickedId == ID_COMBO) {
            focus.reportClick(ID_COMBO);
        }
        results[items.length + 2] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.endFrame();
        return results;
    }

    // Tabs to the collapsed combo and opens it with the activation key; returns the opening frame's results
    private int[] openComboFromKeyboard(KeyboardFocus focus, int selectedId) {
        closedComboFrame(focus);
        focus.queueMove(2);
        closedComboFrame(focus);
        return openComboFrame(focus, selectedId, NO_ELEMENT, Input.ACTIVATE);
    }

    // Button A, an input reporting whether its widget was active, button B
    private int[] inputPanelFrame(KeyboardFocus focus, boolean inputActive, Input... inputs) {
        begin(focus, inputs);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true, focusable(ID_INPUT)),
            0
        };
        focus.reportInput(ID_INPUT, inputActive);
        results[2] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.endFrame();
        return results;
    }

    // As inputPanelFrame with the input disabled, which the renderer never reports active
    private int[] disabledInputPanelFrame(KeyboardFocus focus) {
        begin(focus);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A)),
            focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, false, focusable(ID_INPUT)),
            0
        };
        focus.reportInput(ID_INPUT, false);
        results[2] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        focus.endFrame();
        return results;
    }

    private int singleInputFrame(KeyboardFocus focus, boolean inputActive) {
        begin(focus);
        int result = focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true, focusable(ID_INPUT));
        focus.reportInput(ID_INPUT, inputActive);
        focus.endFrame();
        return result;
    }

    // Button A, then a popup with button B, the combo (expanded with three unselected items when open) and button C
    private int[] comboInPopupFrame(KeyboardFocus focus, boolean comboOpen, Input... inputs) {
        begin(focus, inputs);
        int[] items = comboOpen ? new int[] {ID_ITEM_1, ID_ITEM_2, ID_ITEM_3} : new int[0];
        int[] results = new int[items.length + 4];
        results[0] = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focus.pushPopupScope();
        results[1] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        results[2] = focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, true, focusable(ID_COMBO));
        if (comboOpen) {
            focus.pushComboScope(ID_COMBO);
            for (int i = 0; i < items.length; i++) {
                results[i + 3] = focus.registerComboItem(items[i], false);
            }
            focus.popScope();
        }
        results[items.length + 3] = focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true, focusable(ID_C));
        focus.popScope();
        focus.endFrame();
        return results;
    }

    // As comboInPopupFrame with the combo collapsed and disabled
    private int[] disabledComboInPopupFrame(KeyboardFocus focus) {
        begin(focus);
        int[] results = new int[4];
        results[0] = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true, focusable(ID_A));
        focus.pushPopupScope();
        results[1] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true, focusable(ID_B));
        results[2] = focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, false, focusable(ID_COMBO));
        results[3] = focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true, focusable(ID_C));
        focus.popScope();
        focus.endFrame();
        return results;
    }

    // Tabs onto the input; returns the arrival frame's results, in which the input is not active yet
    private int[] tabToInput(KeyboardFocus focus) {
        inputPanelFrame(focus, false);
        focus.queueMove(2);
        return inputPanelFrame(focus, false);
    }

    private enum Input {
        ACTIVATE,
        ESCAPE,
        UP,
        DOWN,
        CLICK,
        NAVIGATION_OFF
    }
}
