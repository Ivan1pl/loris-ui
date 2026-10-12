package com.ivan1pl.lorisui.imgui.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImGuiSdlGpuBackendTest {
    private static final long DEVICE = 1L;

    @Test
    void shouldRejectZeroGpuDevice() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImGuiSdlGpuBackend(0L));
    }

    @Test
    void shouldReturnGpuDevicePassedToConstructor() {
        Assertions.assertEquals(DEVICE, new ImGuiSdlGpuBackend(DEVICE).getGpuDevice());
    }

    @Test
    void shouldDoNothingWhenUnloadingEmptyCache() {
        ImGuiSdlGpuBackend backend = new ImGuiSdlGpuBackend(DEVICE);

        Assertions.assertDoesNotThrow(backend::unloadTextures);
    }
}
