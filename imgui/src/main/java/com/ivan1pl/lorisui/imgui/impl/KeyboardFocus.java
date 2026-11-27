package com.ivan1pl.lorisui.imgui.impl;

import com.ivan1pl.lorisui.api.component.Focusable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// LorisUI's keyboard focus across the renderers of one frame. Each frame works on a copy of the last completed frame's
// state and replaces it in endFrame, so a failed frame changes nothing.
//
// Focus requests are granted at beginFrame, like the Tab moves, so every element registered in the frame sees the
// final anchor, however early it is drawn, and the last occurrence in draw order is known from the committed order. A
// request is judged against the threshold committed with that order: it is fresh when it is not older than the start
// of the drawn frame before the one that drew its component. Only drawn frames, completed with at least one element,
// count, so frames that draw nothing, such as those before a screen's first draw, never age a request made for it.
// A frame that reports a mouse click grants no request, so the click wins completely.
// An input is released when the anchor leaves it, derived once from the committed anchor after the Tab moves and the
// requests, so a request undoing a same-frame Tab move releases nothing and a Tab or a request releases alike.
// The focus is tracked and written back whether keyboard navigation is enabled or not; the switch only gates the keys.
public final class KeyboardFocus {
    public static final int FOCUS_LOOK = 1;
    public static final int KEYBOARD_FOCUS_STARTED = 1 << 1;
    public static final int ACTIVATE = 1 << 2;
    // Delivered to the owning combo when Escape closes its expanded combo; an activated item closes it on ACTIVATE
    public static final int CLOSE_COMBO_SCOPE = 1 << 3;
    // Delivered to an input in the frame keyboard focus lands on it; Dear ImGui activates it one frame later
    public static final int TAKE_INPUT_FOCUS = 1 << 4;
    // Delivered to an input the committed anchor leaves, active or still pending; it deactivates itself if active
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
    private Set<Integer> committedFocusedIds = new HashSet<>();
    // The Focusables last written as focused, compared by identity: each component instance has its own focus state
    private Set<Focusable> committedFocused = newIdentitySet();
    // Only drawn frames, completed with at least one element, age requests; until one completes, all are accepted
    private long lastDrawnFrameStart;
    private boolean hasDrawnFrame;
    // The threshold the committed order's requests are judged against: the one of the frame that drew it
    private long committedRequestsSince;
    private boolean committedAcceptsAllRequests;

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
    private long frameStart;
    // The requests judged in this frame with the stamps read then, compared by identity
    private final Map<Focusable, Long> settlements = new IdentityHashMap<>();

    private int queuedMoves;

    public void queueMove(int step) {
        queuedMoves += step;
    }

