package com.ivan1pl.lorisui.imgui.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImGuiVulkanBackendTest {
    @Test
    void shouldRejectNegativeQueueFamilyIndexBeforeCheckingVulkanObjects() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImGuiVulkanBackend(null, -1, null));
    }

    @Test
    void shouldRejectNullDevice() {
        Assertions.assertThrows(NullPointerException.class, () -> new ImGuiVulkanBackend(null, 0, null));
    }
}
