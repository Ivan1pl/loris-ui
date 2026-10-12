package com.ivan1pl.lorisui.example.lwjgl;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.example.shared.ExampleScreen;
import com.ivan1pl.lorisui.example.shared.ExampleSettings;
import com.ivan1pl.lorisui.lwjgl.ImGuiLwjglGlfwGl3Display;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Locale;

public class LorisLwjglGlfwGl3Example {
    private static final String WINDOW_TITLE = "LorisUI LWJGL GLFW + OpenGL 3 Example";

    private boolean uiClosed = false;
    private boolean closeRequested = false;

    void main() throws IOException {
        Locale.setDefault(ExampleSettings.LOCALE);
        GLFWErrorCallback.createPrint(System.err).set();
        try {
            checkGlfw(GLFW.glfwInit(), "initialize GLFW");
            try {
                setWindowHints();
                long window = GLFW.glfwCreateWindow(ExampleSettings.WINDOW_WIDTH, ExampleSettings.WINDOW_HEIGHT,
                        WINDOW_TITLE, MemoryUtil.NULL, MemoryUtil.NULL);
                checkGlfw(window != MemoryUtil.NULL, "create the window");
                try {
                    GLFW.glfwMakeContextCurrent(window);
                    GLFW.glfwSwapInterval(1);
                    GL.createCapabilities();
                    GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
                    setApplicationCallbacks(window);
                    runUi(window);
                } finally {
                    // The display has given the window's callbacks back by now, so all of them are the application's
                    Callbacks.glfwFreeCallbacks(window);
                    GLFW.glfwDestroyWindow(window);
                }
            } finally {
                GLFW.glfwTerminate();
            }
        } finally {
            GLFWErrorCallback errorCallback = GLFW.glfwSetErrorCallback(null);
            if (errorCallback != null) {
                errorCallback.free();
            }
        }
    }

    // The display chains its callbacks in front of these in initialize(), so they only receive the events the UI does
    // not consume. Closing the display is not allowed inside a GLFW callback, so Escape only requests it.
    private void setApplicationCallbacks(long window) {
        GLFW.glfwSetKeyCallback(window, (_, key, _, action, _) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                closeRequested = true;
            }
        });
        GLFW.glfwSetMouseButtonCallback(window, (_, button, action, _) ->
                System.out.println("Application received mouse button " + button + " "
                        + (action == GLFW.GLFW_PRESS ? "press" : "release")));
    }

    // The display is closed when this method returns, while the OpenGL context and the window still exist
    private void runUi(long window) throws IOException {
        try (ImGuiLwjglGlfwGl3Display display = new ImGuiLwjglGlfwGl3Display(window)) {
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

    private void runLoop(long window, ImGuiLwjglGlfwGl3Display display) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer windowWidth = stack.mallocInt(1);
            IntBuffer windowHeight = stack.mallocInt(1);
            IntBuffer pixelWidth = stack.mallocInt(1);
            IntBuffer pixelHeight = stack.mallocInt(1);
            while (!GLFW.glfwWindowShouldClose(window)) {
                GLFW.glfwPollEvents();
                if (closeRequested && !uiClosed) {
                    display.close();
                    uiClosed = true;
                }
                GLFW.glfwGetWindowSize(window, windowWidth, windowHeight);
                GLFW.glfwGetFramebufferSize(window, pixelWidth, pixelHeight);
                GL11.glViewport(0, 0, pixelWidth.get(0), pixelHeight.get(0));
                GL11.glClearColor(0f, 0f, 0f, 1f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                if (!uiClosed) {
                    display.render(windowWidth.get(0), windowHeight.get(0));
                }
                GLFW.glfwSwapBuffers(window);
            }
        }
    }

    private static void setWindowHints() {
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_SRGB_CAPABLE, GLFW.GLFW_TRUE);
    }

    // The error callback has already printed GLFW's description of the failure
    private static void checkGlfw(boolean success, String action) {
        if (!success) {
            throw new IllegalStateException("Failed to " + action);
        }
    }
}
