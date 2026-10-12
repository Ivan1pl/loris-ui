package com.ivan1pl.lorisui.imgui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImGuiSdlVulkanRendererTest {
    private static final long WINDOW_HANDLE = 1L;
    private static final int COLOR_ATTACHMENT_FORMAT = 37;
    private static final int FRAMES_IN_FLIGHT = 2;

    @Test
    void shouldRejectZeroWindowHandleBeforeCheckingBackend() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiSdlVulkanRenderer(null, 0L, COLOR_ATTACHMENT_FORMAT, FRAMES_IN_FLIGHT));
    }

    @Test
    void shouldRejectUndefinedColorAttachmentFormat() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiSdlVulkanRenderer(null, WINDOW_HANDLE, 0, FRAMES_IN_FLIGHT));
    }

    @Test
    void shouldRejectZeroFramesInFlight() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiSdlVulkanRenderer(null, WINDOW_HANDLE, COLOR_ATTACHMENT_FORMAT, 0));
    }

    @Test
    void shouldRejectNegativeColorAttachmentFormat() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiSdlVulkanRenderer(null, WINDOW_HANDLE, -1, FRAMES_IN_FLIGHT));
    }

    @Test
    void shouldRejectNegativeFramesInFlight() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiSdlVulkanRenderer(null, WINDOW_HANDLE, COLOR_ATTACHMENT_FORMAT, -1));
    }

    @Test
    void shouldCheckWindowHandleThenFormatThenFramesInFlight() {
        IllegalArgumentException windowFirst = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiSdlVulkanRenderer(null, 0L, 0, 0));
        IllegalArgumentException formatSecond = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ImGuiSdlVulkanRenderer(null, WINDOW_HANDLE, 0, 0));

        Assertions.assertEquals("SDL window handle must not be 0", windowFirst.getMessage());
        Assertions.assertEquals("Color attachment format must be a defined VkFormat: 0", formatSecond.getMessage());
    }

    @Test
    void shouldRejectNullBackend() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new ImGuiSdlVulkanRenderer(null, WINDOW_HANDLE, COLOR_ATTACHMENT_FORMAT, FRAMES_IN_FLIGHT));
    }
}
