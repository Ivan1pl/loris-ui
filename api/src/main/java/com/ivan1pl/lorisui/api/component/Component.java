package com.ivan1pl.lorisui.api.component;

/**
 * Marks a UI element that can be placed in a component tree, for example as content of a {@link Panel}.
 *
 * <p>This interface declares no methods. Each backend knows how to draw the component types of this package; custom
 * component types also need support in the backend that draws them. The components that can take the keyboard focus
 * implement the {@link Focusable} subinterface; implementing it on a custom component type does not make that
 * component focusable.
 */
public interface Component {
}
