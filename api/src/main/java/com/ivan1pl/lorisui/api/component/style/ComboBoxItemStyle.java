package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

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

    public Color getSelectedColor() {
        return selectedColor.get();
    }

    public Color getHoveredColor() {
        return hoveredColor.get();
    }

    public Color getActiveColor() {
        return activeColor.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supplier<Color> selectedColor = () -> null;
        private Supplier<Color> hoveredColor = () -> null;
        private Supplier<Color> activeColor = () -> null;

        private Builder() {}

        public Builder selectedColor(Supplier<Color> selectedColor) {
            this.selectedColor = selectedColor;
            return this;
        }

        public Builder selectedColor(Color selectedColor) {
            return selectedColor(() -> selectedColor);
        }

        public Builder hoveredColor(Supplier<Color> hoveredColor) {
            this.hoveredColor = hoveredColor;
            return this;
        }

        public Builder hoveredColor(Color hoveredColor) {
            return hoveredColor(() -> hoveredColor);
        }

        public Builder activeColor(Supplier<Color> activeColor) {
            this.activeColor = activeColor;
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
