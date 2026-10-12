package com.ivan1pl.lorisui.api.component;

/**
 * Describes how an image is mapped onto the area it is drawn in, for {@link Image} components and image
 * {@link Background}s.
 * <p>
 * Obtain an instance from one of the static factories; the nested classes are the concrete variants and expose the
 * values given to the factory. Coordinates and sizes are in source pixels of the image, with {@code (0, 0)} at its
 * top-left corner. Arguments are stored as given, without validation.
 * <p>
 * The {@code *Resize} variants, {@link #subImageResize subImageResize} and {@link #spriteResize spriteResize}, draw a
 * 9-slice image: a region of the image is cut into three rows of three cells, so that it can be resized without
 * distorting its borders. {@code x1}, {@code x2}, {@code x3} and {@code y1} are the column widths and the height of
 * the top row, {@code x4}, {@code x5}, {@code x6} and {@code y2} those of the middle row, and {@code x7}, {@code x8},
 * {@code x9} and {@code y3} those of the bottom row, all in source pixels. The rows are stacked from the region's top
 * edge and each row's cells start at the region's left edge. When the image is drawn into an area of {@code width}
 * by {@code height} pixels:
 * <ul>
 *   <li>the four corners keep their size;</li>
 *   <li>the top and bottom middle cells stretch horizontally, to {@code width - x1 - x3} and
 *       {@code width - x7 - x9};</li>
 *   <li>the left and right middle cells stretch vertically, to {@code height - y1 - y3};</li>
 *   <li>the center stretches both ways, to {@code width - x4 - x6} by {@code height - y1 - y3}.</li>
 * </ul>
 */
public sealed abstract class ImageMode {
    private ImageMode() {}

    /**
     * Creates a mode that tiles the image at its native pixel size, starting at the top-left corner of the area.
     * A backend may draw every tile separately, so the cost of drawing grows with the number of tiles; prefer images
     * that are not much smaller than the area they cover.
     *
     * @return a {@link Repeat} mode
     */
    public static ImageMode repeat() {
        return new Repeat();
    }

    /**
     * Creates a mode that stretches the whole image to fill the area.
     *
     * @return a {@link Fit} mode
     */
    public static ImageMode fit() {
        return new Fit();
    }

    /**
     * Creates a mode that stretches a rectangular part of the image to fill the area. The bounds are inclusive, so
     * {@code subImage(0, 0, 15, 15)} selects a 16 by 16 pixel region.
     *
     * @param fromX x coordinate of the region's left column, inclusive
     * @param fromY y coordinate of the region's top row, inclusive
     * @param toX   x coordinate of the region's right column, inclusive
     * @param toY   y coordinate of the region's bottom row, inclusive
     * @return a {@link SubImage} mode
     */
    public static ImageMode subImage(int fromX, int fromY, int toX, int toY) {
        return new SubImage(fromX, fromY, toX, toY);
    }

    /**
     * Creates a mode that stretches one cell of a sprite sheet to fill the area. The image is treated as a grid of
     * {@code spriteWidth} by {@code spriteHeight} cells with {@code imageWidth / spriteWidth} columns (integer
     * division of the whole image's width). The index is 1-based and counted row-major: index 1 is the top-left
     * cell, and the count continues left to right, then top to bottom.
     *
     * @param spriteWidth  width of one cell in pixels
     * @param spriteHeight height of one cell in pixels
     * @param spriteIndex  1-based, row-major index of the cell
     * @return a {@link Sprite} mode
     */
    public static ImageMode sprite(int spriteWidth, int spriteHeight, int spriteIndex) {
        return new Sprite(spriteWidth, spriteHeight, spriteIndex);
    }

