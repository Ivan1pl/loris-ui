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

Tests live in `<module>/src/test/java`, in the same package as the class under test (so package-private and `protected static` members are reachable). There is no test `module-info.java`; tests run on the classpath.

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

Use mocks for collaborators whose calls you verify (callbacks such as button actions in `api`). Prefer a small hand-written fake when it is simpler than stubbing, for example a recording `Backend`, which implements only `render`, or a recording `LorisScreen` that logs its lifecycle hooks. `ImGuiBackend` is not mocked: the behavior under test (registry, texture cache) is its own `final` code, so tests use a hand-written subclass instead (see below). Modules whose tests need no mocks (`imgui`, `lwjgl`) declare JUnit only; don't add the Mockito wiring until a test uses it. Call `Assertions.*` and `Mockito.*` through the class name, since static imports aren't used (see `java/formatting.md`).

### No Native Toolkit Calls in Tests
Tests must not call `imgui.ImGui.*` or anything else that needs a native ImGui or GL context, because none exists in a unit test. Tests also must not reference imgui-java types at all: `imgui-java-app` is `compileOnly` in `loris-ui-imgui`, so it is absent from the test compile classpath. Test renderer-side and host logic through pure seams instead:
- **Backend registry and textures**: a hand-written `ImGuiBackend` subclass (an anonymous one, or a private recording fake) implements `createTexture` (returning fresh `TextureData` and recording the normalized path) and `deleteTexture` (recording deletions), so tests cover lookup, registration, the per-backend texture cache, key normalization, `unloadTextures()` and its in-frame rejection without GL. Each test creates its own backend, so the cache is never shared between tests.
- **Pixel masking** on a plain `ByteBuffer`, and **class-path resource lookup** with a test resource.
- **Host-neutral `api` logic**: `ScreenManager` (switching, hook pairing, the re-entrancy guard, `beginDeactivation()` followed by `deactivate()`) with recording screens; `DeferredRelease` (immediate and deferred release, nested operations, failure merging in `run`, `runAll` and `runAfterFailure`) with plain `Runnable`s; and `ResourceBundleKeyResolver` with test bundles in `api/src/test/resources`. Host teardown logic belongs in these seams, so it can be tested without a window.
- **Glyph ranges**: `ImGuiFont.buildGlyphRanges` is package-private and uses imgui-java's pure-Java `ImFontGlyphRangesBuilder`, so tests check unsigned ranges above `U+7FFF` and the `IllegalArgumentException` validation without a context (the test calls only the LorisUI method, not imgui-java types).
- **Native-free gating**: `ImGuiOpenGlRenderer`, `ImGuiFont` and `ImGuiLwjglDisplay` check their preconditions before any native call, so tests cover argument validation, calls before `initialize()`, `dispose()`/`close()` no-ops and calls after `close()` with a fake backend and a dummy non-zero window handle. Such a test must fail or return before the code under test reaches Dear ImGui, OpenGL or SDL, and the classes under test must not touch natives in static initializers.

The native lifecycle itself (context creation and teardown, font replay, GL textures, re-attach) can't be unit-tested; it is verified by running the examples.

### Float Assertions
Compare `float` results (colour channels, `Color.mask`) with a tolerance, `Assertions.assertEquals(expected, actual, 1e-6f)`, rather than exact equality. Channel values that went through 8-bit packing need a tolerance of at least `1 / 255f`, unless the expected value is itself written as `n / 255f`.

### Asynchronous Code
Code that runs on another thread (the `LorisScreen` layout worker) is tested by polling against a deadline, not by fixed sleeps, so tests stay fast and don't flake. Checks that a worker thread has ended join it with a timeout.

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
