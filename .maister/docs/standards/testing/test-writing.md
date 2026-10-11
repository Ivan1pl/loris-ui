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
Modules that use Mockito declare `mockito-core` (version from root `ext`) and load it as a Java agent, which avoids the JDK 21+ warning about dynamically loaded agents. The agent jar comes from a non-transitive `mockitoAgent` configuration, and a lazy argument provider resolves the path only when the test runs:

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

Use mocks for collaborators whose calls you verify (callbacks, `ImGuiBackend`). Prefer a small hand-written fake when it is simpler than stubbing, for example a recording `Backend`, which implements only `render`. Call `Assertions.*` and `Mockito.*` through the class name, since static imports aren't used (see `java/formatting.md`).

### No Native Toolkit Calls in Tests
Tests must not call `imgui.ImGui.*` or anything else that needs a native ImGui or GL context, because none exists in a unit test. Tests also must not reference imgui-java types at all: `imgui-java-app` is `compileOnly` in `loris-ui-imgui`, so it is absent from the test compile classpath. Test renderer-side logic through pure seams instead: the `ImGuiBackend` registry lookup, the texture cache via `Mockito.mock(ImGuiBackend.class)` (stubbing `loadTexture`), pixel masking on a plain `ByteBuffer`, and class-path resource lookup with a test resource.

### Shared Static State
`Renderer.TEXTURES` is a static cache shared by every test in the JVM. Tests that touch it use unique resource paths (for example `"textures/" + UUID.randomUUID() + ".png"`) so cache hits from other tests can't affect them.

### Float Assertions
Compare `float` results (colour channels, `Color.mask`) with a tolerance, `Assertions.assertEquals(expected, actual, 1e-6f)`, rather than exact equality. Channel values that went through 8-bit packing need a tolerance of at least `1 / 255f`, unless the expected value is itself written as `n / 255f`.

### Asynchronous Code
Code that runs on another thread (the `LorisScreen` layout executor) is tested by polling against a deadline, not by fixed sleeps, so tests stay fast and don't flake.
