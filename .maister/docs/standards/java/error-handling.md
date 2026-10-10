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
`null` means "not configured / use toolkit default" for optional properties (style colours, backgrounds, fonts, tooltips) and is checked at the use site: `x == null ? null : x.get()` in getters, `if (value != null)` before pushing ImGui state.
Required arguments (ids, mandatory suppliers/callbacks) are validated with `Objects.requireNonNull(x, "x")` in builders/constructors so misuse fails fast at build time.
No nullability annotations; avoid `Optional` fields and parameters.

```java
public Builder id(Supplier<String> id) {
    this.id = Objects.requireNonNull(id, "id");
    return this;
}
public Color getBackground() {
    return background == null ? null : background.get();
}
```
