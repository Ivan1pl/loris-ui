package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

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

    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    public Color getGrabColor() {
        return grabColor.get();
    }

    public Color getHoveredGrabColor() {
        return hoveredGrabColor.get();
    }

    public Color getActiveGrabColor() {
        return activeGrabColor.get();
    }

    public Float getRounding() {
        return rounding.get();
    }

    public Float getSize() {
        return size.get();
    }

    public Float getPadding() {
        return padding.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supplier<Color> backgroundColor;
        private Supplier<Color> grabColor;
        private Supplier<Color> hoveredGrabColor;
        private Supplier<Color> activeGrabColor;
        private Supplier<Float> rounding;
        private Supplier<Float> size;
        private Supplier<Float> padding;

        private Builder() {}

        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder grabColor(Color grabColor) {
            return grabColor(() -> grabColor);
        }

        public Builder grabColor(Supplier<Color> grabColor) {
            this.grabColor = grabColor;
            return this;
        }

        public Builder hoveredGrabColor(Color hoveredGrabColor) {
            return hoveredGrabColor(() -> hoveredGrabColor);
        }

        public Builder hoveredGrabColor(Supplier<Color> hoveredGrabColor) {
            this.hoveredGrabColor = hoveredGrabColor;
            return this;
        }

        public Builder activeGrabColor(Color activeGrabColor) {
            return activeGrabColor(() -> activeGrabColor);
        }

        public Builder activeGrabColor(Supplier<Color> activeGrabColor) {
            this.activeGrabColor = activeGrabColor;
            return this;
        }

        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        public Builder rounding(Supplier<Float> rounding) {
            this.rounding = rounding;
            return this;
        }

        public Builder size(float size) {
            return size(() -> size);
        }

        public Builder size(Supplier<Float> size) {
            this.size = size;
            return this;
        }

        public Builder padding(float padding) {
            return padding(() -> padding);
        }

        public Builder padding(Supplier<Float> padding) {
            this.padding = padding;
            return this;
        }

        public ScrollbarStyle build() {
            return new ScrollbarStyle(
                    backgroundColor, grabColor, hoveredGrabColor, activeGrabColor, rounding, size, padding);
        }
    }
}
