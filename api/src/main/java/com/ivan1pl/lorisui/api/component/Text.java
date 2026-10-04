package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public class Text implements Component {
    private final Function<BundleKeyResolver, String> text;
    private final IntUnaryOperator width;
    private final Align align;
    private final Font font;
    private final Supplier<Color> color;

    private Text(Function<BundleKeyResolver, String> text, IntUnaryOperator width, Align align, Font font,
                 Supplier<Color> color) {
        this.text = text;
        this.width = width;
        this.align = align;
        this.font = font;
        this.color = color;
    }

    public String getText(BundleKeyResolver resolver) {
        return text == null ? "" : text.apply(resolver);
    }

    public int getWidth(int w) {
        return width.applyAsInt(w);
    }

    public Align getAlign() {
        return align;
    }

    public Font getFont() {
        return font;
    }

    public Color getColor() {
        return color == null ? null : color.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum Align {
        LEFT,
        RIGHT,
        CENTER
    }

    public static class Builder {
        private Function<BundleKeyResolver, String> text;
        private IntUnaryOperator width;
        private Align align = Align.LEFT;
        private Font font;
        private Supplier<Color> color;

        private Builder() {}

        public Builder text(String text) {
            this.text = resolver -> text;
            return this;
        }

        public Builder text(Supplier<String> text) {
            this.text = resolver -> text.get();
            return this;
        }

        public Builder text(Function<BundleKeyResolver, String> text) {
            this.text = text;
            return this;
        }

        public Builder localizedText(String key) {
            this.text = resolver -> resolver.resolveResourceKey(key);
            return this;
        }

        public Builder localizedText(Supplier<String> key) {
            this.text = resolver -> resolver.resolveResourceKey(key.get());
            return this;
        }

        public Builder append(String text) {
            if (this.text == null) {
                return text(text);
            } else {
                Function<BundleKeyResolver, String> previous = this.text;
                this.text = resolver -> previous.apply(resolver) + text;
                return this;
            }
        }

        public Builder append(Supplier<String> text) {
            if (this.text == null) {
                return text(text);
            } else {
                Function<BundleKeyResolver, String> previous = this.text;
                this.text = resolver -> previous.apply(resolver) + text.get();
                return this;
            }
        }

        public Builder append(Function<BundleKeyResolver, String> text) {
            if (this.text == null) {
                return text(text);
            } else {
                Function<BundleKeyResolver, String> previous = this.text;
                this.text = resolver -> previous.apply(resolver) + text.apply(resolver);
                return this;
            }
        }

        public Builder appendLocalized(String key) {
            if (this.text == null) {
                return localizedText(key);
            } else {
                Function<BundleKeyResolver, String> previous = this.text;
                this.text = resolver -> previous.apply(resolver) + resolver.resolveResourceKey(key);
                return this;
            }
        }

        public Builder appendLocalized(Supplier<String> key) {
            if (this.text == null) {
                return localizedText(key);
            } else {
                Function<BundleKeyResolver, String> previous = this.text;
                this.text = resolver -> previous.apply(resolver) + resolver.resolveResourceKey(key.get());
                return this;
            }
        }

        public Builder width(int width) {
            return width(_ -> width);
        }

        public Builder width(IntUnaryOperator width) {
            this.width = width;
            return this;
        }

        public Builder align(Align align) {
            this.align = align;
            return this;
        }

        public Builder font(Font font) {
            this.font = font;
            return this;
        }

        public Builder color(Color color) {
            this.color = () -> color;
            return this;
        }

        public Builder color(Supplier<Color> color) {
            this.color = color;
            return this;
        }

        public Text build() {
            return new Text(text, width, align, font, color);
        }
    }
}
