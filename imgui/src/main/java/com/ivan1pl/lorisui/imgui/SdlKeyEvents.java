package com.ivan1pl.lorisui.imgui;

import imgui.flag.ImGuiKey;
import imgui.sdl3.ImGuiImplSdl3;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;

final class SdlKeyEvents {
    private SdlKeyEvents() {}

    // Returns true when the event must not reach Dear ImGui; a requested focus move is queued on the renderer
    static boolean divertTab(ImGuiRenderer renderer, long eventPtr) {
        return renderer.divertTab(classify(eventPtr, renderer.getWindowHandle()));
    }

    private static TabKeyFilter.Result classify(long eventPtr, long sdlWindow) {
        int type = SDL_Event.ntype(eventPtr);
        if (type != SDLEvents.SDL_EVENT_KEY_DOWN && type != SDLEvents.SDL_EVENT_KEY_UP) {
            return TabKeyFilter.Result.PASS;
        }
        long keyEvent = eventPtr + SDL_Event.KEY;
        // The same mapping as Dear ImGui's SDL3 backend, so exactly the events it would treat as Tab are diverted
        boolean tab = ImGuiImplSdl3.keyEventToImGuiKey(SDL_KeyboardEvent.nkey(keyEvent),
                SDL_KeyboardEvent.nscancode(keyEvent)) == ImGuiKey.Tab;
        // A Tab event of another window is classified like a release: withheld without a move
        boolean pressOrRepeat = tab
                && SDL_KeyboardEvent.ndown(keyEvent)
                && SDL_KeyboardEvent.nwindowID(keyEvent) == SDLVideo.SDL_GetWindowID(sdlWindow);
        int mod = SDL_KeyboardEvent.nmod(keyEvent);
        return TabKeyFilter.classify(tab, pressOrRepeat,
                (mod & SDLKeycode.SDL_KMOD_SHIFT) != 0,
                (mod & SDLKeycode.SDL_KMOD_CTRL) != 0,
                (mod & SDLKeycode.SDL_KMOD_ALT) != 0,
                (mod & SDLKeycode.SDL_KMOD_GUI) != 0);
    }
}
