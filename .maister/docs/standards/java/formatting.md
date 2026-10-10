# Java Formatting

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
IntelliJ default layout: one alphabetically sorted block of project and third-party imports, a blank line, then `java.*` imports. Use explicit single-type imports; no static imports; avoid wildcard imports.

```java
import com.ivan1pl.lorisui.api.component.TextInput;
import imgui.ImGui;

import java.util.Map;
```

### Member Ordering
Within a class: static constants → instance fields (final first, then mutable state) → constructors → public getters/behaviour → private helpers → static factory (`builder()`) → nested enums/records → nested static `Builder` last. One blank line between methods; no blank lines between consecutive abstract/interface method declarations.
