package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Styles a text or number input field: its background, text, selection and border colors, corner rounding
 * and border thickness.
 *
 * <p>Set it with {@link com.ivan1pl.lorisui.api.component.TextInput.Builder#style(TextInputStyle)} or
 * {@link com.ivan1pl.lorisui.api.component.NumberInput.Builder#style(TextInputStyle)}. Every property is
 * optional; an unset property keeps the toolkit default.
 */
public class TextInputStyle implements Style {
    private final Supplier<Color> backgroundColor;
    private final Supplier<Color> textColor;
    private final Supplier<Color> selectedTextBackgroundColor;
    private final Supplier<Color> borderColor;
    private final Supplier<Float> rounding;
    private final Supplier<Float> borderThickness;

    private TextInputStyle(Supplier<Color> backgroundColor, Supplier<Color> textColor,
                           Supplier<Color> selectedTextBackgroundColor, Supplier<Color> borderColor,
                           Supplier<Float> rounding, Supplier<Float> borderThickness) {
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.selectedTextBackgroundColor = selectedTextBackgroundColor;
        this.borderColor = borderColor;
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
     * Returns the color of the entered text, evaluated on each call.
     *
     * @return the color of the entered text, or {@code null} to keep the toolkit default
     */
    public Color getTextColor() {
        return textColor.get();
    }

    /**
     * Returns the background color of selected text, evaluated on each call.
     *
     * @return the background color of selected text, or {@code null} to keep the toolkit default
     */
    public Color getSelectedTextBackgroundColor() {
        return selectedTextBackgroundColor.get();
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
     * Creates a builder for a {@link TextInputStyle} with every property unset.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link TextInputStyle}. Obtain one with {@link TextInputStyle#builder()}.
     *
     * <p>No property is required; each property left unset keeps the toolkit default.
     */
    public static class Builder {
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Color> textColor = () -> null;
        private Supplier<Color> selectedTextBackgroundColor = () -> null;
        private Supplier<Color> borderColor = () -> null;
        private Supplier<Float> rounding = () -> null;
        private Supplier<Float> borderThickness = () -> null;

        private Builder() {}

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
         * Sets the color of the entered text.
         *
         * @param textColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder textColor(Color textColor) {
            return textColor(() -> textColor);
        }

        /**
         * Sets the color of the entered text. The supplier is evaluated each time the style is applied and may return
         * {@code null} to keep the toolkit default.
         *
         * @param textColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code textColor} is {@code null}
         */
        public Builder textColor(Supplier<Color> textColor) {
            this.textColor = Objects.requireNonNull(textColor, "textColor");
            return this;
        }

        /**
         * Sets the background color of selected text.
         *
         * @param selectedTextBackgroundColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder selectedTextBackgroundColor(Color selectedTextBackgroundColor) {
            return selectedTextBackgroundColor(() -> selectedTextBackgroundColor);
        }

        /**
         * Sets the background color of selected text. The supplier is evaluated each time the style is applied and may
         * return {@code null} to keep the toolkit default.
         *
         * @param selectedTextBackgroundColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code selectedTextBackgroundColor} is {@code null}
         */
        public Builder selectedTextBackgroundColor(Supplier<Color> selectedTextBackgroundColor) {
            this.selectedTextBackgroundColor =
                    Objects.requireNonNull(selectedTextBackgroundColor, "selectedTextBackgroundColor");
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
         * Sets the corner rounding radius of the field.
         *
         * @param rounding the rounding radius in pixels
         * @return this builder
         */
        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
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
         * Sets the border thickness of the field.
         *
         * @param borderThickness the border thickness in pixels
         * @return this builder
         */
        public Builder borderThickness(float borderThickness) {
            return borderThickness(() -> borderThickness);
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
         * Creates the style from the properties set so far.
         *
         * @return a new {@link TextInputStyle}
         */
        public TextInputStyle build() {
            return new TextInputStyle(backgroundColor, textColor, selectedTextBackgroundColor, borderColor, rounding,
                    borderThickness);
        }
    }
}
