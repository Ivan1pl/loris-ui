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

    @Test
    void shouldRejectUpdatePixelBufferSmallerThanRegionBeforeAnyVulkanCall() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> VulkanTextures.update(null, 0, null, ByteBuffer.allocateDirect(7), 0, 0, 2, 1, 0L,
                        "canvas texture 4x4"));

        Assertions.assertTrue(exception.getMessage().contains("canvas texture 4x4"), exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("2x1"), exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("7 bytes"), exception.getMessage());
    }

    @Test
    void shouldSizeUpdateGuardByRegionNotOffset() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> VulkanTextures.update(null, 0, null, ByteBuffer.allocateDirect(23), 5, 7, 2, 3, 0L,
                        "canvas texture 16x16"));

        Assertions.assertTrue(exception.getMessage().contains("2x3"), exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("23 bytes"), exception.getMessage());
        Assertions.assertTrue(exception.getMessage().endsWith("needs 24"), exception.getMessage());
    }
}
