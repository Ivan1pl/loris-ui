package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.List;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

public class ModalPopup extends Panel {
    protected ModalPopup(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                         IntBinaryOperator height, Layout layout, Supplier<Background> background,
                         Supplier<Boolean> open, List<IntBiFunction<Component>> content, List<Style> styles,
                         Supplier<Panel> tooltip, boolean scrollable) {
        super(id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable);
    }

    public static Builder popupBuilder(String id) {
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    /**
     * Builder for {@link ModalPopup}. Required arguments are checked eagerly; Supplier setters reject null;
     * constant overloads of optional properties accept null meaning unset/toolkit default.
     * {@code size(...)} is required and checked at {@code build()}.
     */
    public static class Builder extends BaseBuilder<ModalPopup, Builder> {
        private Builder(Supplier<String> id) {
            super(id);
            open(() -> false);
        }

        @Override
        public ModalPopup build() {
            return doBuild(ModalPopup::new);
        }
    }
}
