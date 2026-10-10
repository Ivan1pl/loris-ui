package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.font.Font;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextInput implements Component {
    private final Supplier<Integer> maxLength;
    private final Supplier<String> valueGetter;
    private final Consumer<String> valueSetter;
    private final int width;
    private final Font font;
    private final TextInputStyle style;

    private TextInput(Supplier<Integer> maxLength, Supplier<String> valueGetter,
                      Consumer<String> valueSetter, int width, Font font, TextInputStyle style) {
        this.maxLength = maxLength;
        this.valueGetter = valueGetter;
        this.valueSetter = valueSetter;
        this.width = width;
        this.font = font;
        this.style = style;
    }

    /**
     * Returns the bound value as text to display, evaluated on each call. The value is not limited in length.
     *
     * @return the bound value, or {@code ""} when the bound getter yields {@code null}
     */
    public String getValue() {
        String value = valueGetter.get();
        return value == null ? "" : value;
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
     * Commits edited text to the bound setter, truncated to the current maximum length.
     *
     * @param value the edited text; must not be {@code null}
     */
    public void validateInput(String value) {
        if (value.length() > maxLength.get()) {
            value = value.substring(0, maxLength.get());
        }
        valueSetter.accept(value);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TextInput}. Required arguments are checked eagerly; Supplier setters reject null; constant
     * overloads of optional properties accept null meaning unset/toolkit default.
     */
    public static class Builder {
        private Supplier<Integer> maxLength = () -> 100;
        private Supplier<String> valueGetter;
        private Consumer<String> valueSetter;
        private int width;
        private Font font;
        private TextInputStyle style = TextInputStyle.builder().build();

        private Builder() {}

        public Builder maxLength(int maxLength) {
            return maxLength(() -> maxLength);
        }

        public Builder maxLength(Supplier<Integer> maxLength) {
            this.maxLength = Objects.requireNonNull(maxLength, "maxLength");
            return this;
        }

        public Builder value(Supplier<String> getter, Consumer<String> setter) {
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
         * Builds the text input. Unless set, the maximum length is 100.
         *
         * @return a new {@link TextInput}
         * @throws NullPointerException if {@code value(...)} was never called
         */
        public TextInput build() {
            return new TextInput(maxLength, Objects.requireNonNull(valueGetter, "valueGetter"),
                    Objects.requireNonNull(valueSetter, "valueSetter"), width, font, style);
        }
    }
}
