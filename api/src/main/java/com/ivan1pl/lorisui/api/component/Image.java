package com.ivan1pl.lorisui.api.component;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A component that draws an image into an area of a fixed size, mapped onto that area as described by its
 * {@link ImageMode}.
 * <p>
 * The image is given by a resource path: a class-path resource name, where a leading {@code /} is optional. On the
 * module path the resource must be outside any Java package of a named module, or in a package that the application
 * module opens unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough. If the image
 * cannot be loaded, the screen's render call fails with an exception each time the component is drawn.
 * <p>
 * Create instances with {@link #builder()}.
 */
public class Image implements Component {
    private final int width;
    private final int height;
    private final Supplier<String> image;
    private final Supplier<ImageMode> imageMode;

    private Image(int width, int height, Supplier<String> image, Supplier<ImageMode> imageMode) {
        this.width = width;
        this.height = height;
        this.image = image;
        this.imageMode = imageMode;
    }

    /**
     * Returns the width of the image area.
     *
     * @return the width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the image area.
     *
     * @return the height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the image's resource path, evaluated on each call.
     *
     * @return the class-path resource name of the image
     */
    public String getImage() {
        return image.get();
    }

    /**
     * Returns how the image is mapped onto its area, evaluated on each call.
     *
     * @return the image mode
     */
    public ImageMode getImageMode() {
        return imageMode.get();
    }

    /**
     * Creates a builder for an image component.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Image}, obtained from {@link Image#builder()}.
     * <p>
     * {@code image(...)} is required: {@link #build()} throws {@link NullPointerException} if it was never called.
     * The size defaults to 0 by 0 pixels.
     */
    public static class Builder {
        private int width;
        private int height;
        private Supplier<String> image;
        private Supplier<ImageMode> imageMode;

        private Builder() {}

        /**
         * Sets the size of the image area.
         *
         * @param width  the width in pixels
         * @param height the height in pixels
         * @return this builder
         */
        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * Sets a fixed image and image mode.
         *
         * @param image     resource path of the image: a class-path resource name, where a leading {@code /} is
         *                  optional (see {@link Image} for module-path requirements); must not be null
         * @param imageMode how the image is mapped onto its area; must not be null
         * @return this builder
         * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
         */
        public Builder image(String image, ImageMode imageMode) {
            Objects.requireNonNull(image, "image");
            Objects.requireNonNull(imageMode, "imageMode");
            return image(() -> image, () -> imageMode);
        }

        /**
         * Sets a dynamic image and image mode, evaluated each time the image is drawn.
         *
         * @param image     supplies the resource path of the image (see {@link Image} for the path format); must not
         *                  be null
         * @param imageMode supplies how the image is mapped onto its area; must not be null
         * @return this builder
         * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
         */
        public Builder image(Supplier<String> image, Supplier<ImageMode> imageMode) {
            this.image = Objects.requireNonNull(image, "image");
            this.imageMode = Objects.requireNonNull(imageMode, "imageMode");
            return this;
        }

        /**
         * Builds the image component.
         *
         * @return a new {@link Image}
         * @throws NullPointerException if {@code image(...)} was never called
         */
        public Image build() {
            return new Image(width, height, Objects.requireNonNull(image, "image"),
                    Objects.requireNonNull(imageMode, "imageMode"));
        }
    }
}
