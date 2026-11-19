package com.ivan1pl.lorisui.api.component;

/**
 * A mouse button reported by the pointer events of a {@link Canvas}.
 * <p>
 * The declaration order ({@link #ordinal()}) is the order of preference when several buttons are pressed in the same
 * frame: the button declared first wins.
 */
public enum MouseButton {
    /**
     * The primary (usually left) mouse button.
     */
    LEFT,
    /**
     * The secondary (usually right) mouse button.
     */
    RIGHT,
    /**
     * The middle mouse button, usually the wheel when it is pressed.
     */
    MIDDLE
}
