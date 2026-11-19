# Component & Style Builders

### Nested Static Builder per Component and Style
Every Component and Style has a nested `public static class Builder` and a non-public constructor; instances are created only through a static factory (`builder()`, or `panelBuilder(id)` / `buttonBuilder(id)` / `popupBuilder(id)` for id-based panels). The Builder has a private no-arg constructor, fields initialised to defaults, fluent setters named exactly after the property (no `with`/`set` prefix) returning `this`, and a terminal `build()` that passes all fields to the private constructor. Related values are grouped in one setter (`size(w, h)`, `value(getter, setter)`).

```java
public static Builder builder() {
    return new Builder();
}

public static class Builder {
    private Supplier<Float> rounding = () -> null;

    private Builder() {}

    public Builder size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public ProgressBar build() {
        return new ProgressBar(width, height, progress, backgroundColor, barColor, rounding);
    }
}
```

### Self-Typed BaseBuilder for Panel Subclasses
Panel-derived components (Button, Popup, ModalPopup) use `Panel.BaseBuilder<PanelType extends Panel, BuilderType extends BaseBuilder<PanelType, BuilderType>>`. `BaseBuilder` is package-private, as are its constructor, `doBuild(...)` and the nested `PanelFactory`, and `Panel`'s constructor; the Panel family is therefore closed to `api.component`. Its public setters still appear on, and chain from, each public `Builder`. Shared setters return `(BuilderType) this` under method-level `@SuppressWarnings("unchecked")`. Subclass builders are `public static class Builder extends BaseBuilder<Sub, Builder>`, add their own fields/setters, and implement `build()` via `doBuild(...)` with a constructor reference or lambda. Panel subclass constructors are private and take the full base parameter list first.

```java
public static class Builder extends BaseBuilder<ModalPopup, Builder> {
    @Override
    public ModalPopup build() {
        return doBuild(ModalPopup::new);
    }
}
```

### Dynamic Properties via Suppliers
Properties that may change at runtime are stored as functional types and evaluated at render time: `Supplier<T>` for values, `IntBinaryOperator` / `IntBiFunction` for values depending on the screen size `(w, h)` in logical window coordinates (always the screen's size, not the parent's, also for nested panels; renderers pass it unchanged to children), `Function<BundleKeyResolver, String>` for localisable text, `Consumer<T>` for two-way bindings. Builders offer an overload pair where the constant overload delegates to the functional overload; getters unwrap with `.get()` / `.applyAsInt(w, h)`.

```java
public Builder barColor(Color barColor) {
    return barColor(() -> barColor);
}

public Builder barColor(Supplier<Color> barColor) {
    this.barColor = Objects.requireNonNull(barColor, "barColor");
    return this;
}
```

### Event Callbacks and Trigger Methods
Event callbacks are `Runnable` properties (`Button`'s `action` and `hoverAction`) that default to a no-op `() -> {}` and are set with a single `requireNonNull` setter (no constant overload, no null-as-unset). The component exposes a public trigger method per event (`click()`, `hover()`) that runs the callback only when `isEnabled()` is true; backends call the trigger, never the raw `Runnable`, so the enabled gate lives in one place and applications can trigger the event themselves. Keyboard input reuses the same triggers instead of adding callbacks: a keyboard focus start counts as a hover start (`hover()`, at most once per frame even when the pointer hover starts in the same frame, and again when a focused button is enabled again), and the activation key on a focused element counts as a click (`click()`); the Javadoc of the callback setters and triggers names both the pointer and the keyboard events. The setter's Javadoc states that the callback runs on the host's UI thread and that an exception it throws propagates to the caller of the host's render call; the trigger's Javadoc states when backends call it and that applications may call it on the host's UI thread.

```java
// Button.Builder
private Runnable hoverAction = () -> {};

public Builder hoverAction(Runnable hoverAction) {
    this.hoverAction = Objects.requireNonNull(hoverAction, "hoverAction");
    return this;
}

// Button
public void hover() {
    if (isEnabled()) {
        hoverAction.run();
    }
}
```

### Callbacks with Arguments
An event that carries data (coordinates, a mouse button, a wheel delta) uses a nested `@FunctionalInterface` named `<Event>Action` instead of a `Runnable` (`Canvas.ClickAction`, `Canvas.DragAction`, `Canvas.WheelAction`), declared among the component's nested types before its records and the `Builder` (see `java/formatting.md` › Member Ordering), with one `void` abstract method named after the event that takes the event's values as plain parameters (`click(int x, int y, MouseButton button)`). As with `Runnable` callbacks, the builder field defaults to a no-op lambda with unnamed parameters (`(_, _, _) -> {}`) and is set with a single `requireNonNull` setter named after the property (`clickAction(ClickAction)`), with no constant overload and no null-as-unset. The component exposes a public trigger with the same name and parameters as the interface method, which checks object arguments with `requireNonNull` first (also while disabled) and then calls the action only when `isEnabled()` is true; backends call the trigger, never the action. Javadoc follows the event-callback rules above: the setter states that the callback runs on the host's UI thread, that its exceptions propagate to the caller of the host's render call and when it fires; the trigger states when backends call it, what each argument means (units, ranges, values that may lie outside the component) and that applications may call it on the host's UI thread; the interface and its method document every parameter.

```java
// Canvas
@FunctionalInterface
public interface ClickAction {
    void click(int x, int y, MouseButton button);
}

public void click(int x, int y, MouseButton button) {
    Objects.requireNonNull(button, "button");
    if (isEnabled()) {
        clickAction.click(x, y, button);
    }
}

// Canvas.Builder
private ClickAction clickAction = (_, _, _) -> {};

public Builder clickAction(ClickAction clickAction) {
    this.clickAction = Objects.requireNonNull(clickAction, "clickAction");
    return this;
}
```
