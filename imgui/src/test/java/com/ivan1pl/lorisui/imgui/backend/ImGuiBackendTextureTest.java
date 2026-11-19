package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.component.Canvas;
import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
                () -> backend.loadTexture((String) null));
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

    @Test
    void shouldCreateCanvasTextureFromWholeImageInRgbaOrderThenReuseItWithoutUpdate() {
        Canvas canvas = Canvas.builder().size(20, 10).resolution(2, 1).build();
        canvas.setPixel(0, 0, 0x80112233);
        canvas.setPixel(1, 0, 0xFF445566);

        TextureData first = backend.loadTexture(canvas);
        TextureData second = backend.loadTexture(canvas);

        Assertions.assertSame(first, second);
        Assertions.assertEquals(2, first.width());
        Assertions.assertEquals(1, first.height());
        Assertions.assertEquals(List.of(new CanvasUpload(first, 0, 0, 2, 1, List.of(
                (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x80,
                (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0xFF))), backend.canvasCreated);
        Assertions.assertEquals(List.of(), backend.canvasUpdated);
    }

    @Test
    void shouldUploadOnlyDirtyRectangleAfterEdit() {
        Canvas canvas = Canvas.builder().size(4, 4).build();
        TextureData texture = backend.loadTexture(canvas);
        canvas.setPixel(1, 2, 0xFF010203);
        canvas.setPixel(2, 3, 0x7F040506);

        Assertions.assertSame(texture, backend.loadTexture(canvas));

        Assertions.assertEquals(List.of(new CanvasUpload(texture, 1, 2, 2, 2, List.of(
                (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0xFF, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x7F))),
                backend.canvasUpdated);
        Assertions.assertEquals(1, backend.canvasCreated.size());
    }

    @Test
    void shouldPassEachUploadExactlyItsOwnBytesAcrossGrowingShrinkingAndUnloadedScratchBuffer() {
        Canvas small = Canvas.builder().size(1, 1).build();
        Canvas large = Canvas.builder().size(2, 2).build();
        small.setPixel(0, 0, 0xFF010203);
        large.setPixels(0, 0, 2, 2, new int[] {0x01000004, 0x02000005, 0x03000006, 0x04000007}, 0, 2);
        List<Byte> largeBytes = List.of(
                (byte) 0, (byte) 0, (byte) 0x04, (byte) 0x01, (byte) 0, (byte) 0, (byte) 0x05, (byte) 0x02,
                (byte) 0, (byte) 0, (byte) 0x06, (byte) 0x03, (byte) 0, (byte) 0, (byte) 0x07, (byte) 0x04);

        TextureData smallTexture = backend.loadTexture(small);
        TextureData largeTexture = backend.loadTexture(large);
        small.setPixel(0, 0, 0x7F0A0B0C);
        backend.loadTexture(small);
        backend.unloadTextures();
        TextureData reloaded = backend.loadTexture(large);

        Assertions.assertEquals(List.of(
                new CanvasUpload(smallTexture, 0, 0, 1, 1, List.of((byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0xFF)),
                new CanvasUpload(largeTexture, 0, 0, 2, 2, largeBytes),
                new CanvasUpload(reloaded, 0, 0, 2, 2, largeBytes)), backend.canvasCreated);
        Assertions.assertEquals(List.of(new CanvasUpload(smallTexture, 0, 0, 1, 1,
                List.of((byte) 0x0A, (byte) 0x0B, (byte) 0x0C, (byte) 0x7F))), backend.canvasUpdated);
    }

    @Test
    void shouldEvictCanvasAtStartOfSecondRenderWithoutItAndCreateItAgainFromWholeImage() {
        backend.registerRenderer(Drawn.class, new DrawnRenderer());
        Canvas evicted = Canvas.builder().size(2, 2).build();
        Canvas kept = Canvas.builder().size(2, 2).build();
        Drawn drawEvicted = new Drawn(() -> backend.loadTexture(evicted));
        Drawn drawKept = new Drawn(() -> backend.loadTexture(kept));

        render(drawEvicted, drawKept);
        TextureData evictedTexture = backend.canvasCreated.get(0).texture();
        render(drawKept);
        Assertions.assertEquals(List.of(), backend.deleted);
        evicted.setPixel(1, 1, 0xFFFFFFFF);
        render(drawKept);

        Assertions.assertEquals(List.of(evictedTexture), backend.deleted);
        TextureData recreated = backend.loadTexture(evicted);
        Assertions.assertNotSame(evictedTexture, recreated);
        Assertions.assertEquals(3, backend.canvasCreated.size());
        CanvasUpload upload = backend.canvasCreated.get(2);
        Assertions.assertEquals(2, upload.width());
        Assertions.assertEquals(2, upload.height());
        Assertions.assertEquals((byte) 0xFF, upload.rgba().get(15));
        Assertions.assertEquals(List.of(), backend.canvasUpdated);
    }

    @Test
    void shouldNotEvictInNestedRenders() {
        backend.registerRenderer(Drawn.class, new DrawnRenderer());
        Canvas canvas = Canvas.builder().size(2, 2).build();
        Drawn drawCanvas = new Drawn(() -> backend.loadTexture(canvas));
        Drawn nested = new Drawn(() -> {
            backend.render(100, 100, List.of(), (key, _) -> key);
            backend.render(100, 100, List.of(), (key, _) -> key);
        });

        render(drawCanvas);
        render(nested);

        Assertions.assertEquals(List.of(), backend.deleted);
        render(nested);
        Assertions.assertEquals(List.of(backend.canvasCreated.get(0).texture()), backend.deleted);
    }

    @Test
    void shouldDeleteEveryEvictedTextureAndClearDrawnCanvasesWhenDeletesFail() {
        backend.registerRenderer(Drawn.class, new DrawnRenderer());
        List<Drawn> drawAll = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Canvas canvas = Canvas.builder().size(2, 2).build();
            drawAll.add(new Drawn(() -> backend.loadTexture(canvas)));
        }
        Drawn drawLast = drawAll.get(3);
        AtomicBoolean drawnAfterFailure = new AtomicBoolean();
        render(drawAll.toArray(new Drawn[0]));
        render(drawLast);
        backend.failDelete = true;

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> render(new Drawn(() -> drawnAfterFailure.set(true))));

        Assertions.assertEquals(2, thrown.getSuppressed().length);
        Assertions.assertFalse(drawnAfterFailure.get());
        Assertions.assertEquals(Set.of(backend.canvasCreated.get(0).texture(), backend.canvasCreated.get(1).texture(),
                backend.canvasCreated.get(2).texture()), new HashSet<>(backend.deleted));
        Assertions.assertEquals(3, backend.deleted.size());
        backend.failDelete = false;
        render();
        Assertions.assertEquals(4, backend.deleted.size());
        Assertions.assertSame(backend.canvasCreated.get(3).texture(), backend.deleted.get(3));
    }

    @Test
    void shouldDeleteCanvasTexturesOnUnloadButNotWhileDrawingAndCreateThemAgain() throws IOException {
        backend.registerRenderer(Drawn.class, new DrawnRenderer());
        Canvas canvas = Canvas.builder().size(2, 2).build();
        TextureData canvasTexture = backend.loadTexture(canvas);
        TextureData imageTexture = backend.loadTexture("x.png");

        Assertions.assertThrows(IllegalStateException.class,
                () -> render(new Drawn(() -> backend.loadTexture(canvas)), new Drawn(backend::unloadTextures)));
        Assertions.assertEquals(List.of(), backend.deleted);
        backend.unloadTextures();

        Assertions.assertEquals(Set.of(canvasTexture, imageTexture), new HashSet<>(backend.deleted));
        Assertions.assertNotSame(canvasTexture, backend.loadTexture(canvas));
        Assertions.assertEquals(2, backend.canvasCreated.size());
        Assertions.assertEquals(List.of(), backend.canvasUpdated);
    }

    @Test
    void shouldUploadWholeImageAfterFailedUpdateAndCreateAgainAfterFailedCreate() {
        Canvas canvas = Canvas.builder().size(3, 3).build();
        backend.failCanvasCreate = true;
        Assertions.assertThrows(IllegalStateException.class, () -> backend.loadTexture(canvas));
        backend.failCanvasCreate = false;
        TextureData texture = backend.loadTexture(canvas);
        Assertions.assertEquals(1, backend.canvasCreated.size());

        canvas.setPixel(2, 2, 0xFF0000FF);
        backend.failCanvasUpdate = true;
        Assertions.assertThrows(IllegalStateException.class, () -> backend.loadTexture(canvas));
        backend.failCanvasUpdate = false;
        backend.loadTexture(canvas);
        canvas.setPixel(0, 1, 0xFF00FF00);
        backend.loadTexture(canvas);

        Assertions.assertEquals(2, backend.canvasUpdated.size());
        CanvasUpload whole = backend.canvasUpdated.get(0);
        Assertions.assertEquals(new CanvasUpload(texture, 0, 0, 3, 3, whole.rgba()), whole);
        Assertions.assertEquals((byte) 0xFF, whole.rgba().get(34));
        Assertions.assertEquals(new CanvasUpload(texture, 0, 1, 1, 1,
                List.of((byte) 0, (byte) 0xFF, (byte) 0, (byte) 0xFF)), backend.canvasUpdated.get(1));
    }

    @Test
    void shouldRejectNullCanvasWithoutRecordingIt() {
        backend.registerRenderer(Drawn.class, new DrawnRenderer());

        NullPointerException thrown = Assertions.assertThrows(NullPointerException.class,
                () -> backend.loadTexture((Canvas) null));
        render();
        render();

        Assertions.assertTrue(thrown.getMessage().contains("canvas"), thrown.getMessage());
        Assertions.assertEquals(List.of(), backend.canvasCreated);
        Assertions.assertEquals(List.of(), backend.deleted);
    }

    private void render(Drawn... drawn) {
        backend.render(100, 100, List.of(drawn), (key, _) -> key);
    }

    private static final class RecordingBackend extends ImGuiBackend {
        private final List<TextureData> created = new ArrayList<>();
        private final List<String> createdPaths = new ArrayList<>();
        private final List<TextureData> deleted = new ArrayList<>();
        private final List<CanvasUpload> canvasCreated = new ArrayList<>();
        private final List<CanvasUpload> canvasUpdated = new ArrayList<>();
        private long nextCanvasId = 1;
        private boolean failCreate;
        private boolean failDelete;
        private boolean failCanvasCreate;
        private boolean failCanvasUpdate;

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

        @Override
        protected TextureData createCanvasTexture(int width, int height, ByteBuffer rgbaPixels) {
            if (failCanvasCreate) {
                throw new IllegalStateException("Cannot create canvas texture");
            }
            TextureData texture = new TextureData(nextCanvasId++, width, height);
            canvasCreated.add(new CanvasUpload(texture, 0, 0, width, height, bytes(rgbaPixels)));
            return texture;
        }

        @Override
        protected void updateCanvasTexture(TextureData texture, int x, int y, int width, int height,
                                           ByteBuffer rgbaPixels) {
            if (failCanvasUpdate) {
                throw new IllegalStateException("Cannot update canvas texture");
            }
            canvasUpdated.add(new CanvasUpload(texture, x, y, width, height, bytes(rgbaPixels)));
        }

        private static List<Byte> bytes(ByteBuffer rgbaPixels) {
            List<Byte> bytes = new ArrayList<>();
            for (int i = rgbaPixels.position(); i < rgbaPixels.limit(); i++) {
                bytes.add(rgbaPixels.get(i));
            }
            return bytes;
        }
    }

    private record CanvasUpload(TextureData texture, int x, int y, int width, int height, List<Byte> rgba) {}

    private record Drawn(Runnable action) implements Component {}

    private static final class DrawnRenderer extends Renderer<Drawn> {
        @Override
        public void render(Drawn component, int w, int h, RendererProvider rendererProvider,
                           BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
            component.action().run();
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
