package com.ivan1pl.lorisui.api.component;

public sealed abstract class ImageMode {
    private ImageMode() {}

    public static ImageMode repeat() {
        return new Repeat();
    }

    public static ImageMode fit() {
        return new Fit();
    }

    public static ImageMode subImage(int fromX, int fromY, int toX, int toY) {
        return new SubImage(fromX, fromY, toX, toY);
    }

    public static ImageMode sprite(int spriteWidth, int spriteHeight, int spriteIndex) {
        return new Sprite(spriteWidth, spriteHeight, spriteIndex);
    }

    public static ImageMode spriteResize(int spriteWidth, int spriteHeight, int spriteIndex,
                                         int x1, int x2, int x3, int y1,
                                         int x4, int x5, int x6, int y2,
                                         int x7, int x8, int x9, int y3) {
        return new SpriteResize(spriteWidth, spriteHeight, spriteIndex, x1, x2, x3, y1, x4, x5, x6, y2, x7, x8, x9, y3);
    }

    public static ImageMode subImageResize(int fromX, int fromY,
                                           int x1, int x2, int x3, int y1,
                                           int x4, int x5, int x6, int y2,
                                           int x7, int x8, int x9, int y3) {
        return new SubImageResize(fromX, fromY, x1, x2, x3, y1, x4, x5, x6, y2, x7, x8, x9, y3);
    }

    public static final class Repeat extends ImageMode {
        private Repeat() {}
    }

    public static final class Fit extends ImageMode {
        private Fit() {}
    }

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

        public int getFromX() {
            return fromX;
        }

        public int getFromY() {
            return fromY;
        }

        public int getToX() {
            return toX;
        }

        public int getToY() {
            return toY;
        }
    }

    public static final class Sprite extends ImageMode {
        private final int spriteWidth;
        private final int spriteHeight;
        private final int spriteIndex;

        private Sprite(int spriteWidth, int spriteHeight, int spriteIndex) {
            this.spriteWidth = spriteWidth;
            this.spriteHeight = spriteHeight;
            this.spriteIndex = spriteIndex;
        }

        public int getSpriteWidth() {
            return spriteWidth;
        }

        public int getSpriteHeight() {
            return spriteHeight;
        }

        public int getSpriteIndex() {
            return spriteIndex;
        }
    }

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

        public int getSpriteWidth() {
            return spriteWidth;
        }

        public int getSpriteHeight() {
            return spriteHeight;
        }

        public int getSpriteIndex() {
            return spriteIndex;
        }

        public int getX1() {
            return x1;
        }

        public int getX2() {
            return x2;
        }

        public int getX3() {
            return x3;
        }

        public int getY1() {
            return y1;
        }

        public int getX4() {
            return x4;
        }

        public int getX5() {
            return x5;
        }

        public int getX6() {
            return x6;
        }

        public int getY2() {
            return y2;
        }

        public int getX7() {
            return x7;
        }

        public int getX8() {
            return x8;
        }

        public int getX9() {
            return x9;
        }

        public int getY3() {
            return y3;
        }
    }

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

        public int getFromX() {
            return fromX;
        }

        public int getFromY() {
            return fromY;
        }

        public int getX1() {
            return x1;
        }

        public int getX2() {
            return x2;
        }

        public int getX3() {
            return x3;
        }

        public int getY1() {
            return y1;
        }

        public int getX4() {
            return x4;
        }

        public int getX5() {
            return x5;
        }

        public int getX6() {
            return x6;
        }

        public int getY2() {
            return y2;
        }

        public int getX7() {
            return x7;
        }

        public int getX8() {
            return x8;
        }

        public int getX9() {
            return x9;
        }

        public int getY3() {
            return y3;
        }
    }
}
