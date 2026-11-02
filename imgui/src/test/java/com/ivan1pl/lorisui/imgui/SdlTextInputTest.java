package com.ivan1pl.lorisui.imgui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class SdlTextInputTest {
    private final AtomicInteger starts = new AtomicInteger();
    private final AtomicInteger stops = new AtomicInteger();
    private final AtomicBoolean startAccepted = new AtomicBoolean(true);
    private final SdlTextInput textInput = new SdlTextInput(
            () -> {
                starts.incrementAndGet();
                return startAccepted.get();
            },
            stops::incrementAndGet);

    @Test
    void shouldStartTextInputWhenWanted() {
        textInput.update(true);

        Assertions.assertEquals(1, starts.get());
        Assertions.assertEquals(0, stops.get());
    }

    @Test
    void shouldStartTextInputOnlyOnceWhileWanted() {
        textInput.update(true);
        textInput.update(true);
        textInput.update(true);

        Assertions.assertEquals(1, starts.get());
    }

    @Test
    void shouldStopTextInputWhenNoLongerWanted() {
        textInput.update(true);
        textInput.update(false);

        Assertions.assertEquals(1, starts.get());
        Assertions.assertEquals(1, stops.get());
    }

    @Test
    void shouldNeitherStartNorStopWhileNeverWanted() {
        textInput.update(false);
        textInput.update(false);

        Assertions.assertEquals(0, starts.get());
        Assertions.assertEquals(0, stops.get());
    }

    @Test
    void shouldRetryRefusedStartOnNextUpdate() {
        startAccepted.set(false);
        textInput.update(true);
        startAccepted.set(true);
        textInput.update(true);

        Assertions.assertEquals(2, starts.get());
    }

    @Test
    void shouldNotStopTextInputWhoseStartWasRefused() {
        startAccepted.set(false);
        textInput.update(true);
        textInput.update(false);
        textInput.release();

        Assertions.assertEquals(0, stops.get());
    }

    @Test
    void shouldStopStartedTextInputOnRelease() {
        textInput.update(true);
        textInput.release();

        Assertions.assertEquals(1, stops.get());
    }

    @Test
    void shouldNotStopTextInputOnReleaseWhenNotStarted() {
        textInput.release();
        textInput.update(true);
        textInput.update(false);
        textInput.release();

        Assertions.assertEquals(1, stops.get());
    }

    @Test
    void shouldStartTextInputAgainAfterRelease() {
        textInput.update(true);
        textInput.release();
        textInput.update(true);

        Assertions.assertEquals(2, starts.get());
        Assertions.assertEquals(1, stops.get());
    }
}
