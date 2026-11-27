package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A component that shows an editable pixel image and reports mouse clicks, drags and wheel motion in image-pixel
 * coordinates.
 *
 * <p>A canvas has two sizes, both fixed when it is built. Its display size ({@link Builder#size(int, int)}) is the
 * area it takes on screen, in logical window pixels. Its resolution ({@link Builder#resolution(int, int)}, the
 * display size by default) is the size of its image, in image pixels. Every pixel starts fully transparent
 * ({@code 0x00000000}); an optional {@linkplain #getBackgroundColor() background color} fills the whole display area
 * behind the image and shows through transparent pixels.
 *
 * <p>The canvas shows its {@linkplain #getVisibleRegion() visible region}, a rectangle of the image that defaults to
 * the whole image, stretched over the display size without keeping its aspect ratio. Changing the region pans and
 * zooms the view without changing any pixel. Scaled pixels are drawn as sharp squares by most backends and smoothed
 * by some.
 *
 * <p>Pixels are packed as {@code 0xAARRGGBB}, as in {@link Color#getRGBA()}. A pixel set to a {@link Color} looks the
 * same as that color drawn by other components, for example as a panel background. The same value stored in an image
 * file and shown with an {@link Image} may look different.
 *
 * <p>Place a canvas inside a panel; using it directly as a top-level component of a screen is not supported.
 *
 * <p>The pixel, visible-region and pixel-update methods may be called from any thread; each call is atomic. Several
 * calls made between two frames may be shown partly applied, so make a change that must appear at once with one
 * {@link #setPixels(int, int, int, int, int[], int, int) setPixels} call. The pixels live only in this instance, so
 * keep a canvas in a field of its screen instead of building it in {@code layout()}: a canvas built there starts
 * empty after every relayout. Draw a canvas on one display only. Keep the resolution at or below 4096 &times; 4096
 * pixels; larger images may fail to display on some graphics hardware.
 *
 * <p>While the canvas is {@linkplain #isEnabled() enabled}, pressing a mouse button over it runs its click action,
 * moving the pointer with that button held runs its drag action, and turning the mouse wheel over it runs its wheel
 * action instead of scrolling the panel that contains it. Each action receives the image pixel under the pointer,
 * mapped through the visible region. The first button pressed owns the gesture until it is released: other buttons
 * pressed meanwhile are ignored, and drags are reported even while the pointer is outside the canvas, with
 * coordinates outside the image. During such a gesture other components under the pointer may show their hovered
 * look and run their hover actions, but they are not clicked. The canvas never takes keyboard focus and is not
 * {@link Focusable}, and a mouse gesture on it does not capture the keyboard, so keys pressed meanwhile still reach
 * the application.
 *
 * <p>Create a canvas with {@link #builder()}.
 */
public class Canvas implements Component {
    private static final int MAX_PIXELS = Integer.MAX_VALUE / 4;

    private final int width;
    private final int height;
    private final int resolutionWidth;
    private final int resolutionHeight;
    private final Supplier<Color> backgroundColor;
    private final Supplier<Boolean> enabled;
    private final ClickAction clickAction;
    private final DragAction dragAction;
    private final WheelAction wheelAction;
    private final Object lock = new Object();
    private final int[] pixels;
    private Region visibleRegion;
    private boolean dirty;
    private int dirtyMinX;
    private int dirtyMinY;
    private int dirtyMaxX;
    private int dirtyMaxY;

    private Canvas(int width, int height, int resolutionWidth, int resolutionHeight, Supplier<Color> backgroundColor,
                   Supplier<Boolean> enabled, ClickAction clickAction, DragAction dragAction,
                   WheelAction wheelAction) {
        this.width = width;
        this.height = height;
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
        this.backgroundColor = backgroundColor;
        this.enabled = enabled;
        this.clickAction = clickAction;
        this.dragAction = dragAction;
        this.wheelAction = wheelAction;
        this.pixels = new int[resolutionWidth * resolutionHeight];
        this.visibleRegion = new Region(0, 0, resolutionWidth, resolutionHeight);
    }

    /**
     * Returns the display width of the canvas.
     *
     * @return the width in logical window pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the display height of the canvas.
     *
     * @return the height in logical window pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the width of the image.
     *
     * @return the image width in image pixels
     */
    public int getResolutionWidth() {
        return resolutionWidth;
    }

    /**
     * Returns the height of the image.
     *
     * @return the image height in image pixels
     */
    public int getResolutionHeight() {
        return resolutionHeight;
    }

    /**
     * Returns the background color, evaluated on each call. It fills the whole display area behind the image.
     *
     * @return the background color, or {@code null} for no background
     */
    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    /**
     * Returns whether the canvas currently reports pointer events, evaluated on each call.
     *
     * @return {@code true} when the canvas is enabled
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Returns the color of one pixel. May be called from any thread.
     *
     * @param x the column, from 0 to the resolution width minus 1
     * @param y the row, from 0 to the resolution height minus 1
     * @return the pixel packed as {@code 0xAARRGGBB}
     * @throws IllegalArgumentException if the pixel lies outside the image
     */
    public int getPixel(int x, int y) {
        checkPixel(x, y);
        synchronized (lock) {
            return pixels[y * resolutionWidth + x];
        }
    }

    /**
     * Sets the color of one pixel. May be called from any thread; the change is shown the next time the canvas is
     * drawn.
     *
     * @param x    the column, from 0 to the resolution width minus 1
     * @param y    the row, from 0 to the resolution height minus 1
     * @param argb the color packed as {@code 0xAARRGGBB}
     * @throws IllegalArgumentException if the pixel lies outside the image
     */
    public void setPixel(int x, int y, int argb) {
        checkPixel(x, y);
        synchronized (lock) {
            pixels[y * resolutionWidth + x] = argb;
            markDirty(x, y, 1, 1);
        }
    }

    /**
     * Sets the color of one pixel to a {@link Color}. May be called from any thread; the change is shown the next time
     * the canvas is drawn. The color is checked before the coordinates.
     *
     * @param x     the column, from 0 to the resolution width minus 1
     * @param y     the row, from 0 to the resolution height minus 1
     * @param color the color; must not be null
     * @throws NullPointerException     if {@code color} is {@code null}
     * @throws IllegalArgumentException if the pixel lies outside the image
     */
    public void setPixel(int x, int y, Color color) {
        Objects.requireNonNull(color, "color");
        setPixel(x, y, color.getRGBA());
    }

    /**
     * Sets the colors of a rectangle of pixels from an array. Pixel {@code (x + i, y + j)} takes
     * {@code argb[offset + j * scanSize + i]}, each packed as {@code 0xAARRGGBB}. The array is copied, not kept. May be
     * called from any thread; the change is atomic and is shown the next time the canvas is drawn. A rectangle of
     * width or height 0 is validated and then changes nothing.
     *
     * <p>The arguments are checked in this order, all before any pixel changes: {@code argb} is not {@code null}; the
     * width and height are not negative; the rectangle lies inside the image; {@code offset} is not negative;
     * {@code scanSize} is at least {@code width}; for a non-empty rectangle, the array holds the last addressed
     * element.
     *
     * @param x        the column of the rectangle's left edge
     * @param y        the row of the rectangle's top edge
     * @param width    the rectangle width in image pixels
     * @param height   the rectangle height in image pixels
     * @param argb     the colors; must not be null
     * @param offset   the index of the color of pixel {@code (x, y)}
     * @param scanSize the distance in the array between the starts of consecutive rows
     * @throws NullPointerException     if {@code argb} is {@code null}
     * @throws IllegalArgumentException if the width or height is negative, the rectangle lies outside the image,
     *                                  {@code offset} is negative, {@code scanSize} is less than {@code width} or the
     *                                  array is too short
     */
    public void setPixels(int x, int y, int width, int height, int[] argb, int offset, int scanSize) {
        checkArrayAccess(x, y, width, height, argb, offset, scanSize);
        if (width == 0 || height == 0) {
            return;
        }
        synchronized (lock) {
            for (int j = 0; j < height; j++) {
                System.arraycopy(argb, offset + j * scanSize, pixels, (y + j) * resolutionWidth + x, width);
            }
            markDirty(x, y, width, height);
        }
    }

    /**
     * Copies the colors of a rectangle of pixels into an array. Pixel {@code (x + i, y + j)} is written to
     * {@code argb[offset + j * scanSize + i]}, packed as {@code 0xAARRGGBB}; no other element of the array changes.
     * May be called from any thread; the copy is atomic.
     *
     * <p>The arguments are checked in the same order as for
     * {@link #setPixels(int, int, int, int, int[], int, int) setPixels}, all before the array is written.
     *
     * @param x        the column of the rectangle's left edge
     * @param y        the row of the rectangle's top edge
     * @param width    the rectangle width in image pixels
     * @param height   the rectangle height in image pixels
     * @param argb     receives the colors; must not be null
     * @param offset   the index that receives the color of pixel {@code (x, y)}
     * @param scanSize the distance in the array between the starts of consecutive rows
     * @throws NullPointerException     if {@code argb} is {@code null}
     * @throws IllegalArgumentException if the width or height is negative, the rectangle lies outside the image,
     *                                  {@code offset} is negative, {@code scanSize} is less than {@code width} or the
     *                                  array is too short
     */
    public void getPixels(int x, int y, int width, int height, int[] argb, int offset, int scanSize) {
        checkArrayAccess(x, y, width, height, argb, offset, scanSize);
        if (width == 0 || height == 0) {
            return;
        }
        synchronized (lock) {
            for (int j = 0; j < height; j++) {
                System.arraycopy(pixels, (y + j) * resolutionWidth + x, argb, offset + j * scanSize, width);
            }
        }
    }

    /**
     * Returns the visible region: the rectangle of the image that is stretched over the display size. May be called
     * from any thread; the region is read atomically.
     *
     * @return the visible region in image pixels; never {@code null}
     */
    public Region getVisibleRegion() {
        synchronized (lock) {
            return visibleRegion;
        }
    }

    /**
     * Sets the visible region: the rectangle of the image that is stretched over the display size, without keeping
     * its aspect ratio. May be called from any thread; the change is atomic and is shown the next time the canvas is
     * drawn. No pixel changes.
     *
     * @param x      the column of the region's left edge
     * @param y      the row of the region's top edge
     * @param width  the region width in image pixels
     * @param height the region height in image pixels
     * @throws IllegalArgumentException if the width or height is not positive or the region does not lie inside the
     *                                  image; the visible region is then unchanged
     */
    public void setVisibleRegion(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || x < 0 || y < 0 || (long) x + width > resolutionWidth
                || (long) y + height > resolutionHeight) {
            throw new IllegalArgumentException("Visible region at (" + x + ", " + y + ") of size " + width + "x"
                    + height + " must have a positive size and lie inside the " + resolutionWidth + "x"
                    + resolutionHeight + " image");
        }
        Region region = new Region(x, y, width, height);
        synchronized (lock) {
            visibleRegion = region;
        }
    }

    /**
     * Runs the click action if the canvas is currently {@link #isEnabled() enabled}; does nothing otherwise. Backends
     * call this when a mouse button is pressed over the canvas and no other button already owns a gesture on it.
     * Applications may call it on the host's UI thread to trigger the click action as if the button had been pressed.
     *
     * @param x      the column of the image pixel under the pointer
     * @param y      the row of the image pixel under the pointer
     * @param button the pressed button; must not be null
     * @throws NullPointerException if {@code button} is {@code null}
     */
    public void click(int x, int y, MouseButton button) {
        Objects.requireNonNull(button, "button");
        if (isEnabled()) {
            clickAction.click(x, y, button);
        }
    }

    /**
     * Runs the drag action if the canvas is currently {@link #isEnabled() enabled}; does nothing otherwise. Backends
     * call this, at most once per frame, while the button that started a gesture on the canvas is held and the
     * pointer motion since the last drag adds up to at least one image pixel on either axis. Applications may call it
     * on the host's UI thread to trigger the drag action as if the pointer had moved.
     *
     * <p>The pointer may be outside the canvas, so the coordinates may lie outside the image. The deltas are the
     * motion in whole image pixels, truncated toward zero; the remainder carries over to the next drag, and a motion
     * of less than one image pixel just before the button is released is not reported. Because the coordinates are
     * rounded down and the deltas are truncated, the sum of {@code dx} (or {@code dy}) over a gesture need not equal
     * the change in {@code x} (or {@code y}).
     *
     * @param x      the column of the image pixel under the pointer
     * @param y      the row of the image pixel under the pointer
     * @param dx     the horizontal motion in image pixels, positive to the right
     * @param dy     the vertical motion in image pixels, positive downward
     * @param button the button that owns the gesture; must not be null
     * @throws NullPointerException if {@code button} is {@code null}
     */
    public void drag(int x, int y, int dx, int dy, MouseButton button) {
        Objects.requireNonNull(button, "button");
        if (isEnabled()) {
            dragAction.drag(x, y, dx, dy, button);
        }
    }

    /**
     * Runs the wheel action if the canvas is currently {@link #isEnabled() enabled}; does nothing otherwise. Backends
     * call this, at most once per frame, when the vertical mouse wheel moves while the pointer is over the canvas,
     * whatever modifier keys are held. Applications may call it on the host's UI thread to trigger the wheel action as
     * if the wheel had moved.
     *
     * @param x     the column of the image pixel under the pointer
     * @param y     the row of the image pixel under the pointer
     * @param delta the wheel motion: positive when the wheel is turned away from the user, about 1 per notch, and
     *              fractional for touchpads
     */
    public void wheel(int x, int y, float delta) {
        if (isEnabled()) {
            wheelAction.wheel(x, y, delta);
        }
    }

    /**
     * Takes the pixels changed since the last take, or the whole image, and clears the pending changes. May be called
     * from any thread; the take is atomic.
     *
     * <p>Backends call this while drawing the canvas, to update the image they show. Applications should not call it:
     * the changes it takes are not taken again, so an application call, or a second backend drawing the same canvas,
     * makes a backend miss them.
     *
     * @param wholeImage {@code true} to take the whole image, {@code false} to take only the bounding rectangle of the
     *                   pixels changed since the last take
     * @return the taken pixels; {@code null} when {@code wholeImage} is {@code false} and no pixel changed
     */
    public PixelUpdate takePixelUpdate(boolean wholeImage) {
        synchronized (lock) {
            PixelUpdate update;
            if (wholeImage) {
                update = copyRegion(0, 0, resolutionWidth, resolutionHeight);
            } else if (dirty) {
                update = copyRegion(dirtyMinX, dirtyMinY, dirtyMaxX - dirtyMinX, dirtyMaxY - dirtyMinY);
            } else {
                update = null;
            }
            dirty = false;
            return update;
        }
    }

    private void checkPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= resolutionWidth || y >= resolutionHeight) {
            throw new IllegalArgumentException("Pixel (" + x + ", " + y + ") is outside the " + resolutionWidth + "x"
                    + resolutionHeight + " image");
        }
    }

    private void checkArrayAccess(int x, int y, int width, int height, int[] argb, int offset, int scanSize) {
        Objects.requireNonNull(argb, "argb");
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Region size must not be negative: " + width + "x" + height);
        }
        if (x < 0 || y < 0 || (long) x + width > resolutionWidth || (long) y + height > resolutionHeight) {
            throw new IllegalArgumentException("Region at (" + x + ", " + y + ") of size " + width + "x" + height
                    + " is outside the " + resolutionWidth + "x" + resolutionHeight + " image");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative: " + offset);
        }
        if (scanSize < width) {
            throw new IllegalArgumentException("scanSize must not be less than the region width: " + scanSize + " < "
                    + width);
        }
        // Also bounds every array index the copy loops of a non-empty region compute, so they cannot overflow.
        if (width > 0 && height > 0 && offset + (long) (height - 1) * scanSize + width > argb.length) {
            throw new IllegalArgumentException("Array of length " + argb.length + " is too short for offset " + offset
                    + ", scan size " + scanSize + " and region " + width + "x" + height);
        }
    }

    private void markDirty(int x, int y, int width, int height) {
        if (dirty) {
            dirtyMinX = Math.min(dirtyMinX, x);
            dirtyMinY = Math.min(dirtyMinY, y);
            dirtyMaxX = Math.max(dirtyMaxX, x + width);
            dirtyMaxY = Math.max(dirtyMaxY, y + height);
        } else {
            dirtyMinX = x;
            dirtyMinY = y;
            dirtyMaxX = x + width;
            dirtyMaxY = y + height;
            dirty = true;
        }
    }

    private PixelUpdate copyRegion(int x, int y, int width, int height) {
        int[] argb = new int[width * height];
        for (int j = 0; j < height; j++) {
            System.arraycopy(pixels, (y + j) * resolutionWidth + x, argb, j * width, width);
        }
        return new PixelUpdate(x, y, width, height, argb);
    }

    /**
     * Creates a builder for a canvas.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Action run when a mouse button is pressed over an enabled canvas. Set it with
     * {@link Builder#clickAction(ClickAction)}.
     */
    @FunctionalInterface
    public interface ClickAction {
        /**
         * Handles a mouse button pressed over the canvas.
         *
         * @param x      the column of the image pixel under the pointer
         * @param y      the row of the image pixel under the pointer
         * @param button the pressed button; never {@code null}
         */
        void click(int x, int y, MouseButton button);
    }

    /**
     * Action run when the pointer moves while a mouse button that started a gesture on an enabled canvas is held. Set
     * it with {@link Builder#dragAction(DragAction)}; the arguments are described in
     * {@link Canvas#drag(int, int, int, int, MouseButton)}.
     */
    @FunctionalInterface
    public interface DragAction {
        /**
         * Handles pointer motion during a gesture on the canvas.
         *
         * @param x      the column of the image pixel under the pointer; may lie outside the image
         * @param y      the row of the image pixel under the pointer; may lie outside the image
         * @param dx     the horizontal motion in whole image pixels, positive to the right
         * @param dy     the vertical motion in whole image pixels, positive downward
         * @param button the button that owns the gesture; never {@code null}
         */
        void drag(int x, int y, int dx, int dy, MouseButton button);
    }

    /**
     * Action run when the mouse wheel moves while the pointer is over an enabled canvas. Set it with
     * {@link Builder#wheelAction(WheelAction)}.
     */
    @FunctionalInterface
    public interface WheelAction {
        /**
         * Handles mouse wheel motion over the canvas.
         *
         * @param x     the column of the image pixel under the pointer
         * @param y     the row of the image pixel under the pointer
         * @param delta the wheel motion: positive when the wheel is turned away from the user, about 1 per notch, and
         *              fractional for touchpads
         */
        void wheel(int x, int y, float delta);
    }

    /**
     * A rectangle of a canvas image, in image pixels. The record performs no validation.
     *
     * @param x      the column of the left edge
     * @param y      the row of the top edge
     * @param width  the width
     * @param height the height
     */
    public record Region(int x, int y, int width, int height) {
    }

    /**
     * Pixels taken from a canvas with {@link Canvas#takePixelUpdate(boolean)}: a rectangle of the image and the colors
     * of its pixels.
     * <p>
     * The array is a fresh copy owned by the caller: the canvas never uses it again, and the caller may modify it. The
     * record is therefore not deeply immutable, and {@link #equals(Object)} compares the arrays by identity, not by
     * content.
     *
     * @param x      the column of the rectangle's left edge
     * @param y      the row of the rectangle's top edge
     * @param width  the rectangle width in image pixels
     * @param height the rectangle height in image pixels
     * @param argb   the colors packed as {@code 0xAARRGGBB}, row by row from the top, {@code width * height} elements
     *               with no gaps between rows
     */
    public record PixelUpdate(int x, int y, int width, int height, int[] argb) {
    }

    /**
     * Builder for {@link Canvas}, obtained from {@link Canvas#builder()}.
     * <p>
     * {@link #size(int, int) size(...)} is required and is checked by {@link #build()}. Unless set, the resolution
     * equals the display size, there is no background, the canvas is enabled, and clicks, drags and wheel motion do
     * nothing.
     */
    public static class Builder {
        private Integer width;
        private Integer height;
        private Integer resolutionWidth;
        private Integer resolutionHeight;
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Boolean> enabled = () -> true;
        private ClickAction clickAction = (_, _, _) -> {};
        private DragAction dragAction = (_, _, _, _, _) -> {};
        private WheelAction wheelAction = (_, _, _) -> {};

        private Builder() {}

        /**
         * Sets the display size of the canvas. Required.
         *
         * @param width  the width in logical window pixels; must be positive
         * @param height the height in logical window pixels; must be positive
         * @return this builder
         * @throws IllegalArgumentException if {@code width} or {@code height} is not positive; the width is checked
         *                                  first
         */
        public Builder size(int width, int height) {
            requirePositive(width, "width");
            requirePositive(height, "height");
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * Sets the size of the image. Defaults to the display size. The image is stretched over the display size, so a
         * resolution smaller than the display size shows each image pixel as a block of window pixels.
         *
         * @param width  the image width in image pixels; must be positive
         * @param height the image height in image pixels; must be positive
         * @return this builder
         * @throws IllegalArgumentException if {@code width} or {@code height} is not positive; the width is checked
         *                                  first
         */
        public Builder resolution(int width, int height) {
            requirePositive(width, "resolution width");
            requirePositive(height, "resolution height");
            this.resolutionWidth = width;
            this.resolutionHeight = height;
            return this;
        }

        /**
         * Sets a fixed background color, drawn over the whole display area behind the image.
         *
         * @param backgroundColor the background color, or {@code null} for no background
         * @return this builder
         */
        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        /**
         * Sets a dynamic background color, evaluated on each call to {@link Canvas#getBackgroundColor()}.
         *
         * @param backgroundColor supplies the background color, or {@code null} for no background; must not be null
         *                        itself
         * @return this builder
         * @throws NullPointerException if {@code backgroundColor} is {@code null}
         */
        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
            return this;
        }

        /**
         * Sets whether the canvas reports pointer events. Defaults to {@code true}. A disabled canvas is still drawn,
         * but runs no actions, ends a running gesture and leaves the mouse wheel to the panel that contains it.
         *
         * @param enabled {@code true} to enable the canvas
         * @return this builder
         */
        public Builder enabled(boolean enabled) {
            return enabled(() -> enabled);
        }

        /**
         * Sets whether the canvas reports pointer events from a supplier, evaluated each time the canvas is drawn or
         * one of its actions is triggered. A disabled canvas is still drawn, but runs no actions, ends a running
         * gesture and leaves the mouse wheel to the panel that contains it.
         *
         * @param enabled supplies whether the canvas is enabled; must not be null and must not return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code enabled} is {@code null}
         */
        public Builder enabled(Supplier<Boolean> enabled) {
            this.enabled = Objects.requireNonNull(enabled, "enabled");
            return this;
        }

        /**
         * Sets the action run when a mouse button is pressed over the enabled canvas, unless another button already
         * owns a gesture on it. It runs on the host's UI thread. An exception thrown by the action propagates to the
         * caller of the host's render call. By default pressing a button does nothing.
         *
         * @param clickAction the click action; must not be null
         * @return this builder
         * @throws NullPointerException if {@code clickAction} is {@code null}
         */
        public Builder clickAction(ClickAction clickAction) {
            this.clickAction = Objects.requireNonNull(clickAction, "clickAction");
            return this;
        }

        /**
         * Sets the action run, at most once per frame, while the button that started a gesture on the enabled canvas
         * is held and the pointer has moved by at least one image pixel, also while the pointer is outside the canvas
         * (see {@link Canvas#drag(int, int, int, int, MouseButton)}). It runs on the host's UI thread. An exception
         * thrown by the action propagates to the caller of the host's render call. By default dragging does nothing.
         *
         * @param dragAction the drag action; must not be null
         * @return this builder
         * @throws NullPointerException if {@code dragAction} is {@code null}
         */
        public Builder dragAction(DragAction dragAction) {
            this.dragAction = Objects.requireNonNull(dragAction, "dragAction");
            return this;
        }

        /**
         * Sets the action run, at most once per frame, when the vertical mouse wheel moves while the pointer is over
         * the enabled canvas, whatever modifier keys are held. It runs on the host's UI thread. An exception thrown by
         * the action propagates to the caller of the host's render call. By default the wheel does nothing.
         *
         * @param wheelAction the wheel action; must not be null
         * @return this builder
         * @throws NullPointerException if {@code wheelAction} is {@code null}
         */
        public Builder wheelAction(WheelAction wheelAction) {
            this.wheelAction = Objects.requireNonNull(wheelAction, "wheelAction");
            return this;
        }

        /**
         * Builds the canvas. All its pixels start fully transparent and its visible region is the whole image.
         *
         * @return a new {@link Canvas}
         * @throws NullPointerException     if {@link #size(int, int) size(...)} was never called
         * @throws IllegalArgumentException if the image (the resolution, or the display size when no resolution was
         *                                  set) has more than 536,870,911 pixels
         */
        public Canvas build() {
            Objects.requireNonNull(width, "size");
            int imageWidth = resolutionWidth == null ? width : resolutionWidth;
            int imageHeight = resolutionHeight == null ? height : resolutionHeight;
            if ((long) imageWidth * imageHeight > MAX_PIXELS) {
                throw new IllegalArgumentException("Canvas resolution " + imageWidth + "x" + imageHeight
                        + " has more than " + MAX_PIXELS + " pixels");
            }
            return new Canvas(width, height, imageWidth, imageHeight, backgroundColor, enabled, clickAction,
                    dragAction, wheelAction);
        }

        private static void requirePositive(int value, String label) {
            if (value <= 0) {
                throw new IllegalArgumentException(label + " must be positive: " + value);
            }
        }
    }
}
