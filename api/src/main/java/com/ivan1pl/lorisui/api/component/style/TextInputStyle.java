package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.function.Supplier;

public class TextInputStyle implements Style {
    private final Supplier<Color> backgroundColor;
    private final Supplier<Color> textColor;
    private final Supplier<Color> selectedTextBackgroundColor;
    private final Supplier<Color> borderColor;
    private final Supplier<Float> rounding;
    private final Supplier<Float> borderThickness;

    public TextInputStyle(Supplier<Color> backgroundColor, Supplier<Color> textColor,
                          Supplier<Color> selectedTextBackgroundColor, Supplier<Color> borderColor,
                          Supplier<Float> rounding, Supplier<Float> borderThickness) {
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.selectedTextBackgroundColor = selectedTextBackgroundColor;
        this.borderColor = borderColor;
        this.rounding = rounding;
        this.borderThickness = borderThickness;
    }

    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    public Color getTextColor() {
        return textColor.get();
    }

    public Color getSelectedTextBackgroundColor() {
        return selectedTextBackgroundColor.get();
    }

    public Color getBorderColor() {
        return borderColor.get();
    }

    public Float getRounding() {
        return rounding.get();
    }

    public Float getBorderThickness() {
        return borderThickness.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Color> textColor = () -> null;
        private Supplier<Color> selectedTextBackgroundColor = () -> null;
        private Supplier<Color> borderColor = () -> null;
        private Supplier<Float> rounding = () -> null;
        private Supplier<Float> borderThickness = () -> null;

        private Builder() {}

        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder textColor(Color textColor) {
            return textColor(() -> textColor);
        }

        public Builder textColor(Supplier<Color> textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder selectedTextBackgroundColor(Color selectedTextBackgroundColor) {
            return selectedTextBackgroundColor(() -> selectedTextBackgroundColor);
        }

        public Builder selectedTextBackgroundColor(Supplier<Color> selectedTextBackgroundColor) {
            this.selectedTextBackgroundColor = selectedTextBackgroundColor;
            return this;
        }

        public Builder borderColor(Color borderColor) {
            return borderColor(() -> borderColor);
        }

        public Builder borderColor(Supplier<Color> borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        public Builder rounding(Supplier<Float> rounding) {
            this.rounding = rounding;
            return this;
        }

        public Builder borderThickness(float borderThickness) {
            return borderThickness(() -> borderThickness);
        }

        public Builder borderThickness(Supplier<Float> borderThickness) {
            this.borderThickness = borderThickness;
            return this;
        }

        public TextInputStyle build() {
            return new TextInputStyle(backgroundColor, textColor, selectedTextBackgroundColor, borderColor, rounding, borderThickness);
        }
    }
}
