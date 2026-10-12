package com.ivan1pl.lorisui.lwjgl;

import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.ImGuiRenderer;
import com.ivan1pl.lorisui.imgui.ImGuiSdlVulkanRenderer;
import com.ivan1pl.lorisui.imgui.backend.ImGuiVulkanBackend;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

/**
 * Displays LorisUI screens in an application's own SDL3 window, using the Dear ImGui backend with Vulkan.
 * <p>
 * The display is embedded in an application that owns the SDL window, the Vulkan instance, device and queue, the
 * swapchain and the main loop. The display holds a set of named {@link LorisScreen}s and records the current one into
 * the command buffer the application passes each frame, drawing into the image view it passes with it. It resolves
 * the screens' localization keys from the resource bundles registered with {@link #setResourceBundle}, and tells the
 * application which input events the UI consumes. The display never destroys the window or any Vulkan object of the
 * application.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *     <li>Create the window, the Vulkan instance and device, and a swapchain for the window's surface as
 *     {@link ImGuiSdlVulkanRenderer} requires: its class documentation lists the requirements for the window, the
 *     Vulkan library, the API version and device feature, the queue and the swapchain format.</li>
 *     <li>Create the display for the window, the device, the graphics queue and its queue family, the swapchain's
 *     format and the number of frames in flight; register fonts, custom renderers and style appliers, and resource
 *     bundles; add screens and set the current screen. Register only fonts from trusted sources, as described for
 *     {@link ImGuiRenderer#registerFontFromFile(String, int)}.</li>
 *     <li>Call {@link #initialize()}. The display sets up Dear ImGui with the registered fonts for the window and the
 *     device, uploading the font atlas on the queue, and the current screen's {@link LorisScreen#onStartScreen()}
 *     fires. If the setup fails, the display releases what it had set up and stays uninitialized, so
 *     {@code initialize()} can be called again; if a registered font caused the failure, create a new display
 *     instead.</li>
 *     <li>Each frame, pass every SDL event of the window to {@link #processEvent(long)}, skipping the application's
 *     own handling of the events it consumes, and record the frame as described below.</li>
 *     <li>While the display is initialized, switching screens with {@link #setCurrentScreen} fires
 *     {@link LorisScreen#onEndScreen()} on the old screen and {@link LorisScreen#onStartScreen()} on the new one.
 *     Before {@link #initialize()}, and from the moment the display is closed, even while its release is still
 *     pending, {@link #setCurrentScreen} only records the choice.</li>
 *     <li>Call {@link #close()} after the last frame that uses the display has been submitted, and before the device,
 *     the swapchain image views passed to {@link #render(int, int, VkCommandBuffer, long)} or the window are
 *     destroyed. The current screen's {@link LorisScreen#onEndScreen()} fires, the display waits until the device is
 *     idle and releases its Dear ImGui, Vulkan and SDL resources, including every texture it loaded, and the
 *     background work that computes screen layouts stops until a screen is drawn again. Closing is final: to show UI
 *     again, create a new display. Fonts must then be registered with the new display.</li>
 * </ul>
 *
 * <h2>Frame</h2>
 * A frame of the application's loop, with the display:
 * <ol>
 *     <li>Pass the window's SDL events to {@link #processEvent(long)}. While the window is minimized or its size in
 *     pixels is {@code 0}, skip the frame, for example by waiting for the next event.</li>
 *     <li>Recreate the swapchain if the window's size has changed or the swapchain is out of date, keeping its
 *     format.</li>
 *     <li>Wait until the frame recorded {@code framesInFlight} frames earlier has finished executing, acquire the next
 *     swapchain image, and begin recording the frame's command buffer.</li>
 *     <li>Transition the image to {@code VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL} and clear it, ending any render pass
 *     or rendering instance used for that.</li>
 *     <li>Call {@link #render(int, int, VkCommandBuffer, long)} once with the window's logical size, the command buffer
 *     and the view of the acquired image, or {@code 0} as the image view while the swapchain's extent is smaller than
 *     the window's size in pixels.</li>
 *     <li>Transition the image, for example to {@code VK_IMAGE_LAYOUT_PRESENT_SRC_KHR}, end the command buffer, submit
 *     it on the display's queue and present the image. If {@code render} throws, reset the command buffer instead of
 *     submitting it.</li>
 * </ol>
 * The command buffer, the target image and the number of frames in flight follow the frame contract of
 * {@link ImGuiSdlVulkanRenderer}.
 *
 * <h2>Queue and format</h2>
 * The queue and the color attachment format passed to the constructor follow the rules of
 * {@link ImGuiSdlVulkanRenderer}. Because the format is fixed, an application whose swapchain is recreated with another
 * format closes the display and creates a new one for the new format, registering its fonts, renderers, style
 * appliers, resource bundles and screens with it again.
 *
 * <h2>Closing during a frame</h2>
 * Closing the display from a component callback, such as a button action, or from a screen's
 * {@link LorisScreen#onStartScreen()} or {@link LorisScreen#onEndScreen()} is allowed. The display is closed at
 * once: {@link #processEvent(long)} returns {@code false} and the other methods that require an open display throw.
 * When the display is closed during a frame, that frame draws no UI. The release itself completes when the current
 * frame or screen switch has finished, so the application must let that
 * {@link #render(int, int, VkCommandBuffer, long)}, {@link #setCurrentScreen} or {@link #initialize()} call return
 * before it destroys the device, the image views or the window; it then submits the frame's command buffer as usual.
 * Closing the display between {@link #render(int, int, VkCommandBuffer, long)} and the submission of that frame's
 * command buffer is not supported, because the recorded commands use the display's resources; close it during the
 * frame or after the submission.
 * <p>
 * A screen receives {@link LorisScreen#onEndScreen()} only if its {@link LorisScreen#onStartScreen()} returned
 * normally, and at most once per such start. A screen whose {@link LorisScreen#onStartScreen()} throws is treated as
 * not started, so it receives no {@link LorisScreen#onEndScreen()} for that start, not even when the display is
 * closed. An exception thrown by a hook propagates out of the method that fired it. The hooks must not call
 * {@link #setCurrentScreen}; switch screens later, for example from a component callback.
 * <p>
 * Only one display may be initialized at a time. While initialized, the display leaves its Dear ImGui context current;
 * an application that uses a Dear ImGui context of its own must make that context current again after calling the
 * display.
 * <p>
 * Initializing the display sets the global SDL hints listed in {@link ImGuiSdlVulkanRenderer}; they affect every SDL
 * window of the application and stay set after the display is closed.
 *
 * <h2>Threading</h2>
 * The display is not thread-safe. Create it and call all of its methods on the thread that owns the window. The
 * screen lifecycle hooks and component callbacks run on that thread. The display submits to the queue from that
 * thread; an application that also submits to the queue from other threads must not do so while a method of the
 * display runs, because Vulkan queues need external synchronization. On macOS, the window's thread is the process's
 * main thread, so the JVM may need {@code -XstartOnFirstThread}.
 */
