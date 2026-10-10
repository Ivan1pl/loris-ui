package com.ivan1pl.lorisui.api.component.style;

import com.ivan1pl.lorisui.api.core.Color;

import java.util.Objects;
import java.util.function.Supplier;

public class ComboBoxPopupStyle implements Style {
    private final Supplier<Color> backgroundColor;
    private final Supplier<Float> rounding;
    private final Supplier<Float> borderThickness;

    private ComboBoxPopupStyle(Supplier<Color> backgroundColor, Supplier<Float> rounding,
                               Supplier<Float> borderThickness) {
        this.backgroundColor = backgroundColor;
        this.rounding = rounding;
        this.borderThickness = borderThickness;
    }

    /**
     * Returns the background colour, evaluated on each call.
     *
     * @return the background colour, or {@code null} to keep the toolkit default
     */
    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    /**
     * Returns the rounding, evaluated on each call.
     *
     * @return the rounding, or {@code null} to keep the toolkit default
     */
    public Float getRounding() {
        return rounding.get();
    }

    /**
     * Returns the border thickness, evaluated on each call.
     *
     * @return the border thickness, or {@code null} to keep the toolkit default
     */
    public Float getBorderThickness() {
        return borderThickness.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ComboBoxPopupStyle}. Required arguments are checked eagerly; Supplier setters reject null;
     * constant overloads of optional properties accept null meaning unset/toolkit default.
     */
    public static class Builder {
        private Supplier<Color> backgroundColor = () -> null;
        private Supplier<Float> rounding = () -> null;
        private Supplier<Float> borderThickness = () -> null;

        private Builder() {}

        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
            return this;
        }

        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        public Builder rounding(Supplier<Float> rounding) {
            this.rounding = Objects.requireNonNull(rounding, "rounding");
            return this;
        }

        public Builder rounding(float rounding) {
            return rounding(() -> rounding);
        }

        public Builder borderThickness(Supplier<Float> borderThickness) {
            this.borderThickness = Objects.requireNonNull(borderThickness, "borderThickness");
            return this;
        }

        public Builder borderThickness(float borderThickness) {
            return borderThickness(() -> borderThickness);
        }

        public ComboBoxPopupStyle build() {
            return new ComboBoxPopupStyle(backgroundColor, rounding, borderThickness);
        }
    }
}
