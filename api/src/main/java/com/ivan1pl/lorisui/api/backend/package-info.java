/**
 * Provides the backend port: the interface through which a UI toolkit draws LorisUI components, and the
 * keyboard-navigation settings of the UI it draws.
 * <p>
 * A {@link com.ivan1pl.lorisui.api.backend.Backend} receives the component tree of the displayed screen each time
 * the host renders and draws it with its toolkit. It also holds the keyboard-navigation settings: whether Tab and
 * Shift+Tab move the keyboard focus, and which {@link com.ivan1pl.lorisui.api.backend.ActivationKey} activates the
 * focused element. Applications normally use a backend supplied by a host integration rather than implementing this
 * interface themselves.
 */
package com.ivan1pl.lorisui.api.backend;
