package com.ivan1pl.lorisui.imgui;

import org.lwjgl.sdl.SDLKeyboard;

import java.util.function.BooleanSupplier;

final class SdlTextInput {
    private final BooleanSupplier start;
    private final Runnable stop;
    private boolean started = false;

    SdlTextInput(BooleanSupplier start, Runnable stop) {
        this.start = start;
        this.stop = stop;
    }

    // Dear ImGui's SDL3 platform backend in C starts and stops SDL text input when Dear ImGui's text input request
    // changes, but imgui-java's port does not, and SDL3 delivers no text input events until text input is started
    void update(boolean wanted) {
        if (wanted == started) {
            return;
        }
        if (wanted) {
            // A refused start only disables typing; it is not recorded, so the next frame retries instead of failing
            // a frame that has already been drawn
            started = start.getAsBoolean();
        } else {
            stopTextInput();
        }
    }

    void release() {
        if (started) {
            stopTextInput();
        }
    }

    private void stopTextInput() {
        started = false;
        stop.run();
    }

    static SdlTextInput forWindow(long sdlWindow) {
        return new SdlTextInput(
                () -> SDLKeyboard.SDL_StartTextInput(sdlWindow),
                () -> SDLKeyboard.SDL_StopTextInput(sdlWindow));
    }
}
