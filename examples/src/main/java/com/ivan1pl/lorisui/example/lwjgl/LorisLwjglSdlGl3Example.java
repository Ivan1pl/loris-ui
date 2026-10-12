package com.ivan1pl.lorisui.example.lwjgl;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.example.shared.ExampleScreen;
import com.ivan1pl.lorisui.example.shared.ExampleSettings;
import com.ivan1pl.lorisui.lwjgl.ImGuiLwjglDisplay;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Locale;

public class LorisLwjglExample {
    private static final String WINDOW_TITLE = "LorisUI LWJGL Example";

    private boolean uiClosed = false;

    void main() throws IOException {
        Locale.setDefault(ExampleSettings.LOCALE);
        checkSdl(SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO), "initialize the SDL video subsystem");
        try {
            setGlAttributes();
            long window = SDLVideo.SDL_CreateWindow(WINDOW_TITLE, ExampleSettings.WINDOW_WIDTH,
                    ExampleSettings.WINDOW_HEIGHT, SDLVideo.SDL_WINDOW_OPENGL | SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY);
            checkSdl(window != 0, "create the window");
            try {
                long glContext = SDLVideo.SDL_GL_CreateContext(window);
                checkSdl(glContext != 0, "create the OpenGL context");
                try {
                    checkSdl(SDLVideo.SDL_GL_MakeCurrent(window, glContext), "make the OpenGL context current");
                    // vsync is not supported everywhere, and the example runs without it
                    SDLVideo.SDL_GL_SetSwapInterval(1);
                    GL.createCapabilities();
                    GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
                    runUi(window);
                } finally {
                    SDLVideo.SDL_GL_DestroyContext(glContext);
                }
            } finally {
                SDLVideo.SDL_DestroyWindow(window);
            }
        } finally {
            SDLInit.SDL_Quit();
        }
    }

    // The display is closed when this method returns, while the OpenGL context and the window still exist
    private void runUi(long window) throws IOException {
        try (ImGuiLwjglDisplay display = new ImGuiLwjglDisplay(window)) {
            display.setResourceBundle(ExampleSettings.BUNDLE_NAME, ExampleSettings.BUNDLE_PATH);
            Font font = display.registerFontFromResources(ExampleSettings.FONT_PATH, ExampleSettings.FONT_SIZE);
            display.addScreen(ExampleSettings.SCREEN_NAME, new ExampleScreen(font, () -> {
                display.close();
                uiClosed = true;
            }));
            display.setCurrentScreen(ExampleSettings.SCREEN_NAME);
            display.initialize();
            runLoop(window, display);
        }
    }

    private void runLoop(long window, ImGuiLwjglDisplay display) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Event event = SDL_Event.calloc(stack);
            IntBuffer windowWidth = stack.mallocInt(1);
            IntBuffer windowHeight = stack.mallocInt(1);
            IntBuffer pixelWidth = stack.mallocInt(1);
            IntBuffer pixelHeight = stack.mallocInt(1);
            while (processEvents(event, display)) {
                checkSdl(SDLVideo.SDL_GetWindowSize(window, windowWidth, windowHeight), "read the window size");
                checkSdl(SDLVideo.SDL_GetWindowSizeInPixels(window, pixelWidth, pixelHeight),
                        "read the window size in pixels");
                GL11.glViewport(0, 0, pixelWidth.get(0), pixelHeight.get(0));
                GL11.glClearColor(0f, 0f, 0f, 1f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                if (!uiClosed) {
                    display.render(windowWidth.get(0), windowHeight.get(0));
                }
                checkSdl(SDLVideo.SDL_GL_SwapWindow(window), "swap the window");
            }
        }
    }

    // Returns false once the application should quit. The example has no input handling of its own, so it ignores
    // whether the UI consumed an event.
    private static boolean processEvents(SDL_Event event, ImGuiLwjglDisplay display) {
        while (SDLEvents.SDL_PollEvent(event)) {
            int type = event.type();
            if (type == SDLEvents.SDL_EVENT_QUIT || type == SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED) {
                return false;
            }
            display.processEvent(event.address());
        }
        return true;
    }

    private static void setGlAttributes() {
        setGlAttribute(SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION, 3);
        setGlAttribute(SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION, 3);
        setGlAttribute(SDLVideo.SDL_GL_CONTEXT_PROFILE_MASK, SDLVideo.SDL_GL_CONTEXT_PROFILE_CORE);
        setGlAttribute(SDLVideo.SDL_GL_CONTEXT_FLAGS, SDLVideo.SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG);
        setGlAttribute(SDLVideo.SDL_GL_DOUBLEBUFFER, 1);
        setGlAttribute(SDLVideo.SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 1);
    }

    private static void setGlAttribute(int attribute, int value) {
        checkSdl(SDLVideo.SDL_GL_SetAttribute(attribute, value), "set OpenGL attribute " + attribute);
    }

    private static void checkSdl(boolean success, String action) {
        if (!success) {
            throw new IllegalStateException("Failed to " + action + ": " + SDLError.SDL_GetError());
        }
    }
}