public final class ImGuiLwjglSdlVulkanDisplay extends ImGuiLwjglDisplayBase<ImGuiSdlVulkanRenderer>
        implements AutoCloseable {
    private final ImGuiLwjglSdlInput sdlInput;
    private boolean frameInProgress = false;

    /**
     * Creates a display for an SDL window and the Vulkan device and queue that the application renders it with. Call
     * it on the thread that owns the window. The display shows nothing until it is
     * {@linkplain #initialize() initialized}, and the constructor makes no Dear ImGui, SDL or Vulkan calls. The
     * application keeps ownership of the window, the device and the queue.
     *
     * @param sdlWindow             the native handle of the application's SDL window ({@code SDL_Window*}), created
     *                              with {@code SDL_WINDOW_VULKAN}; must not be {@code 0}
     * @param device                the application's Vulkan device, created as {@link ImGuiSdlVulkanRenderer}
     *                              requires; must not be null
     * @param queueFamilyIndex      the index of the queue family of {@code queue}; must not be negative
     * @param queue                 the queue of {@code device} on which the application submits the frames' command
     *                              buffers (see {@link ImGuiSdlVulkanRenderer}); must not be null
     * @param colorAttachmentFormat the {@code VkFormat} of the images the display draws into, usually the swapchain's
     *                              format; must be positive (not {@code VK_FORMAT_UNDEFINED})
     * @param framesInFlight        the maximum number of frames the application keeps in flight; must be at least 1
     * @throws IllegalArgumentException if {@code sdlWindow} is {@code 0}, checked first; if {@code queueFamilyIndex}
     *                                  is negative, checked before {@code device} and {@code queue}; if {@code queue}
     *                                  does not belong to {@code device}; or if {@code colorAttachmentFormat} is not
     *                                  positive or {@code framesInFlight} is less than 1, checked last
     * @throws NullPointerException     if {@code device} or {@code queue} is null
     */
    public ImGuiLwjglSdlVulkanDisplay(long sdlWindow, VkDevice device, int queueFamilyIndex, VkQueue queue,
                                      int colorAttachmentFormat, int framesInFlight) {
        if (sdlWindow == 0) {
            throw new IllegalArgumentException("sdlWindow must be the handle of an SDL window, not 0");
        }
        ImGuiVulkanBackend backend = new ImGuiVulkanBackend(device, queueFamilyIndex, queue);
        super(backend, new ImGuiSdlVulkanRenderer(backend, sdlWindow, colorAttachmentFormat, framesInFlight));
        this.sdlInput = new ImGuiLwjglSdlInput(sdlWindow, getRenderer());
    }

    /**
     * Initializes the display and fires {@link LorisScreen#onStartScreen()} on the current screen. Call it while the
     * device is alive, before the first {@link #render(int, int, VkCommandBuffer, long)} call. The display uploads the
     * font atlas with a command buffer of its own, which it submits to the queue and waits for. From then on, fonts,
     * renderers and style appliers can no longer be registered.
     * <p>
     * If setting up Dear ImGui fails, the exception propagates, no screen hook fires, and the display releases what it
     * had set up and stays uninitialized, so the call can be retried, for example after a transient Vulkan failure. A
     * failure caused by the device, such as a device without Vulkan 1.3 functions, or by a registered font recurs on
     * every retry, because neither can be changed on this display; create a new display instead.
     * <p>
     * If the screen's {@link LorisScreen#onStartScreen()} throws, the exception propagates, the display stays
     * initialized, and the screen is treated as not started: it receives no {@link LorisScreen#onEndScreen()} for
     * this start. If the hook closes the display, the release completes when the hook has returned, and a failure of
     * the release then propagates from this method.
     *
     * @throws IllegalStateException if the display is closed or already initialized, or if no current screen is set
     *                               (call {@link #setCurrentScreen} first), all checked before any state change; if
     *                               the device does not expose the Vulkan 1.3 functions (see
     *                               {@link ImGuiSdlVulkanRenderer}); if
     *                               Dear ImGui's SDL3 platform backend fails to initialize; if Vulkan fails to create
     *                               the display's resources or to upload the font atlas, with a message that names the
     *                               Vulkan function and its {@code VkResult}; or if a registered font cannot be
     *                               loaded, for example because its data is not a valid TrueType font
     */
    public void initialize() {
        initializeDisplay();
    }

    /**
     * Passes an SDL event of the window to the UI and returns whether the UI consumes it. Pass every event of the
     * window, and skip the application's own handling of the event when this method returns {@code true}.
     * <p>
     * Mouse motion and mouse button events are consumed while the UI wants mouse input, that is while the pointer is
     * over LorisUI content or a mouse drag started on it. Mouse wheel events are consumed while the UI wants mouse
     * input and the pointer is over a scrollable panel. Key and text input events are consumed while a LorisUI text or
     * number input has keyboard focus. Other events, such as window events, are passed to the UI but never consumed.
     * The result describes the most recently rendered frame.
     * <p>
     * Returns {@code false} without reading the event while the display is not initialized, including after
     * {@link #close()}.
     *
     * @param sdlEventPtr the native address of the {@code SDL_Event}, for example {@code event.address()} of an LWJGL
     *                    {@code SDL_Event}
     * @return {@code true} if the UI consumes the event; {@code false} if the application should handle it
     */
    public boolean processEvent(long sdlEventPtr) {
        if (!isInitialized()) {
            return false;
        }
        getRenderer().processSdlEvent(sdlEventPtr);
        return sdlInput.isConsumed(sdlEventPtr);
    }

    /**
     * Records the current screen into a command buffer, drawing it into an image view. Call it once per frame, after
     * the target image has been cleared and before the command buffer is ended and submitted. The display draws the UI
     * over the image's contents; it neither ends nor submits the command buffer.
     * <p>
     * {@code width} and {@code height} are the size of the window in logical window coordinates, as reported by
     * {@code SDL_GetWindowSize}, not in pixels; the pixel density of HiDPI displays is applied automatically.
     * <p>
     * {@code commandBuffer} and the target image follow the frame contract of
     * {@link ImGuiSdlVulkanRenderer#render(LorisScreen, int, int, com.ivan1pl.lorisui.api.resolver.BundleKeyResolver,
     * VkCommandBuffer, long) ImGuiSdlVulkanRenderer.render}, with the display's device, queue and color attachment
     * format. With {@code 0} as {@code targetImageView}, the frame is still built, so the UI keeps processing input
     * and running its callbacks, but nothing is drawn.
     * <p>
     * At the end of the frame, the display starts SDL text input for the window when a LorisUI text or number input
     * has gained keyboard focus, so that typed characters arrive as {@code SDL_EVENT_TEXT_INPUT} events, and stops it
     * when the focus has left. An application that starts or stops SDL text input for the same window itself
     * interferes with typing into the UI. If SDL fails to start text input, the frame is still drawn without an error
     * and the display tries again after the next frame.
     * <p>
     * If the display is closed during the frame, for example from a component callback, the frame draws no UI, and
     * the release completes when the frame has been built, before this method returns; a failure of the release then
     * propagates from this method. The application submits the command buffer as usual.
     *
     * @param width           the window width in logical window coordinates
     * @param height          the window height in logical window coordinates
     * @param commandBuffer   the frame's command buffer, in the recording state, which the application submits after
     *                        this call; must not be null
     * @param targetImageView the handle of the {@code VkImageView} to draw into, usually the view of the acquired
     *                        swapchain image, or {@code 0} to build the frame without drawing it
     * @throws IllegalStateException    if the display is not initialized or is closed, checked before the arguments;
     *                                  if called from a component callback during a frame, if the screen's most
     *                                  recent layout failed (see
     *                                  {@link LorisScreen#render}), if a component or style of the screen has no
     *                                  registered renderer or style applier, or if Vulkan fails to create the buffers
     *                                  of the frame or to load an image; the message then names the Vulkan function
     *                                  and its {@code VkResult}
     * @throws NullPointerException     if {@code commandBuffer} is null; checked after the state of the display
     * @throws IllegalArgumentException if {@code commandBuffer} belongs to another device than the display's; checked
     *                                  after the {@code null} check
     */
    public void render(int width, int height, VkCommandBuffer commandBuffer, long targetImageView) {
        renderFrame(() -> {
            // A nested render from a component callback is rejected by the renderer and must not clear the flag of
            // the frame that is still running, or a close() after it would let that frame draw
            boolean outerFrameInProgress = frameInProgress;
            frameInProgress = true;
            try {
                getRenderer().render(getScreenManager().getCurrentScreen(), width, height, getResolver(),
                        commandBuffer, targetImageView);
            } finally {
                frameInProgress = outerFrameInProgress;
            }
            if (isInitialized()) {
                sdlInput.updateTextInput();
            }
        });
    }

    /**
     * Closes the display. The display is closed at once: {@link #processEvent(long)} returns {@code false} from then
     * on, and registering fonts, renderers or style appliers, {@link #initialize()} and
     * {@link #render(int, int, VkCommandBuffer, long)} throw {@link IllegalStateException}. Closing is final; to show
     * UI again, create a new display. The window, the device and the queue are left to the application.
     * <p>
     * If the display is initialized, it then fires {@link LorisScreen#onEndScreen()} on the current screen, if that
     * screen's {@link LorisScreen#onStartScreen()} returned normally, stops SDL text input for the window if the
     * display started it, waits until the device is idle, releases its Dear ImGui, Vulkan and SDL resources, including
     * every texture it loaded, and stops the background work that computes screen layouts until a screen is drawn
     * again. Call it after the last frame that uses the display has been submitted, and before the device, the
     * swapchain image views passed to {@link #render(int, int, VkCommandBuffer, long)} or the window are destroyed.
     * <p>
     * When called during a frame, for example from a component callback, that frame draws no UI, and the release,
     * including {@link LorisScreen#onEndScreen()}, runs when {@link #render(int, int, VkCommandBuffer, long)} returns;
     * the application then submits the frame's command buffer as usual. When called during a screen switch or a
     * screen's lifecycle hook, the release runs when that switch or hook has finished. A failure of the release
     * propagates from the method that was running. The end hook then fires on the screen that was started when the
     * display was closed; a screen selected with {@link #setCurrentScreen} after that is only recorded. Calling it
     * between {@link #render(int, int, VkCommandBuffer, long)} and the submission of that frame's command buffer is
     * not supported.
     * <p>
     * Does nothing if the display is already closed. Every release step runs even when an earlier one fails. If
     * {@link LorisScreen#onEndScreen()} or a release step throws, the first exception propagates after all steps have
     * run, and the display is closed all the same.
     *
     * @throws RuntimeException the first failure of {@link LorisScreen#onEndScreen()} or of a release step; failures
     *                          of later steps are added to it as suppressed exceptions
     */
    @Override
    public void close() {
        if (frameInProgress) {
            getRenderer().skipFrameDraw();
        }
        closeDisplay();
    }

    @Override
    void setUp() {
        getRenderer().initialize();
    }

    @Override
    void releaseInput() {
        sdlInput.stopTextInputIfStarted();
    }
}
