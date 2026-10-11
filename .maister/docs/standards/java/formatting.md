# Java Formatting

The style below is enforced by Checkstyle (`config/checkstyle/checkstyle.xml`, a failing gate in `./gradlew build`, see `build/gradle.md`) and mirrored in the repository's `.editorconfig`. There is no auto-formatter: write code in this style and fix what Checkstyle reports.

### Indentation, Braces and Line Length
4-space indentation (no tabs), same-line (K&R) braces, 120-column limit, 8-space continuation indent. Wrapped parameter lists in declarations align under the first parameter. Fluent builder chains put one call per line. Empty constructors are written inline as `{}`.

```java
private ProgressBar(int width, int height, Supplier<Float> progress, Supplier<Color> backgroundColor,
                    Supplier<Color> barColor, float rounding) {
    this.width = width;
}

private Builder() {}
```

### Import Layout
IntelliJ default layout: one sorted block of project and third-party imports, a blank line, then `java.*` imports. Within each block, imports are sorted in ASCII order, which is case-sensitive (`com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer` before `com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend`, because uppercase letters sort before lowercase). No static imports. Single-type imports are the default; wildcard imports are acceptable (for example `api.component.*` in `ImGuiBackend`) and need not be expanded.

```java
import com.ivan1pl.lorisui.api.component.TextInput;
import imgui.ImGui;

import java.util.Map;
```

### Member Ordering
Within a class: static constants → instance fields (final first, then mutable state) → constructors → public getters/behaviour → private helpers → static factory (`builder()`) → nested enums/records → nested static `Builder` last. One blank line between methods; no blank lines between consecutive abstract/interface method declarations.
