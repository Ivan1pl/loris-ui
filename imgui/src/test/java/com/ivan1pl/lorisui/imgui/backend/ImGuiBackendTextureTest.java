package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ImGuiBackendTextureTest {
    private final RecordingBackend backend = new RecordingBackend();

    @Test
    void shouldCreateTextureOnceWhenSamePathAndMasksRequestedTwice() throws IOException {
        TextureData first = backend.loadTexture("x.png", Color.RED, null, Color.BLUE);
        TextureData second = backend.loadTexture("x.png", Color.RED, null, Color.BLUE);

        Assertions.assertSame(first, second);
        Assertions.assertEquals(1, backend.created.size());
    }

    @Test
    void shouldCacheNullAndNonNullMasksSeparatelyAndShareEqualColors() throws IOException {
        TextureData unmasked = backend.loadTexture("x.png");
        TextureData masked = backend.loadTexture("x.png", new Color(0.2f, 0.4f, 0.6f, 1f), null, null);

        Assertions.assertNotSame(unmasked, masked);
        Assertions.assertSame(unmasked, backend.loadTexture("x.png", null, null, null));
        Assertions.assertSame(masked, backend.loadTexture("x.png", new Color(0.2f, 0.4f, 0.6f, 1f), null, null));
        Assertions.assertEquals(2, backend.created.size());
    }

    @Test
    void shouldShareTextureBetweenPathsWithAndWithoutLeadingSlash() throws IOException {
        TextureData withSlash = backend.loadTexture("/x.png");
        TextureData withoutSlash = backend.loadTexture("x.png");

        Assertions.assertSame(withSlash, withoutSlash);
        Assertions.assertEquals(List.of("x.png"), backend.createdPaths);
    }

    @Test
    void shouldNotCacheFailedCreate() throws IOException {
        backend.failCreate = true;

        IOException thrown = Assertions.assertThrows(IOException.class, () -> backend.loadTexture("x.png"));

        Assertions.assertTrue(thrown.getMessage().contains("x.png"), thrown.getMessage());
        backend.failCreate = false;
        TextureData texture = backend.loadTexture("x.png");
        Assertions.assertSame(texture, backend.loadTexture("x.png"));
        Assertions.assertEquals(List.of("x.png", "x.png"), backend.createdPaths);
    }

    @Test
    void shouldDeleteEachDistinctTextureOnceAndCreateAgainAfterUnload() throws IOException {
        TextureData aliased = backend.loadTexture("/x.png");
        backend.loadTexture("x.png");
        TextureData masked = backend.loadTexture("x.png", Color.RED, Color.GREEN, Color.BLUE);
        TextureData other = backend.loadTexture("y.png");

        backend.unloadTextures();

        Assertions.assertEquals(3, backend.deleted.size());
        Assertions.assertEquals(Set.of(aliased, masked, other), new HashSet<>(backend.deleted));
        TextureData reloaded = backend.loadTexture("/x.png");
        Assertions.assertNotSame(aliased, reloaded);
        Assertions.assertEquals(4, backend.created.size());
    }

    @Test
    void shouldEmptyCacheAndRethrowFirstFailureWhenDeleteThrows() throws IOException {
        backend.loadTexture("x.png");
        backend.loadTexture("y.png");
        backend.loadTexture("z.png");
        backend.failDelete = true;

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, backend::unloadTextures);

        Assertions.assertEquals(3, backend.deleted.size());
        Assertions.assertEquals(2, thrown.getSuppressed().length);
        backend.failDelete = false;
        backend.loadTexture("x.png");
        Assertions.assertEquals(4, backend.created.size());
        backend.unloadTextures();
        Assertions.assertEquals(4, backend.deleted.size());
    }

    @Test
    void shouldRejectNullResourcePathWithoutCreatingTexture() {
        NullPointerException thrown = Assertions.assertThrows(NullPointerException.class,
                () -> backend.loadTexture(null));
        Assertions.assertThrows(NullPointerException.class, () -> backend.loadTexture(null, Color.RED, null, null));

        Assertions.assertTrue(thrown.getMessage().contains("resourcePath"), thrown.getMessage());
        Assertions.assertEquals(List.of(), backend.createdPaths);
    }

    @Test
    void shouldNotShareTexturesBetweenBackends() throws IOException {
        RecordingBackend otherBackend = new RecordingBackend();

        TextureData first = backend.loadTexture("x.png");
        TextureData second = otherBackend.loadTexture("x.png");

        Assertions.assertNotSame(first, second);
        Assertions.assertEquals(1, backend.created.size());
        Assertions.assertEquals(1, otherBackend.created.size());
        backend.unloadTextures();
        Assertions.assertSame(second, otherBackend.loadTexture("x.png"));
        Assertions.assertEquals(List.of(), otherBackend.deleted);
    }

    @Test
    void shouldRejectUnloadWhileDrawingAndDeleteNothing() throws IOException {
        backend.loadTexture("x.png");
        backend.registerRenderer(Unloader.class, new UnloadingRenderer(backend));

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> backend.render(100, 100, List.of(new Unloader()), (key, _) -> key));

        Assertions.assertTrue(thrown.getMessage().contains("drawn"), thrown.getMessage());
        Assertions.assertEquals(List.of(), backend.deleted);
        backend.unloadTextures();
        Assertions.assertEquals(1, backend.deleted.size());
    }

    @Test
    void shouldStillRejectUnloadAfterNestedRenderReturnsInsideFrame() throws IOException {
        backend.loadTexture("x.png");
        backend.registerRenderer(Nester.class, new NestingRenderer(backend));

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> backend.render(100, 100, List.of(new Nester()), (key, _) -> key));

        Assertions.assertTrue(thrown.getMessage().contains("drawn"), thrown.getMessage());
        Assertions.assertEquals(List.of(), backend.deleted);
        backend.unloadTextures();
        Assertions.assertEquals(1, backend.deleted.size());
    }

    private static final class RecordingBackend extends ImGuiBackend {
        private final List<TextureData> created = new ArrayList<>();
        private final List<String> createdPaths = new ArrayList<>();
        private final List<TextureData> deleted = new ArrayList<>();
        private boolean failCreate;
        private boolean failDelete;

        @Override
        protected TextureData createTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB)
                throws IOException {
            createdPaths.add(resourcePath);
            if (failCreate) {
                throw new IOException("Image not found: " + resourcePath);
            }
            // IDs above the int range, as SDL_GPU texture pointers are
            TextureData texture = new TextureData(Integer.MAX_VALUE + 1L + created.size(), 16, 16);
            created.add(texture);
            return texture;
        }

        @Override
        protected void deleteTexture(TextureData texture) {
            deleted.add(texture);
            if (failDelete) {
                throw new IllegalStateException("Cannot delete texture: " + texture.id());
            }
        }
    }

    private static final class Unloader implements Component {}

    private static final class UnloadingRenderer extends Renderer<Unloader> {
        private final ImGuiBackend backend;

        private UnloadingRenderer(ImGuiBackend backend) {
            this.backend = backend;
        }

        @Override
        public void render(Unloader component, int w, int h, RendererProvider rendererProvider,
                           BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
            backend.unloadTextures();
        }
    }

    private static final class Nester implements Component {}

    // Draws an empty nested component list, then unloads while the outer frame is still open
    private static final class NestingRenderer extends Renderer<Nester> {
        private final ImGuiBackend backend;

        private NestingRenderer(ImGuiBackend backend) {
            this.backend = backend;
        }

        @Override
        public void render(Nester component, int w, int h, RendererProvider rendererProvider,
                           BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
            backend.render(w, h, List.of(), bundleKeyResolver);
            backend.unloadTextures();
        }
    }
}
