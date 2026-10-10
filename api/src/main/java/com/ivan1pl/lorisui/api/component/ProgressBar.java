package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
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

    /**
     * Returns the progress, evaluated on each call.
     *
     * @return the progress fraction, or 0 when no progress was set
     */
    public float getProgress() {
        return progress.get();
    }

    /**
     * Returns the background colour, evaluated on each call.
     *
     * @return the background colour, or {@code null} to keep the toolkit default
     */
    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    /**
     * Returns the bar colour, evaluated on each call.
     *
     * @return the bar colour, or {@code null} to keep the toolkit default
     */
    public Color getBarColor() {
        return barColor.get();
    }

    public float getRounding() {
        return rounding;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ProgressBar}. Required arguments are checked eagerly; Supplier setters reject null; constant
     * overloads of optional properties accept null meaning unset/toolkit default.
     */
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
            this.progress = Objects.requireNonNull(progress, "progress");
            return this;
        }

        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
            return this;
        }

        public Builder barColor(Color barColor) {
            return barColor(() -> barColor);
        }

        public Builder barColor(Supplier<Color> barColor) {
            this.barColor = Objects.requireNonNull(barColor, "barColor");
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
