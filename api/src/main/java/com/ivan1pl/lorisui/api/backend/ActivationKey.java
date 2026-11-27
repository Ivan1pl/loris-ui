package com.ivan1pl.lorisui.api.backend;

/**
 * Key that activates the keyboard-focused element: it presses a focused button, opens a focused combo box and selects
 * a focused combo box item. Set it with {@link Backend#setActivationKey}. Enter submits an edited text or number input
 * whatever this setting is, as described for
 * {@link com.ivan1pl.lorisui.api.component.TextInput.Builder#submitAction(Runnable)
 * TextInput.Builder.submitAction} and
 * {@link com.ivan1pl.lorisui.api.component.NumberInput.Builder#submitAction(Runnable)
 * NumberInput.Builder.submitAction}.
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
