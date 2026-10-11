package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A rectangular container that lays out other components horizontally or vertically, in the order they were added.
 *
 * <p>A panel listed directly in a screen's component tree, and every popup, is placed at its own position on the
 * screen. A panel nested in another panel is placed by its parent's {@link Layout}, and its position is not used.
 * A panel can also have a background, a tooltip and styles that apply to the panel and everything inside it.
 *
 * <p>Every panel has an ID. Panel IDs are stable keys that must be unique among siblings, that is among the panels
 * of the same parent or among the top-level panels of a screen. Backends use the ID to recognize the same panel
 * from one frame to the next, so it should not change while the panel is shown.
 *
 * <p>Create a panel with {@link #panelBuilder(String)} or {@link #panelBuilder(Supplier)}.
 */
public class Panel implements Component {
    private final Supplier<String> id;
    private final IntBinaryOperator x;
    private final IntBinaryOperator y;
    private final IntBinaryOperator width;
    private final IntBinaryOperator height;
    private final Layout layout;
    private final Supplier<Background> background;
    private final Supplier<Boolean> open;
    private final List<IntBiFunction<Component>> content;
    private final List<Style> styles;
    private final Supplier<Panel> tooltip;
    private final boolean scrollable;

    Panel(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
          IntBinaryOperator height, Layout layout, Supplier<Background> background, Supplier<Boolean> open,
          List<IntBiFunction<Component>> content, List<Style> styles, Supplier<Panel> tooltip, boolean scrollable) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.layout = layout;
        this.background = background;
        this.open = open;
        this.content = Collections.unmodifiableList(content);
        this.styles = Collections.unmodifiableList(styles);
        this.tooltip = tooltip;
        this.scrollable = scrollable;
    }

    /**
     * Returns the panel's ID, evaluated on each call. The ID is a stable key that is unique among the panel's
     * siblings.
     *
     * @return the panel ID
     */
    public String getId() {
        return id.get();
    }

    /**
     * Returns the horizontal position for the given screen size, evaluated on each call.
     *
     * @param w screen width
     * @param h screen height
     * @return the x coordinate, or 0 when no position was set
     */
    public int getX(int w, int h) {
        return x.applyAsInt(w, h);
    }

    /**
     * Returns the vertical position for the given screen size, evaluated on each call.
     *
     * @param w screen width
     * @param h screen height
     * @return the y coordinate, or 0 when no position was set
     */
    public int getY(int w, int h) {
        return y.applyAsInt(w, h);
    }

    /**
     * Returns the width for the given screen size, evaluated on each call.
     *
     * @param w screen width
     * @param h screen height
     * @return the panel width, in pixels
     */
    public int getWidth(int w, int h) {
        return width.applyAsInt(w, h);
    }

    /**
     * Returns the height for the given screen size, evaluated on each call.
     *
     * @param w screen width
     * @param h screen height
     * @return the panel height, in pixels
     */
    public int getHeight(int w, int h) {
        return height.applyAsInt(w, h);
    }

    /**
     * Returns the direction in which the panel arranges its content.
     *
     * @return the layout; never {@code null}
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Returns the panel's background, evaluated on each call.
     *
     * @return the background, or {@code null} when the panel has no background
     */
    public Background getBackground() {
        return background.get();
    }

    /**
     * Returns the panel's content in the order it was added. Each entry is a function that receives the screen width
     * and height and returns the component to draw; it is evaluated each time the panel is drawn.
     *
     * @return an unmodifiable list of content functions
     */
    public List<IntBiFunction<Component>> getContent() {
        return content;
    }

    /**
     * Returns the styles that apply to the panel and everything inside it, in the order they were added.
     *
     * @return an unmodifiable list of styles
     */
    public List<Style> getStyles() {
        return styles;
    }

    /**
     * Returns whether the panel is currently shown, evaluated on each call.
     *
     * @return {@code true} when the panel is shown
     */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Returns the panel shown as a tooltip while the pointer is over this panel, evaluated on each call.
     *
     * @return the tooltip panel, or {@code null} when the panel has no tooltip
     */
    public Panel getTooltip() {
        return tooltip.get();
    }

    /**
     * Returns whether the panel's content may be scrolled, for example with the mouse wheel, when it does not fit in
     * the panel. Backends that lack the feature may ignore this setting.
     *
     * @return {@code true} when the panel is scrollable
     */
    public boolean isScrollable() {
        return scrollable;
    }

    static Supplier<Background> colorBackground(Supplier<Color> color) {
        return () -> {
            Color value = color.get();
            return value == null ? null : Background.color(value);
        };
    }

    /**
     * Returns a new builder for a panel with a fixed ID.
     *
     * @param id the panel ID, a stable key unique among the panel's siblings; must not be null
     * @return a new panel builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder panelBuilder(String id) {
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    /**
     * Returns a new builder for a panel whose ID is supplied by a function, evaluated each time the ID is read.
     *
     * @param id supplies the panel ID, which must stay stable and unique among the panel's siblings; must not be null
     * @return a new panel builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder panelBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    abstract static class BaseBuilder<PanelType extends Panel,
            BuilderType extends BaseBuilder<PanelType, BuilderType>> {
        private final Supplier<String> id;
        private IntBinaryOperator x = (_, _) -> 0;
        private IntBinaryOperator y = (_, _) -> 0;
        private IntBinaryOperator width;
        private IntBinaryOperator height;
        private Layout layout = Layout.VERTICAL;
        private Supplier<Background> background = () -> null;
        private Supplier<Boolean> open = () -> true;
        private final List<IntBiFunction<Component>> content = new ArrayList<>();
        private final List<Style> styles = new ArrayList<>();
        private Supplier<Panel> tooltip = () -> null;
        private boolean scrollable = true;

        BaseBuilder(Supplier<String> id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Sets a fixed position of the panel's top-left corner on the screen. The position is used for panels listed
         * directly in a screen's component tree and for popups; it defaults to (0, 0).
         *
         * @param x the horizontal position, in pixels from the left edge of the screen
         * @param y the vertical position, in pixels from the top edge of the screen
         * @return this builder
         */
        public BuilderType position(int x, int y) {
            return position((_, _) -> x, (_, _) -> y);
        }

        /**
         * Sets the position of the panel's top-left corner from suppliers, evaluated each time the panel is drawn.
         *
         * @param x supplies the horizontal position, in pixels from the left edge of the screen; must not be null
         * @param y supplies the vertical position, in pixels from the top edge of the screen; must not be null
         * @return this builder
         * @throws NullPointerException if {@code x} or {@code y} is {@code null}
         * @see #position(int, int)
         */
        public BuilderType position(IntSupplier x, IntSupplier y) {
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            return position((_, _) -> x.getAsInt(), (_, _) -> y.getAsInt());
        }

        /**
         * Sets the position of the panel's top-left corner as functions of the screen width and height, evaluated
         * each time the panel is drawn.
         *
         * @param x computes the horizontal position, in pixels from the left edge of the screen, from the screen
         *          width and height; must not be null
         * @param y computes the vertical position, in pixels from the top edge of the screen, from the screen width
         *          and height; must not be null
         * @return this builder
         * @throws NullPointerException if {@code x} or {@code y} is {@code null}
         * @see #position(int, int)
         */
        @SuppressWarnings("unchecked")
        public BuilderType position(IntBinaryOperator x, IntBinaryOperator y) {
            this.x = Objects.requireNonNull(x, "x");
            this.y = Objects.requireNonNull(y, "y");
            return (BuilderType) this;
        }

        /**
         * Sets a fixed size of the panel. The size is required.
         *
         * @param width  the width, in pixels
         * @param height the height, in pixels
         * @return this builder
         */
        public BuilderType size(int width, int height) {
            return size((_, _) -> width, (_, _) -> height);
        }

        /**
         * Sets the size of the panel from suppliers, evaluated each time the panel is drawn. The size is required.
         *
         * @param width  supplies the width, in pixels; must not be null
         * @param height supplies the height, in pixels; must not be null
         * @return this builder
         * @throws NullPointerException if {@code width} or {@code height} is {@code null}
         */
        public BuilderType size(IntSupplier width, IntSupplier height) {
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            return size((_, _) -> width.getAsInt(), (_, _) -> height.getAsInt());
        }

        /**
         * Sets the size of the panel as functions of the screen width and height, evaluated each time the panel is
         * drawn. The size is required.
         *
         * @param width  computes the width, in pixels, from the screen width and height; must not be null
         * @param height computes the height, in pixels, from the screen width and height; must not be null
         * @return this builder
         * @throws NullPointerException if {@code width} or {@code height} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType size(IntBinaryOperator width, IntBinaryOperator height) {
            this.width = Objects.requireNonNull(width, "width");
            this.height = Objects.requireNonNull(height, "height");
            return (BuilderType) this;
        }

        /**
         * Sets the direction in which the panel arranges its content. Defaults to {@link Layout#VERTICAL}.
         *
         * @param layout the layout; must not be null
         * @return this builder
         * @throws NullPointerException if {@code layout} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType layout(Layout layout) {
            this.layout = Objects.requireNonNull(layout, "layout");
            return (BuilderType) this;
        }

        /**
         * Sets an image background, replacing any background set before. By default a panel has no background.
         *
         * @param image     class-path resource name of the image, located as described in
         *                  {@link Background#image(String, ImageMode)}; must not be null
         * @param imageMode how the image is fitted to the panel; must not be null
         * @return this builder
         * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
         */
        public BuilderType backgroundImage(String image, ImageMode imageMode) {
            return background(Background.image(image, imageMode));
        }

        /**
         * Sets a solid color background, replacing any background set before. By default a panel has no background.
         *
         * @param backgroundColor the fill color, or {@code null} for no background
         * @return this builder
         */
        public BuilderType backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        /**
         * Sets a solid color background from a supplier, evaluated each time the panel is drawn, replacing any
         * background set before.
         *
         * @param backgroundColor supplies the fill color, or {@code null} for no background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code backgroundColor} is {@code null}
         */
        public BuilderType backgroundColor(Supplier<Color> backgroundColor) {
            return background(colorBackground(Objects.requireNonNull(backgroundColor, "backgroundColor")));
        }

        /**
         * Sets the background, replacing any background set before. By default a panel has no background.
         *
         * @param background the background, or {@code null} for no background
         * @return this builder
         */
        public BuilderType background(Background background) {
            return background(() -> background);
        }

        /**
         * Sets the background from a supplier, evaluated each time the panel is drawn, replacing any background set
         * before.
         *
         * @param background supplies the background, or {@code null} for no background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code background} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType background(Supplier<Background> background) {
            this.background = Objects.requireNonNull(background, "background");
            return (BuilderType) this;
        }

        /**
         * Sets whether the panel is shown, evaluated each time the panel is drawn. While the supplier returns
         * {@code false}, the panel and its content are hidden.
         *
         * <p>Panels and buttons are shown by default; a {@link ModalPopup} is shown while this supplier returns
         * {@code true} and defaults to hidden. A {@link Popup} is shown with {@link Popup#open()} instead and
         * reports its open state through {@link Popup.Builder#open(java.util.function.Consumer)}.
         *
         * @param open supplies whether the panel is shown; must not be null and must not return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code open} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType open(Supplier<Boolean> open) {
            this.open = Objects.requireNonNull(open, "open");
            return (BuilderType) this;
        }

        /**
         * Adds a component to the panel's content, after any content added before.
         *
         * @param component the component; must not be null
         * @return this builder
         * @throws NullPointerException if {@code component} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType addContent(Component component) {
            Objects.requireNonNull(component, "component");
            this.content.add((_, _) -> component);
            return (BuilderType) this;
        }

        /**
         * Adds a component supplied by a function to the panel's content, after any content added before. The
         * supplier is evaluated each time the panel is drawn, so the component can be replaced while the panel is
         * shown.
         *
         * @param component supplies the component; must not be null and must not return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code component} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType addContent(Supplier<Component> component) {
            Objects.requireNonNull(component, "component");
            this.content.add((_, _) -> component.get());
            return (BuilderType) this;
        }

        /**
         * Adds a component computed from the screen width and height to the panel's content, after any content
         * added before. The function is evaluated each time the panel is drawn.
         *
         * @param component computes the component from the screen width and height; must not be null and must not
         *                  return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code component} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType addContent(IntBiFunction<Component> component) {
            this.content.add(Objects.requireNonNull(component, "component"));
            return (BuilderType) this;
        }

        /**
         * Adds a style that applies to the panel and everything inside it. Styles are applied in the order they were
         * added. Style properties may be ignored by backends that lack the corresponding feature.
         *
         * @param style the style; must not be null
         * @return this builder
         * @throws NullPointerException if {@code style} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType addStyle(Style style) {
            this.styles.add(Objects.requireNonNull(style, "style"));
            return (BuilderType) this;
        }

        /**
         * Sets the panel shown as a tooltip while the pointer is over this panel. By default a panel has no tooltip.
         *
         * @param tooltip the tooltip panel, or {@code null} for no tooltip
         * @return this builder
         */
        public BuilderType tooltip(Panel tooltip) {
            return tooltip(() -> tooltip);
        }

        /**
         * Sets the tooltip panel from a supplier, evaluated each time the panel is drawn.
         *
         * @param tooltip supplies the tooltip panel, or {@code null} for no tooltip; must not be null
         * @return this builder
         * @throws NullPointerException if {@code tooltip} is {@code null}
         * @see #tooltip(Panel)
         */
        @SuppressWarnings("unchecked")
        public BuilderType tooltip(Supplier<Panel> tooltip) {
            this.tooltip = Objects.requireNonNull(tooltip, "tooltip");
            return (BuilderType) this;
        }

        /**
         * Sets whether the panel's content may be scrolled, for example with the mouse wheel, when it does not fit
         * in the panel. Defaults to {@code true}. Backends that lack the feature may ignore this setting.
         *
         * @param scrollable {@code true} to make the panel scrollable
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public BuilderType scrollable(boolean scrollable) {
            this.scrollable = scrollable;
            return (BuilderType) this;
        }

        /**
         * Builds the panel.
         *
         * @return a new panel
         * @throws NullPointerException if {@code size(...)} was never called
         */
        public abstract PanelType build();

        PanelType doBuild(PanelFactory<PanelType> factory) {
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            return factory.createNew(
                    id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable);
        }

        interface PanelFactory<PanelType extends Panel> {
            PanelType createNew(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                        IntBinaryOperator height, Layout layout, Supplier<Background> background,
                        Supplier<Boolean> open, List<IntBiFunction<Component>> content, List<Style> styles,
                        Supplier<Panel> tooltip, boolean scrollable);
        }
    }

    /**
     * Builds a {@link Panel}. Obtain one with {@link Panel#panelBuilder(String)} or
     * {@link Panel#panelBuilder(Supplier)}.
     *
     * <p>{@link #size(int, int) size(...)} is required and is checked by {@link #build()}. All other properties are
     * optional: the panel is placed at (0, 0), arranges its content vertically, has no background, no tooltip and
     * no styles, is shown and is scrollable.
     */
    public static class Builder extends BaseBuilder<Panel, Builder> {
        private Builder(Supplier<String> id) {
            super(id);
        }

        @Override
        public Panel build() {
            return doBuild(Panel::new);
        }
    }
}
