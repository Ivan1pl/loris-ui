package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class CanvasTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Test
    void shouldUseDefaultsWhenOnlySizeIsSet() {
        Canvas canvas = Canvas.builder()
                .size(4, 3)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(4, canvas.getWidth()),
                () -> Assertions.assertEquals(3, canvas.getHeight()),
                () -> Assertions.assertEquals(4, canvas.getResolutionWidth()),
                () -> Assertions.assertEquals(3, canvas.getResolutionHeight()),
                () -> Assertions.assertNull(canvas.getBackgroundColor()),
                () -> Assertions.assertTrue(canvas.isEnabled()),
                () -> Assertions.assertEquals(new Canvas.Region(0, 0, 4, 3), canvas.getVisibleRegion()),
                () -> Assertions.assertArrayEquals(new int[12], canvas.takePixelUpdate(true).argb()),
                () -> Assertions.assertNull(canvas.takePixelUpdate(false)),
                () -> Assertions.assertDoesNotThrow(() -> {
                    canvas.click(0, 0, MouseButton.LEFT);
                    canvas.drag(0, 0, 1, 1, MouseButton.RIGHT);
                    canvas.wheel(0, 0, 1f);
                }));
    }

    @Test
    void shouldValidateBuilderArguments() {
        Canvas.Builder builder = Canvas.builder();
        Canvas withResolution = Canvas.builder()
                .size(360, 180)
                .resolution(120, 60)
                .backgroundColor(() -> Color.BLUE)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(120, withResolution.getResolutionWidth()),
                () -> Assertions.assertEquals(60, withResolution.getResolutionHeight()),
                () -> Assertions.assertSame(Color.BLUE, withResolution.getBackgroundColor()),
                () -> assertNullPointer("size", () -> Canvas.builder().build()),
                () -> assertNullPointer("size", () -> Canvas.builder().resolution(2, 2).build()),
                () -> assertIllegalArgument("width must be positive: 0", () -> builder.size(0, -1)),
                () -> assertIllegalArgument("height must be positive: -1", () -> builder.size(1, -1)),
                () -> assertIllegalArgument("resolution width must be positive: -2", () -> builder.resolution(-2, 0)),
                () -> assertIllegalArgument("resolution height must be positive: 0", () -> builder.resolution(2, 0)),
                () -> assertIllegalArgument("Canvas resolution 65536x8192 has more than 536870911 pixels",
                        () -> Canvas.builder().size(10, 10).resolution(65536, 8192).build()),
                () -> assertIllegalArgument("Canvas resolution 30000x30000 has more than 536870911 pixels",
                        () -> Canvas.builder().size(30000, 30000).build()),
                () -> assertNullPointer("backgroundColor", () -> builder.backgroundColor((Supplier<Color>) null)),
                () -> assertNullPointer("enabled", () -> builder.enabled((Supplier<Boolean>) null)),
                () -> assertNullPointer("clickAction", () -> builder.clickAction(null)),
                () -> assertNullPointer("dragAction", () -> builder.dragAction(null)),
                () -> assertNullPointer("wheelAction", () -> builder.wheelAction(null)));
    }

    @Test
    void shouldReadSuppliersOnEveryCallAndAcceptConstantOverloads() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        AtomicReference<Color> background = new AtomicReference<>(Color.RED);
        Canvas.ClickAction click = Mockito.mock(Canvas.ClickAction.class);
        Canvas dynamic = Canvas.builder()
                .size(10, 10)
                .backgroundColor(background::get)
                .enabled(enabled::get)
                .clickAction(click)
                .build();
        Canvas constant = Canvas.builder()
                .size(10, 10)
                .backgroundColor(Color.GREEN)
                .enabled(false)
                .clickAction(click)
                .build();
        Canvas noBackground = Canvas.builder()
                .size(10, 10)
                .backgroundColor((Color) null)
                .build();

        Color firstBackground = dynamic.getBackgroundColor();
        dynamic.click(1, 1, MouseButton.LEFT);
        enabled.set(false);
        background.set(null);
        dynamic.click(2, 2, MouseButton.LEFT);
        constant.click(3, 3, MouseButton.LEFT);

        Mockito.verify(click).click(1, 1, MouseButton.LEFT);
        Mockito.verifyNoMoreInteractions(click);
        Assertions.assertAll(
                () -> Assertions.assertSame(Color.RED, firstBackground),
                () -> Assertions.assertNull(dynamic.getBackgroundColor()),
                () -> Assertions.assertFalse(dynamic.isEnabled()),
                () -> Assertions.assertSame(Color.GREEN, constant.getBackgroundColor()),
                () -> Assertions.assertFalse(constant.isEnabled()),
                () -> Assertions.assertNull(noBackground.getBackgroundColor()));
    }

    @Test
    void shouldRoundTripPixelsWithoutRetainingCallerArrays() {
        Canvas canvas = Canvas.builder()
                .size(4, 4)
                .build();
        int[] source = {
            -1, -1, -1, -1, -1,
            -1, 0x11223344, 0x55667788, -1, -1,
            -1, 0x99AABBCC, 0xDDEEFF00, -1, -1};

        canvas.setPixel(0, 0, 0x80FF0000);
        canvas.setPixel(3, 3, new Color(0x40102030));
        canvas.setPixels(1, 2, 2, 2, source, 6, 5);
        source[6] = 7;
        int[] target = new int[10];
        Arrays.fill(target, 5);
        canvas.getPixels(1, 2, 2, 2, target, 1, 4);

        Assertions.assertAll(
                () -> Assertions.assertEquals(0x80FF0000, canvas.getPixel(0, 0)),
                () -> Assertions.assertEquals(0x40102030, canvas.getPixel(3, 3)),
                () -> Assertions.assertEquals(0x11223344, canvas.getPixel(1, 2)),
                () -> Assertions.assertEquals(0x55667788, canvas.getPixel(2, 2)),
                () -> Assertions.assertEquals(0x99AABBCC, canvas.getPixel(1, 3)),
                () -> Assertions.assertEquals(0xDDEEFF00, canvas.getPixel(2, 3)),
                () -> Assertions.assertEquals(0, canvas.getPixel(0, 3)),
                () -> Assertions.assertArrayEquals(
                        new int[] {5, 0x11223344, 0x55667788, 5, 5, 0x99AABBCC, 0xDDEEFF00, 5, 5, 5}, target),
                () -> assertNullPointer("color", () -> canvas.setPixel(-1, -1, (Color) null)));
    }

    @Test
    void shouldCheckArrayArgumentsInPromisedOrder() {
        Canvas canvas = Canvas.builder()
                .size(4, 3)
                .build();

        for (ArrayAccess access : List.<ArrayAccess>of(Canvas::setPixels, Canvas::getPixels)) {
            Assertions.assertAll(
                    () -> assertNullPointer("argb", () -> access.apply(canvas, -1, 0, -1, -1, null, -1, -5)),
                    () -> assertIllegalArgument("Region size must not be negative: -1x2",
                            () -> access.apply(canvas, -1, 0, -1, 2, new int[0], -1, -5)),
                    () -> assertIllegalArgument("Region at (-1, 0) of size 1x1 is outside the 4x3 image",
                            () -> access.apply(canvas, -1, 0, 1, 1, new int[0], -1, -5)),
                    () -> assertIllegalArgument("Region at (3, 0) of size 2x1 is outside the 4x3 image",
                            () -> access.apply(canvas, 3, 0, 2, 1, new int[0], -1, -5)),
                    () -> assertIllegalArgument("offset must not be negative: -1",
                            () -> access.apply(canvas, 0, 0, 2, 1, new int[0], -1, -5)),
                    () -> assertIllegalArgument("scanSize must not be less than the region width: 1 < 2",
                            () -> access.apply(canvas, 0, 0, 2, 1, new int[0], 0, 1)),
                    () -> assertIllegalArgument(
                            "Array of length 4 is too short for offset 1, scan size 3 and region 2x2",
                            () -> access.apply(canvas, 0, 0, 2, 2, new int[4], 1, 3)),
                    () -> Assertions.assertDoesNotThrow(() -> access.apply(canvas, 4, 3, 0, 0, new int[0], 9, 0)));
        }

        Assertions.assertAll(
                () -> assertIllegalArgument("Pixel (4, 0) is outside the 4x3 image", () -> canvas.getPixel(4, 0)),
                () -> assertIllegalArgument("Pixel (0, -1) is outside the 4x3 image", () -> canvas.setPixel(0, -1, 1)),
                () -> assertIllegalArgument("Pixel (0, 3) is outside the 4x3 image",
                        () -> canvas.setPixel(0, 3, Color.RED)),
                () -> Assertions.assertNull(canvas.takePixelUpdate(false)),
                () -> Assertions.assertArrayEquals(new int[12], canvas.takePixelUpdate(true).argb()));
    }

    @Test
    void shouldValidateEmptyRegionsAndChangeNothing() {
        Canvas canvas = Canvas.builder()
                .size(4, 3)
                .build();
        int[] target = {5, 6};
        int max = Integer.MAX_VALUE;

        for (ArrayAccess access : List.<ArrayAccess>of(Canvas::setPixels, Canvas::getPixels)) {
            Assertions.assertAll(
                    () -> Assertions.assertDoesNotThrow(() -> access.apply(canvas, 0, 0, 0, 3, new int[0], 9, 0)),
                    () -> Assertions.assertDoesNotThrow(() -> access.apply(canvas, 4, 1, 0, 2, target, 0, max)),
                    () -> Assertions.assertDoesNotThrow(() -> access.apply(canvas, 1, 3, 3, 0, new int[0], 7, 3)),
                    () -> assertIllegalArgument("Region at (0, 2) of size 0x2 is outside the 4x3 image",
                            () -> access.apply(canvas, 0, 2, 0, 2, new int[0], 0, 0)),
                    () -> assertIllegalArgument("Region at (2, 0) of size 3x0 is outside the 4x3 image",
                            () -> access.apply(canvas, 2, 0, 3, 0, new int[0], 0, 3)),
                    () -> assertIllegalArgument("scanSize must not be less than the region width: 1 < 2",
                            () -> access.apply(canvas, 0, 0, 2, 0, new int[0], 0, 1)));
        }

        Assertions.assertAll(
                () -> Assertions.assertArrayEquals(new int[] {5, 6}, target),
                () -> Assertions.assertNull(canvas.takePixelUpdate(false)),
                () -> Assertions.assertArrayEquals(new int[12], canvas.takePixelUpdate(true).argb()));
    }

    @Test
    void shouldRejectArgumentsWhoseSumsOverflowIntWithoutAnyChange() {
        Canvas canvas = Canvas.builder()
                .size(4, 3)
                .build();
        int max = Integer.MAX_VALUE;

        for (ArrayAccess access : List.<ArrayAccess>of(Canvas::setPixels, Canvas::getPixels)) {
            Assertions.assertAll(
                    () -> assertIllegalArgument("Region at (2147483647, 0) of size 1x1 is outside the 4x3 image",
                            () -> access.apply(canvas, max, 0, 1, 1, new int[1], 0, 1)),
                    () -> assertIllegalArgument("Region at (0, 1) of size 1x2147483647 is outside the 4x3 image",
                            () -> access.apply(canvas, 0, 1, 1, max, new int[1], 0, 1)),
                    () -> assertIllegalArgument(
                            "Array of length 4 is too short for offset 2147483647, scan size 1 and region 1x1",
                            () -> access.apply(canvas, 0, 0, 1, 1, new int[4], max, 1)),
                    () -> assertIllegalArgument(
                            "Array of length 8 is too short for offset 0, scan size 2147483647 and region 1x2",
                            () -> access.apply(canvas, 0, 0, 1, 2, new int[8], 0, max)));
        }

        Assertions.assertAll(
                () -> assertIllegalArgument(
                        "Visible region at (1, 0) of size 2147483647x1 must have a positive size and lie inside "
                                + "the 4x3 image", () -> canvas.setVisibleRegion(1, 0, max, 1)),
                () -> Assertions.assertEquals(new Canvas.Region(0, 0, 4, 3), canvas.getVisibleRegion()),
                () -> Assertions.assertNull(canvas.takePixelUpdate(false)),
                () -> Assertions.assertArrayEquals(new int[12], canvas.takePixelUpdate(true).argb()));
    }

    @Test
    void shouldMergeEditsIntoOneDirtyRectangleAndTakeCopies() {
        Canvas canvas = Canvas.builder()
                .size(10, 10)
                .resolution(5, 4)
                .build();

        canvas.setPixel(1, 2, 0xFF000001);
        canvas.setPixels(3, 0, 1, 2, new int[] {0xFF000002, 0xFF000003}, 0, 1);
        Canvas.PixelUpdate merged = canvas.takePixelUpdate(false);
        Canvas.PixelUpdate afterTake = canvas.takePixelUpdate(false);
        merged.argb()[0] = 42;
        Canvas.PixelUpdate whole = canvas.takePixelUpdate(true);

        Assertions.assertAll(
                () -> Assertions.assertEquals(1, merged.x()),
                () -> Assertions.assertEquals(0, merged.y()),
                () -> Assertions.assertEquals(3, merged.width()),
                () -> Assertions.assertEquals(3, merged.height()),
                () -> Assertions.assertArrayEquals(new int[] {
                    42, 0, 0xFF000002,
                    0, 0, 0xFF000003,
                    0xFF000001, 0, 0}, merged.argb()),
                () -> Assertions.assertNull(afterTake),
                () -> Assertions.assertEquals(0, canvas.getPixel(1, 0)),
                () -> Assertions.assertEquals(new Canvas.Region(0, 0, 5, 4),
                        new Canvas.Region(whole.x(), whole.y(), whole.width(), whole.height())),
                () -> Assertions.assertEquals(20, whole.argb().length),
                () -> Assertions.assertEquals(0xFF000001, whole.argb()[2 * 5 + 1]),
                () -> Assertions.assertNull(canvas.takePixelUpdate(false)));

        canvas.setPixel(4, 3, 1);
        canvas.takePixelUpdate(true);
        Assertions.assertNull(canvas.takePixelUpdate(false));
    }

    @Test
    void shouldSetVisibleRegionAndKeepItOnRejection() {
        Canvas canvas = Canvas.builder()
                .size(100, 50)
                .resolution(20, 10)
                .build();

        canvas.setVisibleRegion(2, 3, 18, 7);

        Assertions.assertAll(
                () -> Assertions.assertEquals(new Canvas.Region(2, 3, 18, 7), canvas.getVisibleRegion()),
                () -> Assertions.assertNull(canvas.takePixelUpdate(false)),
                () -> assertIllegalArgument(
                        "Visible region at (0, 0) of size 0x5 must have a positive size and lie inside the 20x10 "
                                + "image", () -> canvas.setVisibleRegion(0, 0, 0, 5)),
                () -> assertIllegalArgument(
                        "Visible region at (0, 0) of size 5x-1 must have a positive size and lie inside the 20x10 "
                                + "image", () -> canvas.setVisibleRegion(0, 0, 5, -1)),
                () -> assertIllegalArgument(
                        "Visible region at (-1, 0) of size 5x5 must have a positive size and lie inside the 20x10 "
                                + "image", () -> canvas.setVisibleRegion(-1, 0, 5, 5)),
                () -> assertIllegalArgument(
                        "Visible region at (1, 0) of size 20x5 must have a positive size and lie inside the 20x10 "
                                + "image", () -> canvas.setVisibleRegion(1, 0, 20, 5)),
                () -> assertIllegalArgument(
                        "Visible region at (0, 1) of size 5x2147483647 must have a positive size and lie inside "
                                + "the 20x10 image", () -> canvas.setVisibleRegion(0, 1, 5, Integer.MAX_VALUE)),
                () -> Assertions.assertEquals(new Canvas.Region(2, 3, 18, 7), canvas.getVisibleRegion()));
    }

    @Test
    void shouldPassTriggerArgumentsOnlyWhileEnabled() {
        Canvas.ClickAction click = Mockito.mock(Canvas.ClickAction.class);
        Canvas.DragAction drag = Mockito.mock(Canvas.DragAction.class);
        Canvas.WheelAction wheel = Mockito.mock(Canvas.WheelAction.class);
        Canvas enabled = Canvas.builder()
                .size(10, 10)
                .clickAction(click)
                .dragAction(drag)
                .wheelAction(wheel)
                .build();
        Canvas disabled = Canvas.builder()
                .size(10, 10)
                .enabled(() -> false)
                .clickAction(click)
                .dragAction(drag)
                .wheelAction(wheel)
                .build();

        enabled.click(1, 2, MouseButton.MIDDLE);
        enabled.drag(-3, 40, -1, 2, MouseButton.RIGHT);
        enabled.wheel(5, 6, -0.5f);
        disabled.click(1, 2, MouseButton.LEFT);
        disabled.drag(1, 2, 1, 1, MouseButton.LEFT);
        disabled.wheel(1, 2, 1f);

        Mockito.verify(click).click(1, 2, MouseButton.MIDDLE);
        Mockito.verify(drag).drag(-3, 40, -1, 2, MouseButton.RIGHT);
        Mockito.verify(wheel).wheel(5, 6, -0.5f);
        Mockito.verifyNoMoreInteractions(click, drag, wheel);
        Assertions.assertAll(
                () -> Assertions.assertFalse(disabled.isEnabled()),
                () -> assertNullPointer("button", () -> enabled.click(0, 0, null)),
                () -> assertNullPointer("button", () -> enabled.drag(0, 0, 1, 1, null)),
                () -> assertNullPointer("button", () -> disabled.click(0, 0, null)));
    }

    @Test
    void shouldReportEveryCrossThreadEditInSomeUpdate() throws InterruptedException {
        int size = 64;
        Canvas canvas = Canvas.builder()
                .size(size, size)
                .build();
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                Assertions.assertTrue(start.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
                for (int y = 0; y < size; y++) {
                    if (y % 2 == 0) {
                        for (int x = 0; x < size; x++) {
                            canvas.setPixel(x, y, expectedPixel(x, y, size));
                        }
                    } else {
                        int[] row = new int[size];
                        for (int x = 0; x < size; x++) {
                            row[x] = expectedPixel(x, y, size);
                        }
                        canvas.setPixels(0, y, size, 1, row, 0, size);
                    }
                }
            } catch (Throwable failure) {
                workerFailure.set(failure);
            }
        });
        boolean[] seen = new boolean[size * size];

        worker.start();
        start.countDown();
        while (worker.isAlive()) {
            markSeen(canvas.takePixelUpdate(false), seen, size);
        }
        Assertions.assertTrue(worker.join(TIMEOUT), "worker did not terminate within " + TIMEOUT);
        markSeen(canvas.takePixelUpdate(false), seen, size);

        Assertions.assertNull(workerFailure.get());
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Assertions.assertEquals(expectedPixel(x, y, size), canvas.getPixel(x, y));
                Assertions.assertTrue(seen[y * size + x], "pixel (" + x + ", " + y + ") missing from every update");
            }
        }
    }

    private static int expectedPixel(int x, int y, int size) {
        return 0xFF000000 | (y * size + x + 1);
    }

    private static void markSeen(Canvas.PixelUpdate update, boolean[] seen, int size) {
        if (update == null) {
            return;
        }
        for (int j = 0; j < update.height(); j++) {
            for (int i = 0; i < update.width(); i++) {
                int x = update.x() + i;
                int y = update.y() + j;
                if (update.argb()[j * update.width() + i] == expectedPixel(x, y, size)) {
                    seen[y * size + x] = true;
                }
            }
        }
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }

    private static void assertIllegalArgument(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(IllegalArgumentException.class, executable).getMessage());
    }

    @FunctionalInterface
    private interface ArrayAccess {
        void apply(Canvas canvas, int x, int y, int width, int height, int[] argb, int offset, int scanSize);
    }
}
