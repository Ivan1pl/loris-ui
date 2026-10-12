package com.ivan1pl.lorisui.example.lwjgl;

import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

// The window's swapchain with one image view and one render-finished semaphore per image. Its sRGB format is chosen
// once, so the display, whose format is fixed, never has to be replaced.
final class ExampleVulkanSwapchain implements AutoCloseable {
    // The value of VkSurfaceCapabilitiesKHR.currentExtent when the window's size determines the extent
    private static final int UNDEFINED_EXTENT = 0xFFFFFFFF;

    private final ExampleVulkanDevice device;
    private final long window;
    private final int format;
    private long swapchain = VK10.VK_NULL_HANDLE;
    private Extent extent;
    private long[] images = new long[0];
    private long[] imageViews = new long[0];
    private long[] renderFinishedSemaphores = new long[0];

    ExampleVulkanSwapchain(ExampleVulkanDevice device, long window) {
        this.device = device;
        this.window = window;
        this.format = chooseFormat();
        try {
            create();
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, this::close);
            throw failure;
        }
    }

    int getFormat() {
        return format;
    }

    Extent getExtent() {
        return extent;
    }

    long getSwapchain() {
        return swapchain;
    }

    long getImage(int index) {
        return images[index];
    }

    long getImageView(int index) {
        return imageViews[index];
    }

    long getRenderFinishedSemaphore(int index) {
        return renderFinishedSemaphores[index];
    }

    // The surface's current extent or, where the window's size determines it, the window's size in pixels
    Extent desiredExtent() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return desiredExtent(surfaceCapabilities(stack), stack);
        }
    }

    void recreate() {
        ExampleVulkanDevice.check(VK10.vkDeviceWaitIdle(device.getDevice()), "vkDeviceWaitIdle");
        if (!supportsFormat(format)) {
            throw new IllegalStateException("The window's Vulkan surface no longer supports the swapchain format "
                    + format + " with the sRGB nonlinear color space");
        }
        destroyImageResources();
        create();
    }

    @Override
    public void close() {
        DeferredRelease.runAll(
                this::destroyImageResources,
                () -> KHRSwapchain.vkDestroySwapchainKHR(device.getDevice(), swapchain, null));
    }

    // Prefers B8G8R8A8_SRGB; the display's images are sRGB textures, so only an sRGB format shows them as the
    // OpenGL examples do
    private int chooseFormat() {
        if (supportsFormat(VK10.VK_FORMAT_B8G8R8A8_SRGB)) {
            return VK10.VK_FORMAT_B8G8R8A8_SRGB;
        }
        if (supportsFormat(VK10.VK_FORMAT_R8G8B8A8_SRGB)) {
            return VK10.VK_FORMAT_R8G8B8A8_SRGB;
        }
        throw new IllegalStateException("The window's Vulkan surface supports neither VK_FORMAT_B8G8R8A8_SRGB nor "
                + "VK_FORMAT_R8G8B8A8_SRGB with the sRGB nonlinear color space, which the example needs to show sRGB "
                + "colors correctly");
    }

    private boolean supportsFormat(int candidate) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            ExampleVulkanDevice.check(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(),
                    device.getSurface(), count, null), "vkGetPhysicalDeviceSurfaceFormatsKHR");
            VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            ExampleVulkanDevice.check(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(),
                    device.getSurface(), count, formats), "vkGetPhysicalDeviceSurfaceFormatsKHR");
            for (VkSurfaceFormatKHR surfaceFormat : formats) {
                if (surfaceFormat.format() == candidate
                        && surfaceFormat.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    return true;
                }
            }
            return false;
        }
    }

    private VkSurfaceCapabilitiesKHR surfaceCapabilities(MemoryStack stack) {
        VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        ExampleVulkanDevice.check(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getPhysicalDevice(),
                device.getSurface(), capabilities), "vkGetPhysicalDeviceSurfaceCapabilitiesKHR");
        return capabilities;
    }

    private Extent desiredExtent(VkSurfaceCapabilitiesKHR capabilities, MemoryStack stack) {
        VkExtent2D current = capabilities.currentExtent();
        if (current.width() != UNDEFINED_EXTENT) {
            return new Extent(current.width(), current.height());
        }
        IntBuffer width = stack.mallocInt(1);
        IntBuffer height = stack.mallocInt(1);
        LorisLwjglSdlVulkanExample.checkSdl(SDLVideo.SDL_GetWindowSizeInPixels(window, width, height),
                "read the window size in pixels");
        VkExtent2D min = capabilities.minImageExtent();
        VkExtent2D max = capabilities.maxImageExtent();
        return new Extent(clampUnsigned(width.get(0), min.width(), max.width()),
                clampUnsigned(height.get(0), min.height(), max.height()));
    }

    // The image extents are uint32, so they are compared unsigned; unlike Math.clamp, this never throws
    private static int clampUnsigned(int value, int min, int max) {
        if (Integer.compareUnsigned(value, min) < 0) {
            return min;
        }
        if (Integer.compareUnsigned(value, max) > 0) {
            return max;
        }
        return value;
    }

    // Replaces the current swapchain, if any, through oldSwapchain; the field keeps the old one until the new one
    // exists, so close() also releases a swapchain whose replacement failed
    private void create() {
        VkDevice vkDevice = device.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR capabilities = surfaceCapabilities(stack);
            Extent newExtent = desiredExtent(capabilities, stack);
            int imageCount = capabilities.minImageCount() + 1;
            if (capabilities.maxImageCount() > 0) {
                imageCount = Math.min(imageCount, capabilities.maxImageCount());
            }
            VkSwapchainCreateInfoKHR info = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType$Default()
                    .surface(device.getSurface())
                    .minImageCount(imageCount)
                    .imageFormat(format)
                    .imageColorSpace(KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .imageExtent(e -> e.width(newExtent.width()).height(newExtent.height()))
                    .imageArrayLayers(1)
                    .imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                    .preTransform(capabilities.currentTransform())
                    // The lowest supported bit is OPAQUE wherever the surface supports it
                    .compositeAlpha(Integer.lowestOneBit(capabilities.supportedCompositeAlpha()))
                    .presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR)
                    .clipped(true)
                    .oldSwapchain(swapchain);
            LongBuffer handle = stack.mallocLong(1);
            ExampleVulkanDevice.check(KHRSwapchain.vkCreateSwapchainKHR(vkDevice, info, null, handle),
                    "vkCreateSwapchainKHR");
            KHRSwapchain.vkDestroySwapchainKHR(vkDevice, swapchain, null);
            swapchain = handle.get(0);
            extent = newExtent;

            IntBuffer count = stack.mallocInt(1);
            ExampleVulkanDevice.check(KHRSwapchain.vkGetSwapchainImagesKHR(vkDevice, swapchain, count, null),
                    "vkGetSwapchainImagesKHR");
            LongBuffer imageHandles = stack.mallocLong(count.get(0));
            ExampleVulkanDevice.check(KHRSwapchain.vkGetSwapchainImagesKHR(vkDevice, swapchain, count, imageHandles),
                    "vkGetSwapchainImagesKHR");
            images = new long[count.get(0)];
            imageHandles.get(images);
        }
        imageViews = new long[images.length];
        renderFinishedSemaphores = new long[images.length];
        for (int i = 0; i < images.length; i++) {
            imageViews[i] = createImageView(images[i]);
            renderFinishedSemaphores[i] = device.createSemaphore();
        }
    }

    private long createImageView(long image) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo info = VkImageViewCreateInfo.calloc(stack)
                    .sType$Default()
                    .image(image)
                    .viewType(VK10.VK_IMAGE_VIEW_TYPE_2D)
                    .format(format)
                    .subresourceRange(range -> range
                            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                            .levelCount(1)
                            .layerCount(1));
            LongBuffer view = stack.mallocLong(1);
            ExampleVulkanDevice.check(VK10.vkCreateImageView(device.getDevice(), info, null, view),
                    "vkCreateImageView");
            return view.get(0);
        }
    }

    // Destroying VK_NULL_HANDLE is a no-op, so views and semaphores whose creation failed are skipped
    private void destroyImageResources() {
        for (int i = 0; i < imageViews.length; i++) {
            VK10.vkDestroySemaphore(device.getDevice(), renderFinishedSemaphores[i], null);
            VK10.vkDestroyImageView(device.getDevice(), imageViews[i], null);
        }
        images = new long[0];
        imageViews = new long[0];
        renderFinishedSemaphores = new long[0];
    }

    record Extent(int width, int height) {}
}
