# Documentation Index

**IMPORTANT**: Read this file at the beginning of any development task to understand available documentation and standards.

Each entry says what a file covers and when to read it. The files themselves hold the details.

## Quick Reference

### Project Documentation
Project-level documentation covering vision, goals, architecture, technology choices and manual verification.

### Technical Standards
Coding standards, conventions, and best practices organized by domain.

---

## Project Documentation

Located in `.maister/docs/project/`

### Vision (`project/vision.md`)
What LorisUI is (a declarative, toolkit-agnostic Java UI library for game menus and HUDs with pluggable rendering backends and hosts), its target users, key features, success criteria and firm constraints. Read it for product scope questions.

### Roadmap (`project/roadmap.md`)
Open work only, as dependency-ordered phases with effort estimates, plus the current state and known gaps. Read it before proposing new features or scope changes.

### Tech Stack (`project/tech-stack.md`)
Language and toolchain versions, libraries and their dependency scopes, build, test and publishing setup. Read it before adding or upgrading a dependency.

### Architecture (`project/architecture.md`)
Module layering, the `Backend` port and the ImGui backend's internals, the frame, input and teardown flows, the screen lifecycle, extension points and the known architectural concerns and accepted limitations. Read it before changing a module boundary, a frame step or a lifecycle rule.

### Manual Testing (`project/manual-testing.md`)
Manual verification procedures for what can't be unit-tested: how to run the six examples (also all at once), the native lifecycle per backend, OpenGL ES under ANGLE, SDL_GPU, Vulkan, keyboard navigation and focusable components, the canvas, and the imgui-java upgrade checks. Read it before a release, after changing native or renderer code, and on every imgui-java upgrade.

---

## Technical Standards

### Global Standards

Located in `.maister/docs/standards/global/`

#### Coding Style (`standards/global/coding-style.md`)
Naming, the Checkstyle gate and `.editorconfig`, small focused functions, dead code removal, no backward-compatibility shims, DRY.

#### Commenting (`standards/global/commenting.md`)
When to comment, no change-history comments, and the Javadoc requirements for exported API (contract, null semantics, threading; shared phrasings), stating a behavior once and linking to it, and no references to external specs.

#### Conventions (`standards/global/conventions.md`)
Project structure, documentation upkeep, version control, dependencies, a lightweight solo-maintainer process, never committing build or IDE outputs, and standards as rules with INDEX entries as pointers.

#### Error Handling (`standards/global/error-handling.md`)
Clear messages, fail fast, specific exceptions, handling at boundaries, guaranteed cleanup.

#### Minimal Implementation (`standards/global/minimal-implementation.md`)
Build only what is needed; no stubs, speculative abstractions or unused code.

#### Validation (`standards/global/validation.md`)
Validate early at boundaries with specific messages and allowlists, consistently across entry points.

### Architecture Standards

Located in `.maister/docs/standards/architecture/`

#### Modules (`standards/architecture/modules.md`)
The layered module chain, what each layer may depend on, package and JPMS naming, `requires` / `requires static` / qualified-export rules, and the MIT-only helper modules.

#### Component Model (`standards/architecture/component-model.md`)
Marker and behavioral interfaces (incl. the threading contract of behavioral interfaces), the `Backend` port and its settings, renderer and style-applier registration, texture and canvas backend hooks, host obligations (logical window size, sRGB framebuffer, input capture), and one backend per renderer.

### Java Standards

Located in `.maister/docs/standards/java/`

#### Language (`standards/java/language.md`)
Java 25 only; sealed hierarchies, pattern `switch`, records only for data carriers, `var` and stream usage.

#### Naming (`standards/java/naming.md`)
Type role suffixes, the `ImGui` prefix rule, camel-cased acronyms, generic type parameter names.

#### Formatting (`standards/java/formatting.md`)
Indentation, braces, 120 columns, wrapping, import layout and member ordering.

#### Class Design (`standards/java/class-design.md`)
Field and getter conventions, unmodifiable collections, `@Override`, `@SuppressWarnings` and `@SafeVarargs`.

#### Error Handling (`standards/java/error-handling.md`)
Which exception for which failure, argument-check order, native result codes, multi-step and deferred teardown, host rules, and the null convention.

### Components Standards

Located in `.maister/docs/standards/components/`

#### Builders (`standards/components/builders.md`)
The nested `Builder` shape, dynamic `Supplier` properties, callbacks and their gated triggers, the enabled state with `disabledStyle`, and backend-consumed requests.

#### Renderers and Styles (`standards/components/renderers-and-styles.md`)
The renderer and style-applier shape, IDs, enabled and disabled drawing, push/pop balance, textures, cross-frame input state helpers, and keyboard-focus registration.

### Build Standards

Located in `.maister/docs/standards/build/`

#### Gradle (`standards/build/gradle.md`)
Gradle and DSL, plugins, shared configuration, the Checkstyle and Javadoc gates, local publishing and licenses, dependency scopes, and the example run tasks.

### Testing Standards

Located in `.maister/docs/standards/testing/`

#### Test Writing (`standards/testing/test-writing.md`)
Behavior-focused tests, the JUnit and Mockito setup, fakes instead of native types, native-free seams and gating tests, argument-check order tests, async test techniques, and what is left to manual testing.

### Frontend Standards

*Not initialized for this project. If you need frontend standards, you can:*
- *Add them manually using the docs-manager skill*
- *Run `/maister:standards-discover --scope=frontend` to auto-discover*

### Backend Standards

*Not initialized for this project. If you need backend standards, you can:*
- *Add them manually using the docs-manager skill*
- *Run `/maister:standards-discover --scope=backend` to auto-discover*

---

## How to Use This Documentation

1. **Start Here**: Always read this INDEX.md first to understand what documentation exists
2. **Project Context**: Read relevant project documentation before starting work
3. **Standards**: This index only points to the standards — open and follow the specific standard files relevant to your task; don't rely on the index alone
4. **Keep Updated**: Update documentation when making significant changes
5. **Customize**: Adapt all documentation to your project's specific needs

## Updating Documentation

- Project documentation should be updated when goals, tech stack, or architecture changes
- Technical standards should be updated when team conventions evolve
- Always update INDEX.md when adding, removing, or significantly changing documentation; keep entries as short pointers, not summaries
