package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.font.Font;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NumberInput implements Component {
    private final byte[] buffer;
    private final byte[] emptyBuffer;
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
        this.buffer = new byte[Integer.toString(Integer.MAX_VALUE/10).length()];
        this.emptyBuffer = new byte[Integer.toString(Integer.MAX_VALUE/10).length()];
        this.valueGetter = valueGetter;
        this.valueSetter = valueSetter;
        this.width = width;
        this.font = font;
        this.style = style;
    }

    public byte[] getBuffer() {
        byte[] value = Integer.toString(valueGetter.get()).getBytes(StandardCharsets.UTF_8);
        System.arraycopy(emptyBuffer, 0, buffer, 0, emptyBuffer.length);
        System.arraycopy(value, 0, buffer, 0, value.length);
        return buffer;
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

    public static class Builder {
        private Supplier<Integer> minValue;
        private Supplier<Integer> maxValue;
        private Supplier<Integer> valueGetter;
        private Consumer<Integer> valueSetter;
        private int width;
        private Font font;
        private TextInputStyle style = TextInputStyle.builder().build();

        private Builder() {}

        public Builder minValue(int minValue) {
            this.minValue = () -> minValue;
            return this;
        }

        public Builder minValue(Supplier<Integer> minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder maxValue(int maxValue) {
            this.maxValue = () -> maxValue;
            return this;
        }

        public Builder maxValue(Supplier<Integer> maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder value(Supplier<Integer> getter, Consumer<Integer> setter) {
            this.valueGetter = getter;
            this.valueSetter = setter;
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
            this.style = style;
            return this;
        }

        public NumberInput build() {
            return new NumberInput(minValue, maxValue, valueGetter, valueSetter, width, font, style);
        }
    }
}
