package com.ivan1pl.lorisui.api.component;

public abstract sealed class Background {
    private Background() {}

    public static Background image(String image, ImageMode imageMode) {
        return new Image(image, imageMode);
    }

    public static Background maskedImage(String image, ImageMode imageMode,
                                         com.ivan1pl.lorisui.api.core.Color redColor,
                                         com.ivan1pl.lorisui.api.core.Color greenColor,
                                         com.ivan1pl.lorisui.api.core.Color blueColor) {
        return new MaskedImage(image, imageMode, redColor, greenColor, blueColor);
    }

    public static Background color(com.ivan1pl.lorisui.api.core.Color color) {
        return new Color(color);
    }

    public static sealed class Image extends Background {
        private final String image;
        private final ImageMode imageMode;

        private Image(String image, ImageMode imageMode) {
            this.image = image;
            this.imageMode = imageMode;
        }

        public String getImage() {
            return image;
        }

        public ImageMode getImageMode() {
            return imageMode;
        }
    }

    public static final class MaskedImage extends Image {
        private final com.ivan1pl.lorisui.api.core.Color redColor;
        private final com.ivan1pl.lorisui.api.core.Color greenColor;
        private final com.ivan1pl.lorisui.api.core.Color blueColor;

        public MaskedImage(String image, ImageMode imageMode,
                           com.ivan1pl.lorisui.api.core.Color redColor,
                           com.ivan1pl.lorisui.api.core.Color greenColor,
                           com.ivan1pl.lorisui.api.core.Color blueColor) {
            super(image, imageMode);
            this.redColor = redColor;
            this.greenColor = greenColor;
            this.blueColor = blueColor;
        }

        public com.ivan1pl.lorisui.api.core.Color getRedColor() {
            return redColor;
        }

        public com.ivan1pl.lorisui.api.core.Color getGreenColor() {
            return greenColor;
        }

        public com.ivan1pl.lorisui.api.core.Color getBlueColor() {
            return blueColor;
        }
    }

    public static final class Color extends Background {
        private final com.ivan1pl.lorisui.api.core.Color color;

        private Color(com.ivan1pl.lorisui.api.core.Color color) {
            this.color = color;
        }

        public com.ivan1pl.lorisui.api.core.Color getColor() {
            return color;
        }
    }
}
