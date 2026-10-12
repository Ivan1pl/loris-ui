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
Event callbacks are `Runnable` properties (`Button`'s `action` and `hoverAction`) that default to a no-op `() -> {}` and are set with a single `requireNonNull` setter (no constant overload, no null-as-unset). The component exposes a public trigger method per event (`click()`, `hover()`) that runs the callback only when `isEnabled()` is true; backends call the trigger, never the raw `Runnable`, so the enabled gate lives in one place and applications can trigger the event themselves. The setter's Javadoc states that the callback runs on the host's UI thread and that an exception it throws propagates to the caller of the host's render call; the trigger's Javadoc states when backends call it and that applications may call it on the host's UI thread.

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
