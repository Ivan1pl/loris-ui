package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.List;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

/**
 * A panel that runs an action when the user clicks it.
 *
 * <p>A button is a {@link Panel}: its content, for example a {@link Text} label, is laid out like a panel's, and it
 * supports the same position, size, background, tooltip and style settings. In addition, a button can show a
 * different background while the pointer is over it (hovered), while it is being pressed (active) and while it is
 * disabled; each of these falls back to the base background when unset. A disabled button ignores clicks.
 *
 * <p>Create a button with {@link #buttonBuilder(String)} or {@link #buttonBuilder(Supplier)}. Its ID follows the
 * same rules as a {@link Panel} ID.
 */
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

    /**
     * Returns the background shown while the button is being pressed, evaluated on each call.
     *
     * @return the active background, or {@code null} when unset, in which case the hovered or base background is
     *         used
     */
    public Background getActiveBackground() {
        return activeBackground.get();
    }

    /**
     * Returns the background shown while the pointer is over the button, evaluated on each call.
     *
     * @return the hovered background, or {@code null} when unset, in which case the base background is used
     */
    public Background getHoveredBackground() {
        return hoveredBackground.get();
    }

    /**
     * Returns the background shown while the button is disabled, evaluated on each call.
     *
     * @return the disabled background, or {@code null} when unset, in which case the base background is used
     */
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
     * Backends call this when the user clicks the button. Applications may call it on the host's UI thread to
     * trigger the action as if the button had been clicked.
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

    /**
     * Returns a new builder for a button with a fixed ID.
     *
     * @param id the button ID, a stable key unique among the button's siblings; must not be null
     * @return a new button builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder buttonBuilder(String id) {
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    /**
     * Returns a new builder for a button whose ID is supplied by a function, evaluated each time the ID is read.
     *
     * @param id supplies the button ID, which must stay stable and unique among the button's siblings; must not be
     *           null
     * @return a new button builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder buttonBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    /**
     * Builds a {@link Button}. Obtain one with {@link Button#buttonBuilder(String)} or
     * {@link Button#buttonBuilder(Supplier)}.
     *
     * <p>{@link #size(int, int) size(...)} is required and is checked by {@link #build()}. All other properties are
     * optional and default as for a {@link Panel.Builder}; in addition the button has no hovered, active or disabled
     * background, is enabled and does nothing when clicked.
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

        /**
         * Sets the active background, shown while the button is being pressed, to an image.
         *
         * @param image     class-path resource name of the image, located as described in
         *                  {@link Background#image(String, ImageMode)}; must not be null
         * @param imageMode how the image is fitted to the button; must not be null
         * @return this builder
         * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
         */
        public Builder activeBackgroundImage(String image, ImageMode imageMode) {
            return activeBackground(Background.image(image, imageMode));
        }

        /**
         * Sets the active background, shown while the button is being pressed, to a solid color.
         *
         * @param color the fill color, or {@code null} to use the hovered or base background
         * @return this builder
         */
        public Builder activeBackgroundColor(Color color) {
            return activeBackgroundColor(() -> color);
        }

        /**
         * Sets the active background to a solid color from a supplier, evaluated each time the button is drawn.
         *
         * @param color supplies the fill color, or {@code null} to use the hovered or base background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code color} is {@code null}
         */
        public Builder activeBackgroundColor(Supplier<Color> color) {
            return activeBackground(colorBackground(Objects.requireNonNull(color, "color")));
        }

        /**
         * Sets the active background, shown while the button is being pressed.
         *
         * @param background the background, or {@code null} to use the hovered or base background
         * @return this builder
         */
        public Builder activeBackground(Background background) {
            return activeBackground(() -> background);
        }

        /**
         * Sets the active background from a supplier, evaluated each time the button is drawn.
         *
         * @param background supplies the background, or {@code null} to use the hovered or base background; must not
         *                   be null
         * @return this builder
         * @throws NullPointerException if {@code background} is {@code null}
         */
        public Builder activeBackground(Supplier<Background> background) {
            this.activeBackground = Objects.requireNonNull(background, "background");
            return this;
        }

        /**
         * Sets the hovered background, shown while the pointer is over the button, to an image.
         *
         * @param image     class-path resource name of the image, located as described in
         *                  {@link Background#image(String, ImageMode)}; must not be null
         * @param imageMode how the image is fitted to the button; must not be null
         * @return this builder
         * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
         */
        public Builder hoveredBackgroundImage(String image, ImageMode imageMode) {
            return hoveredBackground(Background.image(image, imageMode));
        }

        /**
         * Sets the hovered background, shown while the pointer is over the button, to a solid color.
         *
         * @param color the fill color, or {@code null} to use the base background
         * @return this builder
         */
        public Builder hoveredBackgroundColor(Color color) {
            return hoveredBackgroundColor(() -> color);
        }

        /**
         * Sets the hovered background to a solid color from a supplier, evaluated each time the button is drawn.
         *
         * @param color supplies the fill color, or {@code null} to use the base background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code color} is {@code null}
         */
        public Builder hoveredBackgroundColor(Supplier<Color> color) {
            return hoveredBackground(colorBackground(Objects.requireNonNull(color, "color")));
        }

        /**
         * Sets the hovered background, shown while the pointer is over the button.
         *
         * @param background the background, or {@code null} to use the base background
         * @return this builder
         */
        public Builder hoveredBackground(Background background) {
            return hoveredBackground(() -> background);
        }

        /**
         * Sets the hovered background from a supplier, evaluated each time the button is drawn.
         *
         * @param background supplies the background, or {@code null} to use the base background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code background} is {@code null}
         */
        public Builder hoveredBackground(Supplier<Background> background) {
            this.hoveredBackground = Objects.requireNonNull(background, "background");
            return this;
        }

        /**
         * Sets the disabled background, shown while the button is disabled, to an image.
         *
         * @param image     class-path resource name of the image, located as described in
         *                  {@link Background#image(String, ImageMode)}; must not be null
         * @param imageMode how the image is fitted to the button; must not be null
         * @return this builder
         * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
         */
        public Builder disabledBackgroundImage(String image, ImageMode imageMode) {
            return disabledBackground(Background.image(image, imageMode));
        }

        /**
         * Sets the disabled background, shown while the button is disabled, to a solid color.
         *
         * @param color the fill color, or {@code null} to use the base background
         * @return this builder
         */
        public Builder disabledBackgroundColor(Color color) {
            return disabledBackgroundColor(() -> color);
        }

        /**
         * Sets the disabled background to a solid color from a supplier, evaluated each time the button is drawn.
         *
         * @param color supplies the fill color, or {@code null} to use the base background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code color} is {@code null}
         */
        public Builder disabledBackgroundColor(Supplier<Color> color) {
            return disabledBackground(colorBackground(Objects.requireNonNull(color, "color")));
        }

        /**
         * Sets the disabled background, shown while the button is disabled.
         *
         * @param background the background, or {@code null} to use the base background
         * @return this builder
         */
        public Builder disabledBackground(Background background) {
            return disabledBackground(() -> background);
        }

        /**
         * Sets the disabled background from a supplier, evaluated each time the button is drawn.
         *
         * @param background supplies the background, or {@code null} to use the base background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code background} is {@code null}
         */
        public Builder disabledBackground(Supplier<Background> background) {
            this.disabledBackground = Objects.requireNonNull(background, "background");
            return this;
        }

        /**
         * Sets whether the button accepts clicks. Defaults to {@code true}.
         *
         * @param enabled {@code true} to enable the button
         * @return this builder
         */
        public Builder enabled(boolean enabled) {
            return enabled(() -> enabled);
        }

        /**
         * Sets whether the button accepts clicks from a supplier, evaluated each time the button is drawn or clicked.
         *
         * @param enabled supplies whether the button is enabled; must not be null and must not return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code enabled} is {@code null}
         */
        public Builder enabled(Supplier<Boolean> enabled) {
            this.enabled = Objects.requireNonNull(enabled, "enabled");
            return this;
        }

        /**
         * Sets the action run when the user clicks the enabled button. It runs on the host's UI thread. By default
         * clicking does nothing.
         *
         * @param action the action; must not be null
         * @return this builder
         * @throws NullPointerException if {@code action} is {@code null}
         */
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
