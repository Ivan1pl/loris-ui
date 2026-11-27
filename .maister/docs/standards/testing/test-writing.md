## Test Writing

### Test Behavior
Focus on what code does, not how it does it, to allow safe refactoring.

### Clear Names
Use descriptive names explaining what's tested and expected (`shouldReturnErrorWhenUserNotFound`).

### Mock External Dependencies
Isolate tests by mocking databases, APIs, and external services.

### Fast Execution
Keep unit tests fast (milliseconds) so developers run them frequently.

### Risk-Based Testing
Prioritize testing based on business criticality and likelihood of bugs.

### Balance Coverage and Velocity
Adjust test coverage based on project needs and team workflow.

### Critical Path Focus
Ensure core user workflows and critical business logic are well-tested.

### Appropriate Depth
Match edge case testing to the risk profile of the code.

### JUnit 6 Test Setup
Each test-bearing module declares the same JUnit test dependencies: `testImplementation platform('org.junit:junit-bom:6.0.0')`, `testImplementation 'org.junit.jupiter:junit-jupiter'` and `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`. The JUnit Platform is enabled once in root `allprojects` with `tasks.withType(Test).configureEach { useJUnitPlatform() }`; modules don't redeclare it.

Tests live in `<module>/src/test/java`, in the same package as the class under test (so package-private and `protected static` members are reachable). There is no test `module-info.java`; tests run on the classpath. A module without tests declares no JUnit dependencies.

### Mockito
Only `api` uses Mockito. A module that needs it declares `mockito-core` (version from root `ext`) and loads it as a Java agent, which avoids the JDK 21+ warning about dynamically loaded agents. The agent jar comes from a non-transitive `mockitoAgent` configuration, and a lazy argument provider resolves the path only when the test runs:

```groovy
configurations {
    mockitoAgent
}

dependencies {
    testImplementation "org.mockito:mockito-core:${rootProject.ext.mockitoVersion}"
    mockitoAgent("org.mockito:mockito-core:${rootProject.ext.mockitoVersion}") {
        transitive = false
    }
}

tasks.withType(Test).configureEach {
    FileCollection agent = configurations.mockitoAgent
    jvmArgumentProviders.add({ ["-javaagent:${agent.singleFile.absolutePath}"] } as CommandLineArgumentProvider)
}
```

Use mocks for collaborators whose calls you verify (e.g. callbacks such as button actions in `api`). Prefer a small hand-written fake when it is simpler than stubbing (e.g. a recording `Backend` or a recording `LorisScreen`). `ImGuiBackend` is not mocked: the behavior under test is its own `final` code, so tests use a hand-written subclass instead (see below). Modules whose tests need no mocks declare JUnit only; don't add the Mockito wiring until a test uses it. Call `Assertions.*` and `Mockito.*` through the class name, since static imports aren't used (see `java/formatting.md`).

