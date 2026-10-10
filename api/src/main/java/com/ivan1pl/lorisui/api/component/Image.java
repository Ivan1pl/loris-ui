package com.ivan1pl.lorisui.api.component;

import java.util.Objects;
import java.util.function.Supplier;

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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getImage() {
        return image.get();
    }

    public ImageMode getImageMode() {
        return imageMode.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Image}. Required arguments are checked eagerly; Supplier setters reject null; constant
     * overloads of optional properties accept null meaning unset/toolkit default.
     */
    public static class Builder {
        private int width;
        private int height;
        private Supplier<String> image;
        private Supplier<ImageMode> imageMode;

        private Builder() {}

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder image(String image, ImageMode imageMode) {
            Objects.requireNonNull(image, "image");
            Objects.requireNonNull(imageMode, "imageMode");
            return image(() -> image, () -> imageMode);
        }

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
