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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

// Destroying or freeing VK_NULL_HANDLE is a no-op in Vulkan, so the release steps of objects that were never created
// do nothing.
public final class VulkanTextures {
    private VulkanTextures() {}

    public static long createSampler(VkDevice device, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo info = VkSamplerCreateInfo.calloc(stack)
                    .sType$Default()
                    .magFilter(VK10.VK_FILTER_LINEAR)
                    .minFilter(VK10.VK_FILTER_LINEAR)
                    .mipmapMode(VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .addressModeU(VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeV(VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeW(VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .minLod(-1000.0f)
                    .maxLod(1000.0f)
                    .maxAnisotropy(1.0f);
            LongBuffer sampler = stack.mallocLong(1);
            check(VK10.vkCreateSampler(device, info, null, sampler), "vkCreateSampler", what);
            return sampler.get(0);
        }
    }

    public static long createDescriptorSetLayout(VkDevice device, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
            bindings.get(0)
                    .binding(0)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .stageFlags(VK10.VK_SHADER_STAGE_FRAGMENT_BIT);
            VkDescriptorSetLayoutCreateInfo info = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType$Default()
                    .pBindings(bindings);
            LongBuffer layout = stack.mallocLong(1);
            check(VK10.vkCreateDescriptorSetLayout(device, info, null, layout), "vkCreateDescriptorSetLayout", what);
            return layout.get(0);
        }
    }

    public static Texture upload(VkDevice device, int queueFamilyIndex, VkQueue queue, ByteBuffer rgba, int width,
                                 int height, int format, long sampler, long layout, String what) {
        long size = (long) width * height * 4;
        if (rgba.remaining() < size) {
            throw new IllegalArgumentException("Pixel data of " + what + " holds " + rgba.remaining()
                    + " bytes, but a " + width + "x" + height + " RGBA image needs " + size);
        }
        long image = VK10.VK_NULL_HANDLE;
        long memory = VK10.VK_NULL_HANDLE;
        long view = VK10.VK_NULL_HANDLE;
        long descriptorPool = VK10.VK_NULL_HANDLE;
        long descriptorSet = VK10.VK_NULL_HANDLE;
        try {
            image = createImage(device, width, height, format, what);
            memory = allocateImageMemory(device, image, what);
            view = createImageView(device, image, format, what);
            descriptorPool = createDescriptorPool(device, what);
            descriptorSet = allocateDescriptorSet(device, descriptorPool, layout, what);
            writeDescriptorSet(device, descriptorSet, sampler, view);
            copyToImage(device, queueFamilyIndex, queue, rgba, width, height, image, what);
            return new Texture(image, memory, view, descriptorPool, descriptorSet);
        } catch (Throwable failure) {
            Texture created = new Texture(image, memory, view, descriptorPool, descriptorSet);
            DeferredRelease.runAfterFailure(failure, () -> destroy(device, created));
            throw failure;
        }
    }

    public static void destroy(VkDevice device, Texture texture) {
        DeferredRelease.runAll(
                () -> VK10.vkDestroyDescriptorPool(device, texture.descriptorPool(), null),
                () -> VK10.vkDestroyImageView(device, texture.view(), null),
                () -> VK10.vkDestroyImage(device, texture.image(), null),
                () -> VK10.vkFreeMemory(device, texture.memory(), null));
    }

    public static int memoryType(VkDevice device, int typeBits, int properties, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
            VK10.vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), memoryProperties);
            for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
                boolean allowed = (typeBits & (1 << i)) != 0;
                if (allowed && (memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("No suitable Vulkan memory type for " + what);
    }

    public static void check(int result, String vkFunction, String what) {
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException(vkFunction + " failed with VkResult " + result + " for " + what);
        }
    }

    private static long createImage(VkDevice device, int width, int height, int format, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo info = VkImageCreateInfo.calloc(stack)
                    .sType$Default()
                    .imageType(VK10.VK_IMAGE_TYPE_2D)
                    .format(format)
                    .extent(extent -> extent.width(width).height(height).depth(1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK10.VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK10.VK_IMAGE_USAGE_SAMPLED_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            LongBuffer image = stack.mallocLong(1);
            check(VK10.vkCreateImage(device, info, null, image), "vkCreateImage", what);
            return image.get(0);
        }
    }

    private static long allocateImageMemory(VkDevice device, long image, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            VK10.vkGetImageMemoryRequirements(device, image, requirements);
            long memory = allocateMemory(device, requirements, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, what);
            try {
                check(VK10.vkBindImageMemory(device, image, memory, 0L), "vkBindImageMemory", what);
            } catch (Throwable failure) {
                DeferredRelease.runAfterFailure(failure, () -> VK10.vkFreeMemory(device, memory, null));
                throw failure;
            }
            return memory;
        }
    }

    private static long allocateMemory(VkDevice device, VkMemoryRequirements requirements, int properties,
                                       String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryAllocateInfo info = VkMemoryAllocateInfo.calloc(stack)
                    .sType$Default()
                    .allocationSize(requirements.size())
                    .memoryTypeIndex(memoryType(device, requirements.memoryTypeBits(), properties, what));
            LongBuffer memory = stack.mallocLong(1);
            check(VK10.vkAllocateMemory(device, info, null, memory), "vkAllocateMemory", what);
            return memory.get(0);
        }
    }

