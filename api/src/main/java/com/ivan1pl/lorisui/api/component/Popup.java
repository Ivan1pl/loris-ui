package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

public class Popup extends Panel {
    private final Consumer<Boolean> openSetter;
    private boolean shouldOpen = false;

    protected Popup(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                    IntBinaryOperator height, Layout layout, Supplier<Background> background, Supplier<Boolean> open,
                    List<IntBiFunction<Component>> content, List<Style> styles, Supplier<Panel> tooltip,
                    boolean scrollable, Consumer<Boolean> openSetter) {
        super(id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable);
        this.openSetter = openSetter;
    }

    public Consumer<Boolean> getOpenSetter() {
        return openSetter;
    }

    public boolean isShouldOpen() {
        return shouldOpen;
    }

    public void setShouldOpen(boolean shouldOpen) {
        this.shouldOpen = shouldOpen;
    }

    public void open() {
        this.shouldOpen = true;
    }

    public static Builder popupBuilder(String id) {
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    public static Builder popupBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    /**
     * Builder for {@link Popup}. Required arguments are checked eagerly; Supplier setters reject null;
     * constant overloads of optional properties accept null meaning unset/toolkit default.
     * {@code size(...)} is required and checked at {@code build()}.
     */
    public static class Builder extends BaseBuilder<Popup, Builder> {
        private Consumer<Boolean> openSetter = _ -> {};

        private Builder(Supplier<String> id) {
            super(id);
            open(() -> false);
        }

        public Builder open(Consumer<Boolean> openSetter) {
            this.openSetter = Objects.requireNonNull(openSetter, "openSetter");
            return this;
        }

        @Override
        public Popup build() {
            return doBuild(((id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable) ->
                    new Popup(id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable,
                            openSetter)));
        }
    }
}