### No Native Toolkit Calls in Tests
Tests must not call `imgui.ImGui.*` or anything else that needs a native ImGui or GL context, because none exists in a unit test. Tests also must not reference imgui-java, GLFW or SDL types at all: they are `compileOnly` in the library modules, so they are absent from the test compile classpath. The one exception is `lwjgl-vulkan`, which is on the test classpath where tested constructors or methods take Vulkan parameters; tests never create Vulkan objects (a `VkDevice` cannot exist without a Vulkan driver) and pass them only as `null` literals. Likewise jME is `compileOnly` in `jme3`, so `jme3` tests run without jME, and jME-related seams use no jME class beyond inlined compile-time constants. Test renderer-side and host logic through pure seams instead:
- **Native-free helpers** (cross-frame input trackers, keyboard focus, key filters, host key-release rules, teardown and screen lifecycle, pixel packing, tiling) are driven directly through their plain-value methods (ints, booleans, real `api` components) and asserted on their results and on the triggers they run, with counting callbacks or small hand-written fakes instead of Mockito (e.g. `ButtonInputTracker`, `KeyboardFocus` with `RecordingFocusable`, `DeliveredKeys`). Host teardown logic belongs in such seams, so it can be tested without a window.
- **Backend registry and textures**: a hand-written `ImGuiBackend` subclass implements the four texture hooks, recording what they receive (copying pixel bytes, because the backend reuses its scratch buffer); fakes that test nothing about textures implement the canvas hooks trivially. Each test creates its own backend, so the cache is never shared between tests. Drawing is simulated without Dear ImGui by registering a test component with a test renderer that runs a callback.
- **Native-free gating**: classes that check their preconditions before any native call (renderer variants, backends, displays) are tested for argument validation, calls before `initialize()`, `dispose()`/`close()` no-ops and calls after `close()`, with a fake backend and dummy non-zero handles. Such a test must fail or return before the code under test reaches Dear ImGui, OpenGL, GLFW or SDL, and the classes under test must not touch natives in static initializers; constructors may create static method references or lambdas that call natives (neither runs the native class's static initializer until invoked), while callback and Impl objects are created only in `initialize()`. Shared base behavior (`ImGuiRenderer`, `ImGuiLwjglDisplayBase`) is covered once, through one variant's tests.
- **Classes that cannot be constructed in a test** (constructors that take Vulkan objects): test only the checks reachable with `null` object arguments, that is the primitive arguments, which such classes check first, and then the `NullPointerException` for the first `null` object (order rule in `java/error-handling.md` › Exception Types by Situation). Everything behind a successful construction is an accepted gap, listed in `project/architecture.md` › Known Architectural Concerns.

What cannot be unit-tested (the native lifecycle of every variant, the native renderers such as `CanvasRenderer`, and the keyboard-navigation integration) is verified manually with the examples, following the procedures in `project/manual-testing.md`.

### Test Promised Argument-Check Order
When a method's or constructor's Javadoc promises an order of argument checks (window handle, then format, then frames in flight; primitives before objects so native-free tests can reach them), add a test that passes several invalid arguments at once and asserts which check fires, by exception type and the message (or a stable part of it). The order rule itself is in `java/error-handling.md` › Exception Types by Situation. One such test per promised order is enough; each argument check keeps its own test with only that argument invalid. Example: `ImGuiSdlVulkanRendererTest.shouldCheckWindowHandleThenFormatThenFramesInFlight`.

```java
IllegalArgumentException windowFirst = Assertions.assertThrows(IllegalArgumentException.class,
        () -> new ImGuiSdlVulkanRenderer(null, 0L, 0, 0));
IllegalArgumentException formatSecond = Assertions.assertThrows(IllegalArgumentException.class,
        () -> new ImGuiSdlVulkanRenderer(null, WINDOW_HANDLE, 0, 0));

Assertions.assertEquals("SDL window handle must not be 0", windowFirst.getMessage());
Assertions.assertEquals("Color attachment format must be a defined VkFormat: 0", formatSecond.getMessage());
```

### Float Assertions
Compare `float` results (colour channels, `Color.mask`) with a tolerance, `Assertions.assertEquals(expected, actual, 1e-6f)`, rather than exact equality. Channel values that went through 8-bit packing need a tolerance of at least `1 / 255f`, unless the expected value is itself written as `n / 255f`.

### Asynchronous Code
Code that runs on another thread (the `LorisScreen` layout worker) is tested by polling against a deadline, not by fixed sleeps, so tests stay fast and don't flake. Checks that a worker thread has ended join it with a timeout; a cross-thread visibility test (e.g. in `FocusStateTest`) gates its worker with a latch and joins it with a timeout.

When a test asserts on state observed *before* the worker finishes (for example that the first frame draws no components), block the worker on a `CountDownLatch` until that observation is made, then release it and poll; otherwise the worker may finish first and the test becomes flaky. Await the latch with the same timeout, so a broken test fails instead of hanging the layout thread.

```java
CountDownLatch firstRenderDone = new CountDownLatch(1);
TestScreen screen = new TestScreen(() -> {
    awaitUninterruptibly(firstRenderDone);
    return content;
});

screen.render(backend, 100, 100, RESOLVER);
firstRenderDone.countDown();
renderUntilRendered(screen, backend, content);
```
