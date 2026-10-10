package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

public class ComboBoxStyle implements Style {
    private final Supplier<Color> backgroundColor;
    private final Supplier<Color> backgroundColorHovered;
    private final Supplier<Color> borderColor;
    private final Supplier<Color> arrowButtonColor;
    private final Supplier<Color> arrowButtonColorHovered;
    private final Supplier<Color> arrowColor;
    private final Supplier<Float> rounding;
    private final Supplier<Float> borderThickness;

    private ComboBoxStyle(Supplier<Color> backgroundColor, Supplier<Color> backgroundColorHovered,
                          Supplier<Color> borderColor, Supplier<Color> arrowButtonColor,
                          Supplier<Color> arrowButtonColorHovered, Supplier<Color> arrowColor, Supplier<Float> rounding,
                          Supplier<Float> borderThickness) {
        this.backgroundColor = backgroundColor;
        this.backgroundColorHovered = backgroundColorHovered;
        this.borderColor = borderColor;
        this.arrowButtonColor = arrowButtonColor;
        this.arrowButtonColorHovered = arrowButtonColorHovered;
        this.arrowColor = arrowColor;
        this.rounding = rounding;
        this.borderThickness = borderThickness;
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
     * Returns the background colour hovered, evaluated on each call.
     *
     * @return the background colour hovered, or {@code null} to keep the toolkit default
     */
    public Color getBackgroundColorHovered() {
        return backgroundColorHovered.get();
    }

    /**
     * Returns the border colour, evaluated on each call.
     *
     * @return the border colour, or {@code null} to keep the toolkit default
     */
    public Color getBorderColor() {
        return borderColor.get();
    }

    /**
     * Returns the arrow button colour, evaluated on each call.
     *
     * @return the arrow button colour, or {@code null} to keep the toolkit default
     */
    public Color getArrowButtonColor() {
        return arrowButtonColor.get();
    }

    /**
     * Returns the arrow button colour hovered, evaluated on each call.
     *
     * @return the arrow button colour hovered, or {@code null} to keep the toolkit default
     */
    public Color getArrowButtonColorHovered() {
        return arrowButtonColorHovered.get();
    }

    /**
     * Returns the arrow colour, evaluated on each call.
     *
     * @return the arrow colour, or {@code null} to keep the toolkit default
     */
    public Color getArrowColor() {
        return arrowColor.get();
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
     * Returns the border thickness, evaluated on each call.
     *
     * @return the border thickness, or {@code null} to keep the toolkit default
     */
    public Float getBorderThickness() {
        return borderThickness.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ComboBoxStyle}. Required arguments are checked eagerly; Supplier setters reject null;
     * constant overloads of optional properties accept null meaning unset/toolkit default.
     */
    public static class Builder {
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Color> backgroundColorHovered = () -> null;
        private Supplier<Color> borderColor = () -> null;
        private Supplier<Color> arrowButtonColor = () -> null;
        private Supplier<Color> arrowButtonColorHovered = () -> null;
        private Supplier<Color> arrowColor = () -> null;
        private Supplier<Float> rounding = () -> null;
        private Supplier<Float> borderThickness = () -> null;

        private Builder() {}

        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
            return this;
        }

        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        public Builder backgroundColorHovered(Supplier<Color> backgroundColorHovered) {
            this.backgroundColorHovered = Objects.requireNonNull(backgroundColorHovered, "backgroundColorHovered");
            return this;
        }

        public Builder backgroundColorHovered(Color backgroundColorHovered) {
            return backgroundColorHovered(() -> backgroundColorHovered);
        }

        public Builder borderColor(Supplier<Color> borderColor) {
            this.borderColor = Objects.requireNonNull(borderColor, "borderColor");
            return this;
        }

        public Builder borderColor(Color borderColor) {
            return borderColor(() -> borderColor);
        }

        public Builder arrowButtonColor(Supplier<Color> arrowButtonColor) {
            this.arrowButtonColor = Objects.requireNonNull(arrowButtonColor, "arrowButtonColor");
            return this;
        }

        public Builder arrowButtonColor(Color arrowButtonColor) {
            return arrowButtonColor(() -> arrowButtonColor);
        }

        public Builder arrowButtonColorHovered(Supplier<Color> arrowButtonColorHovered) {
            this.arrowButtonColorHovered = Objects.requireNonNull(arrowButtonColorHovered, "arrowButtonColorHovered");
            return this;
        }

        public Builder arrowButtonColorHovered(Color arrowButtonColorHovered) {
            return arrowButtonColorHovered(() -> arrowButtonColorHovered);
        }

        public Builder arrowColor(Supplier<Color> arrowColor) {
            this.arrowColor = Objects.requireNonNull(arrowColor, "arrowColor");
            return this;
        }

        public Builder arrowColor(Color arrowColor) {
            return arrowColor(() -> arrowColor);
        }

        public Builder rounding(Supplier<Float> rounding) {
            this.rounding = Objects.requireNonNull(rounding, "rounding");
            return this;
        }

        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        public Builder borderThickness(Supplier<Float> borderThickness) {
            this.borderThickness = Objects.requireNonNull(borderThickness, "borderThickness");
            return this;
        }

        public Builder borderThickness(float borderThickness) {
            return borderThickness(() -> borderThickness);
        }

        public ComboBoxStyle build() {
            return new ComboBoxStyle(backgroundColor, backgroundColorHovered, borderColor, arrowButtonColor,
                    arrowButtonColorHovered, arrowColor, rounding, borderThickness);
        }
    }
}
