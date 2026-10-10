package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

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
     * Returns the selected colour, evaluated on each call.
     *
     * @return the selected colour, or {@code null} to keep the toolkit default
     */
    public Color getSelectedColor() {
        return selectedColor.get();
    }

    /**
     * Returns the hovered colour, evaluated on each call.
     *
     * @return the hovered colour, or {@code null} to keep the toolkit default
     */
    public Color getHoveredColor() {
        return hoveredColor.get();
    }

    /**
     * Returns the active colour, evaluated on each call.
     *
     * @return the active colour, or {@code null} to keep the toolkit default
     */
    public Color getActiveColor() {
        return activeColor.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ComboBoxItemStyle}. Required arguments are checked eagerly; Supplier setters reject null;
     * constant overloads of optional properties accept null meaning unset/toolkit default.
     */
    public static class Builder {
        private Supplier<Color> selectedColor = () -> null;
        private Supplier<Color> hoveredColor = () -> null;
        private Supplier<Color> activeColor = () -> null;

        private Builder() {}

        public Builder selectedColor(Supplier<Color> selectedColor) {
            this.selectedColor = Objects.requireNonNull(selectedColor, "selectedColor");
            return this;
        }

        public Builder selectedColor(Color selectedColor) {
            return selectedColor(() -> selectedColor);
        }

        public Builder hoveredColor(Supplier<Color> hoveredColor) {
            this.hoveredColor = Objects.requireNonNull(hoveredColor, "hoveredColor");
            return this;
        }

        public Builder hoveredColor(Color hoveredColor) {
            return hoveredColor(() -> hoveredColor);
        }

        public Builder activeColor(Supplier<Color> activeColor) {
            this.activeColor = Objects.requireNonNull(activeColor, "activeColor");
            return this;
        }

        public Builder activeColor(Color activeColor) {
            return activeColor(() -> activeColor);
        }

        public ComboBoxItemStyle build() {
            return new ComboBoxItemStyle(selectedColor, hoveredColor, activeColor);
        }
    }
}
