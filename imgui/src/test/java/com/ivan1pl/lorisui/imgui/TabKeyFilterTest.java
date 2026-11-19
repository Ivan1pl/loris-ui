package com.ivan1pl.lorisui.imgui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TabKeyFilterTest {
    @Test
    void shouldMoveForwardOnTabPress() {
        Assertions.assertEquals(TabKeyFilter.Result.MOVE_FORWARD,
                TabKeyFilter.classify(true, true, false, false, false, false));
    }

    @Test
    void shouldMoveBackOnShiftTabPress() {
        Assertions.assertEquals(TabKeyFilter.Result.MOVE_BACK,
                TabKeyFilter.classify(true, true, true, false, false, false));
    }

    @Test
    void shouldWithholdWithoutMoveWhenCtrlAltOrSuperIsHeld() {
        Assertions.assertEquals(TabKeyFilter.Result.WITHHOLD,
                TabKeyFilter.classify(true, true, false, true, false, false));
        Assertions.assertEquals(TabKeyFilter.Result.WITHHOLD,
                TabKeyFilter.classify(true, true, false, false, true, false));
        Assertions.assertEquals(TabKeyFilter.Result.WITHHOLD,
                TabKeyFilter.classify(true, true, false, false, false, true));
        Assertions.assertEquals(TabKeyFilter.Result.WITHHOLD,
                TabKeyFilter.classify(true, true, true, true, false, false));
    }

    @Test
    void shouldWithholdTabReleaseAndPassOtherKeys() {
        Assertions.assertEquals(TabKeyFilter.Result.WITHHOLD,
                TabKeyFilter.classify(true, false, false, false, false, false));
        Assertions.assertEquals(TabKeyFilter.Result.WITHHOLD,
                TabKeyFilter.classify(true, false, true, true, true, true));
        Assertions.assertEquals(TabKeyFilter.Result.PASS,
                TabKeyFilter.classify(false, true, false, false, false, false));
        Assertions.assertEquals(TabKeyFilter.Result.PASS,
                TabKeyFilter.classify(false, false, true, true, true, true));
    }
}
