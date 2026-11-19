package com.ivan1pl.lorisui.api.backend;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.util.List;

/**
 * Draws LorisUI components with a concrete UI toolkit and holds the keyboard-navigation settings of the UI it draws.
 * <p>
 * A backend is the port between the toolkit-agnostic component model and a rendering toolkit. Screens pass their
 * component tree to it through {@link com.ivan1pl.lorisui.api.screen.LorisScreen#render}, and host integrations
 * create the backend and drive rendering.
 * <p>
 * <b>Keyboard navigation.</b> While keyboard navigation is enabled, Tab moves the keyboard focus forward and
 * Shift+Tab moves it back through the enabled buttons, text inputs, number inputs and combo boxes, in the order they
 * are drawn, wrapping around at either end. Disabled buttons are skipped. While a popup is open or a combo box is
 * expanded, the cycle stays inside the innermost one of them, and Tab, Shift+Tab and the Up and Down arrow keys move
 * through the items of an expanded combo box. The {@linkplain #getActivationKey() activation key} presses the focused
 * button, opens the focused combo box and selects the focused combo box item. Escape closes an expanded combo box and
 * returns the focus to it; otherwise it clears the keyboard focus, so that the next Tab starts again from the first
 * element. Escape never closes a popup.
 * <p>
 * The navigation settings can be changed at any time on the host's UI thread and take effect from the next key event
 * or frame.
 */
public interface Backend {
    /**
     * Draws the given top-level components. Called on the host's UI thread each time the host renders, with the
     * screen size in logical window coordinates and the list most recently returned by
     * {@link com.ivan1pl.lorisui.api.screen.LorisScreen#layout() LorisScreen.layout()} (an empty list before the
     * first layout completes). Backends must not modify the list.
     *
     * @param w                 screen width in logical window coordinates
     * @param h                 screen height in logical window coordinates
     * @param components        top-level components to draw, in order; must not be null
     * @param bundleKeyResolver resolver for localized text; must not be null
     */
    void render(int w, int h, List<Component> components, BundleKeyResolver bundleKeyResolver);

    /**
     * Returns the key that activates the keyboard-focused element. The default is {@link ActivationKey#ENTER}.
     *
     * @return the activation key, never {@code null}
     */
    ActivationKey getActivationKey();

    /**
     * Sets the key that activates the keyboard-focused element: it presses a focused button, opens a focused combo box
     * with the focus on its selected item, and selects a focused combo box item, closing the combo box and returning
     * the focus to it. The key acts when it is pressed without Ctrl, Alt or Super, not when it repeats while held.
     *
     * <p>Can be called at any time on the host's UI thread; the new key takes effect from the next frame.
     *
     * @param activationKey key that activates the focused element; must not be null
     * @throws NullPointerException if {@code activationKey} is null
     */
    void setActivationKey(ActivationKey activationKey);

    /**
     * Returns whether keyboard navigation with Tab, Shift+Tab, the activation key and Escape is enabled. It is enabled
     * by default.
     *
     * @return {@code true} if keyboard navigation is enabled
     */
    boolean isKeyboardNavigationEnabled();

    /**
     * Enables or disables keyboard navigation with Tab, Shift+Tab, the {@linkplain #getActivationKey() activation key}
     * and Escape.
     *
     * <p>Keyboard navigation is enabled by default. While a button or combo box has the keyboard focus, a text or
     * number input is about to be edited after receiving it, or a combo box is expanded, the UI captures every key,
     * not only the navigation keys, so hosts keep those key events from the game. On screens where Tab or other keys
     * belong to the game, such as a gameplay HUD with buttons, disable keyboard navigation, so that a stray Tab does
     * not focus a button and take the game's keys until Escape or a click clears the focus.
     *
     * <p>Disabling it clears the keyboard focus from the next frame drawn while it is disabled. While it is disabled,
     * no element is focused or shows a focus look, Tab, Shift+Tab, the activation key and Escape have no navigation
     * meaning, and the UI does not capture them unless a text or number input is being edited. Tab never reaches the
     * toolkit's own navigation either way, and Ctrl+Tab never moves or highlights panels.
     *
     * <p>Can be called at any time on the host's UI thread; the change takes effect from the next key event or frame.
     *
     * @param enabled {@code true} to enable keyboard navigation, {@code false} to disable it
     */
    void setKeyboardNavigationEnabled(boolean enabled);
}