    /**
     * Creates a mode that draws one cell of a sprite sheet as a 9-slice image. The cell is selected as in
     * {@link #sprite(int, int, int)}, and the 9-slice region starts at the cell's top-left corner. See the class
     * description for the 9-slice layout and how the cells stretch.
     *
     * @param spriteWidth  width of one cell in pixels
     * @param spriteHeight height of one cell in pixels
     * @param spriteIndex  1-based, row-major index of the cell
     * @param x1           width of the top-left cell
     * @param x2           width of the top middle cell
     * @param x3           width of the top-right cell
     * @param y1           height of the top row
     * @param x4           width of the middle-left cell
     * @param x5           width of the center cell
     * @param x6           width of the middle-right cell
     * @param y2           height of the middle row
     * @param x7           width of the bottom-left cell
     * @param x8           width of the bottom middle cell
     * @param x9           width of the bottom-right cell
     * @param y3           height of the bottom row
     * @return a {@link SpriteResize} mode
     */
    public static ImageMode spriteResize(int spriteWidth, int spriteHeight, int spriteIndex,
                                         int x1, int x2, int x3, int y1,
                                         int x4, int x5, int x6, int y2,
                                         int x7, int x8, int x9, int y3) {
        return new SpriteResize(spriteWidth, spriteHeight, spriteIndex, x1, x2, x3, y1, x4, x5, x6, y2, x7, x8, x9, y3);
    }

    /**
     * Creates a mode that draws a part of the image as a 9-slice image. The 9-slice region starts at
     * {@code (fromX, fromY)}. See the class description for the 9-slice layout and how the cells stretch.
     *
     * @param fromX x coordinate of the region's left edge
     * @param fromY y coordinate of the region's top edge
     * @param x1    width of the top-left cell
     * @param x2    width of the top middle cell
     * @param x3    width of the top-right cell
     * @param y1    height of the top row
     * @param x4    width of the middle-left cell
     * @param x5    width of the center cell
     * @param x6    width of the middle-right cell
     * @param y2    height of the middle row
     * @param x7    width of the bottom-left cell
     * @param x8    width of the bottom middle cell
     * @param x9    width of the bottom-right cell
     * @param y3    height of the bottom row
     * @return a {@link SubImageResize} mode
     */
    public static ImageMode subImageResize(int fromX, int fromY,
                                           int x1, int x2, int x3, int y1,
                                           int x4, int x5, int x6, int y2,
                                           int x7, int x8, int x9, int y3) {
        return new SubImageResize(fromX, fromY, x1, x2, x3, y1, x4, x5, x6, y2, x7, x8, x9, y3);
    }

    /**
     * Tiles the image at its native pixel size. Created by {@link ImageMode#repeat()}. A backend may draw every tile
     * separately, so prefer images that are not much smaller than the area they cover.
     */
    public static final class Repeat extends ImageMode {
        private Repeat() {}
    }

    /**
     * Stretches the whole image to fill the area. Created by {@link ImageMode#fit()}.
     */
    public static final class Fit extends ImageMode {
        private Fit() {}
    }

    /**
     * Stretches a rectangular part of the image, given by inclusive bounds, to fill the area. Created by
     * {@link ImageMode#subImage(int, int, int, int)}.
     */
    public static final class SubImage extends ImageMode {
        private final int fromX;
        private final int fromY;
        private final int toX;
        private final int toY;

        private SubImage(int fromX, int fromY, int toX, int toY) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        /**
         * Returns the x coordinate of the region's left column.
         *
         * @return the left bound, inclusive
         */
        public int getFromX() {
            return fromX;
        }

        /**
         * Returns the y coordinate of the region's top row.
         *
         * @return the top bound, inclusive
         */
        public int getFromY() {
            return fromY;
        }

        /**
         * Returns the x coordinate of the region's right column.
         *
         * @return the right bound, inclusive
         */
        public int getToX() {
            return toX;
        }

        /**
         * Returns the y coordinate of the region's bottom row.
         *
         * @return the bottom bound, inclusive
         */
        public int getToY() {
            return toY;
        }
    }

