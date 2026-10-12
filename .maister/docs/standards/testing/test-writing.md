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

Tests live in `<module>/src/test/java`, in the same package as the class under test (so package-private and `protected static` members are reachable). There is no test `module-info.java`; tests run on the classpath. `api`, `imgui`, `imgui-vulkan`, `jme3` and `lwjgl` have tests; `imgui-gles3` and `examples` have none (a module without tests declares no JUnit dependencies).

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

Use mocks for collaborators whose calls you verify (callbacks such as button actions in `api`). Prefer a small hand-written fake when it is simpler than stubbing, for example a recording `Backend`, which implements only `render`, or a recording `LorisScreen` that logs its lifecycle hooks. `ImGuiBackend` is not mocked: the behavior under test (registry, texture cache) is its own `final` code, so tests use a hand-written subclass instead (see below). Modules whose tests need no mocks (`imgui`, `imgui-vulkan`, `jme3`, `lwjgl`) declare JUnit only; don't add the Mockito wiring until a test uses it. Call `Assertions.*` and `Mockito.*` through the class name, since static imports aren't used (see `java/formatting.md`).

### No Native Toolkit Calls in Tests
Tests must not call `imgui.ImGui.*` or anything else that needs a native ImGui or GL context, because none exists in a unit test. Tests also must not reference imgui-java, GLFW or SDL types at all: `imgui-java-app` and the LWJGL bindings are `compileOnly` in the library modules, so they are absent from the test compile classpath. The one exception is `lwjgl-vulkan`, which is on the test compile and runtime classpaths of `imgui`, `imgui-vulkan` and `lwjgl` (`testCompileOnly` + `testRuntimeOnly`), because `ImGuiVulkanBackend` and `ImGuiLwjglSdlVulkanDisplay` take Vulkan objects in their constructors and `VulkanTextures.upload` in its parameters. Tests never create Vulkan objects (a `VkDevice` cannot exist without a Vulkan driver) and pass them only as `null` literals; `lwjgl-opengles` is on the `imgui` test runtime classpath only, so the GLES classes under test link. Likewise jME is `compileOnly` in `jme3`, so `jme3` tests run without jME. Test renderer-side and host logic through pure seams instead:
- **Backend registry and textures**: a hand-written `ImGuiBackend` subclass (an anonymous one, or a private recording fake) implements `createTexture` (returning fresh `TextureData` and recording the normalized path) and `deleteTexture` (recording deletions), so tests cover lookup, registration, the per-backend texture cache, key normalization, `unloadTextures()` and its in-frame rejection without GL. Each test creates its own backend, so the cache is never shared between tests.
- **Pixel masking** on a plain `ByteBuffer`, and **class-path resource lookup** with a test resource.
- **Repeat tiling**: the pure `TextureTiling.spans(length, tileLength)` (exact fit, partial edge tile, an area smaller than one tile, empty results for non-positive lengths, `IllegalArgumentException` for a non-positive tile length).
- **Image decoding**: `DecodedImage.load` with a missing resource throws `IOException` naming the path before any STB call. The same test through `ImGuiOpenGlBackend.loadTexture` and `ImGuiOpenGlEsBackend.loadTexture`, with no GL context, proves that the GL backends decode before saving any GL state (LWJGL's capability lookup would otherwise throw `IllegalStateException`).
- **jME renderer choice**: the package-private `ImGuiJmeGraphicsApi.forRenderer(String)` (`"ANGLE_GLES3"`, `"LWJGL-OpenGL*"` values, an unsupported value named in the message, `null`). It references only an inlined jME `String` constant (`AppSettings.ANGLE_GLES3`), so it runs with jME absent from the test classpath; such seams use no jME class beyond compile-time constants.
- **Embedded data**: the SPIR-V words in `ImGuiImplVulkanShaders` (`imgui-vulkan`; length and the `0x07230203` magic number of each array), so a damaged copy of the upstream arrays fails fast.
- **Vulkan upload size guard**: the static `VulkanTextures.upload` (`imgui-vulkan`) rejects pixel data smaller than `width * height * 4` bytes with `IllegalArgumentException` (naming the label, the size and the byte count) before any Vulkan call, so the test passes `null` for the device and queue.
- **GLFW dispatch rules**: `ImGuiLwjglGlfwGl3Display` routes every callback through the package-private `dispatch(Runnable)` seam, which tests call on an uninitialized display: `close()` inside a dispatch is rejected and surfaces from the next `close()`, and a dispatch failure suppresses later dispatches and is rethrown exactly once from the next `render()`/`close()`.
- **Host-neutral `api` logic**: `ScreenManager` (switching, hook pairing, the re-entrancy guard, `beginDeactivation()` followed by `deactivate()`) with recording screens; `DeferredRelease` (immediate and deferred release, nested operations, failure merging in `run`, `runAll` and `runAfterFailure`) with plain `Runnable`s; and `ResourceBundleKeyResolver` with test bundles in `api/src/test/resources`. Host teardown logic belongs in these seams, so it can be tested without a window.
- **Glyph ranges**: `ImGuiFont.buildGlyphRanges` is package-private and uses imgui-java's pure-Java `ImFontGlyphRangesBuilder`, so tests check unsigned ranges above `U+7FFF` and the `IllegalArgumentException` validation without a context (the test calls only the LorisUI method, not imgui-java types).
- **Native-free gating**: the renderer variants (`ImGuiSdlGl3Renderer`, `ImGuiGlfwGl3Renderer`, `ImGuiSdlGles3Renderer`, `ImGuiSdlGpuRenderer`), `ImGuiSdlGpuBackend`, `ImGuiFont` and the LWJGL displays (`ImGuiLwjglSdlGl3Display`, `ImGuiLwjglGlfwGl3Display`, `ImGuiLwjglSdlGpuDisplay`) check their preconditions before any native call, so tests cover argument validation (`0` handles, a `0` command buffer before any state check), calls before `initialize()`, `dispose()`/`close()` no-ops and calls after `close()` with a fake backend and dummy non-zero window, device and command-buffer handles. Shared base behavior (`ImGuiRenderer`, `ImGuiLwjglDisplayBase`) is covered once, through the SDL3 + OpenGL 3 variant's tests. Such a test must fail or return before the code under test reaches Dear ImGui, OpenGL, GLFW or SDL, and the classes under test must not touch natives in static initializers. Constructors may create callback holders or static method references to `GLFW`/`SDL*` methods: a method reference loads the class without running its static initializer, so it stays native-free; callback objects and Impl objects are created only in `initialize()`.
- **Classes that cannot be constructed in a test** (constructors that take Vulkan objects: `ImGuiVulkanBackend`, `ImGuiSdlVulkanRenderer`, `ImGuiLwjglSdlVulkanDisplay`): test only the checks reachable with `null` object arguments, that is the primitive arguments, which such classes check first with `IllegalArgumentException`, and then the `NullPointerException` for the first `null` object. Their check order (primitives before objects) is the rule in `java/error-handling.md` › Exception Types by Situation; it keeps every check testable. Everything behind a successful construction (render check order, pre-initialize and close paths, in-frame teardown) is an accepted gap verified manually; the list is kept in `project/architecture.md` › Known Architectural Concerns. Shared base behavior they reuse stays covered by the SDL3 + OpenGL 3 tests.

The native lifecycle itself (context creation and teardown, font replay, GL and GLES textures and the GL state they restore, the GLES draw path under jME's ANGLE renderer, GPU texture uploads and releases, the SDL_GPU frame, the Vulkan pipeline, uploads, synchronization, swapchain recreation and teardown including the in-frame close, native GLFW callback chaining and the monitor-callback swap, re-attach) can't be unit-tested; it is verified by running the six examples, the Vulkan one also with `-Dlorisui.example.gpuDebug=true` for validation.

### Test Promised Argument-Check Order
When a method's or constructor's Javadoc promises an order of argument checks (window handle, then format, then frames in flight; primitives before objects so native-free tests can reach them), add a test that passes several invalid arguments at once and asserts which check fires, by exception type and the message (or a stable part of it). The order rule itself is in `java/error-handling.md` › Exception Types by Situation. One such test per promised order is enough; each argument check keeps its own test with only that argument invalid. Examples: `ImGuiLwjglSdlVulkanDisplayTest.shouldRejectZeroWindowHandleFirst`, `ImGuiSdlVulkanRendererTest.shouldCheckWindowHandleThenFormatThenFramesInFlight`.

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
