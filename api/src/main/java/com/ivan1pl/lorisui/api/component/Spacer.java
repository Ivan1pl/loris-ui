package com.ivan1pl.lorisui.api.component;

/**
 * An empty component that takes up space in a panel's layout, for example to separate or indent other content.
 *
 * <p>Create a spacer with {@link #builder()}.
 */
public class Spacer implements Component {
    private final int width;
    private final int height;

    private Spacer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the width of the space.
     *
     * @return the width, in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the space.
     *
     * @return the height, in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns a new builder for a spacer.
     *
     * @return a new spacer builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link Spacer}. Obtain one with {@link Spacer#builder()}.
     *
     * <p>No property is required; the width and height default to 0.
     */
    public static class Builder {
        private int width = 0;
        private int height = 0;

        private Builder() {}

        /**
         * Sets the width of the space. Defaults to 0.
         *
         * @param width the width, in pixels
         * @return this builder
         */
        public Builder width(int width) {
            this.width = width;
            return this;
        }

        /**
         * Sets the height of the space. Defaults to 0.
         *
         * @param height the height, in pixels
         * @return this builder
         */
        public Builder height(int height) {
            this.height = height;
            return this;
        }

        /**
         * Builds the spacer.
         *
         * @return a new {@link Spacer}
         */
        public Spacer build() {
            return new Spacer(width, height);
        }
    }
}
