package com.ivan1pl.lorisui.imgui.renderer;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.backend.TextureData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

class RendererTest {
    @Test
    void shouldLoadTextureOnceWhenSamePathAndMasksRequestedTwice() throws IOException {
        ImGuiBackend backend = mockBackend();
        String path = uniquePath();

        TextureData first = Renderer.loadTexture(backend, path, Color.RED, null, Color.BLUE);
        TextureData second = Renderer.loadTexture(backend, path, Color.RED, null, Color.BLUE);

        Assertions.assertSame(first, second);
        Mockito.verify(backend, Mockito.times(1)).loadTexture(path, Color.RED, null, Color.BLUE);
    }

    @Test
    void shouldCacheNullAndNonNullMasksSeparately() throws IOException {
        ImGuiBackend backend = mockBackend();
        String path = uniquePath();

        TextureData unmasked = Renderer.loadTexture(backend, path);
        TextureData masked = Renderer.loadTexture(backend, path, Color.RED, Color.GREEN, Color.BLUE);

        Assertions.assertNotSame(unmasked, masked);
        Assertions.assertSame(unmasked, Renderer.loadTexture(backend, path));
        Assertions.assertSame(masked, Renderer.loadTexture(backend, path, Color.RED, Color.GREEN, Color.BLUE));
        Mockito.verify(backend, Mockito.times(1)).loadTexture(path, null, null, null);
        Mockito.verify(backend, Mockito.times(1)).loadTexture(path, Color.RED, Color.GREEN, Color.BLUE);
    }

    @Test
    void shouldShareEntryWhenMaskColorsAreEqualInstances() throws IOException {
        ImGuiBackend backend = mockBackend();
        String path = uniquePath();

        TextureData first = Renderer.loadTexture(backend, path, new Color(0.2f, 0.4f, 0.6f, 1f), null, null);
        TextureData second = Renderer.loadTexture(backend, path, new Color(0.2f, 0.4f, 0.6f, 1f), null, null);

        Assertions.assertSame(first, second);
        Mockito.verify(backend, Mockito.times(1))
                .loadTexture(Mockito.eq(path), Mockito.any(Color.class), Mockito.isNull(), Mockito.isNull());
    }

    @Test
    void shouldCacheDifferentPathsSeparately() throws IOException {
        ImGuiBackend backend = mockBackend();
        String firstPath = uniquePath();
        String secondPath = uniquePath();

        TextureData first = Renderer.loadTexture(backend, firstPath);
        TextureData second = Renderer.loadTexture(backend, secondPath);

        Assertions.assertNotSame(first, second);
        Mockito.verify(backend, Mockito.times(1)).loadTexture(firstPath, null, null, null);
        Mockito.verify(backend, Mockito.times(1)).loadTexture(secondPath, null, null, null);
    }

    private static ImGuiBackend mockBackend() throws IOException {
        AtomicInteger nextId = new AtomicInteger();
        ImGuiBackend backend = Mockito.mock(ImGuiBackend.class);
        Mockito.when(backend.loadTexture(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(_ -> new TextureData(nextId.incrementAndGet(), 16, 16));
        return backend;
    }

    // The texture cache is static and never cleared, so every test needs paths no other test uses
    private static String uniquePath() {
        return "textures/" + UUID.randomUUID() + ".png";
    }
}
