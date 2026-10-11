package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.List;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

/**
 * A popup that blocks interaction with the rest of the UI while it is shown.
 *
 * <p>A modal popup is shown while its open binding, set with the builder's {@link ModalPopup.Builder#open(Supplier)},
 * returns {@code true}, and hidden while it returns {@code false}; by default it is hidden. Like a {@link Popup}, it
 * is always placed at its own position on the screen, even when it is nested in another panel.
 *
 * <p>Create a modal popup with {@link #popupBuilder(String)}. Its ID follows the same rules as a {@link Panel} ID.
 */
public class ModalPopup extends Panel {
    private ModalPopup(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                       IntBinaryOperator height, Layout layout, Supplier<Background> background,
                       Supplier<Boolean> open, List<IntBiFunction<Component>> content, List<Style> styles,
                       Supplier<Panel> tooltip, boolean scrollable) {
        super(id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable);
    }

    /**
     * Returns a new builder for a modal popup with a fixed ID.
     *
     * @param id the modal popup ID, a stable key unique among the popup's siblings; must not be null
     * @return a new modal popup builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder popupBuilder(String id) {
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    /**
     * Builds a {@link ModalPopup}. Obtain one with {@link ModalPopup#popupBuilder(String)}.
     *
     * <p>{@link #size(int, int) size(...)} is required and is checked by {@link #build()}. Set when the popup is
     * shown with {@link #open(Supplier)}, which defaults to {@code false} (hidden). All other properties are
     * optional and default as for a {@link Panel.Builder}.
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
