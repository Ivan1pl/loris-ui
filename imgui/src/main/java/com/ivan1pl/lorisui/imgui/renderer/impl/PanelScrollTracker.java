package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.component.Panel;

import java.util.HashMap;
import java.util.Map;

final class PanelScrollTracker {
    // Dear ImGui rounds an applied scroll offset to whole pixels (0.5 px), plus 0.01 px for float error
    private static final float TOLERANCE = 0.51f;
    private static final float NO_TARGET = -1f;

    private final Map<Integer, Entry> entries = new HashMap<>();
    private int currentFrame = 0;
    // The judgment of the draw in progress, made by scrollTarget and discarded by update or on any frame change, so
    // it always belongs to the current frame
    private boolean judged;
    private int judgedId;
    private long judgedStamp;
    private float judgedRequest;
    private float judgedTarget;

    // Called before the Begin of the window that scrolls; a negative result means no scroll is applied. A target is
    // returned only when the ID was drawn in each of the two frames before, so the range cached by the later of them
    // was computed from content drawn in the frame before it
    float scrollTarget(Panel panel, int id, int frame) {
        advanceTo(frame);
        if (!panel.requestsScrollPosition()) {
            judged = false;
            return NO_TARGET;
        }

        judged = true;
        judgedId = id;
        judgedStamp = panel.getScrollPositionRequestTime();
        judgedRequest = panel.getRequestedScrollPosition();
        judgedTarget = NO_TARGET;
        Entry entry = entries.get(id);
        if (entry != null && entry.frame == frame - 1 && entry.rangeKnown && entry.range > 0) {
            judgedTarget = judgedRequest * entry.range;
        }
        return judgedTarget;
    }

    // Called after that Begin, with the window's scroll offset and range; only the first draw of an ID in a frame is
    // recorded, so a further draw in the same frame only reports its position. The clear checks run against the entry
    // as recorded by the draw before this one
    void update(Panel panel, int id, int frame, float scrollY, float scrollMaxY) {
        advanceTo(frame);
        panel.reportScrollPosition(scrollMaxY > 0 ? Math.clamp(scrollY / scrollMaxY, 0f, 1f) : 0f);

        boolean judgedHere = judged && judgedId == id;
        Entry entry = entries.get(id);
        if (judgedHere && (isReached(entry, frame, scrollY, scrollMaxY) || isUserDrag(scrollY, scrollMaxY))) {
            clearJudgedRequest(panel);
        }
        if (entry == null) {
            entry = new Entry();
            entries.put(id, entry);
            entry.record(frame, scrollMaxY, false, judgedHere, judgedStamp);
        } else if (entry.frame != frame) {
            entry.record(frame, scrollMaxY, entry.frame == frame - 1, judgedHere, judgedStamp);
        }
        judged = false;
    }

    // Judged only when the same request was already pending at the draw in the previous frame, as the range read after
    // Begin includes content drawn after the request only from then on
    private boolean isReached(Entry entry, int frame, float scrollY, float scrollMaxY) {
        return entry != null && entry.frame == frame - 1 && entry.requestSeen && entry.requestStamp == judgedStamp
                && Math.abs(scrollY - judgedRequest * scrollMaxY) <= TOLERANCE;
    }

    // Only the scrollbar moves the offset inside Begin after the applied target, which Begin clamps to the range
    private boolean isUserDrag(float scrollY, float scrollMaxY) {
        return judgedTarget >= 0 && Math.abs(scrollY - Math.min(judgedTarget, scrollMaxY)) > TOLERANCE;
    }

    // A request renewed since scrollTarget has a new stamp and stays pending; a renewal between the two reads below is
    // the accepted race, as Panel has no conditional clear
    private void clearJudgedRequest(Panel panel) {
        if (panel.requestsScrollPosition() && panel.getScrollPositionRequestTime() == judgedStamp) {
            panel.clearScrollPositionRequest();
        }
    }

    // A skipped frame, or a lower frame when a new Dear ImGui context restarts the count, drops every entry
    private void advanceTo(int frame) {
        if (frame == currentFrame) {
            return;
        }
        int endedFrame = currentFrame;
        if (frame == endedFrame + 1) {
            entries.values().removeIf(entry -> entry.frame != endedFrame);
        } else {
            entries.clear();
        }
        judged = false;
        currentFrame = frame;
    }

    private static final class Entry {
        private int frame;
        private float range;
        private boolean rangeKnown;
        private boolean requestSeen;
        private long requestStamp;

        private void record(int frame, float range, boolean rangeKnown, boolean requestSeen, long requestStamp) {
            this.frame = frame;
            this.range = range;
            this.rangeKnown = rangeKnown;
            this.requestSeen = requestSeen;
            this.requestStamp = requestStamp;
        }
    }
}
