/**
 * Hosts LorisUI screens in an application's own LWJGL window.
 * <p>
 * Each display draws the current {@link com.ivan1pl.lorisui.api.screen.LorisScreen} with the Dear ImGui backend into
 * a window that the application has created, together with its OpenGL context, GPU device or Vulkan device. Choose the
 * display that matches the window library and graphics API:
 * <table class="striped">
 *     <caption>Which display to use</caption>
 *     <thead>
 *         <tr>
 *             <th scope="col">Display</th>
 *             <th scope="col">Window and graphics</th>
 *             <th scope="col">Input and frame</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <th scope="row">{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGl3Display}</th>
 *             <td>SDL3 window with an OpenGL context</td>
 *             <td>The application forwards the window's SDL events to the display.</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglGlfwGl3Display}</th>
 *             <td>GLFW window with an OpenGL context</td>
 *             <td>The display chains its GLFW input callbacks in front of the application's.</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGpuDisplay}</th>
 *             <td>SDL3 window claimed for an SDL_GPU device</td>
 *             <td>The application forwards the window's SDL events to the display, and passes each frame's command
 *             buffer and target texture to it.</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlVulkanDisplay}</th>
 *             <td>SDL3 window with a Vulkan surface and swapchain</td>
 *             <td>The application forwards the window's SDL events to the display, and passes each frame's command
 *             buffer and the view of the target swapchain image to it.</td>
 *         </tr>
 *     </tbody>
 * </table>
 * <p>
 * A typical setup, done on the thread that owns the window:
 * <ol>
 *     <li>create the window and its OpenGL context, the window and the GPU device, or the window, the Vulkan device
 *     and the swapchain, with an sRGB framebuffer (see below);</li>
 *     <li>for the GLFW display, set the application's own input callbacks of the window;</li>
 *     <li>create the display for the window, for the SDL_GPU display also the device, and for the Vulkan display also
 *     the device, the graphics queue and its queue family, the swapchain's format and the number of frames in
 *     flight;</li>
 *     <li>register fonts, custom renderers and style appliers, and resource bundles;</li>
 *     <li>add the screens and select the current one;</li>
 *     <li>call the display's {@code initialize()}, with the OpenGL context current for the OpenGL displays.</li>
 * </ol>
 * Then, in the application's loop:
 * <ul>
 *     <li>{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGl3Display}: forward every SDL event of the window to
 *     {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGl3Display#processEvent(long) processEvent} and skip the
 *     application's own handling of the events it consumes, then clear the window, call
 *     {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGl3Display#render(int, int) render} once and swap the window.</li>
 *     <li>{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglGlfwGl3Display}: process the window's events with
 *     {@code glfwPollEvents} or {@code glfwWaitEvents}, then clear the window, call
 *     {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglGlfwGl3Display#render(int, int) render} once and swap the window.</li>
 *     <li>{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGpuDisplay}: forward every SDL event of the window to
 *     {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGpuDisplay#processEvent(long) processEvent} as for the SDL3 OpenGL
 *     display, then acquire a command buffer and the swapchain texture, clear the texture, call
 *     {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlGpuDisplay#render(int, int, long, long) render} once and submit
 *     the command buffer.</li>
 *     <li>{@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlVulkanDisplay}: forward every SDL event of the window to
 *     {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlVulkanDisplay#processEvent(long) processEvent} as for the SDL3
 *     OpenGL display, then wait for the frame slot, acquire a swapchain image, begin the command buffer, transition the
 *     image to the color attachment layout and clear it, call
 *     {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlVulkanDisplay#render(int, int, org.lwjgl.vulkan.VkCommandBuffer,
 *     long) render} once, transition the image for presenting, end and submit the command buffer, and present. Pass
 *     {@code 0} as the image view while the swapchain is smaller than the window in pixels.</li>
 * </ul>
 * Close the display before destroying the OpenGL context, the GPU device, the Vulkan device or the window; a closed
 * display cannot be used again.
 * <p>
 * Images are uploaded as sRGB textures, so every display needs an sRGB framebuffer to show their true colors. With
 * the OpenGL displays, request an sRGB-capable default framebuffer when creating the window
 * ({@code SDL_GL_FRAMEBUFFER_SRGB_CAPABLE} or {@code GLFW_SRGB_CAPABLE}) and enable {@code GL_FRAMEBUFFER_SRGB}; with
 * the SDL_GPU display, claim the window for the device and set its swapchain to the
 * {@code SDL_GPU_SWAPCHAINCOMPOSITION_SDR_LINEAR} composition before initializing the display; with the Vulkan
 * display, use an sRGB swapchain format.
 * <p>
 * The Vulkan display's device, queue and swapchain format follow the rules of
 * {@link com.ivan1pl.lorisui.imgui.ImGuiSdlVulkanRenderer}, and a swapchain recreated with another format needs a new
 * display (see {@link com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlVulkanDisplay}).
 */
package com.ivan1pl.lorisui.lwjgl;