    /**
     * Stretches one cell of a sprite sheet to fill the area. Created by {@link ImageMode#sprite(int, int, int)}.
     */
    public static final class Sprite extends ImageMode {
        private final int spriteWidth;
        private final int spriteHeight;
        private final int spriteIndex;

        private Sprite(int spriteWidth, int spriteHeight, int spriteIndex) {
            this.spriteWidth = spriteWidth;
            this.spriteHeight = spriteHeight;
            this.spriteIndex = spriteIndex;
        }

        /**
         * Returns the width of one sprite sheet cell.
         *
         * @return the cell width in pixels
         */
        public int getSpriteWidth() {
            return spriteWidth;
        }

        /**
         * Returns the height of one sprite sheet cell.
         *
         * @return the cell height in pixels
         */
        public int getSpriteHeight() {
            return spriteHeight;
        }

        /**
         * Returns the index of the selected cell.
         *
         * @return the 1-based, row-major cell index
         */
        public int getSpriteIndex() {
            return spriteIndex;
        }
    }

    /**
     * Draws one cell of a sprite sheet as a 9-slice image whose region starts at the cell's top-left corner. Created
     * by {@link ImageMode#spriteResize(int, int, int, int, int, int, int, int, int, int, int, int, int, int, int)}.
     * See {@link ImageMode} for the 9-slice layout.
     */
    public static final class SpriteResize extends ImageMode {
        private final int spriteWidth;
        private final int spriteHeight;
        private final int spriteIndex;
        private final int x1;
        private final int x2;
        private final int x3;
        private final int y1;
        private final int x4;
        private final int x5;
        private final int x6;
        private final int y2;
        private final int x7;
        private final int x8;
        private final int x9;
        private final int y3;

        private SpriteResize(int spriteWidth, int spriteHeight, int spriteIndex,
                             int x1, int x2, int x3, int y1,
                             int x4, int x5, int x6, int y2,
                             int x7, int x8, int x9, int y3) {
            this.spriteWidth = spriteWidth;
            this.spriteHeight = spriteHeight;
            this.spriteIndex = spriteIndex;
            this.x1 = x1;
            this.x2 = x2;
            this.x3 = x3;
            this.y1 = y1;
            this.x4 = x4;
            this.x5 = x5;
            this.x6 = x6;
            this.y2 = y2;
            this.x7 = x7;
            this.x8 = x8;
            this.x9 = x9;
            this.y3 = y3;
        }

        /**
         * Returns the width of one sprite sheet cell.
         *
         * @return the cell width in pixels
         */
        public int getSpriteWidth() {
            return spriteWidth;
        }

        /**
         * Returns the height of one sprite sheet cell.
         *
         * @return the cell height in pixels
         */
        public int getSpriteHeight() {
            return spriteHeight;
        }

        /**
         * Returns the index of the selected cell.
         *
         * @return the 1-based, row-major cell index
         */
        public int getSpriteIndex() {
            return spriteIndex;
        }

        /**
         * Returns the width of the top-left cell.
         *
         * @return the width in source pixels
         */
        public int getX1() {
            return x1;
        }

        /**
         * Returns the width of the top middle cell.
         *
         * @return the width in source pixels
         */
        public int getX2() {
            return x2;
        }

        /**
         * Returns the width of the top-right cell.
         *
         * @return the width in source pixels
         */
        public int getX3() {
            return x3;
        }

        /**
         * Returns the height of the top row.
         *
         * @return the height in source pixels
         */
        public int getY1() {
            return y1;
        }

        /**
         * Returns the width of the middle-left cell.
         *
         * @return the width in source pixels
         */
        public int getX4() {
            return x4;
        }

        /**
         * Returns the width of the center cell.
         *
         * @return the width in source pixels
         */
        public int getX5() {
            return x5;
        }

        /**
         * Returns the width of the middle-right cell.
         *
         * @return the width in source pixels
         */
        public int getX6() {
            return x6;
        }

