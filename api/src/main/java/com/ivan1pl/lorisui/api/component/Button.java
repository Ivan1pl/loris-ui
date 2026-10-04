package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.List;
import java.util.Optional;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

public class Button extends Panel {
    private final Supplier<Background> activeBackground;
    private final Supplier<Background> hoveredBackground;
    private final Supplier<Background> disabledBackground;
    private final Supplier<Boolean> enabled;
    private final Runnable action;

    private Button(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                   IntBinaryOperator height, Layout layout, Supplier<Background> background, Supplier<Boolean> open,
                   List<IntBiFunction<Component>> content, List<Style> styles, Supplier<Panel> tooltip,
                   boolean scrollable, Supplier<Background> activeBackground, Supplier<Background> hoveredBackground,
                   Supplier<Background> disabledBackground, Supplier<Boolean> enabled, Runnable action) {
        super(id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable);
        this.activeBackground = activeBackground;
        this.hoveredBackground = hoveredBackground;
        this.disabledBackground = disabledBackground;
        this.enabled = enabled;
        this.action = action;
    }

    public Background getActiveBackground() {
        return activeBackground == null ? null : activeBackground.get();
    }

    public Background getHoveredBackground() {
        return hoveredBackground == null ? null : hoveredBackground.get();
    }

    public Background getDisabledBackground() {
        return disabledBackground == null ? null : disabledBackground.get();
    }

    public Supplier<Boolean> getEnabled() {
        return enabled;
    }

    public Runnable getAction() {
        return action;
    }

    public Background getBackground(boolean hovered, boolean active) {
        if (!enabled.get()) {
            return Optional.ofNullable(getDisabledBackground()).orElse(getBackground());
        }
        Background background = null;
        if (active) {
            background = getActiveBackground();
        }
        if (hovered && background == null) {
            background = getHoveredBackground();
        }
        return background == null ? getBackground() : background;
    }

    public static Builder buttonBuilder(String id) {
        return new Builder(() -> id);
    }

    public static Builder buttonBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    public static class Builder extends BaseBuilder<Button, Builder> {
        private Supplier<Background> activeBackground;
        private Supplier<Background> hoveredBackground;
        private Supplier<Background> disabledBackground;
        private Supplier<Boolean> enabled = () -> true;
        private Runnable action = () -> {};

        private Builder(Supplier<String> id) {
            super(id);
        }

        public Builder activeBackgroundImage(String image, ImageMode imageMode) {
            this.activeBackground = () -> Background.image(image, imageMode);
            return this;
        }

        public Builder activeBackgroundColor(Color color) {
            this.activeBackground = () -> Background.color(color);
            return this;
        }

        public Builder activeBackgroundColor(Supplier<Color> color) {
            this.activeBackground = () -> Background.color(color.get());
            return this;
        }

        public Builder activeBackground(Background background) {
            this.activeBackground = () -> background;
            return this;
        }

        public Builder activeBackground(Supplier<Background> background) {
            this.activeBackground = background;
            return this;
        }

        public Builder hoveredBackgroundImage(String image, ImageMode imageMode) {
            this.hoveredBackground = () -> Background.image(image, imageMode);
            return this;
        }

        public Builder hoveredBackgroundColor(Color color) {
            this.hoveredBackground = () -> Background.color(color);
            return this;
        }

        public Builder hoveredBackgroundColor(Supplier<Color> color) {
            this.hoveredBackground = () -> Background.color(color.get());
            return this;
        }

        public Builder hoveredBackground(Background background) {
            this.hoveredBackground = () -> background;
            return this;
        }

        public Builder hoveredBackground(Supplier<Background> background) {
            this.hoveredBackground = background;
            return this;
        }

        public Builder disabledBackgroundImage(String image, ImageMode imageMode) {
            this.disabledBackground = () -> Background.image(image, imageMode);
            return this;
        }

        public Builder disabledBackgroundColor(Color color) {
            this.disabledBackground = () -> Background.color(color);
            return this;
        }

        public Builder disabledBackgroundColor(Supplier<Color> color) {
            this.disabledBackground = () -> Background.color(color.get());
            return this;
        }

        public Builder disabledBackground(Background background) {
            this.disabledBackground = () -> background;
            return this;
        }

        public Builder disabledBackground(Supplier<Background> background) {
            this.disabledBackground = background;
            return this;
        }

        public Builder enabled(boolean enabled) {
            return enabled(() -> enabled);
        }

        public Builder enabled(Supplier<Boolean> enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }

        @Override
        public Button build() {
            return doBuild((id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable) ->
                    new Button(id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable,
                            activeBackground, hoveredBackground, disabledBackground, enabled, action));
        }
    }
}