    private static long createImageView(VkDevice device, long image, int format, String what) {
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
            check(VK10.vkCreateImageView(device, info, null, view), "vkCreateImageView", what);
            return view.get(0);
        }
    }

    private static long createDescriptorPool(VkDevice device, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolSize.Buffer sizes = VkDescriptorPoolSize.calloc(1, stack);
            sizes.get(0)
                    .type(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1);
            VkDescriptorPoolCreateInfo info = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType$Default()
                    .maxSets(1)
                    .pPoolSizes(sizes);
            LongBuffer pool = stack.mallocLong(1);
            check(VK10.vkCreateDescriptorPool(device, info, null, pool), "vkCreateDescriptorPool", what);
            return pool.get(0);
        }
    }

    private static long allocateDescriptorSet(VkDevice device, long descriptorPool, long layout, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetAllocateInfo info = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType$Default()
                    .descriptorPool(descriptorPool)
                    .pSetLayouts(stack.longs(layout));
            LongBuffer descriptorSet = stack.mallocLong(1);
            check(VK10.vkAllocateDescriptorSets(device, info, descriptorSet), "vkAllocateDescriptorSets", what);
            return descriptorSet.get(0);
        }
    }

    private static void writeDescriptorSet(VkDevice device, long descriptorSet, long sampler, long view) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.get(0)
                    .sampler(sampler)
                    .imageView(view)
                    .imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(1, stack);
            writes.get(0)
                    .sType$Default()
                    .dstSet(descriptorSet)
                    .dstBinding(0)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);
            VK10.vkUpdateDescriptorSets(device, writes, null);
        }
    }

    private static void copyToImage(VkDevice device, int queueFamilyIndex, VkQueue queue, ByteBuffer rgba, int width,
                                    int height, long image, String what) {
        long stagingBuffer = VK10.VK_NULL_HANDLE;
        long stagingMemory = VK10.VK_NULL_HANDLE;
        long commandPool = VK10.VK_NULL_HANDLE;
        long fence = VK10.VK_NULL_HANDLE;
        try {
            long size = (long) width * height * 4;
            stagingBuffer = createStagingBuffer(device, size, what);
            stagingMemory = allocateStagingMemory(device, stagingBuffer, what);
            fillStagingMemory(device, stagingMemory, rgba, size, what);
            commandPool = createCommandPool(device, queueFamilyIndex, what);
            VkCommandBuffer commandBuffer = allocateCommandBuffer(device, commandPool, what);
            recordCopy(commandBuffer, stagingBuffer, image, width, height, what);
            fence = createFence(device, what);
            submitAndWait(device, queue, commandBuffer, fence, what);
        } catch (Throwable failure) {
            DeferredRelease.runAfterFailure(failure,
                    releaseCopyObjects(device, fence, commandPool, stagingBuffer, stagingMemory));
            throw failure;
        }
        releaseCopyObjects(device, fence, commandPool, stagingBuffer, stagingMemory).run();
    }

    private static Runnable releaseCopyObjects(VkDevice device, long fence, long commandPool, long stagingBuffer,
                                               long stagingMemory) {
        return () -> DeferredRelease.runAll(
                () -> VK10.vkDestroyFence(device, fence, null),
                () -> VK10.vkDestroyCommandPool(device, commandPool, null),
                () -> VK10.vkDestroyBuffer(device, stagingBuffer, null),
                () -> VK10.vkFreeMemory(device, stagingMemory, null));
    }

    private static long createStagingBuffer(VkDevice device, long size, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo info = VkBufferCreateInfo.calloc(stack)
                    .sType$Default()
                    .size(size)
                    .usage(VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer buffer = stack.mallocLong(1);
            check(VK10.vkCreateBuffer(device, info, null, buffer), "vkCreateBuffer", what);
            return buffer.get(0);
        }
    }

    private static long allocateStagingMemory(VkDevice device, long stagingBuffer, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            VK10.vkGetBufferMemoryRequirements(device, stagingBuffer, requirements);
            long memory = allocateMemory(device, requirements,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, what);
            try {
                check(VK10.vkBindBufferMemory(device, stagingBuffer, memory, 0L), "vkBindBufferMemory", what);
            } catch (Throwable failure) {
                DeferredRelease.runAfterFailure(failure, () -> VK10.vkFreeMemory(device, memory, null));
                throw failure;
            }
            return memory;
        }
    }

    private static void fillStagingMemory(VkDevice device, long stagingMemory, ByteBuffer rgba, long size,
                                          String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer mapped = stack.mallocPointer(1);
            check(VK10.vkMapMemory(device, stagingMemory, 0L, size, 0, mapped), "vkMapMemory", what);
            try {
                MemoryUtil.memCopy(MemoryUtil.memAddress(rgba), mapped.get(0), size);
            } finally {
                VK10.vkUnmapMemory(device, stagingMemory);
            }
        }
    }

    private static long createCommandPool(VkDevice device, int queueFamilyIndex, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack)
                    .sType$Default()
                    .flags(VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
                    .queueFamilyIndex(queueFamilyIndex);
            LongBuffer commandPool = stack.mallocLong(1);
            check(VK10.vkCreateCommandPool(device, info, null, commandPool), "vkCreateCommandPool", what);
            return commandPool.get(0);
        }
    }

    private static VkCommandBuffer allocateCommandBuffer(VkDevice device, long commandPool, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo info = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType$Default()
                    .commandPool(commandPool)
                    .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            PointerBuffer commandBuffer = stack.mallocPointer(1);
            check(VK10.vkAllocateCommandBuffers(device, info, commandBuffer), "vkAllocateCommandBuffers", what);
            return new VkCommandBuffer(commandBuffer.get(0), device);
        }
    }

    private static void recordCopy(VkCommandBuffer commandBuffer, long stagingBuffer, long image, int width,
                                   int height, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType$Default()
                    .flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            check(VK10.vkBeginCommandBuffer(commandBuffer, beginInfo), "vkBeginCommandBuffer", what);

            VkImageMemoryBarrier.Buffer copyBarrier = imageBarrier(stack, image, 0, VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            VK10.vkCmdPipelineBarrier(commandBuffer, VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, copyBarrier);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.get(0)
                    .imageSubresource(subresource -> subresource
                            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                            .layerCount(1))
                    .imageExtent(extent -> extent.width(width).height(height).depth(1));
            VK10.vkCmdCopyBufferToImage(commandBuffer, stagingBuffer, image, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    region);

            VkImageMemoryBarrier.Buffer useBarrier = imageBarrier(stack, image, VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK10.VK_ACCESS_SHADER_READ_BIT, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VK10.vkCmdPipelineBarrier(commandBuffer, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, useBarrier);

            check(VK10.vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer", what);
        }
    }

    private static VkImageMemoryBarrier.Buffer imageBarrier(MemoryStack stack, long image, int srcAccessMask,
                                                            int dstAccessMask, int oldLayout, int newLayout) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        barrier.get(0)
                .sType$Default()
                .srcAccessMask(srcAccessMask)
                .dstAccessMask(dstAccessMask)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .image(image)
                .subresourceRange(range -> range
                        .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                        .levelCount(1)
                        .layerCount(1));
        return barrier;
    }

    private static long createFence(VkDevice device, String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo info = VkFenceCreateInfo.calloc(stack)
                    .sType$Default();
            LongBuffer fence = stack.mallocLong(1);
            check(VK10.vkCreateFence(device, info, null, fence), "vkCreateFence", what);
            return fence.get(0);
        }
    }

    private static void submitAndWait(VkDevice device, VkQueue queue, VkCommandBuffer commandBuffer, long fence,
                                      String what) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType$Default()
                    .pCommandBuffers(stack.pointers(commandBuffer));
            check(VK10.vkQueueSubmit(queue, submitInfo, fence), "vkQueueSubmit", what);
        }
        try {
            // A timeout of -1 is UINT64_MAX, which waits without a time limit.
            check(VK10.vkWaitForFences(device, fence, true, -1L), "vkWaitForFences", what);
        } catch (Throwable failure) {
            // The submitted copy may still be running, and the callers release the objects it uses once this throws
            DeferredRelease.runAfterFailure(failure,
                    () -> check(VK10.vkQueueWaitIdle(queue), "vkQueueWaitIdle", what));
            throw failure;
        }
    }

    public record Texture(long image, long memory, long view, long descriptorPool, long descriptorSet) {
    }
}
