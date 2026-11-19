package com.ivan1pl.lorisui.imgui.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class KeyboardFocus {
    public static final int FOCUS_LOOK = 1;
    public static final int KEYBOARD_FOCUS_STARTED = 1 << 1;
    public static final int ACTIVATE = 1 << 2;
    // Delivered to the owning combo when Escape closes its expanded combo; an activated item closes it on ACTIVATE
    public static final int CLOSE_COMBO_SCOPE = 1 << 3;
    // Delivered to an input in the frame keyboard focus lands on it; Dear ImGui activates it one frame later
    public static final int TAKE_INPUT_FOCUS = 1 << 4;
    // Delivered to an input whose anchor a Tab move takes, active or still pending; it deactivates itself if active
    public static final int RELEASE_INPUT = 1 << 5;

    private static final int ROOT_SCOPE = -1;

    // State of the last completed frame; a frame's working state starts from it and replaces it in endFrame
    private List<Element> committedOrder = new ArrayList<>();
    private List<Scope> committedScopes = new ArrayList<>();
    private int committedActiveScope = ROOT_SCOPE;
    private boolean committedAnchored;
    private int committedAnchorId;
    private boolean committedKeyboardFocused;
    private boolean committedInputPending;
    private boolean committedCapture;

    private List<Element> frameOrder = new ArrayList<>();
    private List<Scope> frameScopes = new ArrayList<>();
    private List<Integer> openScopeStack = new ArrayList<>();
    private Set<Integer> closingComboOwners = new HashSet<>();
    private boolean anchored;
    private int anchorId;
    private boolean keyboardFocused;
    private boolean focusStartPending;
    private boolean inputPending;
    private boolean inputArrived;
    private boolean releasePending;
    private int releaseId;
    private boolean activeInputReported;
    private int activeInputId;
    private boolean activationPending;
    private int activationId;
    private boolean comboOpened;
    private int openedComboId;
    private boolean mouseClicked;
    private boolean clickReported;
    private int clickedId;

    private int queuedMoves;

    public void queueMove(int step) {
        queuedMoves += step;
    }

    public void beginFrame(boolean activationPressed, boolean escapePressed, boolean upPressed, boolean downPressed,
                           boolean mouseClicked) {
        restoreCommittedState();
        this.mouseClicked = mouseClicked;

        int moves = queuedMoves;
        queuedMoves = 0;
        if (moves != 0) {
            boolean hadAnchor = anchored;
            int previousAnchorId = anchorId;
            applyMoves(moves);
            if (hadAnchor && anchorId != previousAnchorId && isInput(previousAnchorId)) {
                releasePending = true;
                releaseId = previousAnchorId;
            }
        }

        Scope activeScope = committedActiveScope == ROOT_SCOPE ? null : committedScopes.get(committedActiveScope);
        boolean comboScopeActive = activeScope != null && activeScope.combo();
        int arrowMoves = (downPressed ? 1 : 0) - (upPressed ? 1 : 0);
        if (comboScopeActive && arrowMoves != 0) {
            applyMoves(arrowMoves);
        }

        if (escapePressed) {
            if (comboScopeActive) {
                closingComboOwners.add(activeScope.ownerId());
                focusOn(activeScope.ownerId());
            } else {
                anchored = false;
                keyboardFocused = false;
                focusStartPending = false;
            }
        } else if (activationPressed && keyboardFocused && isMemberOfActiveScope(anchorId)) {
            activationPending = true;
            activationId = anchorId;
        }
    }

    public void pushPopupScope() {
        pushScope(false, 0);
    }

    public void pushComboScope(int ownerId) {
        pushScope(true, ownerId);
    }

    public void popScope() {
        openScopeStack.removeLast();
    }

    public int register(int id, Kind kind, boolean enabled) {
        return register(id, kind, enabled, false);
    }

    public int registerComboItem(int id, boolean selected) {
        return register(id, Kind.COMBO_ITEM, true, selected);
    }

    // Clicks are reported in draw order, so of nested elements the innermost, drawn last, wins
    public void reportClick(int id) {
        clickReported = true;
        clickedId = id;
    }

    // Called after the input's widget with whether Dear ImGui has it active
    public void reportInput(int id, boolean active) {
        if (active) {
            activeInputReported = true;
            activeInputId = id;
        }
    }

    public void endFrame() {
        applyClicks();

        if (anchored && indexOf(frameOrder, anchorId) < 0) {
            anchored = false;
            keyboardFocused = false;
        }

        if (anchored && keyboardFocused && frameOrder.get(indexOf(frameOrder, anchorId)).kind() == Kind.INPUT) {
            updateInputActivation();
        }

        if (comboOpened) {
            focusOpenedComboItem();
        }

        int activeScope = activeScope();
        if (anchored && !isInScopeOrOwner(anchorId, activeScope)) {
            anchored = false;
            keyboardFocused = false;
        }
        if (!keyboardFocused) {
            inputPending = false;
        }

        committedOrder = frameOrder;
        committedScopes = frameScopes;
        committedActiveScope = activeScope;
        committedAnchored = anchored;
        committedAnchorId = anchorId;
        committedKeyboardFocused = keyboardFocused;
        committedInputPending = inputPending;
        committedCapture = capturesKeyboard();
    }

    // A failed frame leaves the last completed frame in place; the moves it consumed stay dropped
    public void discardFrame() {
        restoreCommittedState();
    }

    public void reset() {
        committedOrder = new ArrayList<>();
        committedScopes = new ArrayList<>();
        committedActiveScope = ROOT_SCOPE;
        committedAnchored = false;
        committedAnchorId = 0;
        committedKeyboardFocused = false;
        committedInputPending = false;
        committedCapture = false;
        queuedMoves = 0;
        restoreCommittedState();
    }

    public boolean wantsCapture() {
        return committedCapture;
    }

    private void restoreCommittedState() {
        frameOrder = new ArrayList<>();
        frameScopes = new ArrayList<>();
        openScopeStack = new ArrayList<>();
        closingComboOwners = new HashSet<>();
        anchored = committedAnchored;
        anchorId = committedAnchorId;
        keyboardFocused = committedKeyboardFocused;
        focusStartPending = false;
        inputPending = committedInputPending;
        inputArrived = false;
        releasePending = false;
        releaseId = 0;
        activeInputReported = false;
        activeInputId = 0;
        activationPending = false;
        activationId = 0;
        comboOpened = false;
        openedComboId = 0;
        mouseClicked = false;
        clickReported = false;
        clickedId = 0;
    }

    private void pushScope(boolean combo, int ownerId) {
        frameScopes.add(new Scope(openScopeStack.size() + 1, combo, ownerId));
        openScopeStack.add(frameScopes.size() - 1);
    }

    private int register(int id, Kind kind, boolean enabled, boolean selected) {
        int scope = openScopeStack.isEmpty() ? ROOT_SCOPE : openScopeStack.getLast();
        frameOrder.add(new Element(id, kind, enabled, selected, scope));
        int result = 0;
        if (kind == Kind.COMBO && closingComboOwners.contains(id)) {
            result |= CLOSE_COMBO_SCOPE;
        }
        if (kind == Kind.INPUT && releasePending && releaseId == id) {
            result |= RELEASE_INPUT;
        }
        if (!enabled || !keyboardFocused || anchorId != id) {
            return result;
        }
        if (kind == Kind.INPUT) {
            return result | takeInputFocus();
        }
        result |= FOCUS_LOOK;
        // An element that was disabled while focused starts its focus again when it is enabled
        if (focusStartPending || wasRegisteredDisabled(id)) {
            focusStartPending = false;
            result |= KEYBOARD_FOCUS_STARTED;
        }
        if (activationPending && activationId == id) {
            activationPending = false;
            result |= ACTIVATE;
            activate(id, kind, scope);
        }
        return result;
    }

    // An input gets neither the look nor the activation key: Dear ImGui draws its caret and handles Enter
    private int takeInputFocus() {
        if (!focusStartPending) {
            return 0;
        }
        focusStartPending = false;
        inputPending = true;
        inputArrived = true;
        return TAKE_INPUT_FOCUS;
    }

    private void activate(int id, Kind kind, int scope) {
        if (kind == Kind.COMBO) {
            comboOpened = true;
            openedComboId = id;
        } else if (kind == Kind.COMBO_ITEM && scope != ROOT_SCOPE && frameScopes.get(scope).combo()) {
            int ownerId = frameScopes.get(scope).ownerId();
            closingComboOwners.add(ownerId);
            focusOn(ownerId);
        }
    }

    // End rules 1 and 2
    private void applyClicks() {
        if (clickReported) {
            int index = indexOf(frameOrder, clickedId);
            Element clicked = index < 0 ? null : frameOrder.get(index);
            if (clicked != null && clicked.kind() == Kind.COMBO_ITEM && clicked.scope() != ROOT_SCOPE
                    && frameScopes.get(clicked.scope()).combo()) {
                int ownerId = frameScopes.get(clicked.scope()).ownerId();
                closingComboOwners.add(ownerId);
                anchorId = ownerId;
            } else {
                anchorId = clickedId;
            }
            anchored = true;
            keyboardFocused = false;
        } else if (mouseClicked) {
            anchored = false;
            keyboardFocused = false;
        }
    }

    // End rule 4: a pending input is judged only from the frame after its arrival, as Dear ImGui activates it then;
    // an input that is no longer active, or not active by then, keeps only the anchor
    private void updateInputActivation() {
        if (activeInputReported && activeInputId == anchorId) {
            inputPending = false;
        } else if (!inputPending || !inputArrived) {
            keyboardFocused = false;
        }
    }

    // End rule 5: a combo opened from the keyboard focuses its selected item, or its first item
    private void focusOpenedComboItem() {
        int scope = ROOT_SCOPE;
        for (int i = 0; i < frameScopes.size(); i++) {
            Scope candidate = frameScopes.get(i);
            if (candidate.combo() && candidate.ownerId() == openedComboId && isOpen(candidate)) {
                scope = i;
            }
        }
        if (scope == ROOT_SCOPE) {
            return;
        }
        Element target = null;
        for (Element element : frameOrder) {
            if (element.scope() == scope && (target == null || element.selected() && !target.selected())) {
                target = element;
            }
        }
        if (target != null) {
            anchored = true;
            anchorId = target.id();
            keyboardFocused = true;
        }
    }

    // The deepest open scope of this frame; among equals the one drawn last
    private int activeScope() {
        int active = ROOT_SCOPE;
        int activeDepth = 0;
        for (int i = 0; i < frameScopes.size(); i++) {
            Scope scope = frameScopes.get(i);
            if (scope.depth() >= activeDepth && isOpen(scope)) {
                active = i;
                activeDepth = scope.depth();
            }
        }
        return active;
    }

    private boolean isOpen(Scope scope) {
        return !scope.combo() || !closingComboOwners.contains(scope.ownerId());
    }

    private boolean isInScopeOrOwner(int id, int scope) {
        if (frameOrder.get(indexOf(frameOrder, id)).scope() == scope) {
            return true;
        }
        return scope != ROOT_SCOPE && frameScopes.get(scope).combo() && frameScopes.get(scope).ownerId() == id;
    }

    private boolean isMemberOfActiveScope(int id) {
        int index = indexOf(committedOrder, id);
        return index >= 0 && committedOrder.get(index).scope() == committedActiveScope;
    }

    private void focusOn(int id) {
        anchored = true;
        anchorId = id;
        keyboardFocused = true;
        focusStartPending = true;
        inputPending = false;
    }

    private boolean capturesKeyboard() {
        for (Scope scope : frameScopes) {
            if (scope.combo() && isOpen(scope)) {
                return true;
            }
        }
        if (!anchored || !keyboardFocused) {
            return false;
        }
        Element focused = frameOrder.get(indexOf(frameOrder, anchorId));
        return switch (focused.kind()) {
            case BUTTON, COMBO -> focused.enabled();
            // A focused input is pending or active; an active one is captured too, because Dear ImGui's own capture
            // flag reports an input only from the frame after the one in which the widget activated it
            case INPUT, COMBO_ITEM -> true;
        };
    }

    // Moves cycle the members of the active scope; from an anchor outside it, or on its owner, they start afresh
    private void applyMoves(int moves) {
        List<Element> members = new ArrayList<>();
        for (Element element : committedOrder) {
            if (element.scope() == committedActiveScope) {
                members.add(element);
            }
        }
        if (members.isEmpty()) {
            return;
        }
        int direction = moves > 0 ? 1 : -1;
        int index = anchored ? indexOf(members, anchorId) : -1;
        if (index < 0) {
            index = direction > 0 ? -1 : members.size();
        }
        for (int step = 0; step < Math.abs(moves); step++) {
            index = nextFocusable(members, index, direction);
            if (index < 0) {
                return;
            }
        }
        focusOn(members.get(index).id());
    }

    private boolean isInput(int id) {
        int index = indexOf(committedOrder, id);
        return index >= 0 && committedOrder.get(index).kind() == Kind.INPUT;
    }

    private boolean wasRegisteredDisabled(int id) {
        int index = indexOf(committedOrder, id);
        return index >= 0 && !committedOrder.get(index).enabled();
    }

    private static int nextFocusable(List<Element> members, int index, int direction) {
        int size = members.size();
        for (int offset = 1; offset <= size; offset++) {
            int candidate = Math.floorMod(index + direction * offset, size);
            if (members.get(candidate).enabled()) {
                return candidate;
            }
        }
        return -1;
    }

    private static int indexOf(List<Element> order, int id) {
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).id() == id) {
                return i;
            }
        }
        return -1;
    }

    public enum Kind {
        BUTTON,
        INPUT,
        COMBO,
        COMBO_ITEM
    }

    // scope is an index into the frame's scope list, or ROOT_SCOPE
    private record Element(int id, Kind kind, boolean enabled, boolean selected, int scope) {}

    private record Scope(int depth, boolean combo, int ownerId) {}
}
