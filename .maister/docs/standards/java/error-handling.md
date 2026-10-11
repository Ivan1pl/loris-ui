# Java Error Handling & Null Policy

### Exception Types by Situation
- Programming/lifecycle errors (missing renderer registration, registering fonts after initialisation, initializing or disposing the renderer inside a frame, switching screens from inside a screen hook): unchecked `IllegalStateException` with a descriptive message, thrown before any state change.
- Resource loading: declare and throw checked `IOException`, including the resource path in the message.
- Where `IOException` cannot propagate (render loops, framework callbacks): wrap it in `new RuntimeException(e)`.
- Close resources with try-with-resources; keep ImGui push/pop state balanced in `try/finally`.
- No custom exception classes unless callers need to handle them distinctly.

```java
throw new IllegalStateException("Missing renderer for class: " + component.getClass());
```

### Multi-Step Teardown
A release that consists of several steps (screen end hook, textures, platform and renderer objects, context, background work) runs every step even when an earlier one throws. Use `DeferredRelease.runAll(...)` (in `api.screen`) instead of hand-written loops:
- The first failure propagates after all steps have run; later failures are added to it with `addSuppressed`, never the same exception to itself. An `Error` propagates at once.
- `null` steps are rejected with `NullPointerException` before any step runs.
- Release only what was successfully set up. Keep a native or toolkit object in a field only after its initialization succeeded (create imgui-java `ImGuiImplSdl3`/`ImGuiImplGl3` objects fresh on each initialize, and shut down only the kept ones), and skip the release step of anything that is not there.
- A failed setup or frame runs its cleanup with `DeferredRelease.runAfterFailure(failure, cleanup)`, then rethrows the original exception; cleanup failures are added to it as suppressed.
- The object ends in a consistent state (uninitialized, fields cleared) whether or not a step failed.

```java
private void release() {
    DeferredRelease.runAll(
            screenManager::deactivate,
            renderer::dispose,
            LorisScreen::shutdownLayoutUpdates);
}

} catch (Throwable failure) {
    DeferredRelease.runAfterFailure(failure, this::release);
    throw failure;
}
```

### Deferred Teardown in Hosts
Teardown requested while a frame is being drawn or a screen hook is running (a "Hide UI" button, a hook that removes the display) is deferred to the end of the outermost operation, not rejected; the host framework (for example jME, which has already removed the processor when it calls `cleanup()`) could not recover from a rejected call. The low-level pieces still fail fast: the renderer's `initialize()`/`dispose()` and the backend's `unloadTextures()` throw `IllegalStateException` inside a frame, and `ScreenManager` rejects lifecycle calls from hooks. Hosts implement the deferral with one `DeferredRelease` built around their release method:
- Every display operation that can run user code (rendering a frame, a screen switch, the activate step of initialization) runs through `deferredRelease.run(...)`.
- A teardown request stops the display at once (it reports not initialized, stops consuming input), calls `screenManager.beginDeactivation()` and then `deferredRelease.request()`. With no operation running the release runs immediately; otherwise `isPending()` turns `true` and the release runs once, after the outermost operation.
- The release calls `screenManager.deactivate()` as its first step, which fires `onEndScreen()` on the screen that was started at the request. Screen switches made while the teardown is pending only record the choice; the recorded screen becomes current and starts on the next activation.
- If the outermost operation is already throwing, release failures are added to its exception as suppressed; otherwise they propagate from it.

### Input Validation
Malformed arguments that are not lifecycle errors throw `IllegalArgumentException` with the offending value or index in the message, checked at registration before anything is stored (for example glyph ranges with no range, no terminating `0`, a missing last code point or a first code point above the last). Data that is only rejected by the toolkit later (for example a font file that Dear ImGui cannot load at `initialize()`) throws `IllegalStateException` naming the source. Data on which the toolkit would fail a plain assertion, which ends the JVM, is rejected when it is read instead: font data of 100 bytes or less (Dear ImGui asserts `font_data_size > 100`) throws the registration's `IOException` naming the file or resource.

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
