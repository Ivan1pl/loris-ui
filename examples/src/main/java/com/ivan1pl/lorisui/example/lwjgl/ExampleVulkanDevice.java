package com.ivan1pl.lorisui.example.lwjgl;

import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRPortabilityEnumeration;
import org.lwjgl.vulkan.KHRPortabilitySubset;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan13Features;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The Vulkan instance, window surface, device and graphics queue of the Vulkan example
final class ExampleVulkanDevice implements AutoCloseable {
    private static final String VALIDATION_LAYER = "VK_LAYER_KHRONOS_validation";
    private static final String APPLICATION_NAME = "LorisUI Example";

    private final VkDebugUtilsMessengerCallbackEXT debugCallback;
    private final VkInstance instance;
    private long debugMessenger = VK10.VK_NULL_HANDLE;
    private long surface = VK10.VK_NULL_HANDLE;
    private VkDevice device;
    private int queueFamilyIndex;
    private VkQueue queue;

    ExampleVulkanDevice(long window, boolean debug) {
        debugCallback = debug ? VkDebugUtilsMessengerCallbackEXT.create(
                (severity, _, callbackData, _) -> printDebugMessage(severity, callbackData)) : null;
        try {
            instance = createInstance();
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, this::freeDebugCallback);
            throw failure;
        }
        try {
            if (debug) {
                debugMessenger = createDebugMessenger();
            }
            surface = createSurface(window);
            Candidate candidate = pickPhysicalDevice();
            queueFamilyIndex = candidate.queueFamilyIndex();
            device = createDevice(candidate);
            queue = getDeviceQueue();
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure, this::close);
            throw failure;
        }
    }

    VkDevice getDevice() {
        return device;
    }

    VkPhysicalDevice getPhysicalDevice() {
        return device.getPhysicalDevice();
    }

    long getSurface() {
        return surface;
    }

    int getQueueFamilyIndex() {
        return queueFamilyIndex;
    }

    VkQueue getQueue() {
        return queue;
    }

    long createSemaphore() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo info = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
            LongBuffer semaphore = stack.mallocLong(1);
            check(VK10.vkCreateSemaphore(device, info, null, semaphore), "vkCreateSemaphore");
            return semaphore.get(0);
        }
    }

    @Override
    public void close() {
        DeferredRelease.runAll(
                this::destroyDevice,
                () -> SDLVulkan.SDL_Vulkan_DestroySurface(instance, surface, null),
                this::destroyDebugMessenger,
                () -> VK10.vkDestroyInstance(instance, null),
                this::freeDebugCallback);
    }

    static void check(int result, String vkFunction) {
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException(vkFunction + " failed with VkResult " + result);
        }
    }

    private VkInstance createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer sdlExtensions = SDLVulkan.SDL_Vulkan_GetInstanceExtensions();
            LorisLwjglSdlVulkanExample.checkSdl(sdlExtensions != null, "read the Vulkan instance extensions");
            Set<String> availableExtensions = instanceExtensions();
            boolean portability = availableExtensions.contains(
                    KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME);
            PointerBuffer extensions = stack.mallocPointer(sdlExtensions.remaining() + 2);
            extensions.put(sdlExtensions);
            if (portability) {
                extensions.put(stack.UTF8(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME));
            }
            if (debugCallback != null) {
                extensions.put(stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
            }
            extensions.flip();
            VkApplicationInfo applicationInfo = VkApplicationInfo.calloc(stack)
                    .sType$Default()
                    .pApplicationName(stack.UTF8(APPLICATION_NAME))
                    .apiVersion(VK13.VK_API_VERSION_1_3);
            VkInstanceCreateInfo info = VkInstanceCreateInfo.calloc(stack)
                    .sType$Default()
                    .flags(portability ? KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR : 0)
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledExtensionNames(extensions);
            if (debugCallback != null) {
                requireValidationLayer();
                // Chained to the create info, the messenger also reports on instance creation and destruction
                info.ppEnabledLayerNames(stack.pointers(stack.UTF8(VALIDATION_LAYER)))
                        .pNext(debugMessengerInfo(stack));
            }
            PointerBuffer handle = stack.mallocPointer(1);
            check(VK10.vkCreateInstance(info, null, handle), "vkCreateInstance");
            return new VkInstance(handle.get(0), info);
        }
    }

    private static Set<String> instanceExtensions() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            check(VK10.vkEnumerateInstanceExtensionProperties((CharSequence) null, count, null),
                    "vkEnumerateInstanceExtensionProperties");
            VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count.get(0), stack);
            check(VK10.vkEnumerateInstanceExtensionProperties((CharSequence) null, count, properties),
                    "vkEnumerateInstanceExtensionProperties");
            return extensionNames(properties);
        }
    }

    private static void requireValidationLayer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            check(VK10.vkEnumerateInstanceLayerProperties(count, null), "vkEnumerateInstanceLayerProperties");
            VkLayerProperties.Buffer layers = VkLayerProperties.malloc(count.get(0), stack);
            check(VK10.vkEnumerateInstanceLayerProperties(count, layers), "vkEnumerateInstanceLayerProperties");
            for (VkLayerProperties layer : layers) {
                if (layer.layerNameString().equals(VALIDATION_LAYER)) {
                    return;
                }
            }
        }
        throw new IllegalStateException("The Vulkan validation layer " + VALIDATION_LAYER + " is not installed; "
                + "install the Vulkan SDK or run the example without lorisui.example.gpuDebug");
    }

    private VkDebugUtilsMessengerCreateInfoEXT debugMessengerInfo(MemoryStack stack) {
        return VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType$Default()
                .messageSeverity(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                        | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                .messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                        | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                        | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                .pfnUserCallback(debugCallback);
    }

    private long createDebugMessenger() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer messenger = stack.mallocLong(1);
            check(EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(instance, debugMessengerInfo(stack), null, messenger),
                    "vkCreateDebugUtilsMessengerEXT");
            return messenger.get(0);
        }
    }

    private long createSurface(long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer handle = stack.mallocLong(1);
            LorisLwjglSdlVulkanExample.checkSdl(SDLVulkan.SDL_Vulkan_CreateSurface(window, instance, null, handle),
                    "create the Vulkan surface");
            return handle.get(0);
        }
    }

    // Takes the first suitable device, preferring a discrete GPU, or fails naming what each device lacks
    private Candidate pickPhysicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            check(VK10.vkEnumeratePhysicalDevices(instance, count, null), "vkEnumeratePhysicalDevices");
            PointerBuffer handles = stack.mallocPointer(count.get(0));
            check(VK10.vkEnumeratePhysicalDevices(instance, count, handles), "vkEnumeratePhysicalDevices");
            Candidate chosen = null;
            List<String> rejections = new ArrayList<>();
            for (int i = 0; i < handles.capacity(); i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(handles.get(i), instance);
                VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
                VK10.vkGetPhysicalDeviceProperties(physicalDevice, properties);
                String missing = missingRequirement(physicalDevice, properties);
                int family = missing == null ? presentingGraphicsFamily(physicalDevice) : -1;
                if (missing == null && family < 0) {
                    missing = "a graphics queue family that can present to the window";
                }
                if (missing != null) {
                    rejections.add(properties.deviceNameString() + " lacks " + missing);
                    continue;
                }
                Candidate candidate = new Candidate(physicalDevice, family,
                        properties.deviceType() == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU);
                if (chosen == null || candidate.discrete() && !chosen.discrete()) {
                    chosen = candidate;
                }
            }
            if (chosen == null) {
                throw new IllegalStateException(rejections.isEmpty() ? "No Vulkan device found"
                        : "No suitable Vulkan device found: " + String.join("; ", rejections));
            }
            return chosen;
        }
    }

    // Returns null if the device meets the requirements that do not depend on the surface
    private static String missingRequirement(VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties properties) {
        if (Integer.compareUnsigned(properties.apiVersion(), VK13.VK_API_VERSION_1_3) < 0) {
            return "Vulkan 1.3";
        }
        if (!deviceExtensions(physicalDevice).contains(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
            return "the " + KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME + " extension";
        }
        if (!supportsDynamicRendering(physicalDevice)) {
            return "the dynamicRendering feature";
        }
        return null;
    }

    private static Set<String> deviceExtensions(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            check(VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, (CharSequence) null, count, null),
                    "vkEnumerateDeviceExtensionProperties");
            VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count.get(0), stack);
            check(VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, (CharSequence) null, count, properties),
                    "vkEnumerateDeviceExtensionProperties");
            return extensionNames(properties);
        }
    }

    private static Set<String> extensionNames(VkExtensionProperties.Buffer properties) {
        Set<String> names = new HashSet<>();
        for (VkExtensionProperties extension : properties) {
            names.add(extension.extensionNameString());
        }
        return names;
    }

    private static boolean supportsDynamicRendering(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceVulkan13Features features13 = VkPhysicalDeviceVulkan13Features.calloc(stack)
                    .sType$Default();
            VkPhysicalDeviceFeatures2 features = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType$Default()
                    .pNext(features13);
            VK11.vkGetPhysicalDeviceFeatures2(physicalDevice, features);
            return features13.dynamicRendering();
        }
    }

    // Returns -1 if no graphics queue family of the device can present to the surface
    private int presentingGraphicsFamily(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null);
            VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.malloc(count.get(0), stack);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, families);
            IntBuffer presentSupported = stack.mallocInt(1);
            for (int family = 0; family < families.capacity(); family++) {
                if ((families.get(family).queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) == 0) {
                    continue;
                }
                check(KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, family, surface,
                        presentSupported), "vkGetPhysicalDeviceSurfaceSupportKHR");
                if (presentSupported.get(0) == VK10.VK_TRUE) {
                    return family;
                }
            }
            return -1;
        }
    }

    private VkDevice createDevice(Candidate candidate) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean portabilitySubset = deviceExtensions(candidate.physicalDevice())
                    .contains(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME);
            PointerBuffer extensions = stack.mallocPointer(2);
            extensions.put(stack.UTF8(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            if (portabilitySubset) {
                extensions.put(stack.UTF8(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME));
            }
            extensions.flip();
            VkDeviceQueueCreateInfo.Buffer queueInfo = VkDeviceQueueCreateInfo.calloc(1, stack);
            queueInfo.get(0)
                    .sType$Default()
                    .queueFamilyIndex(candidate.queueFamilyIndex())
                    .pQueuePriorities(stack.floats(1.0f));
            VkPhysicalDeviceVulkan13Features features13 = VkPhysicalDeviceVulkan13Features.calloc(stack)
                    .sType$Default()
                    .dynamicRendering(true);
            VkDeviceCreateInfo info = VkDeviceCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(features13)
                    .pQueueCreateInfos(queueInfo)
                    .ppEnabledExtensionNames(extensions);
            PointerBuffer handle = stack.mallocPointer(1);
            check(VK10.vkCreateDevice(candidate.physicalDevice(), info, null, handle), "vkCreateDevice");
            return new VkDevice(handle.get(0), candidate.physicalDevice(), info, VK13.VK_API_VERSION_1_3);
        }
    }

    private VkQueue getDeviceQueue() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer handle = stack.mallocPointer(1);
            VK10.vkGetDeviceQueue(device, queueFamilyIndex, 0, handle);
            return new VkQueue(handle.get(0), device);
        }
    }

    private void destroyDevice() {
        if (device != null) {
            VK10.vkDestroyDevice(device, null);
        }
    }

    private void destroyDebugMessenger() {
        if (debugMessenger != VK10.VK_NULL_HANDLE) {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
    }

    private void freeDebugCallback() {
        if (debugCallback != null) {
            debugCallback.free();
        }
    }

    private static int printDebugMessage(int severity, long callbackData) {
        String level = (severity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0
                ? "error" : "warning";
        System.err.println("Vulkan " + level + ": "
                + VkDebugUtilsMessengerCallbackDataEXT.create(callbackData).pMessageString());
        return VK10.VK_FALSE;
    }

    private record Candidate(VkPhysicalDevice physicalDevice, int queueFamilyIndex, boolean discrete) {}
}
