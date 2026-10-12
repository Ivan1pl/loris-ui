package com.ivan1pl.lorisui.example.lwjgl;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import com.ivan1pl.lorisui.example.shared.ExampleScreen;
import com.ivan1pl.lorisui.example.shared.ExampleSettings;
import com.ivan1pl.lorisui.lwjgl.ImGuiLwjglSdlVulkanDisplay;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Locale;

public class LorisLwjglSdlVulkanExample {
    private static final String WINDOW_TITLE = "LorisUI LWJGL SDL3 + Vulkan Example";
    private static final String GPU_DEBUG_PROPERTY = "lorisui.example.gpuDebug";
    private static final int FRAMES_IN_FLIGHT = 2;
    // UINT64_MAX: wait without a timeout
    private static final long NO_TIMEOUT = -1L;

    private boolean uiClosed = false;
    private boolean swapchainDirty = false;
    private int frameSlot = 0;

    void main() throws IOException {
        Locale.setDefault(ExampleSettings.LOCALE);
        checkSdl(SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO), "initialize the SDL video subsystem");
        try {
            checkSdl(SDLVulkan.SDL_Vulkan_LoadLibrary(vulkanLibraryPath()), "load the Vulkan library");
            try {
                long window = SDLVideo.SDL_CreateWindow(WINDOW_TITLE, ExampleSettings.WINDOW_WIDTH,
                        ExampleSettings.WINDOW_HEIGHT, SDLVideo.SDL_WINDOW_VULKAN | SDLVideo.SDL_WINDOW_RESIZABLE
                                | SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY);
                checkSdl(window != 0, "create the window");
                try {
                    run(window);
                } finally {
                    SDLVideo.SDL_DestroyWindow(window);
                }
            } finally {
                SDLVulkan.SDL_Vulkan_UnloadLibrary();
            }
        } finally {
            SDLInit.SDL_Quit();
        }
    }

    static void checkSdl(boolean success, String action) {
        if (!success) {
            throw sdlFailure(action);
        }
    }

    // SDL must use the Vulkan library that LWJGL loaded, so that both share one loader
    private static String vulkanLibraryPath() {
        if (VK.getFunctionProvider() instanceof SharedLibrary library && library.getPath() != null) {
            return library.getPath();
        }
        throw new IllegalStateException("The path of the Vulkan library that LWJGL loaded is unknown, so SDL cannot "
                + "load the same library");
    }

    // The frame resources, the swapchain and the device are released in that order, after the display is closed and
    // the device is idle
    private void run(long window) throws IOException {
        try (ExampleVulkanDevice device = new ExampleVulkanDevice(window, Boolean.getBoolean(GPU_DEBUG_PROPERTY));
             ExampleVulkanSwapchain swapchain = new ExampleVulkanSwapchain(device, window);
             FrameResources frames = new FrameResources(device)) {
            try {
                runUi(window, device, swapchain, frames);
            } finally {
                // A lost device needs the same teardown, so the result is not checked
                VK10.vkDeviceWaitIdle(device.getDevice());
            }
        }
    }

    // The display is closed when this method returns, before the Vulkan objects it uses are destroyed
    private void runUi(long window, ExampleVulkanDevice device, ExampleVulkanSwapchain swapchain,
                       FrameResources frames) throws IOException {
        try (ImGuiLwjglSdlVulkanDisplay display = new ImGuiLwjglSdlVulkanDisplay(window, device.getDevice(),
                device.getQueueFamilyIndex(), device.getQueue(), swapchain.getFormat(), FRAMES_IN_FLIGHT)) {
            display.setResourceBundle(ExampleSettings.BUNDLE_NAME, ExampleSettings.BUNDLE_PATH);
            Font font = display.registerFontFromResources(ExampleSettings.FONT_PATH, ExampleSettings.FONT_SIZE);
            display.addScreen(ExampleSettings.SCREEN_NAME, new ExampleScreen(font, () -> {
                display.close();
                uiClosed = true;
            }));
            display.setCurrentScreen(ExampleSettings.SCREEN_NAME);
            display.initialize();
            runLoop(window, device, swapchain, frames, display);
        }
    }

    private void runLoop(long window, ExampleVulkanDevice device, ExampleVulkanSwapchain swapchain,
                         FrameResources frames, ImGuiLwjglSdlVulkanDisplay display) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Event event = SDL_Event.calloc(stack);
            IntBuffer windowWidth = stack.mallocInt(1);
            IntBuffer windowHeight = stack.mallocInt(1);
            IntBuffer pixelWidth = stack.mallocInt(1);
            IntBuffer pixelHeight = stack.mallocInt(1);
            while (processEvents(event, display)) {
                checkSdl(SDLVideo.SDL_GetWindowSizeInPixels(window, pixelWidth, pixelHeight),
                        "read the window size in pixels");
                boolean minimized = (SDLVideo.SDL_GetWindowFlags(window) & SDLVideo.SDL_WINDOW_MINIMIZED) != 0;
                ExampleVulkanSwapchain.Extent desiredExtent = swapchain.desiredExtent();
                if (minimized || pixelWidth.get(0) == 0 || pixelHeight.get(0) == 0 || desiredExtent.width() == 0
                        || desiredExtent.height() == 0) {
                    // Nothing can be presented; sleep until the next event instead of spinning
                    checkSdl(SDLEvents.SDL_WaitEvent(null), "wait for an event");
                    continue;
                }
                // The desired extent is computed as on creation, so a recreated swapchain matches it from then on
                if (swapchainDirty || !desiredExtent.equals(swapchain.getExtent())) {
                    swapchain.recreate();
                    swapchainDirty = false;
                }
                checkSdl(SDLVideo.SDL_GetWindowSize(window, windowWidth, windowHeight), "read the window size");
                ExampleVulkanSwapchain.Extent extent = swapchain.getExtent();
                boolean swapchainLagging = extent.width() < pixelWidth.get(0) || extent.height() < pixelHeight.get(0);
                renderFrame(device, swapchain, frames, display, windowWidth.get(0), windowHeight.get(0),
                        swapchainLagging);
            }
        }
    }

    private void renderFrame(ExampleVulkanDevice device, ExampleVulkanSwapchain swapchain, FrameResources frames,
                             ImGuiLwjglSdlVulkanDisplay display, int width, int height, boolean swapchainLagging) {
        long fence = frames.getFence(frameSlot);
        long acquireSemaphore = frames.getAcquireSemaphore(frameSlot);
        ExampleVulkanDevice.check(VK10.vkWaitForFences(device.getDevice(), fence, true, NO_TIMEOUT),
                "vkWaitForFences");
        int imageIndex = acquireImage(device.getDevice(), swapchain, acquireSemaphore);
        if (imageIndex < 0) {
            return;
        }
        VkCommandBuffer commandBuffer = frames.getCommandBuffer(frameSlot);
        recordFrame(commandBuffer, swapchain, imageIndex, display, width, height, swapchainLagging);
        // Reset only now, so that a frame skipped or failed before the submit leaves the fence signaled
        ExampleVulkanDevice.check(VK10.vkResetFences(device.getDevice(), fence), "vkResetFences");
        submitAndPresent(device, swapchain, commandBuffer, imageIndex, acquireSemaphore, fence);
        frameSlot = (frameSlot + 1) % FRAMES_IN_FLIGHT;
    }

    // Returns -1 if the swapchain is out of date, in which case the frame is skipped
    private int acquireImage(VkDevice device, ExampleVulkanSwapchain swapchain, long acquireSemaphore) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer imageIndex = stack.mallocInt(1);
            int result = KHRSwapchain.vkAcquireNextImageKHR(device, swapchain.getSwapchain(), NO_TIMEOUT,
                    acquireSemaphore, VK10.VK_NULL_HANDLE, imageIndex);
            if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                swapchainDirty = true;
                return -1;
            }
            checkSwapchainResult(result, "vkAcquireNextImageKHR");
            return imageIndex.get(0);
        }
    }

    // While the swapchain lags behind the window's size in pixels, the UI updates but draws nothing. If recording
    // fails, the command buffer is reset instead of submitted, as the display requires.
    private void recordFrame(VkCommandBuffer commandBuffer, ExampleVulkanSwapchain swapchain, int imageIndex,
                             ImGuiLwjglSdlVulkanDisplay display, int width, int height, boolean swapchainLagging) {
        long image = swapchain.getImage(imageIndex);
        long imageView = swapchain.getImageView(imageIndex);
        ExampleVulkanDevice.check(VK10.vkResetCommandBuffer(commandBuffer, 0), "vkResetCommandBuffer");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType$Default()
                    .flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            ExampleVulkanDevice.check(VK10.vkBeginCommandBuffer(commandBuffer, beginInfo), "vkBeginCommandBuffer");
        }
        try {
            // The source stage chains with the acquire semaphore, which the submit waits on in the same stage
            transition(commandBuffer, image,
                    VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            clear(commandBuffer, imageView, swapchain.getExtent());
            if (!uiClosed) {
                display.render(width, height, commandBuffer, swapchainLagging ? VK10.VK_NULL_HANDLE : imageView);
            }
            transition(commandBuffer, image,
                    VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                    VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0);
            ExampleVulkanDevice.check(VK10.vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer");
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, () -> ExampleVulkanDevice.check(
                    VK10.vkResetCommandBuffer(commandBuffer, 0), "vkResetCommandBuffer"));
            throw failure;
        }
    }

    private static void transition(VkCommandBuffer commandBuffer, long image, int oldLayout, int newLayout,
                                   int srcStageMask, int srcAccessMask, int dstStageMask, int dstAccessMask) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.get(0)
                    .sType$Default()
                    .srcAccessMask(srcAccessMask)
                    .dstAccessMask(dstAccessMask)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .subresourceRange(range -> range
                            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                            .levelCount(1)
                            .layerCount(1));
            VK10.vkCmdPipelineBarrier(commandBuffer, srcStageMask, dstStageMask, 0, null, null, barrier);
        }
    }

    private static void clear(VkCommandBuffer commandBuffer, long imageView, ExampleVulkanSwapchain.Extent extent) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderingAttachmentInfo.Buffer colorAttachment = VkRenderingAttachmentInfo.calloc(1, stack);
            colorAttachment.get(0)
                    .sType$Default()
                    .imageView(imageView)
                    .imageLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE)
                    .clearValue(value -> value.color().float32(3, 1.0f));
            VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                    .sType$Default()
                    .renderArea(area -> area.extent().set(extent.width(), extent.height()))
                    .layerCount(1)
                    .pColorAttachments(colorAttachment);
            VK13.vkCmdBeginRendering(commandBuffer, renderingInfo);
            VK13.vkCmdEndRendering(commandBuffer);
        }
    }

    // Each swapchain image has its own render-finished semaphore, so a semaphore is never signaled again while a
    // presentation may still wait on it
    private void submitAndPresent(ExampleVulkanDevice device, ExampleVulkanSwapchain swapchain,
                                  VkCommandBuffer commandBuffer, int imageIndex, long acquireSemaphore, long fence) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long renderFinishedSemaphore = swapchain.getRenderFinishedSemaphore(imageIndex);
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType$Default()
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(stack.longs(acquireSemaphore))
                    .pWaitDstStageMask(stack.ints(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(commandBuffer))
                    .pSignalSemaphores(stack.longs(renderFinishedSemaphore));
            ExampleVulkanDevice.check(VK10.vkQueueSubmit(device.getQueue(), submitInfo, fence), "vkQueueSubmit");
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType$Default()
                    .pWaitSemaphores(stack.longs(renderFinishedSemaphore))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(swapchain.getSwapchain()))
                    .pImageIndices(stack.ints(imageIndex));
            checkSwapchainResult(KHRSwapchain.vkQueuePresentKHR(device.getQueue(), presentInfo),
                    "vkQueuePresentKHR");
        }
    }

    // An out-of-date swapchain is recreated before the next frame. A suboptimal one still presents, so it is left to
    // the loop, which recreates it once the desired extent differs: marking it dirty would make a surface that stays
    // suboptimal recreate the swapchain, and wait for the device to go idle, on every frame.
    private void checkSwapchainResult(int result, String vkFunction) {
        if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
            swapchainDirty = true;
        } else if (result != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            ExampleVulkanDevice.check(result, vkFunction);
        }
    }

    // Returns false once the application should quit. The example has no input handling of its own, so it ignores
    // whether the UI consumed an event.
    private static boolean processEvents(SDL_Event event, ImGuiLwjglSdlVulkanDisplay display) {
        while (SDLEvents.SDL_PollEvent(event)) {
            int type = event.type();
            if (type == SDLEvents.SDL_EVENT_QUIT || type == SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED) {
                return false;
            }
            display.processEvent(event.address());
        }
        return true;
    }

    private static IllegalStateException sdlFailure(String action) {
        return new IllegalStateException("Failed to " + action + ": " + SDLError.SDL_GetError());
    }

    // The command buffer, fence and acquire semaphore of each frame in flight; the fences start signaled, so the
    // first wait on each returns at once
    private static final class FrameResources implements AutoCloseable {
        private final ExampleVulkanDevice device;
        private final long commandPool;
        private final VkCommandBuffer[] commandBuffers = new VkCommandBuffer[FRAMES_IN_FLIGHT];
        private final long[] fences = new long[FRAMES_IN_FLIGHT];
        private final long[] acquireSemaphores = new long[FRAMES_IN_FLIGHT];

        private FrameResources(ExampleVulkanDevice device) {
            this.device = device;
            this.commandPool = createCommandPool(device);
            try {
                allocateCommandBuffers();
                for (int slot = 0; slot < FRAMES_IN_FLIGHT; slot++) {
                    fences[slot] = createSignaledFence();
                    acquireSemaphores[slot] = device.createSemaphore();
                }
            } catch (Throwable failure) {
                DeferredRelease.runAfterFailure(failure, this::close);
                throw failure;
            }
        }

        private VkCommandBuffer getCommandBuffer(int slot) {
            return commandBuffers[slot];
        }

        private long getFence(int slot) {
            return fences[slot];
        }

        private long getAcquireSemaphore(int slot) {
            return acquireSemaphores[slot];
        }

        // Destroying VK_NULL_HANDLE is a no-op, and destroying the pool frees its command buffers
        @Override
        public void close() {
            VkDevice vkDevice = device.getDevice();
            for (int slot = 0; slot < FRAMES_IN_FLIGHT; slot++) {
                VK10.vkDestroySemaphore(vkDevice, acquireSemaphores[slot], null);
                VK10.vkDestroyFence(vkDevice, fences[slot], null);
            }
            VK10.vkDestroyCommandPool(vkDevice, commandPool, null);
        }

        private static long createCommandPool(ExampleVulkanDevice device) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack)
                        .sType$Default()
                        .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                        .queueFamilyIndex(device.getQueueFamilyIndex());
                LongBuffer pool = stack.mallocLong(1);
                ExampleVulkanDevice.check(VK10.vkCreateCommandPool(device.getDevice(), info, null, pool),
                        "vkCreateCommandPool");
                return pool.get(0);
            }
        }

        private void allocateCommandBuffers() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkCommandBufferAllocateInfo info = VkCommandBufferAllocateInfo.calloc(stack)
                        .sType$Default()
                        .commandPool(commandPool)
                        .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                        .commandBufferCount(FRAMES_IN_FLIGHT);
                PointerBuffer handles = stack.mallocPointer(FRAMES_IN_FLIGHT);
                ExampleVulkanDevice.check(VK10.vkAllocateCommandBuffers(device.getDevice(), info, handles),
                        "vkAllocateCommandBuffers");
                for (int slot = 0; slot < FRAMES_IN_FLIGHT; slot++) {
                    commandBuffers[slot] = new VkCommandBuffer(handles.get(slot), device.getDevice());
                }
            }
        }

        private long createSignaledFence() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkFenceCreateInfo info = VkFenceCreateInfo.calloc(stack)
                        .sType$Default()
                        .flags(VK10.VK_FENCE_CREATE_SIGNALED_BIT);
                LongBuffer fence = stack.mallocLong(1);
                ExampleVulkanDevice.check(VK10.vkCreateFence(device.getDevice(), info, null, fence),
                        "vkCreateFence");
                return fence.get(0);
            }
        }
    }
}
