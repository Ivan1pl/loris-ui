package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.util.Objects;
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

    /**
     * Returns the composed text, evaluated on each call. Pieces produced by a Supplier or Function that resolve to
     * {@code null} contribute an empty string.
     *
     * @param resolver resolver used for localized pieces
     * @return the text, or an empty string when no text was set; never {@code null}
     */
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
        return color.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum Align {
        LEFT,
        RIGHT,
        CENTER
    }

    /**
     * Builder for {@link Text}. Required arguments are checked eagerly; Supplier setters reject null; constant
     * overloads of optional properties accept null meaning unset/toolkit default.
     */
    public static class Builder {
        // null means "nothing set yet", so append* starts a new composition instead of extending one
        private Function<BundleKeyResolver, String> text;
        private IntUnaryOperator width;
        private Align align = Align.LEFT;
        private Font font;
        private Supplier<Color> color = () -> null;

        private Builder() {}

        public Builder text(String text) {
            Objects.requireNonNull(text, "text");
            return text(_ -> text);
        }

        public Builder text(Supplier<String> text) {
            Objects.requireNonNull(text, "text");
            return text(_ -> text.get());
        }

        public Builder text(Function<BundleKeyResolver, String> text) {
            Objects.requireNonNull(text, "text");
            this.text = resolver -> Objects.requireNonNullElse(text.apply(resolver), "");
            return this;
        }

        public Builder localizedText(String key) {
            Objects.requireNonNull(key, "key");
            return text(resolver -> resolver.resolveResourceKey(key));
        }

        public Builder localizedText(Supplier<String> key) {
            Objects.requireNonNull(key, "key");
            return text(resolver -> resolver.resolveResourceKey(key.get()));
        }

        public Builder append(String text) {
            Objects.requireNonNull(text, "text");
            return append(_ -> text);
        }

        public Builder append(Supplier<String> text) {
            Objects.requireNonNull(text, "text");
            return append(_ -> text.get());
        }

        public Builder append(Function<BundleKeyResolver, String> text) {
            Objects.requireNonNull(text, "text");
            if (this.text == null) {
                return text(text);
            }
            Function<BundleKeyResolver, String> previous = this.text;
            this.text = resolver -> previous.apply(resolver) + Objects.requireNonNullElse(text.apply(resolver), "");
            return this;
        }

        public Builder appendLocalized(String key) {
            Objects.requireNonNull(key, "key");
            return append(resolver -> resolver.resolveResourceKey(key));
        }

        public Builder appendLocalized(Supplier<String> key) {
            Objects.requireNonNull(key, "key");
            return append(resolver -> resolver.resolveResourceKey(key.get()));
        }

        public Builder width(int width) {
            return width(_ -> width);
        }

        public Builder width(IntUnaryOperator width) {
            this.width = Objects.requireNonNull(width, "width");
            return this;
        }

        public Builder align(Align align) {
            this.align = Objects.requireNonNull(align, "align");
            return this;
        }

        public Builder font(Font font) {
            this.font = font;
            return this;
        }

        public Builder color(Color color) {
            return color(() -> color);
        }

        public Builder color(Supplier<Color> color) {
            this.color = Objects.requireNonNull(color, "color");
            return this;
        }

        /**
         * Builds the text component.
         *
         * @return a new {@link Text}
         * @throws NullPointerException if {@code width} was never set
         */
        public Text build() {
            return new Text(text, Objects.requireNonNull(width, "width"), align, font, color);
        }
    }
}
