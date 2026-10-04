package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.font.Font;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextInput implements Component {
    private final byte[] buffer;
    private final byte[] emptyBuffer;
    private final Supplier<Integer> maxLength;
    private final Supplier<String> valueGetter;
    private final Consumer<String> valueSetter;
    private final int width;
    private final Font font;
    private final TextInputStyle style;

    private TextInput(Supplier<Integer> maxLength, Supplier<String> valueGetter,
                      Consumer<String> valueSetter, int width, Font font, TextInputStyle style) {
        this.maxLength = maxLength;
        this.buffer = new byte[1024];
        this.emptyBuffer = new byte[1024];
        this.valueGetter = valueGetter;
        this.valueSetter = valueSetter;
        this.width = width;
        this.font = font;
        this.style = style;
    }

    public byte[] getBuffer() {
        byte[] value = valueGetter.get().getBytes(StandardCharsets.UTF_8);
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
        if (value.length() > maxLength.get()) {
            value = value.substring(0, maxLength.get());
        }
        valueSetter.accept(value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supplier<Integer> maxLength;
        private Supplier<String> valueGetter;
        private Consumer<String> valueSetter;
        private int width;
        private Font font;
        private TextInputStyle style = TextInputStyle.builder().build();

        private Builder() {}

        public Builder maxLength(int maxLength) {
            this.maxLength = () -> maxLength;
            return this;
        }

        public Builder maxLength(Supplier<Integer> maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder value(Supplier<String> getter, Consumer<String> setter) {
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

        public TextInput build() {
            return new TextInput(maxLength, valueGetter, valueSetter, width, font, style);
        }
    }
}
