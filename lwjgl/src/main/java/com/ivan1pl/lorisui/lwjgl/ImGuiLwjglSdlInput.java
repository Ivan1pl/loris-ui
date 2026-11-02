package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.imgui.ImGuiRenderer;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;

/**
 * The SDL input handling shared by the SDL displays: which events the UI consumes.
 */
final class ImGuiLwjglSdlInput {
    private final ImGuiRenderer renderer;

    ImGuiLwjglSdlInput(ImGuiRenderer renderer) {
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
}
