package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A component that shows progress as a bar filled in proportion to a fraction from 0 (empty) to 1 (full).
 * <p>
 * The background color, bar color and corner rounding are optional; when unset they keep the toolkit default. Create
 * instances with {@link #builder()}.
 */
public class ProgressBar implements Component {
    private final int width;
    private final int height;
    private final Supplier<Float> progress;
    private final Supplier<Color> backgroundColor;
    private final Supplier<Color> barColor;
    private final Supplier<Float> rounding;

    private ProgressBar(int width, int height, Supplier<Float> progress, Supplier<Color> backgroundColor,
                        Supplier<Color> barColor, Supplier<Float> rounding) {
        this.width = width;
        this.height = height;
        this.progress = progress;
        this.backgroundColor = backgroundColor;
        this.barColor = barColor;
        this.rounding = rounding;
    }

    /**
     * Returns the width of the bar.
     *
     * @return the width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the bar.
     *
     * @return the height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the progress, evaluated on each call.
     *
     * @return the progress fraction from 0 (empty) to 1 (full), or 0 when no progress was set
     */
    public float getProgress() {
        return progress.get();
    }

    /**
     * Returns the background color, evaluated on each call.
     *
     * @return the background color, or {@code null} to keep the toolkit default
     */
    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    /**
     * Returns the bar color, evaluated on each call.
     *
     * @return the bar color, or {@code null} to keep the toolkit default
     */
    public Color getBarColor() {
        return barColor.get();
    }

    /**
     * Returns the corner rounding, evaluated on each call. The value is passed to the toolkit unvalidated.
     *
     * @return the corner rounding, or {@code null} to keep the toolkit default
     */
    public Float getRounding() {
        return rounding.get();
    }

    /**
     * Creates a builder for a progress bar.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ProgressBar}, obtained from {@link ProgressBar#builder()}.
     * <p>
     * No property is required. The size defaults to 0 by 0 pixels and the progress to 0; the background color, bar
     * color and rounding default to unset, which keeps the toolkit default.
     */
    public static class Builder {
        private int width;
        private int height;
        private Supplier<Float> progress = () -> 0f;
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Color> barColor = () -> null;
        private Supplier<Float> rounding = () -> null;

        private Builder() {}

        /**
         * Sets the size of the bar.
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
         * Sets a fixed progress.
         *
         * @param progress the progress fraction, from 0 (empty) to 1 (full)
         * @return this builder
         */
        public Builder progress(float progress) {
            return progress(() -> progress);
        }

        /**
         * Sets a dynamic progress, evaluated on each call to {@link ProgressBar#getProgress()}.
         *
         * @param progress supplies the progress fraction, from 0 (empty) to 1 (full); must not be null and
         *                 must not return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code progress} is {@code null}
         */
        public Builder progress(Supplier<Float> progress) {
            this.progress = Objects.requireNonNull(progress, "progress");
            return this;
        }

        /**
         * Sets a fixed background color, drawn behind the filled part of the bar.
         *
         * @param backgroundColor the background color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        /**
         * Sets a dynamic background color, evaluated on each call to {@link ProgressBar#getBackgroundColor()}.
         *
         * @param backgroundColor supplies the background color, or {@code null} to keep the toolkit default; must not
         *                        be null itself
         * @return this builder
         * @throws NullPointerException if {@code backgroundColor} is {@code null}
         */
        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
            return this;
        }

        /**
         * Sets a fixed color for the filled part of the bar.
         *
         * @param barColor the bar color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder barColor(Color barColor) {
            return barColor(() -> barColor);
        }

        /**
         * Sets a dynamic color for the filled part of the bar, evaluated on each call to
         * {@link ProgressBar#getBarColor()}.
         *
         * @param barColor supplies the bar color, or {@code null} to keep the toolkit default; must not be null itself
         * @return this builder
         * @throws NullPointerException if {@code barColor} is {@code null}
         */
        public Builder barColor(Supplier<Color> barColor) {
            this.barColor = Objects.requireNonNull(barColor, "barColor");
            return this;
        }

        /**
         * Sets a fixed corner rounding. The value is passed to the toolkit unvalidated.
         *
         * @param rounding the corner rounding
         * @return this builder
         */
        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        /**
         * Sets a dynamic corner rounding, evaluated on each call to {@link ProgressBar#getRounding()}. Non-null values
         * are passed to the toolkit unvalidated.
         *
         * @param rounding supplies the corner rounding, or {@code null} to keep the toolkit default; must not be
         *                 null itself
         * @return this builder
         * @throws NullPointerException if {@code rounding} is {@code null}
         */
        public Builder rounding(Supplier<Float> rounding) {
            this.rounding = Objects.requireNonNull(rounding, "rounding");
            return this;
        }

        /**
         * Builds the progress bar.
         *
         * @return a new {@link ProgressBar}
         */
        public ProgressBar build() {
            return new ProgressBar(width, height, progress, backgroundColor, barColor, rounding);
        }
    }
}
