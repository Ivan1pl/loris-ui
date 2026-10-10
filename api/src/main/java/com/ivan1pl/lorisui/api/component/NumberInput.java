package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.font.Font;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public int getWidth() {
        return width;
    }

    public Font getFont() {
        return font;
    }

    public TextInputStyle getStyle() {
        return style;
    }

    /**
     * Commits edited text to the bound setter. The text is parsed as a decimal integer and clamped to the current
     * minimum and maximum values; text that is not a valid integer is ignored and the setter is not invoked.
     *
     * @param value the edited text; must not be {@code null}
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

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link NumberInput}. Required arguments are checked eagerly; Supplier setters reject null; constant
     * overloads of optional properties accept null meaning unset/toolkit default.
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

        public Builder minValue(int minValue) {
            return minValue(() -> minValue);
        }

        public Builder minValue(Supplier<Integer> minValue) {
            this.minValue = Objects.requireNonNull(minValue, "minValue");
            return this;
        }

        public Builder maxValue(int maxValue) {
            return maxValue(() -> maxValue);
        }

        public Builder maxValue(Supplier<Integer> maxValue) {
            this.maxValue = Objects.requireNonNull(maxValue, "maxValue");
            return this;
        }

        public Builder value(Supplier<Integer> getter, Consumer<Integer> setter) {
            this.valueGetter = Objects.requireNonNull(getter, "getter");
            this.valueSetter = Objects.requireNonNull(setter, "setter");
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder font(Font font) {
            this.font = font;
            return this;
        }

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
