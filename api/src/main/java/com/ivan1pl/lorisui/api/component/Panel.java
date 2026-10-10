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

    protected Panel(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                    IntBinaryOperator height, Layout layout, Supplier<Background> background, Supplier<Boolean> open,
                    List<IntBiFunction<Component>> content, List<Style> styles, Supplier<Panel> tooltip,
                    boolean scrollable) {
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

    public int getWidth(int w, int h) {
        return width.applyAsInt(w, h);
    }

    public int getHeight(int w, int h) {
        return height.applyAsInt(w, h);
    }

    public Layout getLayout() {
        return layout;
    }

    public Background getBackground() {
        return background.get();
    }

    public List<IntBiFunction<Component>> getContent() {
        return content;
    }

    public List<Style> getStyles() {
        return styles;
    }

    public boolean isOpen() {
        return open.get();
    }

    public Panel getTooltip() {
        return tooltip.get();
    }

    public boolean isScrollable() {
        return scrollable;
    }

    static Supplier<Background> colorBackground(Supplier<Color> color) {
        return () -> {
            Color value = color.get();
            return value == null ? null : Background.color(value);
        };
    }

    public static Builder panelBuilder(String id) {
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    public static Builder panelBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    protected static abstract class BaseBuilder<T extends Panel, Builder extends BaseBuilder<T, Builder>> {
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

        protected BaseBuilder(Supplier<String> id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder position(int x, int y) {
            return position((_, _) -> x, (_, _) -> y);
        }

        public Builder position(IntSupplier x, IntSupplier y) {
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            return position((_, _) -> x.getAsInt(), (_, _) -> y.getAsInt());
        }

        @SuppressWarnings("unchecked")
        public Builder position(IntBinaryOperator x, IntBinaryOperator y) {
            this.x = Objects.requireNonNull(x, "x");
            this.y = Objects.requireNonNull(y, "y");
            return (Builder) this;
        }

        public Builder size(int width, int height) {
            return size((_, _) -> width, (_, _) -> height);
        }

        public Builder size(IntSupplier width, IntSupplier height) {
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            return size((_, _) -> width.getAsInt(), (_, _) -> height.getAsInt());
        }

        @SuppressWarnings("unchecked")
        public Builder size(IntBinaryOperator width, IntBinaryOperator height) {
            this.width = Objects.requireNonNull(width, "width");
            this.height = Objects.requireNonNull(height, "height");
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder layout(Layout layout) {
            this.layout = Objects.requireNonNull(layout, "layout");
            return (Builder) this;
        }

        public Builder backgroundImage(String image, ImageMode imageMode) {
            return background(Background.image(image, imageMode));
        }

        public Builder backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            return background(colorBackground(Objects.requireNonNull(backgroundColor, "backgroundColor")));
        }

        public Builder background(Background background) {
            return background(() -> background);
        }

        @SuppressWarnings("unchecked")
        public Builder background(Supplier<Background> background) {
            this.background = Objects.requireNonNull(background, "background");
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder open(Supplier<Boolean> open) {
            this.open = Objects.requireNonNull(open, "open");
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder addContent(Component component) {
            Objects.requireNonNull(component, "component");
            this.content.add((_, _) -> component);
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder addContent(Supplier<Component> component) {
            Objects.requireNonNull(component, "component");
            this.content.add((_, _) -> component.get());
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder addContent(IntBiFunction<Component> component) {
            this.content.add(Objects.requireNonNull(component, "component"));
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder addStyle(Style style) {
            this.styles.add(Objects.requireNonNull(style, "style"));
            return (Builder) this;
        }

        public Builder tooltip(Panel tooltip) {
            return tooltip(() -> tooltip);
        }

        @SuppressWarnings("unchecked")
        public Builder tooltip(Supplier<Panel> tooltip) {
            this.tooltip = Objects.requireNonNull(tooltip, "tooltip");
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder scrollable(boolean scrollable) {
            this.scrollable = scrollable;
            return (Builder) this;
        }

        /**
         * Builds the panel.
         *
         * @return a new panel
         * @throws NullPointerException if {@code size(...)} was never called
         */
        public abstract T build();

        protected T doBuild(PanelFactory<T> factory) {
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            return factory.createNew(
                    id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable);
        }

        protected interface PanelFactory<T extends Panel> {
            T createNew(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                        IntBinaryOperator height, Layout layout, Supplier<Background> background,
                        Supplier<Boolean> open, List<IntBiFunction<Component>> content, List<Style> styles,
                        Supplier<Panel> tooltip, boolean scrollable);
        }
    }

    /**
     * Builder for {@link Panel}. Required arguments are checked eagerly; Supplier setters reject null;
     * constant overloads of optional properties accept null meaning unset/toolkit default.
     * {@code size(...)} is required and checked at {@code build()}.
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
