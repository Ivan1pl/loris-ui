package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.List;
import java.util.Objects;
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
        return activeBackground.get();
    }

    public Background getHoveredBackground() {
        return hoveredBackground.get();
    }

    public Background getDisabledBackground() {
        return disabledBackground.get();
    }

    /**
     * Returns whether the button currently accepts clicks, evaluated on each call.
     *
     * @return {@code true} when the button is enabled
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Runs the button's action if the button is currently {@link #isEnabled() enabled}; does nothing otherwise.
     * Intended to be called by the backend on the render thread when the button is clicked.
     */
    public void click() {
        if (isEnabled()) {
            action.run();
        }
    }

    /**
     * Resolves the background to draw for the given interaction state, evaluated on each call. When the button is
     * disabled, the disabled background is used; otherwise the active background takes priority over the hovered one.
     * Any state background that is unset falls back to the base {@link #getBackground() background}.
     *
     * @param hovered whether the pointer is over the button
     * @param active  whether the button is being pressed
     * @return the background for the state, or {@code null} when neither the state nor the base background is set
     */
    public Background getBackground(boolean hovered, boolean active) {
        if (!isEnabled()) {
            Background disabled = getDisabledBackground();
            return disabled == null ? getBackground() : disabled;
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
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    public static Builder buttonBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    /**
     * Builder for {@link Button}. Required arguments are checked eagerly; Supplier setters reject null;
     * constant overloads of optional properties accept null meaning unset/toolkit default.
     * {@code size(...)} is required and checked at {@code build()}.
     */
    public static class Builder extends BaseBuilder<Button, Builder> {
        private Supplier<Background> activeBackground = () -> null;
        private Supplier<Background> hoveredBackground = () -> null;
        private Supplier<Background> disabledBackground = () -> null;
        private Supplier<Boolean> enabled = () -> true;
        private Runnable action = () -> {};

        private Builder(Supplier<String> id) {
            super(id);
        }

        public Builder activeBackgroundImage(String image, ImageMode imageMode) {
            return activeBackground(Background.image(image, imageMode));
        }

        public Builder activeBackgroundColor(Color color) {
            return activeBackgroundColor(() -> color);
        }

        public Builder activeBackgroundColor(Supplier<Color> color) {
            return activeBackground(colorBackground(Objects.requireNonNull(color, "color")));
        }

        public Builder activeBackground(Background background) {
            return activeBackground(() -> background);
        }

        public Builder activeBackground(Supplier<Background> background) {
            this.activeBackground = Objects.requireNonNull(background, "background");
            return this;
        }

        public Builder hoveredBackgroundImage(String image, ImageMode imageMode) {
            return hoveredBackground(Background.image(image, imageMode));
        }

        public Builder hoveredBackgroundColor(Color color) {
            return hoveredBackgroundColor(() -> color);
        }

        public Builder hoveredBackgroundColor(Supplier<Color> color) {
            return hoveredBackground(colorBackground(Objects.requireNonNull(color, "color")));
        }

        public Builder hoveredBackground(Background background) {
            return hoveredBackground(() -> background);
        }

        public Builder hoveredBackground(Supplier<Background> background) {
            this.hoveredBackground = Objects.requireNonNull(background, "background");
            return this;
        }

        public Builder disabledBackgroundImage(String image, ImageMode imageMode) {
            return disabledBackground(Background.image(image, imageMode));
        }

        public Builder disabledBackgroundColor(Color color) {
            return disabledBackgroundColor(() -> color);
        }

        public Builder disabledBackgroundColor(Supplier<Color> color) {
            return disabledBackground(colorBackground(Objects.requireNonNull(color, "color")));
        }

        public Builder disabledBackground(Background background) {
            return disabledBackground(() -> background);
        }

        public Builder disabledBackground(Supplier<Background> background) {
            this.disabledBackground = Objects.requireNonNull(background, "background");
            return this;
        }

        public Builder enabled(boolean enabled) {
            return enabled(() -> enabled);
        }

        public Builder enabled(Supplier<Boolean> enabled) {
            this.enabled = Objects.requireNonNull(enabled, "enabled");
            return this;
        }

        public Builder action(Runnable action) {
            this.action = Objects.requireNonNull(action, "action");
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
