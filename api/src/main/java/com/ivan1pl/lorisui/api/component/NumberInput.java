package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.font.Font;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An input field for an integer value.
 * <p>
 * The field is bound to the application state through a getter and a setter. It displays the value returned by the
 * getter, and passes edited text to {@link #validateInput(String)}, which parses it, clamps it to the minimum and
 * maximum values and hands the result to the setter. Create instances with {@link #builder()}.
 */
public class NumberInput implements Component {
    private final Supplier<Integer> minValue;
    private final Supplier<Integer> maxValue;
    private final Supplier<Integer> valueGetter;
    private final Consumer<Integer> valueSetter;
    private final int width;
    private final Font font;
    private final TextInputStyle style;

    private NumberInput(Supplier<Integer> minValue, Supplier<Integer> maxValue, Supplier<Integer> valueGetter,
                        Consumer<Integer> valueSetter, int width, Font font, TextInputStyle style) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.valueGetter = valueGetter;
        this.valueSetter = valueSetter;
        this.width = width;
        this.font = font;
        this.style = style;
    }

    /**
     * Returns the bound value as text to display, evaluated on each call.
     *
     * @return the decimal representation of the bound value, or {@code ""} when the bound getter yields {@code null}
     */
    public String getValue() {
        Integer value = valueGetter.get();
        return value == null ? "" : Integer.toString(value);
    }

    /**
     * Returns the width of the input field.
     *
     * @return the width in pixels, or 0 to keep the toolkit default
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the font used to draw the input field.
     *
     * @return the font, or {@code null} to keep the toolkit default
     */
    public Font getFont() {
        return font;
    }

    /**
     * Returns the style of the input field.
     *
     * @return the style; never {@code null}
     */
    public TextInputStyle getStyle() {
        return style;
    }

    /**
     * Commits edited text to the bound setter. The text is parsed as a decimal integer and clamped to the current
     * minimum and maximum values; text that is not a valid integer is ignored and the setter is not invoked.
     *
     * @param value the edited text; must not be null
     */
    public void validateInput(String value) {
        try {
            int numericValue = Integer.parseInt(value);
            if (numericValue < minValue.get()) {
                numericValue = minValue.get();
            }
            if (numericValue > maxValue.get()) {
                numericValue = maxValue.get();
            }
            valueSetter.accept(numericValue);
        } catch (NumberFormatException ignored) {
        }
    }

    /**
     * Creates a builder for a number input.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link NumberInput}, obtained from {@link NumberInput#builder()}.
     * <p>
     * The value bindings are required: call {@link #value(Supplier, Consumer)} before {@link #build()}. Unless set,
     * the minimum value is 0, the maximum value is {@link Integer#MAX_VALUE}, and the width, font and style keep the
     * toolkit defaults.
     */
    public static class Builder {
        private Supplier<Integer> minValue = () -> 0;
        private Supplier<Integer> maxValue = () -> Integer.MAX_VALUE;
        private Supplier<Integer> valueGetter;
        private Consumer<Integer> valueSetter;
        private int width;
        private Font font;
        private TextInputStyle style = TextInputStyle.builder().build();

        private Builder() {}

        /**
         * Sets a constant minimum value. The default is 0.
         *
         * @param minValue the smallest value passed to the setter
         * @return this builder
         */
        public Builder minValue(int minValue) {
            return minValue(() -> minValue);
        }

        /**
         * Sets a dynamic minimum value, evaluated each time edited text is committed.
         *
         * @param minValue supplier of the smallest value passed to the setter; must not be null and must not return
         *                 {@code null}
         * @return this builder
         * @throws NullPointerException if {@code minValue} is null
         */
        public Builder minValue(Supplier<Integer> minValue) {
            this.minValue = Objects.requireNonNull(minValue, "minValue");
            return this;
        }

        /**
         * Sets a constant maximum value. The default is {@link Integer#MAX_VALUE}.
         *
         * @param maxValue the largest value passed to the setter
         * @return this builder
         */
        public Builder maxValue(int maxValue) {
            return maxValue(() -> maxValue);
        }

        /**
         * Sets a dynamic maximum value, evaluated each time edited text is committed.
         *
         * @param maxValue supplier of the largest value passed to the setter; must not be null and must not return
         *                 {@code null}
         * @return this builder
         * @throws NullPointerException if {@code maxValue} is null
         */
        public Builder maxValue(Supplier<Integer> maxValue) {
            this.maxValue = Objects.requireNonNull(maxValue, "maxValue");
            return this;
        }

        /**
         * Binds the input field to a value. Required.
         *
         * @param getter supplier of the displayed value, evaluated each time the field is drawn; must not be null.
         *               A {@code null} result is displayed as an empty field.
         * @param setter receiver of each committed value, already clamped to the minimum and maximum values; must not
         *               be null
         * @return this builder
         * @throws NullPointerException if {@code getter} or {@code setter} is null
         */
        public Builder value(Supplier<Integer> getter, Consumer<Integer> setter) {
            this.valueGetter = Objects.requireNonNull(getter, "getter");
            this.valueSetter = Objects.requireNonNull(setter, "setter");
            return this;
        }

        /**
         * Sets the width of the input field. The default is 0, which keeps the toolkit default.
         *
         * @param width the width in pixels, or 0 to keep the toolkit default
         * @return this builder
         */
        public Builder width(int width) {
            this.width = width;
            return this;
        }

        /**
         * Sets the font used to draw the input field.
         *
         * @param font the font, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder font(Font font) {
            this.font = font;
            return this;
        }

        /**
         * Sets the style of the input field. The default style keeps the toolkit defaults.
         *
         * @param style the style; must not be null
         * @return this builder
         * @throws NullPointerException if {@code style} is null
         */
        public Builder style(TextInputStyle style) {
            this.style = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Builds the number input. Unless set, the minimum value is 0 and the maximum value is
         * {@link Integer#MAX_VALUE}.
         *
         * @return a new {@link NumberInput}
         * @throws NullPointerException if {@code value(...)} was never called
         */
        public NumberInput build() {
            return new NumberInput(minValue, maxValue, Objects.requireNonNull(valueGetter, "valueGetter"),
                    Objects.requireNonNull(valueSetter, "valueSetter"), width, font, style);
        }
    }
}
