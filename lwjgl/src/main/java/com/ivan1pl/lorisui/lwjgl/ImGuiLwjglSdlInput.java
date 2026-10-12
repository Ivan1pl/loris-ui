package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.imgui.ImGuiRenderer;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDL_Event;

/**
 * The SDL input handling shared by the SDL displays: which events the UI consumes, and SDL text input for the window.
 */
final class ImGuiLwjglSdlInput {
    private final long sdlWindow;
    private final ImGuiRenderer renderer;
    private boolean textInputStarted = false;

    ImGuiLwjglSdlInput(long sdlWindow, ImGuiRenderer renderer) {
        this.sdlWindow = sdlWindow;
        this.renderer = renderer;
    }

    boolean isConsumed(long sdlEventPtr) {
        return switch (SDL_Event.ntype(sdlEventPtr)) {
            case SDLEvents.SDL_EVENT_MOUSE_MOTION, SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN,
                 SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP -> renderer.wantsMouseInput();
            case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> renderer.wantsMouseInput() && renderer.isHoveringScrollablePanel();
            case SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP,
                 SDLEvents.SDL_EVENT_TEXT_INPUT -> renderer.wantsKeyboardInput();
            default -> false;
        };
    }

    // Dear ImGui's SDL3 platform backend in C starts and stops SDL text input when Dear ImGui's text input request
    // changes, but imgui-java's port does not, and SDL3 delivers no text input events until text input is started
    void updateTextInput() {
        boolean wanted = renderer.wantsTextInput();
        if (wanted == textInputStarted) {
            return;
        }
        if (wanted) {
            // A refused start only disables typing; it is not recorded, so the next frame retries instead of failing
            // a frame that has already been drawn
            textInputStarted = SDLKeyboard.SDL_StartTextInput(sdlWindow);
        } else {
            stopTextInput();
        }
    }

    void stopTextInputIfStarted() {
        if (textInputStarted) {
            stopTextInput();
        }
    }

    private void stopTextInput() {
        textInputStarted = false;
        SDLKeyboard.SDL_StopTextInput(sdlWindow);
    }
}
