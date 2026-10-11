package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Styles the drop-down list that opens below a {@link com.ivan1pl.lorisui.api.component.ComboBox}: its background
 * color, corner rounding and border thickness.
 *
 * <p>Set it with {@link com.ivan1pl.lorisui.api.component.ComboBox.Builder#popupStyle(ComboBoxPopupStyle)}. Every
 * property is optional; an unset property keeps the toolkit default.
 */
public class ComboBoxPopupStyle implements Style {
    private final Supplier<Color> backgroundColor;
    private final Supplier<Float> rounding;
    private final Supplier<Float> borderThickness;

    private ComboBoxPopupStyle(Supplier<Color> backgroundColor, Supplier<Float> rounding,
                               Supplier<Float> borderThickness) {
        this.backgroundColor = backgroundColor;
        this.rounding = rounding;
        this.borderThickness = borderThickness;
    }

    /**
     * Returns the background color of the drop-down list, evaluated on each call.
     *
     * @return the background color, or {@code null} to keep the toolkit default
     */
    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    /**
     * Returns the corner rounding radius of the drop-down list in pixels, evaluated on each call.
     *
     * @return the rounding radius, or {@code null} to keep the toolkit default
     */
    public Float getRounding() {
        return rounding.get();
    }

    /**
     * Returns the border thickness of the drop-down list in pixels, evaluated on each call.
     *
     * @return the border thickness, or {@code null} to keep the toolkit default
     */
    public Float getBorderThickness() {
        return borderThickness.get();
    }

    /**
     * Creates a builder for a {@link ComboBoxPopupStyle} with every property unset.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link ComboBoxPopupStyle}. Obtain one with {@link ComboBoxPopupStyle#builder()}.
     *
     * <p>No property is required; each property left unset keeps the toolkit default.
     */
    public static class Builder {
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Float> rounding = () -> null;
        private Supplier<Float> borderThickness = () -> null;

        private Builder() {}

        /**
         * Sets the background color of the drop-down list.
         * The supplier is evaluated each time the style is applied and may return {@code null} to keep the
         * toolkit default.
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
         * Sets the background color of the drop-down list.
         *
         * @param backgroundColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        /**
         * Sets the corner rounding radius of the drop-down list in pixels.
         * The supplier is evaluated each time the style is applied and may return {@code null} to keep the
         * toolkit default.
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
         * Sets the corner rounding radius of the drop-down list.
         *
         * @param rounding the rounding radius in pixels
         * @return this builder
         */
        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        /**
         * Sets the border thickness of the drop-down list in pixels.
         * The supplier is evaluated each time the style is applied and may return {@code null} to keep the
         * toolkit default.
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
         * Sets the border thickness of the drop-down list.
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
         * @return a new {@link ComboBoxPopupStyle}
         */
        public ComboBoxPopupStyle build() {
            return new ComboBoxPopupStyle(backgroundColor, rounding, borderThickness);
        }
    }
}
