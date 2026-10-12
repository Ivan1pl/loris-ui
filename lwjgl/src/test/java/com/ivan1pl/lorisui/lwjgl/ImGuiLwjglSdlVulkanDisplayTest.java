package com.ivan1pl.lorisui.lwjgl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// A VkDevice cannot be created without a Vulkan driver, so Vulkan objects are passed only as null literals and every
// case fails on an argument check before the display could be built
class ImGuiLwjglSdlVulkanDisplayTest {
    private static final long WINDOW_HANDLE = 1L;
    private static final int COLOR_ATTACHMENT_FORMAT = 50;
    private static final int FRAMES_IN_FLIGHT = 2;
    private static final String WINDOW_MESSAGE = "sdlWindow must be the handle of an SDL window, not 0";

    @Test
    void shouldRejectZeroWindowHandleFirst() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiLwjglSdlVulkanDisplay(0L, null, -1, null, 0, 0));

        Assertions.assertEquals(WINDOW_MESSAGE, thrown.getMessage());
    }

    @Test
    void shouldRejectNegativeQueueFamilyBeforeVulkanObjects() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiLwjglSdlVulkanDisplay(WINDOW_HANDLE, null, -1, null, COLOR_ATTACHMENT_FORMAT,
                        FRAMES_IN_FLIGHT));
    }

    @Test
    void shouldRejectNullDevice() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new ImGuiLwjglSdlVulkanDisplay(WINDOW_HANDLE, null, 0, null, COLOR_ATTACHMENT_FORMAT,
                        FRAMES_IN_FLIGHT));
    }
}
