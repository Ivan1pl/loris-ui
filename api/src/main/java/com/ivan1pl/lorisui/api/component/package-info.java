/**
 * Provides the components that make up a LorisUI screen: panels, buttons, popups, tabbed panels, text, images,
 * inputs and other widgets.
 *
 * <p>Every component implements the {@link com.ivan1pl.lorisui.api.component.Component} marker interface and is
 * created through a builder obtained from a static factory on its class, for example
 * {@link com.ivan1pl.lorisui.api.component.Panel#panelBuilder(String)} or
 * {@link com.ivan1pl.lorisui.api.component.Spacer#builder()}; the built-in components cannot be constructed
 * directly. A screen's component tree is a list of top-level components, usually panels; a panel lays out its
 * content horizontally or vertically ({@link com.ivan1pl.lorisui.api.component.Layout}).
 *
 * <p>Properties that may change while the UI is shown (text, colors, sizes, positions, visibility, content) can be
 * given either as constants or as functions, such as {@link java.util.function.Supplier}s or functions of the
 * screen size. Functions are evaluated each time the component is drawn, so a component tree built once can follow
 * changing game state. Sizes and positions are in pixels.
 *
 * <p>Null handling is uniform across builders: required arguments must not be null and are checked when the setter
 * is called, or in {@code build()} for required properties that have no default; setters taking a
 * {@code Supplier} reject {@code null}; constant overloads of optional properties accept {@code null} to keep the
 * toolkit default or, for backgrounds, to draw no background.
 */
package com.ivan1pl.lorisui.api.component;
