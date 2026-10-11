package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Styles the scrollbars of scrollable panels: the colors of the track and of the grab (the draggable part),
 * the corner rounding, the scrollbar size and the padding around the grab.
 *
 * <p>Add it to a {@link com.ivan1pl.lorisui.api.component.Panel} with the panel builder's
 * {@code addStyle(...)}; it then applies to the scrollbars of that panel and of every panel inside it. Every
 * property is optional; an unset property keeps the toolkit default.
 */
public class ScrollbarStyle implements Style {
    private final Supplier<Color> backgroundColor;
    private final Supplier<Color> grabColor;
    private final Supplier<Color> hoveredGrabColor;
    private final Supplier<Color> activeGrabColor;
    private final Supplier<Float> rounding;
    private final Supplier<Float> size;
    private final Supplier<Float> padding;

    private ScrollbarStyle(Supplier<Color> backgroundColor, Supplier<Color> grabColor, Supplier<Color> hoveredGrabColor,
                           Supplier<Color> activeGrabColor, Supplier<Float> rounding, Supplier<Float> size,
                           Supplier<Float> padding) {
        this.backgroundColor = backgroundColor;
        this.grabColor = grabColor;
        this.hoveredGrabColor = hoveredGrabColor;
        this.activeGrabColor = activeGrabColor;
        this.rounding = rounding;
        this.size = size;
        this.padding = padding;
    }

    /**
     * Returns the background color of the scrollbar track, evaluated on each call.
     *
     * @return the background color of the scrollbar track, or {@code null} to keep the toolkit default
     */
    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    /**
     * Returns the color of the grab, evaluated on each call.
     *
     * @return the color of the grab, or {@code null} to keep the toolkit default
     */
    public Color getGrabColor() {
        return grabColor.get();
    }

    /**
     * Returns the color of the grab when hovered, evaluated on each call.
     *
     * @return the color of the grab when hovered, or {@code null} to keep the toolkit default
     */
    public Color getHoveredGrabColor() {
        return hoveredGrabColor.get();
    }

    /**
     * Returns the color of the grab while it is dragged, evaluated on each call.
     *
     * @return the color of the grab while it is dragged, or {@code null} to keep the toolkit default
     */
    public Color getActiveGrabColor() {
        return activeGrabColor.get();
    }

    /**
     * Returns the corner rounding radius of the grab in pixels, evaluated on each call.
     *
     * @return the corner rounding radius of the grab, or {@code null} to keep the toolkit default
     */
    public Float getRounding() {
        return rounding.get();
    }

    /**
     * Returns the scrollbar size (the width of a vertical scrollbar, the height of a horizontal one) in pixels,
     * evaluated on each call.
     *
     * @return the scrollbar size (the width of a vertical scrollbar, the height of a horizontal one), or {@code null}
     *         to keep the toolkit default
     */
    public Float getSize() {
        return size.get();
    }

    /**
     * Returns the padding between the grab and the edges of the track in pixels, evaluated on each call.
     *
     * @return the padding between the grab and the edges of the track, or {@code null} to keep the toolkit default
     */
    public Float getPadding() {
        return padding.get();
    }

    /**
     * Creates a builder for a {@link ScrollbarStyle} with every property unset.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link ScrollbarStyle}. Obtain one with {@link ScrollbarStyle#builder()}.
     *
     * <p>No property is required; each property left unset keeps the toolkit default.
     */
    public static class Builder {
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Color> grabColor = () -> null;
        private Supplier<Color> hoveredGrabColor = () -> null;
        private Supplier<Color> activeGrabColor = () -> null;
        private Supplier<Float> rounding = () -> null;
        private Supplier<Float> size = () -> null;
        private Supplier<Float> padding = () -> null;

        private Builder() {}

        /**
         * Sets the background color of the scrollbar track.
         *
         * @param backgroundColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        /**
         * Sets the background color of the scrollbar track. The supplier is evaluated each time the style is applied
         * and may return {@code null} to keep the toolkit default.
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
         * Sets the color of the grab.
         *
         * @param grabColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder grabColor(Color grabColor) {
            return grabColor(() -> grabColor);
        }

        /**
         * Sets the color of the grab. The supplier is evaluated each time the style is applied and may return
         * {@code null} to keep the toolkit default.
         *
         * @param grabColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code grabColor} is {@code null}
         */
        public Builder grabColor(Supplier<Color> grabColor) {
            this.grabColor = Objects.requireNonNull(grabColor, "grabColor");
            return this;
        }

        /**
         * Sets the color of the grab when hovered.
         *
         * @param hoveredGrabColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder hoveredGrabColor(Color hoveredGrabColor) {
            return hoveredGrabColor(() -> hoveredGrabColor);
        }

        /**
         * Sets the color of the grab when hovered. The supplier is evaluated each time the style is applied and may
         * return {@code null} to keep the toolkit default.
         *
         * @param hoveredGrabColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code hoveredGrabColor} is {@code null}
         */
        public Builder hoveredGrabColor(Supplier<Color> hoveredGrabColor) {
            this.hoveredGrabColor = Objects.requireNonNull(hoveredGrabColor, "hoveredGrabColor");
            return this;
        }

        /**
         * Sets the color of the grab while it is dragged.
         *
         * @param activeGrabColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder activeGrabColor(Color activeGrabColor) {
            return activeGrabColor(() -> activeGrabColor);
        }

        /**
         * Sets the color of the grab while it is dragged. The supplier is evaluated each time the style is applied and
         * may return {@code null} to keep the toolkit default.
         *
         * @param activeGrabColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code activeGrabColor} is {@code null}
         */
        public Builder activeGrabColor(Supplier<Color> activeGrabColor) {
            this.activeGrabColor = Objects.requireNonNull(activeGrabColor, "activeGrabColor");
            return this;
        }

        /**
         * Sets the corner rounding radius of the grab.
         *
         * @param rounding the rounding radius in pixels
         * @return this builder
         */
        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        /**
         * Sets the corner rounding radius of the grab in pixels. The supplier is evaluated each time the style is
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
         * Sets the scrollbar size (the width of a vertical scrollbar, the height of a horizontal one).
         *
         * @param size the size in pixels
         * @return this builder
         */
        public Builder size(float size) {
            return size(() -> size);
        }

        /**
         * Sets the scrollbar size (the width of a vertical scrollbar, the height of a horizontal one) in pixels. The
         * supplier is evaluated each time the style is applied and may return {@code null} to keep the toolkit default.
         *
         * @param size the size supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code size} is {@code null}
         */
        public Builder size(Supplier<Float> size) {
            this.size = Objects.requireNonNull(size, "size");
            return this;
        }

        /**
         * Sets the padding between the grab and the edges of the track.
         *
         * @param padding the padding in pixels
         * @return this builder
         */
        public Builder padding(float padding) {
            return padding(() -> padding);
        }

        /**
         * Sets the padding between the grab and the edges of the track in pixels. The supplier is evaluated each time
         * the style is applied and may return {@code null} to keep the toolkit default.
         *
         * @param padding the padding supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code padding} is {@code null}
         */
        public Builder padding(Supplier<Float> padding) {
            this.padding = Objects.requireNonNull(padding, "padding");
            return this;
        }

        /**
         * Creates the style from the properties set so far.
         *
         * @return a new {@link ScrollbarStyle}
         */
        public ScrollbarStyle build() {
            return new ScrollbarStyle(
                    backgroundColor, grabColor, hoveredGrabColor, activeGrabColor, rounding, size, padding);
        }
    }
}
