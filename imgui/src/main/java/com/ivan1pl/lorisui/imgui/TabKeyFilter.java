package com.ivan1pl.lorisui.imgui;

final class TabKeyFilter {
    private TabKeyFilter() {}

    // Every Tab event is withheld from Dear ImGui, whatever its modifiers, so its Ctrl+Tab windowing never runs
    static Result classify(boolean tab, boolean pressOrRepeat, boolean shift, boolean ctrl, boolean alt,
                           boolean superKey) {
        if (!tab) {
            return Result.PASS;
        }
        if (!pressOrRepeat || ctrl || alt || superKey) {
            return Result.WITHHOLD;
        }
        return shift ? Result.MOVE_BACK : Result.MOVE_FORWARD;
    }

    // A move is withheld from Dear ImGui as well
    enum Result {
        PASS,
        WITHHOLD,
        MOVE_FORWARD,
        MOVE_BACK
    }
}
