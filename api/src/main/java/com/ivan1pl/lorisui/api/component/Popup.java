package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

/**
 * A panel shown on top of the rest of the UI on request, which closes when the user clicks outside it.
 *
 * <p>Call {@link #open()} to show the popup, for example from a {@link Button} action. A popup is always placed at
 * its own position on the screen, even when it is nested in another panel. To follow whether the popup is currently
 * shown, set a callback with {@link Popup.Builder#open(java.util.function.Consumer)}. For a popup that blocks the
 * rest of the UI and is shown while a condition holds, use {@link ModalPopup}.
 *
 * <p>Create a popup with {@link #popupBuilder(String)} or {@link #popupBuilder(Supplier)}. Its ID follows the same
 * rules as a {@link Panel} ID.
 */
public class Popup extends Panel {
    private final Consumer<Boolean> openSetter;
    private boolean shouldOpen = false;

    private Popup(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                  IntBinaryOperator height, Layout layout, Supplier<Background> background, Supplier<Boolean> open,
                  List<IntBiFunction<Component>> content, List<Style> styles, Supplier<Panel> tooltip,
                  boolean scrollable, Consumer<Boolean> openSetter) {
        super(id, x, y, width, height, layout, background, open, content, styles, tooltip, scrollable);
        this.openSetter = openSetter;
    }

    /**
     * Returns the callback that backends notify of the popup's open state, as set with
     * {@link Popup.Builder#open(Consumer)}.
     *
     * @return the open-state callback; never {@code null}
     */
    public Consumer<Boolean> getOpenSetter() {
        return openSetter;
    }

    /**
     * Returns whether the popup has been requested to open with {@link #open()} and the request has not been
     * handled yet.
     *
     * @return {@code true} while an open request is pending
     */
    public boolean isShouldOpen() {
        return shouldOpen;
    }

    /**
     * Sets or clears the pending open request. Backends clear the request once they have shown the popup;
     * applications normally call {@link #open()} instead.
     *
     * @param shouldOpen {@code true} to request that the popup be shown, {@code false} to clear the request
     */
    public void setShouldOpen(boolean shouldOpen) {
        this.shouldOpen = shouldOpen;
    }

    /**
     * Requests that the popup be shown. The popup appears the next time the UI is drawn and stays open until the
     * user closes it, for example by clicking outside it. Call this on the host's UI thread, for example from a
     * {@link Button} action.
     */
    public void open() {
        this.shouldOpen = true;
    }

    /**
     * Returns a new builder for a popup with a fixed ID.
     *
     * @param id the popup ID, a stable key unique among the popup's siblings; must not be null
     * @return a new popup builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder popupBuilder(String id) {
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    /**
     * Returns a new builder for a popup whose ID is supplied by a function, evaluated each time the ID is read.
     *
     * @param id supplies the popup ID, which must stay stable and unique among the popup's siblings; must not be
     *           null
     * @return a new popup builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder popupBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    /**
     * Builds a {@link Popup}. Obtain one with {@link Popup#popupBuilder(String)} or
     * {@link Popup#popupBuilder(Supplier)}.
     *
     * <p>{@link #size(int, int) size(...)} is required and is checked by {@link #build()}. All other properties are
     * optional and default as for a {@link Panel.Builder}, except that the inherited {@link #open(Supplier)} binding
     * defaults to {@code false}; no open-state callback is set by default. Show the built popup with
     * {@link Popup#open()}.
     */
    public static class Builder extends BaseBuilder<Popup, Builder> {
        private Consumer<Boolean> openSetter = _ -> {};

        private Builder(Supplier<String> id) {
            super(id);
            open(() -> false);
        }

        /**
         * Sets the callback notified when the popup's open state changes: it receives {@code true} while the popup
         * is shown and {@code false} once it has closed, for example because the user clicked outside it. The
         * callback runs on the host's UI thread and may be called repeatedly with the same value.
         *
         * <p>This overload does not show or hide the popup; use {@link Popup#open()} for that. It differs from the
         * inherited {@link #open(Supplier)}, which binds whether a panel is shown.
         *
         * @param openSetter receives the popup's open state; must not be null
         * @return this builder
         * @throws NullPointerException if {@code openSetter} is {@code null}
         */
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
