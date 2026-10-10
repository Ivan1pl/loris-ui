package com.ivan1pl.lorisui.api.component;

import java.util.Objects;

public abstract sealed class Background {
    private Background() {}

    /**
     * Creates a background that draws an image.
     *
     * @param image     resource path of the image; must not be {@code null}
     * @param imageMode how the image is fitted to the component; must not be {@code null}
     * @return an {@link Image} background
     * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
     */
    public static Background image(String image, ImageMode imageMode) {
        return new Image(image, imageMode);
    }

    /**
     * Creates a background that draws an image recoloured by mask colours: each pixel becomes a blend of the three
     * mask colours weighted by the pixel's red, green and blue intensities.
     *
     * @param image      resource path of the image; must not be {@code null}
     * @param imageMode  how the image is fitted to the component; must not be {@code null}
     * @param redColor   colour weighted by the red channel, or {@code null} to keep the pixel's own colour
     * @param greenColor colour weighted by the green channel, or {@code null} to keep the pixel's own colour
     * @param blueColor  colour weighted by the blue channel, or {@code null} to keep the pixel's own colour
     * @return a {@link MaskedImage} background
     * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
     */
    public static Background maskedImage(String image, ImageMode imageMode,
                                         com.ivan1pl.lorisui.api.core.Color redColor,
                                         com.ivan1pl.lorisui.api.core.Color greenColor,
                                         com.ivan1pl.lorisui.api.core.Color blueColor) {
        return new MaskedImage(image, imageMode, redColor, greenColor, blueColor);
    }

    /**
     * Creates a background filled with a single colour.
     *
     * @param color the fill colour; must not be {@code null} (omit the background instead to draw none)
     * @return a {@link Color} background
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public static Background color(com.ivan1pl.lorisui.api.core.Color color) {
        return new Color(Objects.requireNonNull(color, "color"));
    }

    public static sealed class Image extends Background {
        private final String image;
        private final ImageMode imageMode;

        private Image(String image, ImageMode imageMode) {
            this.image = Objects.requireNonNull(image, "image");
            this.imageMode = Objects.requireNonNull(imageMode, "imageMode");
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

        private MaskedImage(String image, ImageMode imageMode,
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
