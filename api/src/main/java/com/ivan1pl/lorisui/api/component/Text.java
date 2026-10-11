package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * Static, dynamic or localized text.
 * <p>
 * The text is composed from pieces set with the builder's {@code text}, {@code localizedText}, {@code append} and
 * {@code appendLocalized} methods. Pieces backed by a {@link Supplier} or {@link Function} are evaluated each time the
 * text is drawn, so the displayed text follows changes in the application state. Create instances with
 * {@link #builder()}.
 */
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

    /**
     * Returns the width of the text area for the given screen width, evaluated on each call.
     *
     * @param w screen width
     * @return the width in pixels; when it is 0 or less, the backend chooses the width and the alignment is not
     *         applied
     */
    public int getWidth(int w) {
        return width.applyAsInt(w);
    }

    /**
     * Returns the horizontal alignment of the text within its width.
     *
     * @return the alignment; never {@code null}
     */
    public Align getAlign() {
        return align;
    }

    /**
     * Returns the font used to draw the text.
     *
     * @return the font, or {@code null} to keep the toolkit default
     */
    public Font getFont() {
        return font;
    }

    /**
     * Returns the text color, evaluated on each call.
     *
     * @return the text color, or {@code null} to keep the toolkit default
     */
    public Color getColor() {
        return color.get();
    }

    /**
     * Creates a builder for a text component.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Horizontal alignment of text within the width of a {@link Text}. Alignment applies only when the width is
     * greater than 0.
     */
    public enum Align {
        /** Aligns the text to the left edge. */
        LEFT,
        /** Aligns the text to the right edge. */
        RIGHT,
        /** Centers the text horizontally. */
        CENTER
    }

    /**
     * Builder for {@link Text}, obtained from {@link Text#builder()}.
     * <p>
     * The width is required: set it with one of the {@code width} methods before calling {@link #build()}. The text
     * is optional; a text component with no text draws an empty string. The {@code text} and {@code localizedText}
     * methods replace the text set so far, and the {@code append} and {@code appendLocalized} methods add a piece to
     * its end. Unless set, the text is aligned to the left and drawn with the toolkit's default font and color.
     */
    public static class Builder {
        // null means "nothing set yet", so append* starts a new composition instead of extending one
        private Function<BundleKeyResolver, String> text;
        private IntUnaryOperator width;
        private Align align = Align.LEFT;
        private Font font;
        private Supplier<Color> color = () -> null;

        private Builder() {}

        /**
         * Sets constant text, replacing the text set so far.
         *
         * @param text the text; must not be null
         * @return this builder
         * @throws NullPointerException if {@code text} is null
         */
        public Builder text(String text) {
            Objects.requireNonNull(text, "text");
            return text(_ -> text);
        }

        /**
         * Sets dynamic text, replacing the text set so far. The supplier is evaluated each time the text is drawn; a
         * {@code null} result is drawn as an empty string.
         *
         * @param text supplier of the text; must not be null
         * @return this builder
         * @throws NullPointerException if {@code text} is null
         */
        public Builder text(Supplier<String> text) {
            Objects.requireNonNull(text, "text");
            return text(_ -> text.get());
        }

        /**
         * Sets text computed with the screen's {@link BundleKeyResolver}, replacing the text set so far. The function
         * is evaluated each time the text is drawn; a {@code null} result is drawn as an empty string.
         *
         * @param text function computing the text from the resolver; must not be null
         * @return this builder
         * @throws NullPointerException if {@code text} is null
         */
        public Builder text(Function<BundleKeyResolver, String> text) {
            Objects.requireNonNull(text, "text");
            this.text = resolver -> Objects.requireNonNullElse(text.apply(resolver), "");
            return this;
        }

        /**
         * Sets localized text, replacing the text set so far. The key is resolved with the screen's
         * {@link BundleKeyResolver} each time the text is drawn.
         *
         * @param key localization key; must not be null
         * @return this builder
         * @throws NullPointerException if {@code key} is null
         */
        public Builder localizedText(String key) {
            Objects.requireNonNull(key, "key");
            return text(resolver -> resolver.resolveResourceKey(key));
        }

        /**
         * Sets localized text with a dynamic key, replacing the text set so far. The supplier is evaluated and its key
         * resolved with the screen's {@link BundleKeyResolver} each time the text is drawn.
         *
         * @param key supplier of the localization key; must not be null
         * @return this builder
         * @throws NullPointerException if {@code key} is null
         */
        public Builder localizedText(Supplier<String> key) {
            Objects.requireNonNull(key, "key");
            return text(resolver -> resolver.resolveResourceKey(key.get()));
        }

        /**
         * Appends constant text to the end of the text set so far, or sets it when no text was set yet.
         *
         * @param text the text to append; must not be null
         * @return this builder
         * @throws NullPointerException if {@code text} is null
         */
        public Builder append(String text) {
            Objects.requireNonNull(text, "text");
            return append(_ -> text);
        }

        /**
         * Appends dynamic text to the end of the text set so far, or sets it when no text was set yet. The supplier is
         * evaluated each time the text is drawn; a {@code null} result adds nothing.
         *
         * @param text supplier of the text to append; must not be null
         * @return this builder
         * @throws NullPointerException if {@code text} is null
         */
        public Builder append(Supplier<String> text) {
            Objects.requireNonNull(text, "text");
            return append(_ -> text.get());
        }

        /**
         * Appends text computed with the screen's {@link BundleKeyResolver} to the end of the text set so far, or
         * sets it when no text was set yet. The function is evaluated each time the text is drawn; a {@code null}
         * result adds nothing.
         *
         * @param text function computing the text to append from the resolver; must not be null
         * @return this builder
         * @throws NullPointerException if {@code text} is null
         */
        public Builder append(Function<BundleKeyResolver, String> text) {
            Objects.requireNonNull(text, "text");
            if (this.text == null) {
                return text(text);
            }
            Function<BundleKeyResolver, String> previous = this.text;
            this.text = resolver -> previous.apply(resolver) + Objects.requireNonNullElse(text.apply(resolver), "");
            return this;
        }

        /**
         * Appends localized text to the end of the text set so far, or sets it when no text was set yet. The key is
         * resolved with the screen's {@link BundleKeyResolver} each time the text is drawn.
         *
         * @param key localization key; must not be null
         * @return this builder
         * @throws NullPointerException if {@code key} is null
         */
        public Builder appendLocalized(String key) {
            Objects.requireNonNull(key, "key");
            return append(resolver -> resolver.resolveResourceKey(key));
        }

        /**
         * Appends localized text with a dynamic key to the end of the text set so far, or sets it when no text was
         * set yet. The supplier is evaluated and its key resolved with the screen's {@link BundleKeyResolver} each
         * time the text is drawn.
         *
         * @param key supplier of the localization key; must not be null
         * @return this builder
         * @throws NullPointerException if {@code key} is null
         */
        public Builder appendLocalized(Supplier<String> key) {
            Objects.requireNonNull(key, "key");
            return append(resolver -> resolver.resolveResourceKey(key.get()));
        }

        /**
         * Sets a constant width of the text area. With a width of 0 or less, the backend chooses the width and the
         * alignment is not applied.
         *
         * @param width the width in pixels
         * @return this builder
         */
        public Builder width(int width) {
            return width(_ -> width);
        }

        /**
         * Sets the width of the text area as a function of the screen width, evaluated each time the text is drawn.
         * With a width of 0 or less, the backend chooses the width and the alignment is not applied.
         *
         * @param width function from the screen width to the width in pixels; must not be null
         * @return this builder
         * @throws NullPointerException if {@code width} is null
         */
        public Builder width(IntUnaryOperator width) {
            this.width = Objects.requireNonNull(width, "width");
            return this;
        }

        /**
         * Sets the horizontal alignment of the text within its width. The default is {@link Align#LEFT}.
         *
         * @param align the alignment; must not be null
         * @return this builder
         * @throws NullPointerException if {@code align} is null
         */
        public Builder align(Align align) {
            this.align = Objects.requireNonNull(align, "align");
            return this;
        }

        /**
         * Sets the font used to draw the text.
         *
         * @param font the font, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder font(Font font) {
            this.font = font;
            return this;
        }

        /**
         * Sets a constant text color.
         *
         * @param color the text color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder color(Color color) {
            return color(() -> color);
        }

        /**
         * Sets a dynamic text color, evaluated each time the text is drawn. A {@code null} result keeps the toolkit
         * default.
         *
         * @param color supplier of the text color; must not be null
         * @return this builder
         * @throws NullPointerException if {@code color} is null
         */
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
