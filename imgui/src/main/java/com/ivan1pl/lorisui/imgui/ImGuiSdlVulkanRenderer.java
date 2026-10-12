package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiVulkanBackend;
import com.ivan1pl.lorisui.imgui.vulkan.ImGuiImplVulkan;
import imgui.ImDrawData;
import imgui.ImGui;
import imgui.sdl3.ImGuiImplSdl3;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import java.util.Objects;

/**
 * Renders LorisUI screens into an SDL3 window with Dear ImGui and Vulkan, one frame at a time.
 *
 * <p>This variant pairs Dear ImGui's SDL3 platform backend with a Vulkan renderer backend and is used with an
 * {@link ImGuiVulkanBackend}, whose device and queue it draws with. The lifecycle, the font registration and the input
 * queries it shares with the other variants are described in {@link ImGuiRenderer}. With this variant:
 * <ul>
 *     <li>Create the window with {@code SDL_WINDOW_VULKAN}. SDL and LWJGL must use the same Vulkan library, for
 *     example by passing the library that LWJGL loaded to {@code SDL_Vulkan_LoadLibrary}.</li>
 *     <li>Create the Vulkan instance and device for API version 1.3, with the {@code dynamicRendering} feature
 *     enabled. The renderer draws with dynamic rendering and needs no render pass of the application.</li>
 *     <li>Use an sRGB swapchain format, such as {@code VK_FORMAT_B8G8R8A8_SRGB}. The backend's textures are sRGB, so
 *     only with an sRGB format do images look the same as on the OpenGL variants.</li>
 *     <li>Each frame, forward every SDL event of the window to {@link #processSdlEvent(long)} and call
 *     {@link #render} once. {@link #wantsTextInput()} reports whether the host should keep SDL text input
 *     started.</li>
 *     <li>Call {@link #dispose()} before the device is destroyed. It waits until the device is idle before it releases
 *     the textures and the Vulkan resources of the renderer.</li>
 * </ul>
 *
 * <p><b>Color attachment format.</b> The renderer draws into images of the format given at construction, for the
 * renderer's whole life; {@link #dispose()} and {@link #initialize()} keep it. To render into images of another
 * format, for example after the swapchain was recreated with another format, dispose the renderer and create a new
 * one.
 *
 * <p><b>Frame contract.</b> The application owns the frame: it records a command buffer, clears the target image and
 * transitions it to {@code VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL} before calling {@link #render}, and transitions,
 * ends and submits the command buffer afterwards (see {@link #render} for the details). The renderer reuses what it
 * records for a frame {@code framesInFlight} frames later, so the application keeps at most that many frames in
 * flight: before it records a frame, it waits until the frame recorded {@code framesInFlight} frames earlier has
 * finished executing.
 *
 * <p><b>Queue.</b> The backend's queue must be a queue of the graphics-capable queue family whose index the backend was
 * created with, and it must be the queue on which the application submits the command buffers it passes to
 * {@link #render}. {@link #initialize()} submits the
 * upload of the font atlas to that queue and waits for it to finish; images that the UI loads while a frame is built
 * are uploaded the same way.
 *
 * <p>The renderer does not own the Vulkan instance, device or queue. The device must stay alive until
 * {@link #dispose()} has returned; the application destroys it afterwards.
 *
 * <p>Initializing the renderer sets global SDL hints through Dear ImGui's SDL3 platform backend:
 * {@code SDL_HINT_MOUSE_FOCUS_CLICKTHROUGH} to {@code "1"}, {@code SDL_HINT_MOUSE_AUTO_CAPTURE} to {@code "0"} and
 * {@code SDL_HINT_BORDERLESS_WINDOWED_STYLE} to {@code "0"}. They affect every SDL window of the application and stay
 * set after {@link #dispose()}; an application that needs other values sets them again after {@link #initialize()}.
 *
 * <h2>Threading</h2>
 * The renderer is not thread-safe. All methods must be called on the host's UI thread, the thread that owns the
 * window. The renderer submits to the backend's queue from that thread; an application that also submits to the queue
 * from other threads must not do so while a method of the renderer or its backend runs, because Vulkan queues need
 * external synchronization.
 */
public final class ImGuiSdlVulkanRenderer extends ImGuiRenderer {
    private final VkDevice device;
    private final int queueFamilyIndex;
    private final VkQueue queue;
    private final int colorAttachmentFormat;
    private final int framesInFlight;
    private ImGuiImplSdl3 implSdl3;
    private ImGuiImplVulkan implVulkan;
    private boolean skipDraw = false;

