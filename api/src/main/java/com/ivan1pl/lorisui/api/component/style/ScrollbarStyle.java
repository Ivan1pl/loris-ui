package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

public class ScrollbarStyle implements Style {
    private final Supplier<Color> backgroundColor;
    private final Supplier<Color> grabColor;
    private final Supplier<Color> hoveredGrabColor;
    private final Supplier<Color> activeGrabColor;
    private final Supplier<Float> rounding;
    private final Supplier<Float> size;
    private final Supplier<Float> padding;

    private ScrollbarStyle(Supplier<Color> backgroundColor, Supplier<Color> grabColor, Supplier<Color> hoveredGrabColor,
                           Supplier<Color> activeGrabColor, Supplier<Float> rounding, Supplier<Float> size,
                           Supplier<Float> padding) {
        this.backgroundColor = backgroundColor;
        this.grabColor = grabColor;
        this.hoveredGrabColor = hoveredGrabColor;
        this.activeGrabColor = activeGrabColor;
        this.rounding = rounding;
        this.size = size;
        this.padding = padding;
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
     * Returns the grab colour, evaluated on each call.
     *
     * @return the grab colour, or {@code null} to keep the toolkit default
     */
    public Color getGrabColor() {
        return grabColor.get();
    }

    /**
     * Returns the hovered grab colour, evaluated on each call.
     *
     * @return the hovered grab colour, or {@code null} to keep the toolkit default
     */
    public Color getHoveredGrabColor() {
        return hoveredGrabColor.get();
    }

    /**
     * Returns the active grab colour, evaluated on each call.
     *
     * @return the active grab colour, or {@code null} to keep the toolkit default
     */
    public Color getActiveGrabColor() {
        return activeGrabColor.get();
    }

    /**
     * Returns the rounding, evaluated on each call.
     *
     * @return the rounding, or {@code null} to keep the toolkit default
     */
    public Float getRounding() {
        return rounding.get();
    }

    /**
     * Returns the size, evaluated on each call.
     *
     * @return the size, or {@code null} to keep the toolkit default
     */
    public Float getSize() {
        return size.get();
    }

    /**
     * Returns the padding, evaluated on each call.
     *
     * @return the padding, or {@code null} to keep the toolkit default
     */
    public Float getPadding() {
        return padding.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ScrollbarStyle}. Required arguments are checked eagerly; Supplier setters reject null;
     * constant overloads of optional properties accept null meaning unset/toolkit default.
     */
    public static class Builder {
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Color> grabColor = () -> null;
        private Supplier<Color> hoveredGrabColor = () -> null;
        private Supplier<Color> activeGrabColor = () -> null;
        private Supplier<Float> rounding = () -> null;
        private Supplier<Float> size = () -> null;
        private Supplier<Float> padding = () -> null;

        private Builder() {}

        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
            return this;
        }

        public Builder grabColor(Color grabColor) {
            return grabColor(() -> grabColor);
        }

        public Builder grabColor(Supplier<Color> grabColor) {
            this.grabColor = Objects.requireNonNull(grabColor, "grabColor");
            return this;
        }

        public Builder hoveredGrabColor(Color hoveredGrabColor) {
            return hoveredGrabColor(() -> hoveredGrabColor);
        }

        public Builder hoveredGrabColor(Supplier<Color> hoveredGrabColor) {
            this.hoveredGrabColor = Objects.requireNonNull(hoveredGrabColor, "hoveredGrabColor");
            return this;
        }

        public Builder activeGrabColor(Color activeGrabColor) {
            return activeGrabColor(() -> activeGrabColor);
        }

        public Builder activeGrabColor(Supplier<Color> activeGrabColor) {
            this.activeGrabColor = Objects.requireNonNull(activeGrabColor, "activeGrabColor");
            return this;
        }

        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        public Builder rounding(Supplier<Float> rounding) {
            this.rounding = Objects.requireNonNull(rounding, "rounding");
            return this;
        }

        public Builder size(float size) {
            return size(() -> size);
        }

        public Builder size(Supplier<Float> size) {
            this.size = Objects.requireNonNull(size, "size");
            return this;
        }

        public Builder padding(float padding) {
            return padding(() -> padding);
        }

        public Builder padding(Supplier<Float> padding) {
            this.padding = Objects.requireNonNull(padding, "padding");
            return this;
        }

        public ScrollbarStyle build() {
            return new ScrollbarStyle(
                    backgroundColor, grabColor, hoveredGrabColor, activeGrabColor, rounding, size, padding);
        }
    }
}
