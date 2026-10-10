# Java Class Design

### Private Final Fields with Getters
All instance fields are private. Immutable configuration is `private final`, assigned in the constructor. Expose JavaBean-style getters (`getX()`, `isX()` for booleans) — not record-style accessors on regular classes. Mutable fields are limited to runtime UI state (selection, tab index, popup open flag). Wrap incoming collections with `Collections.unmodifiableList`.

### `@Override` and `@SuppressWarnings`
Every overriding or implementing method carries `@Override`. Apply `@SuppressWarnings("unchecked")` at the narrowest scope — on the method doing the cast, never on the class. Use `@SafeVarargs` on final varargs builder methods.