    /**
     * Creates a renderer that draws with the given backend into the given SDL3 window, on the backend's device and
     * queue. Nothing is drawn and no Dear ImGui, SDL or Vulkan state is touched until the renderer is
     * {@linkplain #initialize() initialized}.
     *
     * @param backend               backend that draws the screen's components and whose device and queue the renderer
     *                              uses; must not be null
     * @param sdlWindow             native handle of the SDL3 window to render into ({@code SDL_Window*}), created
     *                              with {@code SDL_WINDOW_VULKAN}; must not be {@code 0}
     * @param colorAttachmentFormat {@code VkFormat} of the images the renderer draws into, usually the swapchain's
     *                              sRGB format; fixed for the renderer's whole life; must be positive (not
     *                              {@code VK_FORMAT_UNDEFINED})
     * @param framesInFlight        maximum number of frames the application keeps in flight; must be at least 1
     * @throws IllegalArgumentException if {@code sdlWindow} is {@code 0}, {@code colorAttachmentFormat} is not positive
     *                                  or {@code framesInFlight} is less than 1; checked in this order, before
     *                                  {@code backend}
     * @throws NullPointerException     if {@code backend} is {@code null}
     */
    public ImGuiSdlVulkanRenderer(ImGuiVulkanBackend backend, long sdlWindow, int colorAttachmentFormat,
                                  int framesInFlight) {
        if (sdlWindow == 0L) {
            throw new IllegalArgumentException("SDL window handle must not be 0");
        }
        if (colorAttachmentFormat <= 0) {
            throw new IllegalArgumentException("Color attachment format must be a defined VkFormat: "
                    + colorAttachmentFormat);
        }
        if (framesInFlight < 1) {
            throw new IllegalArgumentException("Frames in flight must be at least 1: " + framesInFlight);
        }
        super(backend, sdlWindow);
        this.device = backend.getDevice();
        this.queueFamilyIndex = backend.getQueueFamilyIndex();
        this.queue = backend.getQueue();
        this.colorAttachmentFormat = colorAttachmentFormat;
        this.framesInFlight = framesInFlight;
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
     * Renders one frame showing the given screen, recording its draw commands into the given command buffer. Call it
     * once per host frame, while the renderer is initialized.
     *
     * <p>{@code w} and {@code h} are the size of the window in logical window coordinates, as reported by
     * {@code SDL_GetWindowSize}, not in pixels; Dear ImGui's SDL3 backend applies the pixel density of HiDPI displays
     * itself.
     *
     * <p><b>Frame contract.</b>
     * <ul>
     *     <li>The application has begun recording {@code commandBuffer}, a command buffer of the backend's device,
     *     and submits it on the backend's queue.</li>
     *     <li>No render pass or dynamic rendering instance is active on {@code commandBuffer}: the renderer records a
     *     pipeline barrier and a dynamic rendering instance of its own, so the application ends its render pass or
     *     rendering instance, for example the one it cleared the image with, before the call.</li>
     *     <li>The target image is in {@code VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL}, has the color attachment format
     *     given at construction and a sample count of 1, and is at least as large as the window in pixels, as reported
     *     by {@code SDL_GetWindowSizeInPixels}. When the image is smaller, for example while a resize of the swapchain
     *     is pending, pass {@code 0} as {@code targetImageView} for that frame: the UI still updates, and nothing is
     *     drawn.</li>
     *     <li>Earlier writes of the command buffer to the target image are made, or synchronized, in the
     *     {@code VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT} stage. The renderer loads the image's contents and
     *     draws the UI over them.</li>
     *     <li>After the call, the application transitions the image, for example to
     *     {@code VK_IMAGE_LAYOUT_PRESENT_SRC_KHR}, ends the command buffer and submits it.</li>
     *     <li>The application keeps at most {@code framesInFlight} frames in flight (see the class documentation).</li>
     *     <li>If this method throws, the application does not submit the command buffer; it resets it instead.</li>
     *     <li>The renderer must not be disposed between this call and the submission of the command buffer, because
     *     the commands use the renderer's resources, unless {@link #skipFrameDraw()} was called during the frame.</li>
     * </ul>
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
     * @param commandBuffer     the frame's command buffer, in the recording state, which the application submits
     *                          after this call; must not be null
     * @param targetImageView   handle of the {@code VkImageView} to draw into, usually the view of the acquired
     *                          swapchain image, or {@code 0} to build the frame without drawing it
     * @throws NullPointerException     if {@code commandBuffer}, {@code screen} or {@code bundleKeyResolver} is
     *                                  {@code null}; {@code commandBuffer} is checked first
     * @throws IllegalArgumentException if {@code commandBuffer} belongs to another device than the backend's;
     *                                  checked after the {@code null} check and before the state of the renderer
     * @throws IllegalStateException    if the renderer is not initialized or a frame is already being rendered
     *                                  (checked before a frame is started), if the screen's most recent layout failed
     *                                  (see {@link LorisScreen#render}), if the backend has no renderer or style
     *                                  applier for one of its components or styles, or if Vulkan fails to create the
     *                                  buffers of the frame or to load an image; the message then names the Vulkan
     *                                  function and its {@code VkResult}
     */
    public void render(LorisScreen screen, int w, int h, BundleKeyResolver bundleKeyResolver,
                       VkCommandBuffer commandBuffer, long targetImageView) {
        Objects.requireNonNull(commandBuffer, "commandBuffer");
        if (commandBuffer.getDevice() != device) {
            throw new IllegalArgumentException("Command buffer must belong to the backend's device");
        }
        renderFrame(screen, w, h, bundleKeyResolver, () -> drawFrame(commandBuffer, targetImageView));
    }

    /**
     * Makes the frame being rendered record no commands into its command buffer. The UI is still updated, but nothing
     * is drawn, so the renderer may be disposed as soon as {@link #render} has returned, before the application
     * submits the command buffer. Hosts call it when the UI is torn down during a frame, for example from a component
     * callback, and dispose the renderer once {@link #render} has returned.
     *
     * <p>Calling it again during the same frame has no further effect. It affects only the frame being rendered; the
     * next frame is drawn again.
     *
     * @throws IllegalStateException if no frame is being rendered
     */
    public void skipFrameDraw() {
        if (!isRendering()) {
            throw new IllegalStateException("No frame is being rendered");
        }
        skipDraw = true;
    }

    @Override
    void initBackends() {
        if (!device.getCapabilities().Vulkan13) {
            throw new IllegalStateException("The Vulkan device does not expose Vulkan 1.3 functions; create the "
                    + "instance and device for API version 1.3 and enable the dynamicRendering feature");
        }
        ImGuiImplSdl3 sdl3 = new ImGuiImplSdl3();
        if (!sdl3.initForVulkan(getWindowHandle())) {
            throw new IllegalStateException("Dear ImGui SDL3 platform backend failed to initialize");
        }
        implSdl3 = sdl3;
        ImGuiImplVulkan vulkan = new ImGuiImplVulkan();
        try {
            vulkan.init(device, queueFamilyIndex, queue, colorAttachmentFormat, framesInFlight);
        } catch (Throwable failure) {
            // A partial init already owns Vulkan objects, so the failed instance is released here and never kept
            DeferredRelease.runAfterFailure(failure, vulkan::shutdown);
            throw failure;
        }
        implVulkan = vulkan;
    }

    @Override
    void newBackendFrame() {
        // A frame whose draw step never ran, for example because closing it failed, must not suppress this one
        skipDraw = false;
        implSdl3.newFrame();
    }

    @Override
    void awaitDeviceIdle() {
        int result = VK10.vkDeviceWaitIdle(device);
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException("vkDeviceWaitIdle failed with VkResult " + result);
        }
    }

    @Override
    void shutdownRendererBackend() {
        if (implVulkan != null) {
            implVulkan.shutdown();
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
        implVulkan = null;
        implSdl3 = null;
        skipDraw = false;
    }

    private void drawFrame(VkCommandBuffer commandBuffer, long targetImageView) {
        boolean skipped = skipDraw;
        skipDraw = false;
        if (skipped || targetImageView == VK10.VK_NULL_HANDLE) {
            return;
        }
        ImDrawData drawData = ImGui.getDrawData();
        if (drawData.getCmdListsCount() == 0) {
            return;
        }
        // The port also records nothing for an empty framebuffer, for example while the window is minimized
        implVulkan.renderDrawData(drawData, commandBuffer, targetImageView);
    }
}
