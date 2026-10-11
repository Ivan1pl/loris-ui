package com.ivan1pl.lorisui.api.component;

import java.util.Objects;

/**
 * Describes what a {@link Panel} draws behind its content: an image, an image recolored by mask colors, or a single
 * color. A panel without a background draws none.
 * <p>
 * Obtain an instance from one of the static factories. Images are mapped onto the panel's area as described by
 * their {@link ImageMode}.
 * <p>
 * Image paths are class-path resource names, where a leading {@code /} is optional. On the module path the resource
 * must be outside any Java package of a named module, or in a package that the application module opens
 * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough. If an image background
 * cannot be loaded, the screen's render call fails with an exception each time its panel is drawn.
 */
public abstract sealed class Background {
    private Background() {}

    /**
     * Creates a background that draws an image.
     *
     * @param image     resource path of the image (see {@link Background} for the path format); must not be null
     * @param imageMode how the image is mapped onto the panel's area; must not be null
     * @return an {@link Image} background
     * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
     */
    public static Background image(String image, ImageMode imageMode) {
        return new Image(image, imageMode);
    }

    /**
     * Creates a background that draws an image recolored by mask colors: each pixel becomes a blend of the three
     * mask colors weighted by the pixel's red, green and blue intensities, as computed by
     * {@link com.ivan1pl.lorisui.api.core.Color#mask}.
     *
     * @param image      resource path of the image (see {@link Background} for the path format); must not be null
     * @param imageMode  how the image is mapped onto the panel's area; must not be null
     * @param redColor   color weighted by the red channel, or {@code null} to keep the pixel's own color
     * @param greenColor color weighted by the green channel, or {@code null} to keep the pixel's own color
     * @param blueColor  color weighted by the blue channel, or {@code null} to keep the pixel's own color
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
     * Creates a background filled with a single color.
     *
     * @param color the fill color; must not be null (omit the background instead to draw none)
     * @return a {@link Color} background
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public static Background color(com.ivan1pl.lorisui.api.core.Color color) {
        return new Color(Objects.requireNonNull(color, "color"));
    }

    /**
     * A background that draws an image. Created by {@link Background#image(String, ImageMode)};
     * {@link MaskedImage} extends it with mask colors.
     */
    public static sealed class Image extends Background {
        private final String image;
        private final ImageMode imageMode;

        private Image(String image, ImageMode imageMode) {
            this.image = Objects.requireNonNull(image, "image");
            this.imageMode = Objects.requireNonNull(imageMode, "imageMode");
        }

        /**
         * Returns the image's resource path.
         *
         * @return the class-path resource name of the image, never {@code null}
         */
        public String getImage() {
            return image;
        }

        /**
         * Returns how the image is mapped onto the panel's area.
         *
         * @return the image mode, never {@code null}
         */
        public ImageMode getImageMode() {
            return imageMode;
        }
    }

    /**
     * A background that draws an image recolored by mask colors. Created by
     * {@link Background#maskedImage(String, ImageMode, com.ivan1pl.lorisui.api.core.Color,
     * com.ivan1pl.lorisui.api.core.Color, com.ivan1pl.lorisui.api.core.Color)}.
     */
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

        /**
         * Returns the color weighted by the red channel.
         *
         * @return the red mask color, or {@code null} when pixels keep their own color for this channel
         */
        public com.ivan1pl.lorisui.api.core.Color getRedColor() {
            return redColor;
        }

        /**
         * Returns the color weighted by the green channel.
         *
         * @return the green mask color, or {@code null} when pixels keep their own color for this channel
         */
        public com.ivan1pl.lorisui.api.core.Color getGreenColor() {
            return greenColor;
        }

        /**
         * Returns the color weighted by the blue channel.
         *
         * @return the blue mask color, or {@code null} when pixels keep their own color for this channel
         */
        public com.ivan1pl.lorisui.api.core.Color getBlueColor() {
            return blueColor;
        }
    }

    /**
     * A background filled with a single color. Created by
     * {@link Background#color(com.ivan1pl.lorisui.api.core.Color)}.
     */
    public static final class Color extends Background {
        private final com.ivan1pl.lorisui.api.core.Color color;

        private Color(com.ivan1pl.lorisui.api.core.Color color) {
            this.color = color;
        }

        /**
         * Returns the fill color.
         *
         * @return the fill color, never {@code null}
         */
        public com.ivan1pl.lorisui.api.core.Color getColor() {
            return color;
        }
    }
}
