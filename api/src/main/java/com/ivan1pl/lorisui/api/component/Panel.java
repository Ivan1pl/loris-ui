package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    public int getX(int w, int h) {
        return x.applyAsInt(w, h);
    }

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
        return background == null ? null : background.get();
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

    public static Builder panelBuilder(String id) {
        return new Builder(() -> id);
    }

    public static Builder panelBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    protected static abstract class BaseBuilder<T extends Panel, Builder extends BaseBuilder<T, Builder>> {
        private final Supplier<String> id;
        private IntBinaryOperator x;
        private IntBinaryOperator y;
        private IntBinaryOperator width;
        private IntBinaryOperator height;
        private Layout layout = Layout.VERTICAL;
        private Supplier<Background> background;
        private Supplier<Boolean> open = () -> true;
        private final List<IntBiFunction<Component>> content = new ArrayList<>();
        private final List<Style> styles = new ArrayList<>();
        private Supplier<Panel> tooltip = () -> null;
        private boolean scrollable = true;

        protected BaseBuilder(Supplier<String> id) {
            this.id = id;
        }

        public Builder position(int x, int y) {
            return position((_, _) -> x, (_, _) -> y);
        }

        public Builder position(IntSupplier x, IntSupplier y) {
            return position((_, _) -> x.getAsInt(), (_, _) -> y.getAsInt());
        }

        @SuppressWarnings("unchecked")
        public Builder position(IntBinaryOperator x, IntBinaryOperator y) {
            this.x = x;
            this.y = y;
            return (Builder) this;
        }

        public Builder size(int w, int h) {
            return size((_, _) -> w, (_, _) -> h);
        }

        public Builder size(IntSupplier w, IntSupplier h) {
            return size((_, _) -> w.getAsInt(), (_, _) -> h.getAsInt());
        }

        @SuppressWarnings("unchecked")
        public Builder size(IntBinaryOperator w, IntBinaryOperator h) {
            this.width = w;
            this.height = h;
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder layout(Layout layout) {
            this.layout = layout;
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder backgroundImage(String backgroundImage, ImageMode imageMode) {
            this.background = () -> Background.image(backgroundImage, imageMode);
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder backgroundColor(Color backgroundColor) {
            this.background = () -> Background.color(backgroundColor);
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder backgroundColor(Supplier<Color> backgroundColor) {
            this.background = () -> Background.color(backgroundColor.get());
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder background(Background background) {
            this.background = () -> background;
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder background(Supplier<Background> background) {
            this.background = background;
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder open(Supplier<Boolean> open) {
            this.open = open;
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder addContent(Component component) {
            this.content.add((_, _) -> component);
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder addContent(Supplier<Component> component) {
            this.content.add((_, _) -> component.get());
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder addContent(IntBiFunction<Component> component) {
            this.content.add(component);
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder addStyle(Style style) {
            this.styles.add(style);
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder tooltip(Panel tooltip) {
            this.tooltip = () -> tooltip;
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder tooltip(Supplier<Panel> tooltip) {
            this.tooltip = tooltip;
            return (Builder) this;
        }

        @SuppressWarnings("unchecked")
        public Builder scrollable(boolean scrollable) {
            this.scrollable = scrollable;
            return (Builder) this;
        }

        public abstract T build();

        protected T doBuild(PanelFactory<T> factory) {
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
