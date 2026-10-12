package com.ivan1pl.lorisui.imgui.vulkan;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

class VulkanTexturesTest {
    @Test
    void shouldRejectPixelBufferSmallerThanImageBeforeAnyVulkanCall() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> VulkanTextures.upload(null, 0, null, ByteBuffer.allocateDirect(3), 1, 1, 0, 0L, 0L,
                        "texture test.png"));

        Assertions.assertTrue(exception.getMessage().contains("texture test.png"), exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("1x1"), exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("3 bytes"), exception.getMessage());
    }
}
