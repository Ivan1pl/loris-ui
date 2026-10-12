package com.ivan1pl.lorisui.imgui.vulkan;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImGuiImplVulkanShadersTest {
    private static final int SPIRV_MAGIC_NUMBER = 0x07230203;

    @Test
    void shouldKeepTheWordCountsOfTheUpstreamShaders() {
        Assertions.assertEquals(324, ImGuiImplVulkanShaders.VERTEX_SHADER_SPIRV.length);
        Assertions.assertEquals(193, ImGuiImplVulkanShaders.FRAGMENT_SHADER_SPIRV.length);
    }

    @Test
    void shouldStartBothShadersWithTheSpirvMagicNumber() {
        Assertions.assertEquals(SPIRV_MAGIC_NUMBER, ImGuiImplVulkanShaders.VERTEX_SHADER_SPIRV[0]);
        Assertions.assertEquals(SPIRV_MAGIC_NUMBER, ImGuiImplVulkanShaders.FRAGMENT_SHADER_SPIRV[0]);
    }
}
