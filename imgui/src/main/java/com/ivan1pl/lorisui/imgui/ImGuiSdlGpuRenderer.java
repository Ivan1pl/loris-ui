package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiSdlGpuBackend;
import imgui.ImGui;
import imgui.sdl3.ImGuiImplSdl3;
import imgui.sdl3.ImGuiImplSdlGpu3;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLGPU;
import org.lwjgl.sdl.SDL_GPUColorTargetInfo;
import org.lwjgl.system.MemoryStack;

/**
 * Renders LorisUI screens into an SDL3 window with Dear ImGui and SDL_GPU, one frame at a time.
 *
 * <p>This variant pairs Dear ImGui's SDL3 platform backend with its SDL_GPU renderer backend and is used with an
 * {@link ImGuiSdlGpuBackend}, whose GPU device it draws with. The lifecycle, the font registration and the input
 * queries it shares with the other variants are described in {@link ImGuiRenderer}. With this variant:
 * <ul>
 *     <li>Create the window without {@code SDL_WINDOW_OPENGL} and claim it for the GPU device with
 *     {@code SDL_ClaimWindowForGPUDevice} before calling {@link #initialize()}.</li>
 *     <li>Before {@link #initialize()}, set the window's swapchain to the
 *     {@code SDL_GPU_SWAPCHAINCOMPOSITION_SDR_LINEAR} composition with {@code SDL_SetGPUSwapchainParameters}, with the
 *     present mode of your choice. The backend's textures are sRGB, so only with this composition do images look the
 *     same as on the OpenGL variants.
 *     {@link #initialize()} reads the swapchain's texture format, so the swapchain parameters must not change while
 *     the renderer is initialized; to change them, dispose the renderer and initialize it again.</li>
 *     <li>Each frame, forward every SDL event of the window to {@link #processSdlEvent(long)} and call
 *     {@link #render} once. {@link #wantsTextInput()} reports whether the host should keep SDL text input
 *     started.</li>
 *     <li>Call {@link #dispose()} before the window is released from the GPU device or destroyed. It waits until the
 *     GPU device is idle before it releases the textures and the GPU resources of the renderer.</li>
 * </ul>
 *
 * <p><b>Frame contract.</b> The application owns the frame: it acquires a command buffer and the texture to draw into,
 * clears that texture in a render pass of its own before calling {@link #render}, and submits the command buffer
 * afterwards. The renderer records its uploads into the command buffer and draws the UI in a render pass of its own,
 * which keeps the texture's contents. The texture is the window's swapchain texture or a texture with the same format
 * and a sample count of 1. Images that the UI loads while a frame is built are uploaded with command buffers of their
 * own, which are submitted at once and so run before the frame's command buffer.
 *
 * <p>The renderer does not own the GPU device. The device must stay alive until {@link #dispose()} has returned; the
 * application destroys it afterwards.
 *
 * <p>Initializing the renderer sets global SDL hints through Dear ImGui's SDL3 platform backend:
 * {@code SDL_HINT_MOUSE_FOCUS_CLICKTHROUGH} to {@code "1"}, {@code SDL_HINT_MOUSE_AUTO_CAPTURE} to {@code "0"} and
 * {@code SDL_HINT_BORDERLESS_WINDOWED_STYLE} to {@code "0"}. They affect every SDL window of the application and stay
 * set after {@link #dispose()}; an application that needs other values sets them again after {@link #initialize()}.
 *
 * <h2>Threading</h2>
 * The renderer is not thread-safe. All methods must be called on the host's UI thread, the thread that owns the
 * window and submits the GPU device's command buffers.
 */
public final class ImGuiSdlGpuRenderer extends ImGuiRenderer {
    private final long gpuDevice;
    private ImGuiImplSdl3 implSdl3;
    private ImGuiImplSdlGpu3 implSdlGpu3;

    /**
     * Creates a renderer that draws with the given backend into the given SDL3 window, on the backend's GPU device.
     * Nothing is drawn and no Dear ImGui or SDL state is touched until the renderer is
     * {@linkplain #initialize() initialized}.
     *
     * @param backend   backend that draws the screen's components and whose GPU device the renderer uses, not used
     *                  by any other renderer; must not be null
     * @param sdlWindow native handle of the SDL3 window to render into ({@code SDL_Window*}); must not be {@code 0}
     * @throws NullPointerException     if {@code backend} is {@code null}
     * @throws IllegalArgumentException if {@code sdlWindow} is {@code 0}
     */
    public ImGuiSdlGpuRenderer(ImGuiSdlGpuBackend backend, long sdlWindow) {
        super(backend, sdlWindow);
        if (sdlWindow == 0L) {
            throw new IllegalArgumentException("SDL window handle must not be 0");
        }
        this.gpuDevice = backend.getGpuDevice();
    }

    /**
     * Passes an SDL event of the window to Dear ImGui, so that the UI sees mouse, keyboard, text and window input.
     * Hosts forward every event of the window while the renderer is initialized.
     *
     * @param eventPtr native address of the {@code SDL_Event}
     * @throws IllegalStateException if the renderer is not initialized
     */
    public void processSdlEvent(long eventPtr) {
        checkInitialized();
        makeContextCurrent();
        implSdl3.processEvent(eventPtr);
    }

