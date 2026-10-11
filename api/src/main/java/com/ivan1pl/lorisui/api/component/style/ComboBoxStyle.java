package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Styles the closed {@link com.ivan1pl.lorisui.api.component.ComboBox} field: its background, border, arrow
 * button and arrow colors, corner rounding and border thickness.
 *
 * <p>Set it with {@link com.ivan1pl.lorisui.api.component.ComboBox.Builder#style(ComboBoxStyle)}. Use
 * {@link ComboBoxPopupStyle} and {@link ComboBoxItemStyle} for the drop-down list. Every property is optional; an
 * unset property keeps the toolkit default.
 */
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
     * Returns the background color of the field, evaluated on each call.
     *
     * @return the background color of the field, or {@code null} to keep the toolkit default
     */
    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    /**
     * Returns the background color of the field when hovered, evaluated on each call.
     *
     * @return the background color of the field when hovered, or {@code null} to keep the toolkit default
     */
    public Color getBackgroundColorHovered() {
        return backgroundColorHovered.get();
    }

    /**
     * Returns the border color of the field, evaluated on each call.
     *
     * @return the border color of the field, or {@code null} to keep the toolkit default
     */
    public Color getBorderColor() {
        return borderColor.get();
    }

    /**
     * Returns the color of the arrow button, evaluated on each call.
     *
     * @return the color of the arrow button, or {@code null} to keep the toolkit default
     */
    public Color getArrowButtonColor() {
        return arrowButtonColor.get();
    }

    /**
     * Returns the color of the arrow button when hovered, evaluated on each call.
     *
     * @return the color of the arrow button when hovered, or {@code null} to keep the toolkit default
     */
    public Color getArrowButtonColorHovered() {
        return arrowButtonColorHovered.get();
    }

    /**
     * Returns the color of the arrow drawn on the arrow button, evaluated on each call.
     *
     * @return the color of the arrow drawn on the arrow button, or {@code null} to keep the toolkit default
     */
    public Color getArrowColor() {
        return arrowColor.get();
    }

    /**
     * Returns the corner rounding radius of the field in pixels, evaluated on each call.
     *
     * @return the corner rounding radius of the field, or {@code null} to keep the toolkit default
     */
    public Float getRounding() {
        return rounding.get();
    }

    /**
     * Returns the border thickness of the field in pixels, evaluated on each call.
     *
     * @return the border thickness of the field, or {@code null} to keep the toolkit default
     */
    public Float getBorderThickness() {
        return borderThickness.get();
    }

    /**
     * Creates a builder for a {@link ComboBoxStyle} with every property unset.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link ComboBoxStyle}. Obtain one with {@link ComboBoxStyle#builder()}.
     *
     * <p>No property is required; each property left unset keeps the toolkit default.
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

        /**
         * Sets the background color of the field. The supplier is evaluated each time the style is applied and may
         * return {@code null} to keep the toolkit default.
         *
         * @param backgroundColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code backgroundColor} is {@code null}
         */
        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
            return this;
        }

        /**
         * Sets the background color of the field.
         *
         * @param backgroundColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        /**
         * Sets the background color of the field when hovered. The supplier is evaluated each time the style is applied
         * and may return {@code null} to keep the toolkit default.
         *
         * @param backgroundColorHovered the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code backgroundColorHovered} is {@code null}
         */
        public Builder backgroundColorHovered(Supplier<Color> backgroundColorHovered) {
            this.backgroundColorHovered = Objects.requireNonNull(backgroundColorHovered, "backgroundColorHovered");
            return this;
        }

        /**
         * Sets the background color of the field when hovered.
         *
         * @param backgroundColorHovered the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder backgroundColorHovered(Color backgroundColorHovered) {
            return backgroundColorHovered(() -> backgroundColorHovered);
        }

        /**
         * Sets the border color of the field. The supplier is evaluated each time the style is applied and may return
         * {@code null} to keep the toolkit default.
         *
         * @param borderColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code borderColor} is {@code null}
         */
        public Builder borderColor(Supplier<Color> borderColor) {
            this.borderColor = Objects.requireNonNull(borderColor, "borderColor");
            return this;
        }

        /**
         * Sets the border color of the field.
         *
         * @param borderColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder borderColor(Color borderColor) {
            return borderColor(() -> borderColor);
        }

        /**
         * Sets the color of the arrow button. The supplier is evaluated each time the style is applied and may return
         * {@code null} to keep the toolkit default.
         *
         * @param arrowButtonColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code arrowButtonColor} is {@code null}
         */
        public Builder arrowButtonColor(Supplier<Color> arrowButtonColor) {
            this.arrowButtonColor = Objects.requireNonNull(arrowButtonColor, "arrowButtonColor");
            return this;
        }

        /**
         * Sets the color of the arrow button.
         *
         * @param arrowButtonColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder arrowButtonColor(Color arrowButtonColor) {
            return arrowButtonColor(() -> arrowButtonColor);
        }

        /**
         * Sets the color of the arrow button when hovered. The supplier is evaluated each time the style is applied and
         * may return {@code null} to keep the toolkit default.
         *
         * @param arrowButtonColorHovered the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code arrowButtonColorHovered} is {@code null}
         */
        public Builder arrowButtonColorHovered(Supplier<Color> arrowButtonColorHovered) {
            this.arrowButtonColorHovered = Objects.requireNonNull(arrowButtonColorHovered, "arrowButtonColorHovered");
            return this;
        }

        /**
         * Sets the color of the arrow button when hovered.
         *
         * @param arrowButtonColorHovered the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder arrowButtonColorHovered(Color arrowButtonColorHovered) {
            return arrowButtonColorHovered(() -> arrowButtonColorHovered);
        }

        /**
         * Sets the color of the arrow drawn on the arrow button. The supplier is evaluated each time the style is
         * applied and may return {@code null} to keep the toolkit default.
         *
         * @param arrowColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code arrowColor} is {@code null}
         */
        public Builder arrowColor(Supplier<Color> arrowColor) {
            this.arrowColor = Objects.requireNonNull(arrowColor, "arrowColor");
            return this;
        }

        /**
         * Sets the color of the arrow drawn on the arrow button.
         *
         * @param arrowColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder arrowColor(Color arrowColor) {
            return arrowColor(() -> arrowColor);
        }

        /**
         * Sets the corner rounding radius of the field in pixels. The supplier is evaluated each time the style is
         * applied and may return {@code null} to keep the toolkit default.
         *
         * @param rounding the rounding supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code rounding} is {@code null}
         */
        public Builder rounding(Supplier<Float> rounding) {
            this.rounding = Objects.requireNonNull(rounding, "rounding");
            return this;
        }

        /**
         * Sets the corner rounding radius of the field.
         *
         * @param rounding the rounding radius in pixels
         * @return this builder
         */
        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        /**
         * Sets the border thickness of the field in pixels. The supplier is evaluated each time the style is applied
         * and may return {@code null} to keep the toolkit default.
         *
         * @param borderThickness the thickness supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code borderThickness} is {@code null}
         */
        public Builder borderThickness(Supplier<Float> borderThickness) {
            this.borderThickness = Objects.requireNonNull(borderThickness, "borderThickness");
            return this;
        }

        /**
         * Sets the border thickness of the field.
         *
         * @param borderThickness the border thickness in pixels
         * @return this builder
         */
        public Builder borderThickness(float borderThickness) {
            return borderThickness(() -> borderThickness);
        }

        /**
         * Creates the style from the properties set so far.
         *
         * @return a new {@link ComboBoxStyle}
         */
        public ComboBoxStyle build() {
            return new ComboBoxStyle(backgroundColor, backgroundColorHovered, borderColor, arrowButtonColor,
                    arrowButtonColorHovered, arrowColor, rounding, borderThickness);
        }
    }
}
