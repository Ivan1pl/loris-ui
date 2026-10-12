package com.ivan1pl.lorisui.example.lwjgl;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.example.shared.ExampleScreen;
import com.ivan1pl.lorisui.example.shared.ExampleSettings;
import com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGpuDisplay;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLGPU;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_GPUColorTargetInfo;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Locale;

public class LorisLwjglSdlGpuExample {
    private static final String WINDOW_TITLE = "LorisUI LWJGL SDL3 + SDL_GPU Example";
    private static final String GPU_DEBUG_PROPERTY = "lorisui.example.gpuDebug";
    private static final int SHADER_FORMATS = SDLGPU.SDL_GPU_SHADERFORMAT_SPIRV | SDLGPU.SDL_GPU_SHADERFORMAT_DXBC
            | SDLGPU.SDL_GPU_SHADERFORMAT_METALLIB;

    private boolean uiClosed = false;

    void main() throws IOException {
        Locale.setDefault(ExampleSettings.LOCALE);
        checkSdl(SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO), "initialize the SDL video subsystem");
        try {
            long window = SDLVideo.SDL_CreateWindow(WINDOW_TITLE, ExampleSettings.WINDOW_WIDTH,
                    ExampleSettings.WINDOW_HEIGHT, SDLVideo.SDL_WINDOW_RESIZABLE
                            | SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY);
            checkSdl(window != 0, "create the window");
            try {
                long device = SDLGPU.SDL_CreateGPUDevice(SHADER_FORMATS, Boolean.getBoolean(GPU_DEBUG_PROPERTY),
                        (CharSequence) null);
                checkSdl(device != 0, "create the GPU device");
                try {
                    checkSdl(SDLGPU.SDL_ClaimWindowForGPUDevice(device, window), "claim the window for the GPU device");
                    try {
                        setUpSwapchain(window, device);
                        runUi(window, device);
                    } finally {
                        SDLGPU.SDL_WaitForGPUIdle(device);
                        SDLGPU.SDL_ReleaseWindowFromGPUDevice(device, window);
                    }
                } finally {
                    SDLGPU.SDL_DestroyGPUDevice(device);
                }
            } finally {
                SDLVideo.SDL_DestroyWindow(window);
            }
        } finally {
            SDLInit.SDL_Quit();
        }
    }

    // The display's images are sRGB textures; only the SDR_LINEAR swapchain shows them as the OpenGL examples do
    private static void setUpSwapchain(long window, long device) {
        if (!SDLGPU.SDL_WindowSupportsGPUSwapchainComposition(device, window,
                SDLGPU.SDL_GPU_SWAPCHAINCOMPOSITION_SDR_LINEAR)) {
            throw new IllegalStateException("The GPU device does not support the SDR_LINEAR swapchain composition "
                    + "for the window, which the example needs to show sRGB colors correctly");
        }
        checkSdl(SDLGPU.SDL_SetGPUSwapchainParameters(device, window, SDLGPU.SDL_GPU_SWAPCHAINCOMPOSITION_SDR_LINEAR,
                SDLGPU.SDL_GPU_PRESENTMODE_VSYNC), "set the swapchain parameters");
    }

    // The display is closed when this method returns, while the GPU device still owns the window
    private void runUi(long window, long device) throws IOException {
        try (ImGuiLwjglSdlGpuDisplay display = new ImGuiLwjglSdlGpuDisplay(window, device)) {
            display.setResourceBundle(ExampleSettings.BUNDLE_NAME, ExampleSettings.BUNDLE_PATH);
            Font font = display.registerFontFromResources(ExampleSettings.FONT_PATH, ExampleSettings.FONT_SIZE);
            display.addScreen(ExampleSettings.SCREEN_NAME, new ExampleScreen(font, () -> {
                display.close();
                uiClosed = true;
            }));
            display.setCurrentScreen(ExampleSettings.SCREEN_NAME);
            display.initialize();
            runLoop(window, device, display);
        }
    }

    private void runLoop(long window, long device, ImGuiLwjglSdlGpuDisplay display) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Event event = SDL_Event.calloc(stack);
            IntBuffer windowWidth = stack.mallocInt(1);
            IntBuffer windowHeight = stack.mallocInt(1);
            PointerBuffer swapchainTexture = stack.mallocPointer(1);
            SDL_GPUColorTargetInfo.Buffer clearTarget = SDL_GPUColorTargetInfo.calloc(1, stack);
            clearTarget.get(0)
                    .load_op(SDLGPU.SDL_GPU_LOADOP_CLEAR)
                    .store_op(SDLGPU.SDL_GPU_STOREOP_STORE)
                    .clear_color()
                    .set(0f, 0f, 0f, 1f);
            while (processEvents(event, display)) {
                checkSdl(SDLVideo.SDL_GetWindowSize(window, windowWidth, windowHeight), "read the window size");
                long commandBuffer = SDLGPU.SDL_AcquireGPUCommandBuffer(device);
                checkSdl(commandBuffer != 0, "acquire a GPU command buffer");
                if (!SDLGPU.SDL_WaitAndAcquireGPUSwapchainTexture(commandBuffer, window, swapchainTexture, null,
                        null)) {
                    IllegalStateException failure = sdlFailure("acquire the swapchain texture");
                    SDLGPU.SDL_CancelGPUCommandBuffer(commandBuffer);
                    throw failure;
                }
                // SDL forbids cancelling a command buffer once a swapchain texture is acquired, so it is submitted
                // even when the frame fails
                try {
                    drawFrame(display, commandBuffer, swapchainTexture.get(0), clearTarget, windowWidth.get(0),
                            windowHeight.get(0));
                } catch (Throwable failure) {
                    DeferredRelease.runAfterFailure(failure, () -> submit(commandBuffer));
                    throw failure;
                }
                submit(commandBuffer);
            }
        }
    }

    // The texture is 0 while the window is minimized; the UI still runs, but nothing is drawn
    private void drawFrame(ImGuiLwjglSdlGpuDisplay display, long commandBuffer, long targetTexture,
                           SDL_GPUColorTargetInfo.Buffer clearTarget, int width, int height) {
        if (targetTexture != 0) {
            clearTarget.get(0).texture(targetTexture);
            long renderPass = SDLGPU.SDL_BeginGPURenderPass(commandBuffer, clearTarget, null);
            checkSdl(renderPass != 0, "begin the clear render pass");
            SDLGPU.SDL_EndGPURenderPass(renderPass);
        }
        if (!uiClosed) {
            display.render(width, height, commandBuffer, targetTexture);
        }
    }

    private static void submit(long commandBuffer) {
        checkSdl(SDLGPU.SDL_SubmitGPUCommandBuffer(commandBuffer), "submit the GPU command buffer");
    }

    // Returns false once the application should quit. The example has no input handling of its own, so it ignores
    // whether the UI consumed an event.
    private static boolean processEvents(SDL_Event event, ImGuiLwjglSdlGpuDisplay display) {
        while (SDLEvents.SDL_PollEvent(event)) {
            int type = event.type();
            if (type == SDLEvents.SDL_EVENT_QUIT || type == SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED) {
                return false;
            }
            display.processEvent(event.address());
        }
        return true;
    }

    private static void checkSdl(boolean success, String action) {
        if (!success) {
            throw sdlFailure(action);
        }
    }

    private static IllegalStateException sdlFailure(String action) {
        return new IllegalStateException("Failed to " + action + ": " + SDLError.SDL_GetError());
    }
}
