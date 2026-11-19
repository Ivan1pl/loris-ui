package com.ivan1pl.lorisui.api.backend;

/**
 * Key that activates the keyboard-focused element: it presses a focused button, opens a focused combo box and selects
 * a focused combo box item. Set it with {@link Backend#setActivationKey}.
 */
public enum ActivationKey {
    /**
     * The Enter key, or Enter on the numeric keypad. This is the default activation key.
     */
    ENTER,
    /**
     * The Space key.
     */
    SPACE
}