    // The frame runs whether keyboard navigation is enabled or not; while it is disabled, the queued moves are dropped
    // and LorisUI's keys are ignored, but clicks, scopes and the registration results work as usual
    public void beginFrame(long frameStart, boolean navigationEnabled, boolean activationPressed,
                           boolean escapePressed, boolean upPressed, boolean downPressed, boolean mouseClicked) {
        restoreCommittedState();
        this.frameStart = frameStart;
        this.mouseClicked = mouseClicked;

        int moves = queuedMoves;
        queuedMoves = 0;
        if (navigationEnabled && moves != 0) {
            applyMoves(moves);
        }

        applyRequests();
        scheduleRelease();

        if (navigationEnabled) {
            applyKeys(activationPressed, escapePressed, upPressed, downPressed);
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

    public int register(int id, Kind kind, boolean enabled, Focusable focusable) {
        Objects.requireNonNull(focusable, "focusable");
        return register(id, kind, enabled, false, focusable);
    }

    public int registerComboItem(int id, boolean selected) {
        return register(id, Kind.COMBO_ITEM, true, selected, null);
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

        Set<Integer> focusedIds = frameFocusedIds();
        Set<Focusable> focused = newIdentitySet();
        for (Element element : frameOrder) {
            if (element.focusable() != null && focusedIds.contains(element.id())) {
                focused.add(element.focusable());
            }
        }

        committedOrder = frameOrder;
        committedScopes = frameScopes;
        committedActiveScope = activeScope;
        committedAnchored = anchored;
        committedAnchorId = anchorId;
        committedKeyboardFocused = keyboardFocused;
        committedInputPending = inputPending;
        committedCapture = capturesKeyboard();
        committedFocusedIds = focusedIds;
        // Read before this frame updates them, so they still describe the drawn frames before this one
        committedRequestsSince = lastDrawnFrameStart;
        committedAcceptsAllRequests = !hasDrawnFrame;
        if (!frameOrder.isEmpty()) {
            lastDrawnFrameStart = frameStart;
            hasDrawnFrame = true;
        }
        writeFocus(focused);
        settleRequests();
    }

    // A failed frame leaves the last completed frame in place and writes no focus; the moves and the requests it
    // consumed stay dropped, so a retried frame cannot start a focus twice
    public void discardFrame() {
        settleRequests();
        restoreCommittedState();
    }

    // Called only when the renderer is disposed; turning keyboard navigation off keeps the focus
    public void reset() {
        committedOrder = new ArrayList<>();
        committedScopes = new ArrayList<>();
        committedActiveScope = ROOT_SCOPE;
        committedAnchored = false;
        committedAnchorId = 0;
        committedKeyboardFocused = false;
        committedInputPending = false;
        committedCapture = false;
        committedFocusedIds = new HashSet<>();
        lastDrawnFrameStart = 0;
        hasDrawnFrame = false;
        committedRequestsSince = 0;
        committedAcceptsAllRequests = false;
        queuedMoves = 0;
        restoreCommittedState();
        writeFocus(newIdentitySet());
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
        frameStart = 0;
        settlements.clear();
    }

    // Every pending request on the committed order is judged, so each one is settled, granted or not; of the fresh,
    // grantable ones the last drawn wins
    private void applyRequests() {
        Element winner = null;
        for (Element element : committedOrder) {
            if (element.focusable() != null && isFreshRequest(element.focusable()) && isGrantable(element)) {
                winner = element;
            }
        }
        if (winner == null || mouseClicked) {
            return;
        }
        if (committedAnchored && committedAnchorId == winner.id()
                && (committedKeyboardFocused || committedFocusedIds.contains(winner.id()))) {
            // An element focused already keeps its focus without a new start, also undoing a same-frame Tab move
            anchored = true;
            anchorId = winner.id();
            keyboardFocused = committedKeyboardFocused;
            inputPending = committedInputPending;
            focusStartPending = false;
        } else {
            focusOn(winner.id());
        }
    }

    // Enabled when committed and a member of the active scope; nothing while a combo is expanded, as Up/Down own it
    private boolean isGrantable(Element element) {
        return element.enabled() && element.scope() == committedActiveScope && activeComboScope() == null;
    }

    // A Tab wrapping onto the same input keeps it; an input focused and left within one beginFrame was never
    // committed, so it needs no release
    private void scheduleRelease() {
        if (committedAnchored && isInput(committedAnchorId) && (!anchored || anchorId != committedAnchorId)) {
            releasePending = true;
            releaseId = committedAnchorId;
        }
    }

    // The stamp is read once per frame, after the pending flag, and compared overflow-safe
    private boolean isFreshRequest(Focusable focusable) {
        Long stamp = settlements.get(focusable);
        if (stamp == null) {
            if (!focusable.requestsFocus()) {
                return false;
            }
            stamp = focusable.getFocusRequestTime();
            settlements.put(focusable, stamp);
        }
        return committedAcceptsAllRequests || stamp - committedRequestsSince >= 0;
    }

    // A request renewed since beginFrame has a new stamp and stays pending; a renewal between the two reads below is
    // the accepted race, as Focusable has no conditional clear
    private void settleRequests() {
        for (Map.Entry<Focusable, Long> settlement : settlements.entrySet()) {
            Focusable focusable = settlement.getKey();
            if (focusable.requestsFocus() && focusable.getFocusRequestTime() == settlement.getValue()) {
                focusable.setRequestsFocus(false);
            }
        }
        // Cleared once settled, so a later discarded frame that never began does not settle these requests again
        settlements.clear();
    }

    // Up/Down move only in a combo scope; Escape closes the active combo scope, or clears the focus outside one
    private void applyKeys(boolean activationPressed, boolean escapePressed, boolean upPressed, boolean downPressed) {
        Scope comboScope = activeComboScope();
        int arrowMoves = (downPressed ? 1 : 0) - (upPressed ? 1 : 0);
        if (comboScope != null && arrowMoves != 0) {
            applyMoves(arrowMoves);
        }

        if (escapePressed) {
            if (comboScope != null) {
                closingComboOwners.add(comboScope.ownerId());
                focusOn(comboScope.ownerId());
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

    // The committed active scope when it is an expanded combo's, or null
    private Scope activeComboScope() {
        if (committedActiveScope == ROOT_SCOPE) {
            return null;
        }
        Scope scope = committedScopes.get(committedActiveScope);
        return scope.combo() ? scope : null;
    }

    private void pushScope(boolean combo, int ownerId) {
        frameScopes.add(new Scope(openScopeStack.size() + 1, combo, ownerId));
        openScopeStack.add(frameScopes.size() - 1);
    }

    private int register(int id, Kind kind, boolean enabled, boolean selected, Focusable focusable) {
        int scope = openScopeStack.isEmpty() ? ROOT_SCOPE : openScopeStack.getLast();
        frameOrder.add(new Element(id, kind, enabled, selected, scope, focusable));
        if (kind == Kind.COMBO && !enabled) {
            closeDisabledComboScope(id);
        }
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

    // A combo disabled while expanded closes its list. A focus on one of its items returns to the combo without a look,
    // so the combo starts its focus again, without expanding, when it is enabled
    private void closeDisabledComboScope(int ownerId) {
        for (int i = 0; i < committedScopes.size(); i++) {
            Scope scope = committedScopes.get(i);
            if (scope.combo() && scope.ownerId() == ownerId) {
                closingComboOwners.add(ownerId);
                int anchorIndex = indexOf(committedOrder, anchorId);
                if (anchored && anchorIndex >= 0 && committedOrder.get(anchorIndex).scope() == i) {
                    anchorId = ownerId;
                }
            }
        }
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

    // A click anchors the clicked element, or the combo of a clicked item, without the keyboard focus, so the next Tab
    // continues from there; any other click clears the anchor
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

    // A pending input is judged only from the frame after its arrival, as Dear ImGui activates it then;
    // an input that is no longer active, or not active by then, keeps only the anchor
    private void updateInputActivation() {
        if (activeInputReported && activeInputId == anchorId) {
            inputPending = false;
        } else if (!inputPending || !inputArrived) {
            keyboardFocused = false;
        }
    }

    // A combo opened from the keyboard focuses its selected item, or its first item
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

    // The elements reported as focused: a keyboard-focused enabled element, the enabled combo whose items hold the
    // keyboard focus, and an enabled input Dear ImGui has active, also one edited after a mouse click
    private Set<Integer> frameFocusedIds() {
        Set<Integer> ids = new HashSet<>();
        if (anchored && keyboardFocused) {
            Element anchor = frameOrder.get(indexOf(frameOrder, anchorId));
            if (anchor.kind() != Kind.COMBO_ITEM) {
                addIfEnabled(ids, anchorId);
            } else if (anchor.scope() != ROOT_SCOPE && frameScopes.get(anchor.scope()).combo()) {
                addIfEnabled(ids, frameScopes.get(anchor.scope()).ownerId());
            }
        }
        if (activeInputReported) {
            addIfEnabled(ids, activeInputId);
        }
        return ids;
    }

    private void addIfEnabled(Set<Integer> ids, int id) {
        int index = indexOf(frameOrder, id);
        if (index >= 0 && frameOrder.get(index).enabled()) {
            ids.add(id);
        }
    }

    // Writes changes only, so a Focusable that stays focused, or stays unfocused, is not written again
    private void writeFocus(Set<Focusable> focused) {
        Set<Focusable> previous = committedFocused;
        committedFocused = focused;
        for (Focusable focusable : previous) {
            if (!focused.contains(focusable)) {
                focusable.setFocused(false);
            }
        }
        for (Focusable focusable : focused) {
            if (!previous.contains(focusable)) {
                focusable.setFocused(true);
            }
        }
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
            // A focused enabled input is pending or active; an active one is captured too, because Dear ImGui's own
            // capture flag reports an input only from the frame after the one in which the widget activated it
            case BUTTON, COMBO, INPUT -> focused.enabled();
            case COMBO_ITEM -> true;
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

    private static Set<Focusable> newIdentitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
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

    // scope is an index into the frame's scope list, or ROOT_SCOPE; focusable is null for combo items
    private record Element(int id, Kind kind, boolean enabled, boolean selected, int scope, Focusable focusable) {}

    private record Scope(int depth, boolean combo, int ownerId) {}
}
