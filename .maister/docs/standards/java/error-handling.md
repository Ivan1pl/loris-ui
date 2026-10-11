# Java Error Handling & Null Policy

### Exception Types by Situation
- Programming/lifecycle errors (missing renderer registration, registering fonts after initialisation): unchecked `IllegalStateException` with a descriptive message.
- Resource loading: declare and throw checked `IOException`, including the resource path in the message.
- Where `IOException` cannot propagate (render loops, framework callbacks): wrap it in `new RuntimeException(e)`.
- Close resources with try-with-resources; keep ImGui push/pop state balanced in `try/finally`.
- No custom exception classes unless callers need to handle them distinctly.

```java
throw new IllegalStateException("Missing renderer for class: " + component.getClass());
```

### Null Handling
`null` means "not configured / use toolkit default" for optional properties (style colours, backgrounds, fonts, tooltips). Components and styles store optional dynamic properties as a `Supplier` that defaults to `() -> null`, so the field itself is never `null`:
- **Supplier setters** reject `null` with `Objects.requireNonNull(x, "x")`. To unset a property, use the constant overload.
- **Constant overloads** of optional properties accept `null`, meaning unset or toolkit default, and delegate to the Supplier setter (`color(() -> value)`).
- **Getters** return the plain value with `x.get()`, with no null check on the field. The result may be `null` for optional properties, and the use site checks it (`if (value != null)` before pushing ImGui state).

Required arguments (ids, mandatory suppliers and callbacks, sizes) are validated with `Objects.requireNonNull(x, "x")` in builders and constructors, so misuse fails at build time.
- **Validate eagerly, before wrapping.** An overload that wraps its argument in a lambda checks the argument first. Otherwise a `null` would only fail later, on the render thread, inside the lambda.
- **Required bindings whose setter may be skipped** (no default value, for example the `valueGetter`/`valueSetter` of inputs, or `image`/`imageMode` of `Image`) are checked in `build()` with `Objects.requireNonNull(field, "field")`.
- Documented exceptions: a `Text` builder with no text set renders `""`, and a `null` constant background colour means no background.

Builder Javadoc documents this convention per builder and per setter:
- The builder class comment says what it builds, how to obtain it (the static factory) and which properties are required (for example `size(...)` for panels, the value bindings for inputs, `image(...)` for `Image`), with the defaults of the optional ones where relevant.
- Each setter states its null and validation behavior in `@param` / `@throws` (for example "must not be null" with `@throws NullPointerException`, or "or {@code null} to keep the toolkit default"); `build()` documents the exceptions for skipped required bindings.

No nullability annotations; avoid `Optional` fields and parameters.

```java
private Supplier<Color> backgroundColor = () -> null;

public Builder backgroundColor(Color backgroundColor) {
    return backgroundColor(() -> backgroundColor);
}

public Builder backgroundColor(Supplier<Color> backgroundColor) {
    this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
    return this;
}

public Builder position(IntSupplier x, IntSupplier y) {
    Objects.requireNonNull(x, "x");
    Objects.requireNonNull(y, "y");
    return position((_, _) -> x.getAsInt(), (_, _) -> y.getAsInt());
}

public Color getBackgroundColor() {
    return backgroundColor.get();
}
```
