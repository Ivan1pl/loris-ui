# Java Language Usage

### Latest Java LTS Only
Target the latest Java LTS release — currently **Java 25**. Never target non-LTS releases. Upgrade when the next LTS ships (expected: Java 29). Language features available in the current LTS may be used freely.

### Modern Java Features
- Closed variant sets: `sealed abstract class` with a private constructor, lowercase static factories (`ImageMode.sprite(...)`, `Background.color(...)`) and nested `public static final class` variants with private constructors.
- Dispatch with an exhaustive pattern `switch` (no `default` branch); order subclass cases before parent cases.
- Use `instanceof` patterns (`x instanceof Button button`) instead of explicit casts.
- Records only for small immutable data carriers with no behaviour (`TextureData`, `ComboBox.Item`); anything with builders, suppliers or getters is a regular class.
- Use unnamed `_` for ignored lambda parameters and pattern bindings; name ignored catch/method parameters `ignored`.

```java
switch (imageMode) {
    case ImageMode.Fit _ -> renderTextureFit(id, width, height);
    case ImageMode.Sprite sprite -> renderTextureSprite(sprite, id, width, height, tw, th);
}
return position((_, _) -> x, (_, _) -> y);
```

### Explicit Local Types, `var` Sparingly
Declare local variable types explicitly by default. Use `var` only when the type is long/verbose or obvious from the right-hand side (nested generic maps, builder instances, `Styles` handles, `MemoryStack` buffers). Prefer `for` / enhanced `for` loops over streams in library code.