        /**
         * Returns the height of the middle row.
         *
         * @return the height in source pixels
         */
        public int getY2() {
            return y2;
        }

        /**
         * Returns the width of the bottom-left cell.
         *
         * @return the width in source pixels
         */
        public int getX7() {
            return x7;
        }

        /**
         * Returns the width of the bottom middle cell.
         *
         * @return the width in source pixels
         */
        public int getX8() {
            return x8;
        }

        /**
         * Returns the width of the bottom-right cell.
         *
         * @return the width in source pixels
         */
        public int getX9() {
            return x9;
        }

        /**
         * Returns the height of the bottom row.
         *
         * @return the height in source pixels
         */
        public int getY3() {
            return y3;
        }
    }

    /**
     * Draws a part of the image as a 9-slice image whose region starts at {@code (fromX, fromY)}. Created by
     * {@link ImageMode#subImageResize(int, int, int, int, int, int, int, int, int, int, int, int, int, int)}. See
     * {@link ImageMode} for the 9-slice layout.
     */
    public static final class SubImageResize extends ImageMode {
        private final int fromX;
        private final int fromY;
        private final int x1;
        private final int x2;
        private final int x3;
        private final int y1;
        private final int x4;
        private final int x5;
        private final int x6;
        private final int y2;
        private final int x7;
        private final int x8;
        private final int x9;
        private final int y3;

        private SubImageResize(int fromX, int fromY,
                               int x1, int x2, int x3, int y1,
                               int x4, int x5, int x6, int y2,
                               int x7, int x8, int x9, int y3) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.x1 = x1;
            this.x2 = x2;
            this.x3 = x3;
            this.y1 = y1;
            this.x4 = x4;
            this.x5 = x5;
            this.x6 = x6;
            this.y2 = y2;
            this.x7 = x7;
            this.x8 = x8;
            this.x9 = x9;
            this.y3 = y3;
        }

        /**
         * Returns the x coordinate of the region's left edge.
         *
         * @return the left edge in source pixels
         */
        public int getFromX() {
            return fromX;
        }

        /**
         * Returns the y coordinate of the region's top edge.
         *
         * @return the top edge in source pixels
         */
        public int getFromY() {
            return fromY;
        }

        /**
         * Returns the width of the top-left cell.
         *
         * @return the width in source pixels
         */
        public int getX1() {
            return x1;
        }

        /**
         * Returns the width of the top middle cell.
         *
         * @return the width in source pixels
         */
        public int getX2() {
            return x2;
        }

        /**
         * Returns the width of the top-right cell.
         *
         * @return the width in source pixels
         */
        public int getX3() {
            return x3;
        }

        /**
         * Returns the height of the top row.
         *
         * @return the height in source pixels
         */
        public int getY1() {
            return y1;
        }

        /**
         * Returns the width of the middle-left cell.
         *
         * @return the width in source pixels
         */
        public int getX4() {
            return x4;
        }

        /**
         * Returns the width of the center cell.
         *
         * @return the width in source pixels
         */
        public int getX5() {
            return x5;
        }

        /**
         * Returns the width of the middle-right cell.
         *
         * @return the width in source pixels
         */
        public int getX6() {
            return x6;
        }

        /**
         * Returns the height of the middle row.
         *
         * @return the height in source pixels
         */
        public int getY2() {
            return y2;
        }

        /**
         * Returns the width of the bottom-left cell.
         *
         * @return the width in source pixels
         */
        public int getX7() {
            return x7;
        }

        /**
         * Returns the width of the bottom middle cell.
         *
         * @return the width in source pixels
         */
        public int getX8() {
            return x8;
        }

        /**
         * Returns the width of the bottom-right cell.
         *
         * @return the width in source pixels
         */
        public int getX9() {
            return x9;
        }

        /**
         * Returns the height of the bottom row.
         *
         * @return the height in source pixels
         */
        public int getY3() {
            return y3;
        }
    }
}
