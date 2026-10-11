package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.font.Font;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An input field for text.
 * <p>
 * The field is bound to the application state through a getter and a setter. It displays the value returned by the
 * getter, and passes edited text to {@link #validateInput(String)}, which truncates it to the maximum length and hands
 * the result to the setter. Create instances with {@link #builder()}.
 */
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
     * Commits edited text to the bound setter, truncated to the current maximum length.
     *
     * @param value the edited text; must not be null
     * @throws NullPointerException if {@code value} is null
     */
    public void validateInput(String value) {
        if (value.length() > maxLength.get()) {
            value = value.substring(0, maxLength.get());
        }
        valueSetter.accept(value);
    }

    /**
     * Creates a builder for a text input.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TextInput}, obtained from {@link TextInput#builder()}.
     * <p>
     * The value bindings are required: call {@link #value(Supplier, Consumer)} before {@link #build()}. Unless set,
     * the maximum length is 100 characters, and the width, font and style keep the toolkit defaults.
     */
    public static class Builder {
        private Supplier<Integer> maxLength = () -> 100;
        private Supplier<String> valueGetter;
        private Consumer<String> valueSetter;
        private int width;
        private Font font;
        private TextInputStyle style = TextInputStyle.builder().build();

        private Builder() {}

        /**
         * Sets a constant maximum length. The default is 100.
         *
         * @param maxLength the maximum number of characters passed to the setter; must not be negative
         * @return this builder
         */
        public Builder maxLength(int maxLength) {
            return maxLength(() -> maxLength);
        }

        /**
         * Sets a dynamic maximum length, evaluated each time edited text is committed.
         *
         * @param maxLength supplier of the maximum number of characters passed to the setter; must not be null and
         *                  must not return {@code null} or a negative value
         * @return this builder
         * @throws NullPointerException if {@code maxLength} is null
         */
        public Builder maxLength(Supplier<Integer> maxLength) {
            this.maxLength = Objects.requireNonNull(maxLength, "maxLength");
            return this;
        }

        /**
         * Binds the input field to a value. Required.
         *
         * @param getter supplier of the displayed value, evaluated each time the field is drawn; must not be null.
         *               A {@code null} result is displayed as an empty field.
         * @param setter receiver of each committed value, already truncated to the maximum length; must not be null
         * @return this builder
         * @throws NullPointerException if {@code getter} or {@code setter} is null
         */
        public Builder value(Supplier<String> getter, Consumer<String> setter) {
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
