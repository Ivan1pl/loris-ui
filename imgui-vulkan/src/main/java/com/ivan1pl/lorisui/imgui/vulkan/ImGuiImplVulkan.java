/*
 * Ported from backends/imgui_impl_vulkan.cpp of Dear ImGui v1.92.7 (https://github.com/ocornut/imgui).
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2026 Omar Cornut
 * Copyright (c) 2026 Mikołaj Nowak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ivan1pl.lorisui.imgui.vulkan;

import com.ivan1pl.lorisui.api.screen.DeferredRelease;
import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec4;
import imgui.flag.ImGuiBackendFlags;
import imgui.type.ImInt;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkMappedMemoryRange;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryBarrier;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkViewport;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

// Destroying VK_NULL_HANDLE is a no-op in Vulkan, so the release steps of objects that were never created do nothing.
public final class ImGuiImplVulkan {
    private static final String RENDERER_NAME = "lorisui_impl_vulkan";
    private static final String RENDERER = "renderer";
    private static final String FONT_ATLAS = "font atlas";
    // Scale and translate, two floats each
    private static final int PUSH_CONSTANTS_SIZE = 4 * Float.BYTES;
    private static final int POSITION_OFFSET = 0;
    private static final int UV_OFFSET = 8;
    private static final int COLOR_OFFSET = 16;
    private static final long INITIAL_BUFFER_MEMORY_ALIGNMENT = 256L;
    private static final HostBuffer NO_BUFFER = new HostBuffer(VK10.VK_NULL_HANDLE, VK10.VK_NULL_HANDLE, 0L);

    private VkDevice device;
    private long vertexShaderModule = VK10.VK_NULL_HANDLE;
    private long fragmentShaderModule = VK10.VK_NULL_HANDLE;
    private long sampler = VK10.VK_NULL_HANDLE;
    private long descriptorSetLayout = VK10.VK_NULL_HANDLE;
    private long pipelineLayout = VK10.VK_NULL_HANDLE;
    private long pipeline = VK10.VK_NULL_HANDLE;
    private VulkanTextures.Texture fontTexture;
    private HostBuffer[] vertexBuffers;
    private HostBuffer[] indexBuffers;
    private int frameIndex;
    private long bufferMemoryAlignment = INITIAL_BUFFER_MEMORY_ALIGNMENT;

    public void init(VkDevice device, int queueFamilyIndex, VkQueue queue, int colorAttachmentFormat,
                     int framesInFlight) {
        this.device = device;
        ImGuiIO io = ImGui.getIO();
        io.setBackendRendererName(RENDERER_NAME);
        io.addBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);

        vertexShaderModule = createShaderModule(ImGuiImplVulkanShaders.VERTEX_SHADER_SPIRV);
        fragmentShaderModule = createShaderModule(ImGuiImplVulkanShaders.FRAGMENT_SHADER_SPIRV);
        sampler = VulkanTextures.createSampler(device, RENDERER);
        descriptorSetLayout = VulkanTextures.createDescriptorSetLayout(device, RENDERER);
        pipelineLayout = createPipelineLayout();
        pipeline = createPipeline(colorAttachmentFormat);
        destroyShaderModules();

        uploadFontAtlas(io.getFonts(), queueFamilyIndex, queue);

        vertexBuffers = emptyBuffers(framesInFlight);
        indexBuffers = emptyBuffers(framesInFlight);
    }

    public void renderDrawData(ImDrawData drawData, VkCommandBuffer commandBuffer, long targetImageView) {
        int framebufferWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
        int framebufferHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }

        frameIndex = (frameIndex + 1) % vertexBuffers.length;
        if (drawData.getTotalVtxCount() > 0) {
            uploadDrawLists(drawData);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            recordColorAttachmentBarrier(stack, commandBuffer);
            beginRendering(stack, commandBuffer, targetImageView, framebufferWidth, framebufferHeight);
            try {
                setupRenderState(stack, drawData, commandBuffer, framebufferWidth, framebufferHeight);
                drawCommandLists(stack, drawData, commandBuffer, framebufferWidth, framebufferHeight);
            } finally {
                // Leaves a full-framebuffer scissor for application pipelines that use dynamic scissor state
                setScissor(VkRect2D.calloc(1, stack), commandBuffer, 0, 0, framebufferWidth, framebufferHeight);
                VK13.vkCmdEndRendering(commandBuffer);
            }
        }
    }

    public void shutdown() {
        if (device == null) {
            return;
        }
        try {
            DeferredRelease.runAll(
                    this::destroyRenderBuffers,
                    this::destroyFontTexture,
                    () -> VK10.vkDestroyPipeline(device, pipeline, null),
                    () -> VK10.vkDestroyPipelineLayout(device, pipelineLayout, null),
                    () -> VK10.vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null),
                    () -> VK10.vkDestroySampler(device, sampler, null),
                    this::destroyShaderModules,
                    ImGuiImplVulkan::clearImGuiState);
        } finally {
            // An instance is never initialized again, so only a second shutdown has to become a no-op
            device = null;
        }
    }

    private long createShaderModule(int[] spirv) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Stack buffers are direct and in native byte order, as vkCreateShaderModule expects the words
            ByteBuffer code = stack.malloc(spirv.length * Integer.BYTES);
            code.asIntBuffer().put(spirv);
            VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc(stack)
                    .sType$Default()
                    .pCode(code);
            LongBuffer module = stack.mallocLong(1);
            VulkanTextures.check(VK10.vkCreateShaderModule(device, info, null, module), "vkCreateShaderModule",
                    RENDERER);
            return module.get(0);
        }
    }

    private long createPipelineLayout() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
            pushConstants.get(0)
                    .stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                    .offset(0)
                    .size(PUSH_CONSTANTS_SIZE);
            VkPipelineLayoutCreateInfo info = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType$Default()
                    .pSetLayouts(stack.longs(descriptorSetLayout))
                    .pPushConstantRanges(pushConstants);
            LongBuffer layout = stack.mallocLong(1);
            VulkanTextures.check(VK10.vkCreatePipelineLayout(device, info, null, layout), "vkCreatePipelineLayout",
                    RENDERER);
            return layout.get(0);
        }
    }

    private long createPipeline(int colorAttachmentFormat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineRenderingCreateInfo rendering = VkPipelineRenderingCreateInfo.calloc(stack)
                    .sType$Default()
                    .pColorAttachmentFormats(stack.ints(colorAttachmentFormat));
            VkGraphicsPipelineCreateInfo.Buffer info = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            info.get(0)
                    .sType$Default()
                    .pNext(rendering)
                    .pStages(shaderStages(stack))
                    .pVertexInputState(vertexInputState(stack))
                    .pInputAssemblyState(VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                            .sType$Default()
                            .topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST))
                    .pViewportState(VkPipelineViewportStateCreateInfo.calloc(stack)
                            .sType$Default()
                            .viewportCount(1)
                            .scissorCount(1))
                    .pRasterizationState(VkPipelineRasterizationStateCreateInfo.calloc(stack)
                            .sType$Default()
                            .polygonMode(VK10.VK_POLYGON_MODE_FILL)
                            .cullMode(VK10.VK_CULL_MODE_NONE)
                            .frontFace(VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE)
                            .lineWidth(1.0f))
                    .pMultisampleState(VkPipelineMultisampleStateCreateInfo.calloc(stack)
                            .sType$Default()
                            .rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT))
                    .pDepthStencilState(VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                            .sType$Default())
                    .pColorBlendState(colorBlendState(stack))
                    .pDynamicState(VkPipelineDynamicStateCreateInfo.calloc(stack)
                            .sType$Default()
                            .pDynamicStates(stack.ints(VK10.VK_DYNAMIC_STATE_VIEWPORT, VK10.VK_DYNAMIC_STATE_SCISSOR)))
                    .layout(pipelineLayout);
            LongBuffer created = stack.mallocLong(1);
            VulkanTextures.check(VK10.vkCreateGraphicsPipelines(device, VK10.VK_NULL_HANDLE, info, null, created),
                    "vkCreateGraphicsPipelines", RENDERER);
            return created.get(0);
        }
    }

    private VkPipelineShaderStageCreateInfo.Buffer shaderStages(MemoryStack stack) {
        ByteBuffer entryPoint = stack.UTF8("main");
        VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        stages.get(0)
                .sType$Default()
                .stage(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                .module(vertexShaderModule)
                .pName(entryPoint);
        stages.get(1)
                .sType$Default()
                .stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
                .module(fragmentShaderModule)
                .pName(entryPoint);
        return stages;
    }

    private static VkPipelineVertexInputStateCreateInfo vertexInputState(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer bindings = VkVertexInputBindingDescription.calloc(1, stack);
        bindings.get(0)
                .binding(0)
                .stride(ImDrawData.sizeOfImDrawVert())
                .inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX);
        VkVertexInputAttributeDescription.Buffer attributes = VkVertexInputAttributeDescription.calloc(3, stack);
        attributes.get(0)
                .location(0)
                .binding(0)
                .format(VK10.VK_FORMAT_R32G32_SFLOAT)
                .offset(POSITION_OFFSET);
        attributes.get(1)
                .location(1)
                .binding(0)
                .format(VK10.VK_FORMAT_R32G32_SFLOAT)
                .offset(UV_OFFSET);
        attributes.get(2)
                .location(2)
                .binding(0)
                .format(VK10.VK_FORMAT_R8G8B8A8_UNORM)
                .offset(COLOR_OFFSET);
        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType$Default()
                .pVertexBindingDescriptions(bindings)
                .pVertexAttributeDescriptions(attributes);
    }

    private static VkPipelineColorBlendStateCreateInfo colorBlendState(MemoryStack stack) {
        VkPipelineColorBlendAttachmentState.Buffer attachments = VkPipelineColorBlendAttachmentState.calloc(1, stack);
        attachments.get(0)
                .blendEnable(true)
                .srcColorBlendFactor(VK10.VK_BLEND_FACTOR_SRC_ALPHA)
                .dstColorBlendFactor(VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                .colorBlendOp(VK10.VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ONE)
                .dstAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                .alphaBlendOp(VK10.VK_BLEND_OP_ADD)
                .colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT
                        | VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT);
        return VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType$Default()
                .pAttachments(attachments);
    }

    private void uploadFontAtlas(ImFontAtlas fonts, int queueFamilyIndex, VkQueue queue) {
        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ByteBuffer pixels = fonts.getTexDataAsRGBA32(width, height);
        fontTexture = VulkanTextures.upload(device, queueFamilyIndex, queue, pixels, width.get(), height.get(),
                VK10.VK_FORMAT_R8G8B8A8_UNORM, sampler, descriptorSetLayout, FONT_ATLAS);
        fonts.setTexID(fontTexture.descriptorSet());
    }

    private void uploadDrawLists(ImDrawData drawData) {
        long vertexSize = alignBufferSize((long) drawData.getTotalVtxCount() * ImDrawData.sizeOfImDrawVert());
        long indexSize = alignBufferSize((long) drawData.getTotalIdxCount() * ImDrawData.sizeOfImDrawIdx());
        growBuffer(vertexBuffers, vertexSize, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        growBuffer(indexBuffers, indexSize, VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
        HostBuffer vertices = vertexBuffers[frameIndex];
        HostBuffer indices = indexBuffers[frameIndex];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer mapped = stack.mallocPointer(1);
            VulkanTextures.check(VK10.vkMapMemory(device, vertices.memory(), 0L, vertexSize, 0, mapped),
                    "vkMapMemory", RENDERER);
            try {
                long vertexDestination = mapped.get(0);
                VulkanTextures.check(VK10.vkMapMemory(device, indices.memory(), 0L, indexSize, 0, mapped),
                        "vkMapMemory", RENDERER);
                try {
                    copyDrawLists(drawData, vertexDestination, mapped.get(0));
                    flushMappedMemory(stack, vertices, indices);
                } finally {
                    VK10.vkUnmapMemory(device, indices.memory());
                }
            } finally {
                VK10.vkUnmapMemory(device, vertices.memory());
            }
        }
    }

    private void growBuffer(HostBuffer[] buffers, long size, int usage) {
        HostBuffer current = buffers[frameIndex];
        if (current.buffer() != VK10.VK_NULL_HANDLE && current.size() >= size) {
            return;
        }
        buffers[frameIndex] = NO_BUFFER;
        destroyBuffer(current);
        buffers[frameIndex] = createBuffer(size, usage);
    }

    private HostBuffer createBuffer(long size, int usage) {
        long alignedSize = alignBufferSize(size);
        long buffer = VK10.VK_NULL_HANDLE;
        long memory = VK10.VK_NULL_HANDLE;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo info = VkBufferCreateInfo.calloc(stack)
                    .sType$Default()
                    .size(alignedSize)
                    .usage(usage)
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer created = stack.mallocLong(1);
            VulkanTextures.check(VK10.vkCreateBuffer(device, info, null, created), "vkCreateBuffer", RENDERER);
            buffer = created.get(0);

            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            VK10.vkGetBufferMemoryRequirements(device, buffer, requirements);
            bufferMemoryAlignment = Math.max(bufferMemoryAlignment, requirements.alignment());
            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType$Default()
                    .allocationSize(requirements.size())
                    .memoryTypeIndex(VulkanTextures.memoryType(device, requirements.memoryTypeBits(),
                            VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, RENDERER));
            LongBuffer allocated = stack.mallocLong(1);
            VulkanTextures.check(VK10.vkAllocateMemory(device, allocateInfo, null, allocated), "vkAllocateMemory",
                    RENDERER);
            memory = allocated.get(0);

            VulkanTextures.check(VK10.vkBindBufferMemory(device, buffer, memory, 0L), "vkBindBufferMemory", RENDERER);
            return new HostBuffer(buffer, memory, alignedSize);
        } catch (Throwable failure) {
            HostBuffer created = new HostBuffer(buffer, memory, 0L);
            DeferredRelease.runAfterFailure(failure, () -> destroyBuffer(created));
            throw failure;
        }
    }

    private static void copyDrawLists(ImDrawData drawData, long vertexDestination, long indexDestination) {
        long vertexAddress = vertexDestination;
        long indexAddress = indexDestination;
        for (int list = 0; list < drawData.getCmdListsCount(); list++) {
            // Both getters share one buffer, so each list's vertices are copied before its indices are read
            ByteBuffer vertices = drawData.getCmdListVtxBufferData(list);
            MemoryUtil.memCopy(MemoryUtil.memAddress(vertices), vertexAddress, vertices.remaining());
            vertexAddress += vertices.remaining();
            ByteBuffer indices = drawData.getCmdListIdxBufferData(list);
            MemoryUtil.memCopy(MemoryUtil.memAddress(indices), indexAddress, indices.remaining());
            indexAddress += indices.remaining();
        }
    }

    private void flushMappedMemory(MemoryStack stack, HostBuffer vertices, HostBuffer indices) {
        // VK_WHOLE_SIZE as upstream: a size rounded up to nonCoherentAtomSize could exceed the allocation
        VkMappedMemoryRange.Buffer ranges = VkMappedMemoryRange.calloc(2, stack);
        ranges.get(0)
                .sType$Default()
                .memory(vertices.memory())
                .size(VK10.VK_WHOLE_SIZE);
        ranges.get(1)
                .sType$Default()
                .memory(indices.memory())
                .size(VK10.VK_WHOLE_SIZE);
        VulkanTextures.check(VK10.vkFlushMappedMemoryRanges(device, ranges), "vkFlushMappedMemoryRanges", RENDERER);
    }

    private static void recordColorAttachmentBarrier(MemoryStack stack, VkCommandBuffer commandBuffer) {
        VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
        barrier.get(0)
                .sType$Default()
                .srcAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        VK10.vkCmdPipelineBarrier(commandBuffer, VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0, barrier, null, null);
    }

    private static void beginRendering(MemoryStack stack, VkCommandBuffer commandBuffer, long targetImageView,
                                       int framebufferWidth, int framebufferHeight) {
        VkRenderingAttachmentInfo.Buffer colorAttachments = VkRenderingAttachmentInfo.calloc(1, stack);
        colorAttachments.get(0)
                .sType$Default()
                .imageView(targetImageView)
                .imageLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_LOAD)
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE);
        VkRenderingInfo info = VkRenderingInfo.calloc(stack)
                .sType$Default()
                .renderArea(area -> area.extent().set(framebufferWidth, framebufferHeight))
                .layerCount(1)
                .pColorAttachments(colorAttachments);
        VK13.vkCmdBeginRendering(commandBuffer, info);
    }

    private void setupRenderState(MemoryStack stack, ImDrawData drawData, VkCommandBuffer commandBuffer,
                                  int framebufferWidth, int framebufferHeight) {
        VK10.vkCmdBindPipeline(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        if (drawData.getTotalVtxCount() > 0) {
            VK10.vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(vertexBuffers[frameIndex].buffer()),
                    stack.longs(0L));
            int indexType = ImDrawData.sizeOfImDrawIdx() == 2 ? VK10.VK_INDEX_TYPE_UINT16 : VK10.VK_INDEX_TYPE_UINT32;
            VK10.vkCmdBindIndexBuffer(commandBuffer, indexBuffers[frameIndex].buffer(), 0L, indexType);
        }

        VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
        viewport.get(0)
                .x(0.0f)
                .y(0.0f)
                .width(framebufferWidth)
                .height(framebufferHeight)
                .minDepth(0.0f)
                .maxDepth(1.0f);
        VK10.vkCmdSetViewport(commandBuffer, 0, viewport);

        float scaleX = 2.0f / drawData.getDisplaySizeX();
        float scaleY = 2.0f / drawData.getDisplaySizeY();
        float translateX = -1.0f - drawData.getDisplayPosX() * scaleX;
        float translateY = -1.0f - drawData.getDisplayPosY() * scaleY;
        VK10.vkCmdPushConstants(commandBuffer, pipelineLayout, VK10.VK_SHADER_STAGE_VERTEX_BIT, 0,
                stack.floats(scaleX, scaleY, translateX, translateY));
    }

    private void drawCommandLists(MemoryStack stack, ImDrawData drawData, VkCommandBuffer commandBuffer,
                                  int framebufferWidth, int framebufferHeight) {
        float clipOffsetX = drawData.getDisplayPosX();
        float clipOffsetY = drawData.getDisplayPosY();
        float clipScaleX = drawData.getFramebufferScaleX();
        float clipScaleY = drawData.getFramebufferScaleY();
        ImVec4 clipRect = new ImVec4();
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
        LongBuffer descriptorSet = stack.mallocLong(1);
        long boundDescriptorSet = VK10.VK_NULL_HANDLE;
        int globalVertexOffset = 0;
        int globalIndexOffset = 0;
        for (int list = 0; list < drawData.getCmdListsCount(); list++) {
            for (int command = 0; command < drawData.getCmdListCmdBufferSize(list); command++) {
                drawData.getCmdListCmdBufferClipRect(clipRect, list, command);
                float clipMinX = Math.max((clipRect.x - clipOffsetX) * clipScaleX, 0.0f);
                float clipMinY = Math.max((clipRect.y - clipOffsetY) * clipScaleY, 0.0f);
                float clipMaxX = Math.min((clipRect.z - clipOffsetX) * clipScaleX, framebufferWidth);
                float clipMaxY = Math.min((clipRect.w - clipOffsetY) * clipScaleY, framebufferHeight);
                if (clipMaxX <= clipMinX || clipMaxY <= clipMinY) {
                    continue;
                }
                setScissor(scissor, commandBuffer, (int) clipMinX, (int) clipMinY, (int) (clipMaxX - clipMinX),
                        (int) (clipMaxY - clipMinY));

                long textureId = drawData.getCmdListCmdBufferTextureId(list, command);
                if (textureId != boundDescriptorSet) {
                    descriptorSet.put(0, textureId);
                    VK10.vkCmdBindDescriptorSets(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout,
                            0, descriptorSet, null);
                    boundDescriptorSet = textureId;
                }

                VK10.vkCmdDrawIndexed(commandBuffer, drawData.getCmdListCmdBufferElemCount(list, command), 1,
                        drawData.getCmdListCmdBufferIdxOffset(list, command) + globalIndexOffset,
                        drawData.getCmdListCmdBufferVtxOffset(list, command) + globalVertexOffset, 0);
            }
            globalIndexOffset += drawData.getCmdListIdxBufferSize(list);
            globalVertexOffset += drawData.getCmdListVtxBufferSize(list);
        }
    }

    private static void setScissor(VkRect2D.Buffer scissor, VkCommandBuffer commandBuffer, int x, int y, int width,
                                   int height) {
        VkRect2D rect = scissor.get(0);
        rect.offset().set(x, y);
        rect.extent().set(width, height);
        VK10.vkCmdSetScissor(commandBuffer, 0, scissor);
    }

    private long alignBufferSize(long size) {
        return (size + bufferMemoryAlignment - 1) & ~(bufferMemoryAlignment - 1);
    }

    private void destroyRenderBuffers() {
        if (vertexBuffers == null) {
            return;
        }
        Runnable[] steps = new Runnable[vertexBuffers.length + indexBuffers.length];
        for (int slot = 0; slot < vertexBuffers.length; slot++) {
            HostBuffer vertices = vertexBuffers[slot];
            HostBuffer indices = indexBuffers[slot];
            steps[2 * slot] = () -> destroyBuffer(vertices);
            steps[2 * slot + 1] = () -> destroyBuffer(indices);
        }
        DeferredRelease.runAll(steps);
    }

    private void destroyBuffer(HostBuffer buffer) {
        DeferredRelease.runAll(
                () -> VK10.vkDestroyBuffer(device, buffer.buffer(), null),
                () -> VK10.vkFreeMemory(device, buffer.memory(), null));
    }

    private void destroyFontTexture() {
        if (fontTexture != null) {
            VulkanTextures.destroy(device, fontTexture);
        }
    }

    private void destroyShaderModules() {
        try {
            DeferredRelease.runAll(
                    () -> VK10.vkDestroyShaderModule(device, vertexShaderModule, null),
                    () -> VK10.vkDestroyShaderModule(device, fragmentShaderModule, null));
        } finally {
            vertexShaderModule = VK10.VK_NULL_HANDLE;
            fragmentShaderModule = VK10.VK_NULL_HANDLE;
        }
    }

    private static void clearImGuiState() {
        ImGuiIO io = ImGui.getIO();
        io.getFonts().setTexID(0);
        io.removeBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);
        io.setBackendRendererName(null);
    }

    private static HostBuffer[] emptyBuffers(int count) {
        HostBuffer[] buffers = new HostBuffer[count];
        Arrays.fill(buffers, NO_BUFFER);
        return buffers;
    }

    private record HostBuffer(long buffer, long memory, long size) {
    }
}
