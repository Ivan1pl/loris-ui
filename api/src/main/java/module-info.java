/**
 * Defines the toolkit-agnostic LorisUI API: a declarative component model for game menus and HUDs, screens that
 * build and draw component trees, and the backend port that renders them.
 * <p>
 * Applications describe their UI with the components and styles of
 * {@link com.ivan1pl.lorisui.api.component} and {@link com.ivan1pl.lorisui.api.component.style}, built through
 * fluent builders. Dynamic values (text, colors, sizes, visibility) are bound as functions that are evaluated each
 * time the UI is drawn, so a component tree stays valid while the game state changes.
 * <p>
 * A {@link com.ivan1pl.lorisui.api.screen.LorisScreen} produces the component tree of one screen and hands it to a
 * {@link com.ivan1pl.lorisui.api.backend.Backend} each time the host renders. The backend draws the components with
 * a concrete UI toolkit; a host integrates the backend into a game engine, drives rendering and input, and supplies
 * fonts and localized text through {@link com.ivan1pl.lorisui.api.resolver.BundleKeyResolver}.
 * <p>
 * This module has no dependencies outside the JDK and does no drawing itself.
 */
module lorisui.api {
    exports com.ivan1pl.lorisui.api.backend;
    exports com.ivan1pl.lorisui.api.component;
    exports com.ivan1pl.lorisui.api.component.style;
    exports com.ivan1pl.lorisui.api.core;
    exports com.ivan1pl.lorisui.api.font;
    exports com.ivan1pl.lorisui.api.resolver;
    exports com.ivan1pl.lorisui.api.screen;
}
