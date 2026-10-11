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
 * changing game state.
 *
 * <p>Sizes and positions are in logical window pixels: the window coordinates the host passes as the screen size.
 * Where the windowing system measures windows in points, as on macOS and Wayland, a logical window pixel spans as
 * many physical pixels as the display's scale factor; elsewhere it is one physical pixel. Texture and image-file
 * dimensions stay in image pixels; the drawn size of an
 * {@link com.ivan1pl.lorisui.api.component.Image} is in logical window pixels. Panel size functions
 * ({@link java.util.function.IntBinaryOperator}) and panel content functions
 * ({@link com.ivan1pl.lorisui.api.core.IntBiFunction}) receive the screen width and height, also for panels nested
 * in other panels, and so do the position functions of top-level panels and popups (a nested panel is placed by
 * its parent's layout). The tab-label functions of a {@link com.ivan1pl.lorisui.api.component.TabbedPanel} are the
 * exception: they receive the tab button's width and height and are called once, when the tabbed panel is built.
 *
 * <p>Null handling is uniform across builders: required arguments must not be null and are checked when the setter
 * is called, or in {@code build()} for required properties that have no default; setters taking a
 * {@code Supplier} reject {@code null}; constant overloads of optional properties accept {@code null} to keep the
 * toolkit default or, for backgrounds, to draw no background.
 */
package com.ivan1pl.lorisui.api.component;
