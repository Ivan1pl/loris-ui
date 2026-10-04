package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.function.Supplier;

public class ProgressBar implements Component {
    private final int width;
    private final int height;
    private final Supplier<Float> progress;
    private final Supplier<Color> backgroundColor;
    private final Supplier<Color> barColor;
    private final float rounding;

    private ProgressBar(int width, int height, Supplier<Float> progress, Supplier<Color> backgroundColor,
                        Supplier<Color> barColor, float rounding) {
        this.width = width;
        this.height = height;
        this.progress = progress;
        this.backgroundColor = backgroundColor;
        this.barColor = barColor;
        this.rounding = rounding;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Supplier<Float> getProgress() {
        return progress;
    }

    public Supplier<Color> getBackgroundColor() {
        return backgroundColor;
    }

    public Supplier<Color> getBarColor() {
        return barColor;
    }

    public float getRounding() {
        return rounding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int width;
        private int height;
        private Supplier<Float> progress = () -> 0f;
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Color> barColor = () -> null;
        private float rounding = -1f;

        private Builder() {}

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder progress(float progress) {
            return progress(() -> progress);
        }

        public Builder progress(Supplier<Float> progress) {
            this.progress = progress;
            return this;
        }

        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder barColor(Color barColor) {
            return barColor(() -> barColor);
        }

        public Builder barColor(Supplier<Color> barColor) {
            this.barColor = barColor;
            return this;
        }

        public Builder rounding(float rounding) {
            this.rounding = rounding;
            return this;
        }

        public ProgressBar build() {
            return new ProgressBar(width, height, progress, backgroundColor, barColor, rounding);
        }
    }
}
