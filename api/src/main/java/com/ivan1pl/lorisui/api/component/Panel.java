package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.core.IntBiFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A rectangular container that lays out other components horizontally or vertically, in the order they were added.
 *
 * <p>A panel listed directly in a screen's component tree, and every popup, is placed at its own position on the
 * screen. A panel nested in another panel is placed by its parent's {@link Layout}, and its position is not used.
 * The size and content functions of every panel, nested ones included, and the position functions of top-level
 * panels and popups receive the screen width and height, not the parent's size. A panel can also have a background,
 * a tooltip and styles that apply to the panel and everything inside it.
 *
 * <p>A panel's padding insets its content from each edge by that side's padding: top, bottom, left and right.
 * Children are not resized, and their size and content functions still receive the screen size. Content that does
 * not fit is clipped, or scrolls inside the padded area. When the left and right padding together reach the panel's
 * width, or the top and bottom padding together reach its height, the content is not shown, or only partly.
 *
 * <p>Content taller than the panel's content area scrolls vertically. An application reads how far it is scrolled
 * with {@link #getScrollPosition()} and requests a position with {@link #setScrollPosition(float)}. All scroll
 * position methods may be called from any thread, and none of them blocks. The following cases are accepted:
 * <ul>
 *     <li>A user who drags or clicks the panel's scrollbar while a request is being applied overrides the request,
 *     which is then dropped. The mouse wheel and keyboard scrolling of the panel have no effect while a request is
 *     being applied, which normally lasts one to three frames.</li>
 *     <li>A request that is never reached, because the content or the panel changes height in every frame, stays
 *     pending. It is applied each time the panel is drawn until a newer request replaces it or the user drags the
 *     scrollbar. A request renewed in every frame, for example by calling {@code setScrollPosition(1f)} in each
 *     frame to keep a log at the bottom, is never cleared either, because each renewal is a new request that is
 *     judged afresh. The panel then stays pinned at the requested position, and the mouse wheel and keyboard
 *     scrolling of it have no effect. Request the position once per content change instead.</li>
 *     <li>The backend clears a request once it is reached or dropped. A request made from another thread at that
 *     moment may be lost. The backend clears requests on the host's UI thread, so a request made on that thread is
 *     not lost this way.</li>
 *     <li>The position reported after a request may differ from the requested one by up to half a pixel of scroll
 *     offset, because the panel scrolls by whole pixels. On a very small scroll range the difference in position can
 *     be large: content that overflows the panel by less than half a pixel may report 0.0 for a request of 1.0.</li>
 * </ul>
 *
 * <p>Scroll position limitations:
 * <ul>
 *     <li>Draw a panel on one display only. A panel drawn by two displays receives conflicting scroll position
 *     reports.</li>
 *     <li>A panel that is created anew for every layout never sees a request made on an older instance. Request the
 *     position on the instance that is drawn.</li>
 *     <li>When the same instance is drawn more than once in a frame, the last draw is reported, and the first draw
 *     that can apply a request consumes it.</li>
 *     <li>Only the vertical scroll position is reported and can be requested.</li>
 * </ul>
 *
 * <p>Every panel has an ID. Panel IDs are stable keys that must be unique among siblings, that is among the panels
 * of the same parent or among the top-level panels of a screen. Backends use the ID to recognize the same panel
 * from one frame to the next, so it should not change while the panel is shown.
 *
 * <p>Create a panel with {@link #panelBuilder(String)} or {@link #panelBuilder(Supplier)}.
 */
public class Panel implements Component {
    private final Supplier<String> id;
    private final IntBinaryOperator x;
    private final IntBinaryOperator y;
    private final IntBinaryOperator width;
    private final IntBinaryOperator height;
    private final Layout layout;
    private final Supplier<Background> background;
    private final Supplier<Boolean> open;
    private final List<IntBiFunction<Component>> content;
    private final List<Style> styles;
    private final Supplier<Panel> tooltip;
    private final boolean scrollable;
    private final int paddingTop;
    private final int paddingBottom;
    private final int paddingLeft;
    private final int paddingRight;
    private final ScrollState scrollState = new ScrollState();

    Panel(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
          IntBinaryOperator height, Layout layout, Supplier<Background> background, Supplier<Boolean> open,
          List<IntBiFunction<Component>> content, List<Style> styles, Supplier<Panel> tooltip, boolean scrollable,
          int paddingTop, int paddingBottom, int paddingLeft, int paddingRight) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.layout = layout;
        this.background = background;
        this.open = open;
        this.content = Collections.unmodifiableList(content);
        this.styles = Collections.unmodifiableList(styles);
        this.tooltip = tooltip;
        this.scrollable = scrollable;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
    }

    /**
     * Returns the panel's ID, evaluated on each call. The ID is a stable key that is unique among the panel's
     * siblings.
     *
     * @return the panel ID
     */
    public String getId() {
        return id.get();
    }

    /**
     * Returns the horizontal position for the given screen size, evaluated on each call.
     *
     * @param w screen width
     * @param h screen height
     * @return the x coordinate, or 0 when no position was set
     */
    public int getX(int w, int h) {
        return x.applyAsInt(w, h);
    }

    /**
     * Returns the vertical position for the given screen size, evaluated on each call.
     *
     * @param w screen width
     * @param h screen height
     * @return the y coordinate, or 0 when no position was set
     */
    public int getY(int w, int h) {
        return y.applyAsInt(w, h);
    }

    /**
     * Returns the width for the given screen size, evaluated on each call.
     *
     * @param w screen width
     * @param h screen height
     * @return the panel width, in pixels
     */
    public int getWidth(int w, int h) {
        return width.applyAsInt(w, h);
    }

    /**
     * Returns the height for the given screen size, evaluated on each call.
     *
     * @param w screen width
     * @param h screen height
     * @return the panel height, in pixels
     */
    public int getHeight(int w, int h) {
        return height.applyAsInt(w, h);
    }

    /**
     * Returns the direction in which the panel arranges its content.
     *
     * @return the layout; never {@code null}
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Returns the panel's background, evaluated on each call.
     *
     * @return the background, or {@code null} when the panel has no background
     */
    public Background getBackground() {
        return background.get();
    }

    /**
     * Returns the panel's content in the order it was added. Each entry is a function that receives the screen width
     * and height and returns the component to draw; it is evaluated each time the panel is drawn.
     *
     * @return an unmodifiable list of content functions
     */
    public List<IntBiFunction<Component>> getContent() {
        return content;
    }

    /**
     * Returns the styles that apply to the panel and everything inside it, in the order they were added.
     *
     * @return an unmodifiable list of styles
     */
    public List<Style> getStyles() {
        return styles;
    }

    /**
     * Returns whether the panel is currently shown, evaluated on each call.
     *
     * @return {@code true} when the panel is shown
     */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Returns the panel shown as a tooltip while the pointer is over this panel, evaluated on each call.
     *
     * @return the tooltip panel, or {@code null} when the panel has no tooltip
     */
    public Panel getTooltip() {
        return tooltip.get();
    }

    /**
     * Returns whether the panel's content may be scrolled, for example with the mouse wheel, when it does not fit in
     * the panel. Backends that lack the feature may ignore this setting.
     *
     * @return {@code true} when the panel is scrollable
     */
    public boolean isScrollable() {
        return scrollable;
    }

    /**
     * Returns how far the panel's content is scrolled vertically: the scroll offset divided by the scroll range, from
     * 0.0 at the top to 1.0 at the bottom.
     *
     * <p>The value is updated each time the panel is drawn and is that of its most recent draw. It is 0.0 before the
     * panel is first drawn and while its content fits in the panel. While the panel is not drawn, for example while
     * it is closed, in a tab that is not selected or a popup that is not open, and after the host releases the UI, it
     * keeps its last value until the panel is drawn again.
     *
     * @return the scroll position, from 0.0 (top) to 1.0 (bottom)
     */
    public float getScrollPosition() {
        return scrollState.getPosition();
    }

    /**
     * Requests that the panel's content be scrolled vertically to the given position, from 0.0 at the top to 1.0 at
     * the bottom. The request is stamped with {@link System#nanoTime()} and replaces a pending one.
     *
     * <p><b>When a request is applied.</b> A request is applied the first time the panel is drawn after it was drawn
     * in each of the two frames before, because only then is the size of its content known. A panel that is already
     * shown therefore applies it the next time it is drawn, while a panel that appears, for example on a screen's
     * first visit or when its tab is selected, applies it in the third frame in which it is drawn. A frame that fails
     * before it reaches the panel keeps the request pending.
     *
     * <p><b>When a request is cleared.</b> The request stays pending, and is applied again each time the panel is
     * drawn, until the panel has reached the requested position for its content as drawn after the request. Content
     * changed before the request is therefore taken into account, and a panel whose content grows, or that gets
     * smaller, while the request is pending still ends at the requested position once its size settles. A request
     * made while the content fits in the panel, or for the position the panel is already at, is dropped without
     * effect once the panel has been drawn in two consecutive frames after it. {@link #getScrollPosition()} reports
     * the result from the frame in which the request is applied.
     *
     * <p>The request works whether or not the panel is {@linkplain #isScrollable() scrollable}. The backend clears a
     * request only while its stamp is unchanged, so a request renewed in the meantime is kept, apart from the race
     * that the {@linkplain Panel class description} explains. The class description also covers user scrolling while
     * a request is applied, requests that are never reached and the whole-pixel rounding of the result.
     *
     * @param position the position to scroll to, from 0.0 (top) to 1.0 (bottom)
     * @throws IllegalArgumentException if {@code position} is NaN or not in the range 0.0 to 1.0
     */
    public void setScrollPosition(float position) {
        checkScrollPosition(position);
        scrollState.request(position);
    }

    /**
     * Returns whether a request made with {@link #setScrollPosition(float)} is pending. Backends call this to apply
     * and clear requests; applications normally do not.
     *
     * @return {@code true} while a request is pending
     */
    public boolean requestsScrollPosition() {
        return scrollState.isRequested();
    }

    /**
     * Returns the position of the most recent {@link #setScrollPosition(float)} call. The value is meaningful while
     * {@link #requestsScrollPosition()} is {@code true}, and is 0.0 before the first request. Backends call this to
     * apply a pending request, after reading {@link #getScrollPositionRequestTime()}, so that the value belongs to
     * the request with that stamp or to a later one; applications normally do not.
     *
     * @return the requested position, from 0.0 (top) to 1.0 (bottom)
     */
    public float getRequestedScrollPosition() {
        return scrollState.getRequestedPosition();
    }

    /**
     * Returns the {@link System#nanoTime()} value of the most recent {@link #setScrollPosition(float)} call. The
     * value is meaningful while {@link #requestsScrollPosition()} is {@code true}, and is {@code 0} before the first
     * request. Backends call this to drop stale requests; applications normally do not.
     *
     * @return the time stamp of the most recent scroll position request, or {@code 0} if none was made
     */
    public long getScrollPositionRequestTime() {
        return scrollState.getRequestTime();
    }

    /**
     * Clears a pending scroll position request, keeping its position and stamp. Backends call this once a request is
     * reached or dropped; applications normally do not.
     */
    public void clearScrollPositionRequest() {
        scrollState.clear();
    }

    /**
     * Records the panel's current scroll position. Backends call this each time the panel is drawn.
     *
     * <p>Applications normally do not call it: it does not scroll the panel, which
     * {@link #setScrollPosition(float)} does. A value set by an application is reported by
     * {@link #getScrollPosition()} until the panel is next drawn.
     *
     * @param position the scroll position, from 0.0 (top) to 1.0 (bottom)
     * @throws IllegalArgumentException if {@code position} is NaN or not in the range 0.0 to 1.0
     */
    public void reportScrollPosition(float position) {
        checkScrollPosition(position);
        scrollState.report(position);
    }

    /**
     * Returns the padding between the panel's top edge and its content, in logical pixels.
     *
     * @return the top padding; 0 when no padding was set
     */
    public int getPaddingTop() {
        return paddingTop;
    }

    /**
     * Returns the padding between the panel's bottom edge and its content, in logical pixels.
     *
     * @return the bottom padding; 0 when no padding was set
     */
    public int getPaddingBottom() {
        return paddingBottom;
    }

    /**
     * Returns the padding between the panel's left edge and its content, in logical pixels.
     *
     * @return the left padding; 0 when no padding was set
     */
    public int getPaddingLeft() {
        return paddingLeft;
    }

    /**
     * Returns the padding between the panel's right edge and its content, in logical pixels.
     *
     * @return the right padding; 0 when no padding was set
     */
    public int getPaddingRight() {
        return paddingRight;
    }

    private static void checkScrollPosition(float position) {
        if (!(position >= 0f && position <= 1f)) {
            throw new IllegalArgumentException("Scroll position " + position + " is not in the range 0.0 to 1.0");
        }
    }

    static Supplier<Background> colorBackground(Supplier<Color> color) {
        return () -> {
            Color value = color.get();
            return value == null ? null : Background.color(value);
        };
    }

    /**
     * Returns a new builder for a panel with a fixed ID.
     *
     * @param id the panel ID, a stable key unique among the panel's siblings; must not be null
     * @return a new panel builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder panelBuilder(String id) {
        Objects.requireNonNull(id, "id");
        return new Builder(() -> id);
    }

    /**
     * Returns a new builder for a panel whose ID is supplied by a function, evaluated each time the ID is read.
     *
     * @param id supplies the panel ID, which must stay stable and unique among the panel's siblings; must not be null
     * @return a new panel builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder panelBuilder(Supplier<String> id) {
        return new Builder(id);
    }

    abstract static class BaseBuilder<PanelType extends Panel,
            BuilderType extends BaseBuilder<PanelType, BuilderType>> {
        private final Supplier<String> id;
        private IntBinaryOperator x = (_, _) -> 0;
        private IntBinaryOperator y = (_, _) -> 0;
        private IntBinaryOperator width;
        private IntBinaryOperator height;
        private Layout layout = Layout.VERTICAL;
        private Supplier<Background> background = () -> null;
        private Supplier<Boolean> open = () -> true;
        private final List<IntBiFunction<Component>> content = new ArrayList<>();
        private final List<Style> styles = new ArrayList<>();
        private Supplier<Panel> tooltip = () -> null;
        private boolean scrollable = true;
        private int paddingTop = 0;
        private int paddingBottom = 0;
        private int paddingLeft = 0;
        private int paddingRight = 0;

        BaseBuilder(Supplier<String> id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Sets a fixed position of the panel's top-left corner on the screen. The position is used for panels listed
         * directly in a screen's component tree and for popups; it defaults to (0, 0).
         *
         * @param x the horizontal position, in pixels from the left edge of the screen
         * @param y the vertical position, in pixels from the top edge of the screen
         * @return this builder
         */
        public BuilderType position(int x, int y) {
            return position((_, _) -> x, (_, _) -> y);
        }

        /**
         * Sets the position of the panel's top-left corner from suppliers, evaluated each time the panel is drawn.
         *
         * @param x supplies the horizontal position, in pixels from the left edge of the screen; must not be null
         * @param y supplies the vertical position, in pixels from the top edge of the screen; must not be null
         * @return this builder
         * @throws NullPointerException if {@code x} or {@code y} is {@code null}
         * @see #position(int, int)
         */
        public BuilderType position(IntSupplier x, IntSupplier y) {
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            return position((_, _) -> x.getAsInt(), (_, _) -> y.getAsInt());
        }

        /**
         * Sets the position of the panel's top-left corner as functions of the screen width and height, evaluated
         * each time the panel is drawn.
         *
         * @param x computes the horizontal position, in pixels from the left edge of the screen, from the screen
         *          width and height; must not be null
         * @param y computes the vertical position, in pixels from the top edge of the screen, from the screen width
         *          and height; must not be null
         * @return this builder
         * @throws NullPointerException if {@code x} or {@code y} is {@code null}
         * @see #position(int, int)
         */
        @SuppressWarnings("unchecked")
        public BuilderType position(IntBinaryOperator x, IntBinaryOperator y) {
            this.x = Objects.requireNonNull(x, "x");
            this.y = Objects.requireNonNull(y, "y");
            return (BuilderType) this;
        }

        /**
         * Sets a fixed size of the panel. The size is required.
         *
         * @param width  the width, in pixels
         * @param height the height, in pixels
         * @return this builder
         */
        public BuilderType size(int width, int height) {
            return size((_, _) -> width, (_, _) -> height);
        }

        /**
         * Sets the size of the panel from suppliers, evaluated each time the panel is drawn. The size is required.
         *
         * @param width  supplies the width, in pixels; must not be null
         * @param height supplies the height, in pixels; must not be null
         * @return this builder
         * @throws NullPointerException if {@code width} or {@code height} is {@code null}
         */
        public BuilderType size(IntSupplier width, IntSupplier height) {
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            return size((_, _) -> width.getAsInt(), (_, _) -> height.getAsInt());
        }

        /**
         * Sets the size of the panel as functions of the screen width and height, evaluated each time the panel is
         * drawn. The size is required.
         *
         * @param width  computes the width, in pixels, from the screen width and height; must not be null
         * @param height computes the height, in pixels, from the screen width and height; must not be null
         * @return this builder
         * @throws NullPointerException if {@code width} or {@code height} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType size(IntBinaryOperator width, IntBinaryOperator height) {
            this.width = Objects.requireNonNull(width, "width");
            this.height = Objects.requireNonNull(height, "height");
            return (BuilderType) this;
        }

        /**
         * Sets the direction in which the panel arranges its content. Defaults to {@link Layout#VERTICAL}.
         *
         * @param layout the layout; must not be null
         * @return this builder
         * @throws NullPointerException if {@code layout} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType layout(Layout layout) {
            this.layout = Objects.requireNonNull(layout, "layout");
            return (BuilderType) this;
        }

        /**
         * Sets an image background, replacing any background set before. By default a panel has no background.
         *
         * @param image     class-path resource name of the image, located as described in
         *                  {@link Background#image(String, ImageMode)}; must not be null
         * @param imageMode how the image is fitted to the panel; must not be null
         * @return this builder
         * @throws NullPointerException if {@code image} or {@code imageMode} is {@code null}
         */
        public BuilderType backgroundImage(String image, ImageMode imageMode) {
            return background(Background.image(image, imageMode));
        }

        /**
         * Sets a solid color background, replacing any background set before. By default a panel has no background.
         *
         * @param backgroundColor the fill color, or {@code null} for no background
         * @return this builder
         */
        public BuilderType backgroundColor(Color backgroundColor) {
            return backgroundColor(() -> backgroundColor);
        }

        /**
         * Sets a solid color background from a supplier, evaluated each time the panel is drawn, replacing any
         * background set before.
         *
         * @param backgroundColor supplies the fill color, or {@code null} for no background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code backgroundColor} is {@code null}
         */
        public BuilderType backgroundColor(Supplier<Color> backgroundColor) {
            return background(colorBackground(Objects.requireNonNull(backgroundColor, "backgroundColor")));
        }

        /**
         * Sets the background, replacing any background set before. By default a panel has no background.
         *
         * @param background the background, or {@code null} for no background
         * @return this builder
         */
        public BuilderType background(Background background) {
            return background(() -> background);
        }

        /**
         * Sets the background from a supplier, evaluated each time the panel is drawn, replacing any background set
         * before.
         *
         * @param background supplies the background, or {@code null} for no background; must not be null
         * @return this builder
         * @throws NullPointerException if {@code background} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType background(Supplier<Background> background) {
            this.background = Objects.requireNonNull(background, "background");
            return (BuilderType) this;
        }

        /**
         * Sets whether the panel is shown, evaluated each time the panel is drawn. While the supplier returns
         * {@code false}, the panel and its content are hidden.
         *
         * <p>Panels and buttons are shown by default; a {@link ModalPopup} is shown while this supplier returns
         * {@code true} and defaults to hidden. A {@link Popup} is shown with {@link Popup#open()} instead and
         * reports its open state through {@link Popup.Builder#open(java.util.function.Consumer)}.
         *
         * @param open supplies whether the panel is shown; must not be null and must not return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code open} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType open(Supplier<Boolean> open) {
            this.open = Objects.requireNonNull(open, "open");
            return (BuilderType) this;
        }

        /**
         * Adds a component to the panel's content, after any content added before.
         *
         * @param component the component; must not be null
         * @return this builder
         * @throws NullPointerException if {@code component} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType addContent(Component component) {
            Objects.requireNonNull(component, "component");
            this.content.add((_, _) -> component);
            return (BuilderType) this;
        }

        /**
         * Adds a component supplied by a function to the panel's content, after any content added before. The
         * supplier is evaluated each time the panel is drawn, so the component can be replaced while the panel is
         * shown.
         *
         * @param component supplies the component; must not be null and must not return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code component} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType addContent(Supplier<Component> component) {
            Objects.requireNonNull(component, "component");
            this.content.add((_, _) -> component.get());
            return (BuilderType) this;
        }

        /**
         * Adds a component computed from the screen width and height to the panel's content, after any content
         * added before. The function is evaluated each time the panel is drawn.
         *
         * @param component computes the component from the screen width and height; must not be null and must not
         *                  return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code component} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType addContent(IntBiFunction<Component> component) {
            this.content.add(Objects.requireNonNull(component, "component"));
            return (BuilderType) this;
        }

        /**
         * Adds a style that applies to the panel and everything inside it. Styles are applied in the order they were
         * added. Style properties may be ignored by backends that lack the corresponding feature.
         *
         * @param style the style; must not be null
         * @return this builder
         * @throws NullPointerException if {@code style} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public BuilderType addStyle(Style style) {
            this.styles.add(Objects.requireNonNull(style, "style"));
            return (BuilderType) this;
        }

        /**
         * Sets the panel shown as a tooltip while the pointer is over this panel. By default a panel has no tooltip.
         *
         * @param tooltip the tooltip panel, or {@code null} for no tooltip
         * @return this builder
         */
        public BuilderType tooltip(Panel tooltip) {
            return tooltip(() -> tooltip);
        }

        /**
         * Sets the tooltip panel from a supplier, evaluated each time the panel is drawn.
         *
         * @param tooltip supplies the tooltip panel, or {@code null} for no tooltip; must not be null
         * @return this builder
         * @throws NullPointerException if {@code tooltip} is {@code null}
         * @see #tooltip(Panel)
         */
        @SuppressWarnings("unchecked")
        public BuilderType tooltip(Supplier<Panel> tooltip) {
            this.tooltip = Objects.requireNonNull(tooltip, "tooltip");
            return (BuilderType) this;
        }

        /**
         * Sets whether the panel's content may be scrolled, for example with the mouse wheel, when it does not fit
         * in the panel. Defaults to {@code true}. Backends that lack the feature may ignore this setting. It does not
         * affect {@link Panel#setScrollPosition(float)}.
         *
         * @param scrollable {@code true} to make the panel scrollable
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public BuilderType scrollable(boolean scrollable) {
            this.scrollable = scrollable;
            return (BuilderType) this;
        }

        /**
         * Sets the padding between the panel's edges and its content, in logical pixels, on all four sides. Each
         * side defaults to 0. Padding setters apply in the order they are called: this setter replaces the padding
         * of every side set before, and a later per-side setter such as {@link #paddingLeft(int)} replaces only its
         * own side.
         *
         * @param padding the padding of every side, in logical pixels; must not be negative
         * @return this builder
         * @throws IllegalArgumentException if {@code padding} is negative
         * @see #padding(int, int, int, int)
         */
        @SuppressWarnings("unchecked")
        public BuilderType padding(int padding) {
            requireNonNegative(padding, "Padding");
            this.paddingTop = padding;
            this.paddingBottom = padding;
            this.paddingLeft = padding;
            this.paddingRight = padding;
            return (BuilderType) this;
        }

        /**
         * Sets the padding between the panel's edges and its content, in logical pixels, for each side separately,
         * in clockwise order starting at the top, like {@link #paddingTop(int)}, {@link #paddingRight(int)},
         * {@link #paddingBottom(int)} and {@link #paddingLeft(int)} called in that order. Each side defaults to 0.
         * This setter replaces the padding of every side set before. All four values are checked before any is
         * stored, so a rejected call changes no side.
         *
         * @param paddingTop the top padding, in logical pixels; must not be negative
         * @param paddingRight the right padding, in logical pixels; must not be negative
         * @param paddingBottom the bottom padding, in logical pixels; must not be negative
         * @param paddingLeft the left padding, in logical pixels; must not be negative
         * @return this builder
         * @throws IllegalArgumentException if any of the values is negative; the first negative value in clockwise
         *         order from the top is reported
         */
        public BuilderType padding(int paddingTop, int paddingRight, int paddingBottom, int paddingLeft) {
            requireNonNegative(paddingTop, "Top padding");
            requireNonNegative(paddingRight, "Right padding");
            requireNonNegative(paddingBottom, "Bottom padding");
            requireNonNegative(paddingLeft, "Left padding");
            paddingTop(paddingTop);
            paddingRight(paddingRight);
            paddingBottom(paddingBottom);
            return paddingLeft(paddingLeft);
        }

        /**
         * Sets the padding between the panel's top edge and its content, in logical pixels. Defaults to 0. This
         * setter replaces only the top padding; a later {@link #padding(int)} or
         * {@link #padding(int, int, int, int)} call replaces it again.
         *
         * @param paddingTop the top padding, in logical pixels; must not be negative
         * @return this builder
         * @throws IllegalArgumentException if {@code paddingTop} is negative
         * @see #padding(int)
         * @see #padding(int, int, int, int)
         */
        @SuppressWarnings("unchecked")
        public BuilderType paddingTop(int paddingTop) {
            requireNonNegative(paddingTop, "Top padding");
            this.paddingTop = paddingTop;
            return (BuilderType) this;
        }

        /**
         * Sets the padding between the panel's bottom edge and its content, in logical pixels. Defaults to 0. This
         * setter replaces only the bottom padding; a later {@link #padding(int)} or
         * {@link #padding(int, int, int, int)} call replaces it again.
         *
         * @param paddingBottom the bottom padding, in logical pixels; must not be negative
         * @return this builder
         * @throws IllegalArgumentException if {@code paddingBottom} is negative
         * @see #padding(int)
         * @see #padding(int, int, int, int)
         */
        @SuppressWarnings("unchecked")
        public BuilderType paddingBottom(int paddingBottom) {
            requireNonNegative(paddingBottom, "Bottom padding");
            this.paddingBottom = paddingBottom;
            return (BuilderType) this;
        }

        /**
         * Sets the padding between the panel's left edge and its content, in logical pixels. Defaults to 0. This
         * setter replaces only the left padding; a later {@link #padding(int)} or
         * {@link #padding(int, int, int, int)} call replaces it again.
         *
         * @param paddingLeft the left padding, in logical pixels; must not be negative
         * @return this builder
         * @throws IllegalArgumentException if {@code paddingLeft} is negative
         * @see #padding(int)
         * @see #padding(int, int, int, int)
         */
        @SuppressWarnings("unchecked")
        public BuilderType paddingLeft(int paddingLeft) {
            requireNonNegative(paddingLeft, "Left padding");
            this.paddingLeft = paddingLeft;
            return (BuilderType) this;
        }

        /**
         * Sets the padding between the panel's right edge and its content, in logical pixels. Defaults to 0. This
         * setter replaces only the right padding; a later {@link #padding(int)} or
         * {@link #padding(int, int, int, int)} call replaces it again.
         *
         * @param paddingRight the right padding, in logical pixels; must not be negative
         * @return this builder
         * @throws IllegalArgumentException if {@code paddingRight} is negative
         * @see #padding(int)
         * @see #padding(int, int, int, int)
         */
        @SuppressWarnings("unchecked")
        public BuilderType paddingRight(int paddingRight) {
            requireNonNegative(paddingRight, "Right padding");
            this.paddingRight = paddingRight;
            return (BuilderType) this;
        }

        /**
         * Builds the panel.
         *
         * @return a new panel
         * @throws NullPointerException if {@code size(...)} was never called
         */
        public abstract PanelType build();

        PanelType doBuild(PanelFactory<PanelType> factory) {
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            return factory.createNew(id, x, y, width, height, layout, background, open, content, styles, tooltip,
                    scrollable, paddingTop, paddingBottom, paddingLeft, paddingRight);
        }

        private static void requireNonNegative(int value, String label) {
            if (value < 0) {
                throw new IllegalArgumentException(label + " must not be negative: " + value);
            }
        }

        interface PanelFactory<PanelType extends Panel> {
            PanelType createNew(Supplier<String> id, IntBinaryOperator x, IntBinaryOperator y, IntBinaryOperator width,
                                IntBinaryOperator height, Layout layout, Supplier<Background> background,
                                Supplier<Boolean> open, List<IntBiFunction<Component>> content, List<Style> styles,
                                Supplier<Panel> tooltip, boolean scrollable, int paddingTop, int paddingBottom,
                                int paddingLeft, int paddingRight);
        }
    }

    /**
     * Builds a {@link Panel}. Obtain one with {@link Panel#panelBuilder(String)} or
     * {@link Panel#panelBuilder(Supplier)}.
     *
     * <p>{@link #size(int, int) size(...)} is required and is checked by {@link #build()}. All other properties are
     * optional: the panel is placed at (0, 0), arranges its content vertically, has no background, no tooltip and
     * no styles, has no padding, is shown and is scrollable.
     */
    public static class Builder extends BaseBuilder<Panel, Builder> {
        private Builder(Supplier<String> id) {
            super(id);
        }

        @Override
        public Panel build() {
            return doBuild(Panel::new);
        }
    }
}
