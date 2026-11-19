package com.ivan1pl.lorisui.imgui.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        int whileDisabled = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, false);
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
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true);
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, false);
        focus.reportClick(ID_B);
        focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true);
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
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true),
            focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true),
            focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true)
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
        combo.register(ID_A, KeyboardFocus.Kind.COMBO, true);
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
        focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true);
        focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true);
        focus.reportInput(ID_INPUT, true);
        focus.reportClick(ID_INPUT);
        focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true);
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
        whileDisabled[0] = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true);
        focus.pushPopupScope();
        whileDisabled[1] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, false);
        whileDisabled[2] = focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true);
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

    private static void begin(KeyboardFocus focus, Input... inputs) {
        List<Input> pressed = List.of(inputs);
        focus.beginFrame(
                pressed.contains(Input.ACTIVATE),
                pressed.contains(Input.ESCAPE),
                pressed.contains(Input.UP),
                pressed.contains(Input.DOWN),
                pressed.contains(Input.CLICK));
    }

    private static int[] frame(KeyboardFocus focus, int... tokens) {
        begin(focus);
        return registerAndEnd(focus, tokens);
    }

    private static int[] inputFrame(KeyboardFocus focus, Input input, int... tokens) {
        begin(focus, input);
        return registerAndEnd(focus, tokens);
    }

    // Returns the results of the registered buttons only, in draw order
    private static int[] registerAndEnd(KeyboardFocus focus, int... tokens) {
        List<Integer> results = new ArrayList<>();
        for (int token : tokens) {
            switch (token) {
                case POPUP -> focus.pushPopupScope();
                case END_SCOPE -> focus.popScope();
                default -> results.add(focus.register(token, KeyboardFocus.Kind.BUTTON, true));
            }
        }
        focus.endFrame();
        int[] resultArray = new int[results.size()];
        for (int i = 0; i < resultArray.length; i++) {
            resultArray[i] = results.get(i);
        }
        return resultArray;
    }

    private static int[] clickFrame(KeyboardFocus focus, int clickedId, int... enabledButtonIds) {
        begin(focus, Input.CLICK);
        int[] results = new int[enabledButtonIds.length];
        for (int i = 0; i < enabledButtonIds.length; i++) {
            results[i] = focus.register(enabledButtonIds[i], KeyboardFocus.Kind.BUTTON, true);
            if (enabledButtonIds[i] == clickedId) {
                focus.reportClick(clickedId);
            }
        }
        focus.endFrame();
        return results;
    }

    private static int[] frameWithDisabledB(KeyboardFocus focus) {
        begin(focus);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true),
            focus.register(ID_B, KeyboardFocus.Kind.BUTTON, false),
            focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true)
        };
        focus.endFrame();
        return results;
    }

    private static void comboFrame(KeyboardFocus focus) {
        begin(focus);
        focus.register(ID_A, KeyboardFocus.Kind.COMBO, true);
        focus.endFrame();
    }

    // Button A, the collapsed combo, button B
    private static int[] closedComboFrame(KeyboardFocus focus, Input... inputs) {
        begin(focus, inputs);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true),
            focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, true),
            focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true)
        };
        focus.endFrame();
        return results;
    }

    // Button A, the expanded combo with three items, button B; a click on the combo frame is reported after the
    // items, as the combo renderer reports it after the expanded combo ends
    private static int[] openComboFrame(KeyboardFocus focus, int selectedId, int clickedId, Input... inputs) {
        begin(focus, inputs);
        int[] items = {ID_ITEM_1, ID_ITEM_2, ID_ITEM_3};
        int[] results = new int[items.length + 3];
        results[0] = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true);
        results[1] = focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, true);
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
        results[items.length + 2] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true);
        focus.endFrame();
        return results;
    }

    // Tabs to the collapsed combo and opens it with the activation key; returns the opening frame's results
    private static int[] openComboFromKeyboard(KeyboardFocus focus, int selectedId) {
        closedComboFrame(focus);
        focus.queueMove(2);
        closedComboFrame(focus);
        return openComboFrame(focus, selectedId, NO_ELEMENT, Input.ACTIVATE);
    }

    // Button A, an input reporting whether its widget was active, button B
    private static int[] inputPanelFrame(KeyboardFocus focus, boolean inputActive, Input... inputs) {
        begin(focus, inputs);
        int[] results = {
            focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true),
            focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true),
            0
        };
        focus.reportInput(ID_INPUT, inputActive);
        results[2] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true);
        focus.endFrame();
        return results;
    }

    private static int singleInputFrame(KeyboardFocus focus, boolean inputActive) {
        begin(focus);
        int result = focus.register(ID_INPUT, KeyboardFocus.Kind.INPUT, true);
        focus.reportInput(ID_INPUT, inputActive);
        focus.endFrame();
        return result;
    }

    // Button A, then a popup with button B, the combo (expanded with three unselected items when open) and button C
    private static int[] comboInPopupFrame(KeyboardFocus focus, boolean comboOpen, Input... inputs) {
        begin(focus, inputs);
        int[] items = comboOpen ? new int[] {ID_ITEM_1, ID_ITEM_2, ID_ITEM_3} : new int[0];
        int[] results = new int[items.length + 4];
        results[0] = focus.register(ID_A, KeyboardFocus.Kind.BUTTON, true);
        focus.pushPopupScope();
        results[1] = focus.register(ID_B, KeyboardFocus.Kind.BUTTON, true);
        results[2] = focus.register(ID_COMBO, KeyboardFocus.Kind.COMBO, true);
        if (comboOpen) {
            focus.pushComboScope(ID_COMBO);
            for (int i = 0; i < items.length; i++) {
                results[i + 3] = focus.registerComboItem(items[i], false);
            }
            focus.popScope();
        }
        results[items.length + 3] = focus.register(ID_C, KeyboardFocus.Kind.BUTTON, true);
        focus.popScope();
        focus.endFrame();
        return results;
    }

    // Tabs onto the input; returns the arrival frame's results, in which the input is not active yet
    private static int[] tabToInput(KeyboardFocus focus) {
        inputPanelFrame(focus, false);
        focus.queueMove(2);
        return inputPanelFrame(focus, false);
    }

    private enum Input {
        ACTIVATE,
        ESCAPE,
        UP,
        DOWN,
        CLICK
    }
}