    /**
     * Renders one frame showing the given screen into the given texture. Call it once per host frame, while the
     * renderer is initialized, after clearing the texture and before submitting the command buffer (see the class
     * documentation).
     *
     * <p>{@code w} and {@code h} are the size of the window in logical window coordinates, as reported by
     * {@code SDL_GetWindowSize}, not in pixels; Dear ImGui's SDL3 backend applies the pixel density of HiDPI displays
     * itself.
     *
     * <p>When {@code targetTexture} is {@code 0}, for example because {@code SDL_WaitAndAcquireGPUSwapchainTexture}
     * returned no texture while the window is minimized, the frame is still built, so the UI keeps its state and
     * callbacks, but nothing is drawn.
     *
     * <p>The frame is closed even when the screen throws, and the exception is then rethrown to the caller. For
     * example, the {@link IllegalStateException} with which {@link LorisScreen#render} reports a failed layout reaches
     * the host this way. If closing the frame fails too, that failure is added to the screen's exception as a
     * suppressed exception.
     *
     * @param screen            screen to render; must not be null
     * @param w                 screen width in logical window coordinates
     * @param h                 screen height in logical window coordinates
     * @param bundleKeyResolver resolver for localized text; must not be null
     * @param commandBuffer     handle of the frame's {@code SDL_GPUCommandBuffer}, which the application submits after
     *                          this call; must not be {@code 0}
     * @param targetTexture     handle of the {@code SDL_GPUTexture} to draw into, usually the window's swapchain
     *                          texture, or {@code 0} to build the frame without drawing it
     * @throws IllegalArgumentException if {@code commandBuffer} is {@code 0}; checked before anything else
     * @throws NullPointerException     if {@code screen} or {@code bundleKeyResolver} is {@code null}
     * @throws IllegalStateException    if the renderer is not initialized or a frame is already being rendered
     *                                  (checked before a frame is started), if the screen's most recent layout failed
     *                                  (see {@link LorisScreen#render}), if the backend has no renderer or style
     *                                  applier for one of its components or styles, or if SDL cannot begin the render
     *                                  pass; the message then names the SDL error
     */
    public void render(LorisScreen screen, int w, int h, BundleKeyResolver bundleKeyResolver, long commandBuffer,
                       long targetTexture) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("Command buffer handle must not be 0");
        }
        renderFrame(screen, w, h, bundleKeyResolver, () -> drawFrame(commandBuffer, targetTexture));
    }

    @Override
    void initBackends() {
        long window = getWindowHandle();
        int colorTargetFormat = SDLGPU.SDL_GetGPUSwapchainTextureFormat(gpuDevice, window);
        if (colorTargetFormat == SDLGPU.SDL_GPU_TEXTUREFORMAT_INVALID) {
            throw new IllegalStateException("The window is not claimed for the GPU device; call "
                    + "SDL_ClaimWindowForGPUDevice before initialize");
        }
        ImGuiImplSdl3 sdl3 = new ImGuiImplSdl3();
        if (!sdl3.initForSDLGPU(window)) {
            throw new IllegalStateException("Dear ImGui SDL3 platform backend failed to initialize");
        }
        implSdl3 = sdl3;
        ImGuiImplSdlGpu3 sdlGpu3 = new ImGuiImplSdlGpu3();
        try {
            ImGuiImplSdlGpu3.InitInfo initInfo = new ImGuiImplSdlGpu3.InitInfo()
                    .setDevice(gpuDevice)
                    .setColorTargetFormat(colorTargetFormat);
            if (!sdlGpu3.init(initInfo)) {
                throw new IllegalStateException("Dear ImGui SDL_GPU renderer backend failed to initialize");
            }
        } catch (Throwable failure) {
            // Unlike the other backend objects, a failed SDL_GPU backend may already own shaders or a pipeline, so it
            // is shut down here; it is never kept, so the release of the renderer does not shut it down again
            DeferredRelease.runAfterFailure(failure, sdlGpu3::shutdown);
            throw failure;
        }
        implSdlGpu3 = sdlGpu3;
    }

    @Override
    void newBackendFrame() {
        implSdlGpu3.newFrame();
        implSdl3.newFrame();
    }

    @Override
    void awaitDeviceIdle() {
        if (!SDLGPU.SDL_WaitForGPUIdle(gpuDevice)) {
            throw new IllegalStateException("SDL_WaitForGPUIdle failed: " + SDLError.SDL_GetError());
        }
    }

    @Override
    void shutdownRendererBackend() {
        if (implSdlGpu3 != null) {
            implSdlGpu3.shutdown();
        }
    }

    @Override
    void shutdownPlatformBackend() {
        if (implSdl3 != null) {
            implSdl3.shutdown();
        }
    }

    @Override
    void clearBackends() {
        implSdlGpu3 = null;
        implSdl3 = null;
    }

    private void drawFrame(long commandBuffer, long targetTexture) {
        if (targetTexture == 0L) {
            return;
        }
        long drawData = ImGui.getDrawData().ptr;
        implSdlGpu3.prepareDrawData(drawData, commandBuffer);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_GPUColorTargetInfo.Buffer colorTargets = SDL_GPUColorTargetInfo.calloc(1, stack);
            colorTargets.get(0)
                    .texture(targetTexture)
                    .load_op(SDLGPU.SDL_GPU_LOADOP_LOAD)
                    .store_op(SDLGPU.SDL_GPU_STOREOP_STORE);
            long renderPass = SDLGPU.SDL_BeginGPURenderPass(commandBuffer, colorTargets, null);
            if (renderPass == 0L) {
                throw new IllegalStateException("SDL_BeginGPURenderPass failed: " + SDLError.SDL_GetError());
            }
            try {
                implSdlGpu3.renderDrawData(drawData, commandBuffer, renderPass);
            } finally {
                SDLGPU.SDL_EndGPURenderPass(renderPass);
            }
        }
    }
}
