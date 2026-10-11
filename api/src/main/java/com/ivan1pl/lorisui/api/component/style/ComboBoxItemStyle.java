package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Styles the items of an open {@link com.ivan1pl.lorisui.api.component.ComboBox} list: the background colors of the
 * selected item, the item under the mouse and the item being clicked.
 *
 * <p>Set it with {@link com.ivan1pl.lorisui.api.component.ComboBox.Builder#itemStyle(ComboBoxItemStyle)}. Every
 * property is optional; an unset property keeps the toolkit default.
 */
public class ComboBoxItemStyle implements Style {
    private final Supplier<Color> selectedColor;
    private final Supplier<Color> hoveredColor;
    private final Supplier<Color> activeColor;

    private ComboBoxItemStyle(Supplier<Color> selectedColor, Supplier<Color> hoveredColor,
                              Supplier<Color> activeColor) {
        this.selectedColor = selectedColor;
        this.hoveredColor = hoveredColor;
        this.activeColor = activeColor;
    }

    /**
     * Returns the background color of the selected item, evaluated on each call.
     *
     * @return the background color of the selected item, or {@code null} to keep the toolkit default
     */
    public Color getSelectedColor() {
        return selectedColor.get();
    }

    /**
     * Returns the background color of the item under the mouse, evaluated on each call.
     *
     * @return the background color when hovered, or {@code null} to keep the toolkit default
     */
    public Color getHoveredColor() {
        return hoveredColor.get();
    }

    /**
     * Returns the background color of the item being clicked, evaluated on each call.
     *
     * @return the background color when active, or {@code null} to keep the toolkit default
     */
    public Color getActiveColor() {
        return activeColor.get();
    }

    /**
     * Creates a builder for a {@link ComboBoxItemStyle} with every property unset.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link ComboBoxItemStyle}. Obtain one with {@link ComboBoxItemStyle#builder()}.
     *
     * <p>No property is required; each property left unset keeps the toolkit default.
     */
    public static class Builder {
        private Supplier<Color> selectedColor = () -> null;
        private Supplier<Color> hoveredColor = () -> null;
        private Supplier<Color> activeColor = () -> null;

        private Builder() {}

        /**
         * Sets the background color of the selected item.
         * The supplier is evaluated each time the style is applied and may return {@code null} to keep the
         * toolkit default.
         *
         * @param selectedColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code selectedColor} is {@code null}
         */
        public Builder selectedColor(Supplier<Color> selectedColor) {
            this.selectedColor = Objects.requireNonNull(selectedColor, "selectedColor");
            return this;
        }

        /**
         * Sets the background color of the selected item.
         *
         * @param selectedColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder selectedColor(Color selectedColor) {
            return selectedColor(() -> selectedColor);
        }

        /**
         * Sets the background color of the item under the mouse.
         * The supplier is evaluated each time the style is applied and may return {@code null} to keep the
         * toolkit default.
         *
         * @param hoveredColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code hoveredColor} is {@code null}
         */
        public Builder hoveredColor(Supplier<Color> hoveredColor) {
            this.hoveredColor = Objects.requireNonNull(hoveredColor, "hoveredColor");
            return this;
        }

        /**
         * Sets the background color of the item under the mouse.
         *
         * @param hoveredColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder hoveredColor(Color hoveredColor) {
            return hoveredColor(() -> hoveredColor);
        }

        /**
         * Sets the background color of the item being clicked.
         * The supplier is evaluated each time the style is applied and may return {@code null} to keep the
         * toolkit default.
         *
         * @param activeColor the color supplier; must not be null
         * @return this builder
         * @throws NullPointerException if {@code activeColor} is {@code null}
         */
        public Builder activeColor(Supplier<Color> activeColor) {
            this.activeColor = Objects.requireNonNull(activeColor, "activeColor");
            return this;
        }

        /**
         * Sets the background color of the item being clicked.
         *
         * @param activeColor the color, or {@code null} to keep the toolkit default
         * @return this builder
         */
        public Builder activeColor(Color activeColor) {
            return activeColor(() -> activeColor);
        }

        /**
         * Creates the style from the properties set so far.
         *
         * @return a new {@link ComboBoxItemStyle}
         */
        public ComboBoxItemStyle build() {
            return new ComboBoxItemStyle(selectedColor, hoveredColor, activeColor);
        }
    }
}
