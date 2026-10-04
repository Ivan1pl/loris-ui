package com.ivan1pl.lorisui.api.component;

public class Spacer implements Component {
    private final int width;
    private final int height;

    public Spacer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int width = 0;
        private int height = 0;

        private Builder() {}

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Spacer build() {
            return new Spacer(width, height);
        }
    }
}
