# Java Error Handling & Null Policy

### Exception Types by Situation
- Programming/lifecycle errors (missing renderer registration, registering fonts after initialisation, initializing or disposing the renderer inside a frame, switching screens from inside a screen hook): unchecked `IllegalStateException` with a descriptive message, thrown before any state change.
- Resource loading: declare and throw checked `IOException`, including the resource path in the message.
- Where `IOException` cannot propagate (render loops, framework callbacks): wrap it in `new RuntimeException(e)`.
- Native handles that must not be `0` (a GLFW or SDL window, an SDL_GPU device or command buffer) throw `IllegalArgumentException` from the constructor or method, checked before any state check or native call. Malformed primitives and mismatched objects (a negative queue family index, an undefined format, fewer than one frame in flight, a queue or command buffer of another device) throw `IllegalArgumentException` too; `null` object arguments throw `NullPointerException`. A constructor whose object arguments cannot exist in a unit test (Vulkan objects: no `VkDevice` exists without a driver) checks its primitive arguments first, before those objects and before a `null` backend, so every check stays testable; other classes keep checking a `null` backend first.
- An object argument of a per-frame call (a `VkCommandBuffer`) is checked before the receiver's own state checks: `null` throws `NullPointerException`, then an object of another device throws `IllegalArgumentException`. A host that delegates the call to a renderer runs its own state checks (closed, not initialized) first and leaves these argument checks to the renderer instead of repeating them; a raw `0` handle that the host rejects itself (an SDL_GPU command buffer) is still checked before anything else.
- A failed native call that reports only a result code throws `IllegalStateException` naming the function, the result code and the object it was for. Build that message in one helper per graphics API from a label the caller passes; callers rethrow the exception unchanged instead of wrapping or rebuilding it (the Vulkan helper, message formats and labels are listed in `project/architecture.md` › `VulkanTextures`). GL failures include the GL info log (shader compile and link errors).
- Close resources with try-with-resources; keep ImGui push/pop state balanced in `try/finally`.
- No custom exception classes unless callers need to handle them distinctly.

```java
throw new IllegalStateException("Missing renderer for class: " + component.getClass());
```

### Multi-Step Teardown
A release that consists of several steps (screen end hook, textures, platform and renderer objects, context, background work) runs every step even when an earlier one throws. Use `DeferredRelease.runAll(...)` (in `api.screen`) instead of hand-written loops:
- The first failure propagates after all steps have run; later failures are added to it with `addSuppressed`, never the same exception to itself. An `Error` propagates at once.
- `null` steps are rejected with `NullPointerException` before any step runs.
- Release only what was successfully set up. Keep a native or toolkit object in a field only after its initialization succeeded (create every Impl object, imgui-java's `ImGuiImplSdl3`, `ImGuiImplGlfw`, `ImGuiImplGl3` and `ImGuiImplSdlGpu3` and LorisUI's ports `ImGuiImplGles3` and `ImGuiImplVulkan`, fresh on each initialize, and shut down only the kept ones), and skip the release step of anything that is not there. The documented exception is a renderer Impl whose failed `init` may already own native objects: a failed `ImGuiImplSdlGpu3.init` (shaders or the pipeline), `ImGuiImplGles3.init` (GL program, buffers, font texture) or `ImGuiImplVulkan.init` (pipeline, buffers, font atlas) is shut down on the spot with `DeferredRelease.runAfterFailure(failure, impl::shutdown)` and still not kept in the field; these `shutdown()` methods destroy only what exists, so they are safe after a partial `init`.
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

A host whose frame is recorded into a command buffer that the application submits after `render` returns must not let the deferred release destroy objects that buffer still uses. When it is closed during its own frame, it first makes the renderer record nothing for that frame, then requests the release as above; the release runs when `render` returns, and the application submits the buffer as usual. A close between `render` and the submit is documented as unsupported. The host's frame-in-progress state must survive a nested `render` that the renderer rejects, so restore its previous value instead of clearing it. The Vulkan display's protocol (`skipFrameDraw()`, its flag and the release order) is described in `project/architecture.md` › loris-ui-lwjgl.

A host whose input arrives through native callbacks (`ImGuiLwjglGlfwGl3Display` and GLFW) adds two rules, because no Java exception may cross into native code and GLFW forbids destroying cursors inside a callback:
- `close()` called during one of the display's callback dispatches throws `IllegalStateException` before any state change, so the release never runs inside a callback. The application requests the close there (for example with a flag) and calls `close()` after `glfwPollEvents` returns. Closing from a component callback during `render()` is still deferred as above.
- Each dispatch catches the first `Throwable`, from LorisUI or from the application's chained callback, and keeps it; later dispatches do nothing until it is rethrown. The next `render()` (before drawing) or `close()` (after closing, with a release failure added as suppressed) clears it and rethrows it: a `RuntimeException` or `Error` as-is, anything else wrapped in `new RuntimeException(failure)`.

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
