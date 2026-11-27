package com.ivan1pl.lorisui.api.component;

/**
 * A component that can take the keyboard focus and that applications can ask to take it.
 *
 * <p>The focus is the element that {@linkplain com.ivan1pl.lorisui.api.backend.Backend keyboard navigation} acts
 * on: Tab and Shift+Tab move it, and the activation key acts on it. Through this interface an application reads
 * whether a component has the focus with {@link #isFocused()} and requests the focus for it with
 * {@link #setRequestsFocus(boolean)}. Focus tracking and requests work whether keyboard navigation is enabled or not.
 *
 * <p>{@link Button}, {@link TextInput}, {@link NumberInput} and {@link ComboBox} implement this interface;
 * {@link Canvas} does not. Only these built-in components, drawn by the backend's built-in support for them, take
 * part: implementing {@code Focusable} on a custom component does not make it focusable.
 *
 * <p>All methods may be called from any thread, and none of them blocks. The backend clears a request once it has
 * granted or dropped it. A request made from another thread at that moment may be lost, and so may a renewed request
 * that gets the same {@link System#nanoTime()} value as the one being cleared. The backend clears requests on the
 * host's UI thread, so a request made on that thread can be lost only in the second way, which needs
 * {@code System.nanoTime()} to return the same value for it as for a request made before the current frame began.
 *
 * <p>Limitations:
 * <ul>
 *     <li>A component that is created anew for every frame never sees a request made on an older instance. Request
 *     the focus on the instance that is drawn.</li>
 *     <li>Draw a focusable component on one display only. A component drawn by two displays receives conflicting
 *     focus reports.</li>
 * </ul>
 */
public interface Focusable extends Component {
    /**
     * Returns whether the component currently accepts user input and can take the focus, evaluated on each call.
     * LorisUI's part of the call is thread-safe; a supplier given to the component's builder runs on the calling
     * thread.
     *
     * @return {@code true} when the component is enabled
     */
    boolean isEnabled();

    /**
     * Returns whether the component had the focus at the end of the most recently completed frame.
     *
     * <p>The focus comes from Tab, Shift+Tab or a granted {@linkplain #setRequestsFocus(boolean) request}. A combo box
     * also has the focus while it is on one of the items of its expanded list, and a text or number input also has it
     * while it is being edited, including after a mouse click. A click on a button or combo box does not give it the
     * focus; it only sets where the next Tab continues.
     *
     * <p>The value is {@code false} while the component is disabled, when it was not drawn in that frame and after
     * the host releases the UI, for example when its display is closed. It is updated once per completed frame, so
     * while the UI is being drawn it reports the previous frame. When the same instance is drawn more than once in a
     * frame, it is {@code true} if any occurrence has the focus.
     *
     * @return {@code true} if the component had the focus at the end of the most recently completed frame
     */
    boolean isFocused();

    /**
     * Records whether the component has the focus. Backends call this at the end of a frame in which the component's
     * focus changed.
     *
     * <p>Applications normally do not call it: it does not move the focus, which {@link #setRequestsFocus(boolean)}
     * does. A value set by an application is reported by {@link #isFocused()} until the component's focus next
     * changes.
     *
     * @param focused {@code true} if the component has the focus
     */
    void setFocused(boolean focused);

    /**
     * Returns whether a focus request made with {@link #setRequestsFocus(boolean) setRequestsFocus(true)} is pending.
     *
     * @return {@code true} while a request is pending
     */
    boolean requestsFocus();

    /**
     * Requests the focus for the component, or cancels a pending request.
     *
     * <p>{@code true} requests the focus and stamps the request with {@link System#nanoTime()}; it replaces and
     * renews a pending request. {@code false} only cancels a pending request; it never removes the focus.
     *
     * <p><b>When a request is judged.</b> A request is judged when the component is next drawn in a part of the UI
     * that takes input, so not inside a tooltip, for example. It is kept in either of these cases:
     * <ul>
     *     <li>it was made no earlier than the start of the last frame before that draw in which any button, input or
     *     combo box was drawn;</li>
     *     <li>no such frame has completed since the host last set up the UI.</li>
     * </ul>
     * Frames that draw none of these, for example while a screen's first layout is being built, do not count. A
     * request made in {@link com.ivan1pl.lorisui.api.screen.LorisScreen#onStartScreen()} therefore works on the
     * screen's first visit, as does a request for a component that appears in the next frame. A request for a
     * component that stays hidden while other buttons, inputs or combo boxes are drawn is dropped when the component
     * is drawn again.
     *
     * <p><b>When a request is granted.</b> A kept request is granted when the frame after that draw begins, if all of
     * these hold:
     * <ul>
     *     <li>that frame reports no mouse click;</li>
     *     <li>the component was enabled;</li>
     *     <li>it was inside the innermost open popup, or outside all popups when none was open;</li>
     *     <li>no combo box was expanded.</li>
     * </ul>
     * Keyboard navigation need not be enabled. The focus therefore appears one frame after the draw. A grant acts
     * like Tab reaching the component: a button runs its hover action, an input starts being edited, and a combo box
     * gets its focus look without expanding; an input that is being edited elsewhere stops being edited. Nothing
     * changes when the component already has the focus or is being edited.
     *
     * <p><b>Competing input.</b> Among several requests judged in the same frame, the last one drawn that can take
     * the focus wins, and the others are dropped. A request overrides Tab or Shift+Tab pressed in the frame in which
     * it is granted, and a mouse click in that frame drops it. With keyboard navigation enabled, Escape in that frame
     * clears the new focus, and the activation key acts on the newly focused button or combo box.
     *
     * <p><b>Dropped requests.</b> A judged request that is not granted is dropped; it is never kept for a later draw.
     * A kept request is dropped too when the frame in which it would be granted fails. A request for a component that
     * is not drawn stays pending, as {@link #requestsFocus()} reports, until the component is drawn again. Once a
     * request is granted or dropped, the backend clears it with {@code setRequestsFocus(false)}, but only while its
     * stamp is unchanged, so a request renewed in the meantime is kept, apart from the races the interface
     * description explains.
     *
     * @param requestsFocus {@code true} to request the focus, {@code false} to cancel a pending request
     */
    void setRequestsFocus(boolean requestsFocus);

    /**
     * Returns the {@link System#nanoTime()} value of the most recent {@code setRequestsFocus(true)} call. The value
     * is meaningful while {@link #requestsFocus()} is {@code true}, and is {@code 0} before the first request.
     * Backends call this to drop stale requests; applications normally do not.
     *
     * @return the time stamp of the most recent focus request, or {@code 0} if the focus was never requested
     */
    long getFocusRequestTime();
}
