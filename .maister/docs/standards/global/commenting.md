## Commenting

### Let Code Speak
Write code that explains itself through structure and naming.

### Comment Sparingly
Add brief comments only when the logic isn't self-evident from the code.

### No Change Comments
Avoid comments about recent fixes or changes; comments should be timeless explanations, not changelogs.

### Javadoc on Exported API
Public types and public/protected members in exported packages (and every public type and member of the non-modular `jme3`) require Javadoc describing the contract, null semantics (which arguments/returns may be null) and threading expectations where relevant. Internal (`impl`) and private code needs no Javadoc; use sparse `//` comments only to explain non-obvious "why".
- **User-facing**: the audience is game developers reading IDE hovers and generated HTML. Describe observable behavior and contracts, never implementation details (thread names, executors, cache data structures, toolkit call sequences, error-recovery mechanics). `imgui` docs may name Dear ImGui/OpenGL, its documented toolkit; `api` docs stay toolkit-neutral.
- **Shape**: a third-person summary first sentence ("Returns…", "Sets…"); `@param` for every parameter and type parameter, `@return` on non-void methods, `@throws` for every exception a caller can trigger. Overrides inherit their docs unless the contract differs. Constants, enum values and record components are documented. Write doclint-clean HTML with `{@link}` / `{@code}`.
- **Every exported package** has a `package-info.java` with an overview paragraph, and every `module-info.java` has a module doc comment.
- **American spelling** in prose: color, behavior, initialize, localized.

Shared phrasings keep the API docs consistent:
- Required arguments: "must not be null".
- Constant overloads of optional properties: "or {@code null} to keep the toolkit default" (or the documented exception, e.g. "for no background").
- Getters of Supplier-backed properties: "evaluated on each call"; nullable returns state when they are `null`.
- Resource-path parameters: the path is a class-path resource name and a leading `/` is optional; on the module path, the resource must be outside any package of a named module or in a package the application module opens unconditionally (`opens pkg;`), because a qualified `opens pkg to …` is not enough for class-loader lookup.
