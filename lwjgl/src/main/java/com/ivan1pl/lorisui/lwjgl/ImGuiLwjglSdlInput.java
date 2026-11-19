package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.imgui.ImGuiRenderer;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;

/**
 * The SDL input handling shared by the SDL displays: which events the UI consumes.
 */
final class ImGuiLwjglSdlInput {
    private final ImGuiRenderer renderer;
    private final DeliveredKeys deliveredKeys = new DeliveredKeys();

    ImGuiLwjglSdlInput(ImGuiRenderer renderer) {
        this.renderer = renderer;
    }

    boolean isConsumed(long sdlEventPtr) {
        int type = SDL_Event.ntype(sdlEventPtr);
        return switch (type) {
            case SDLEvents.SDL_EVENT_MOUSE_MOTION, SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN,
                 SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP -> renderer.wantsMouseInput();
            case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> renderer.wantsMouseInput() && renderer.isHoveringScrollablePanel();
            case SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP -> deliveredKeys.isConsumed(
                    SDL_KeyboardEvent.nscancode(sdlEventPtr + SDL_Event.KEY),
                    type == SDLEvents.SDL_EVENT_KEY_DOWN,
                    renderer.wantsKeyboardInput());
            case SDLEvents.SDL_EVENT_TEXT_INPUT -> renderer.wantsKeyboardInput();
            default -> false;
        };
    }
}
