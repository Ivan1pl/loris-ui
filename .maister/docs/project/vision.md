# Project Vision

## Pitch
LorisUI is a declarative Java UI library that helps game developers build in-game menus, HUDs and tool screens by describing them as plain component trees (panels, buttons, text, inputs, images, popups) and rendering them through a pluggable backend. Today that backend is Dear ImGui, hosted in jMonkeyEngine 3 or in an application's own LWJGL/SDL3 loop.

## Problem Statement
Immediate-mode GUI libraries such as Dear ImGui are fast and flexible. But using them directly couples game UI code to the toolkit's API, the render loop and its global state. Retained-mode alternatives tied to a particular engine are hard to move between engines and are usually heavyweight.

LorisUI separates *what* the UI is (a toolkit-agnostic component model in `loris-ui-api`) from *how* it gets drawn (the backend modules). Game code describes screens once. The backend handles rendering, input, fonts, textures and styling.

## Target Users
- **Primary**: the author's own game projects. LorisUI was extracted from a game codebase.
- **Secondary**: Java game developers, especially jMonkeyEngine users and developers with their own LWJGL/SDL3 loop, who want a lightweight, themeable UI layer built on modern Java.
- **Future**: developers on other Java hosts and renderers once more backends exist.

## Key Features
- **Toolkit-agnostic component model** (`api` module, zero external dependencies): `Panel`, `Button`, `Text`, `Image`, `TextInput`, `NumberInput`, `ComboBox`, `ProgressBar`, `TabbedPanel`, `Popup`/`ModalPopup`, `Spacer`, plus layout, background and style types.
- **Builder-based, declarative screens**: subclass `LorisScreen`, implement `layout()`, and return a `List<Component>`. Layout is recomputed off the render thread when invalidated.
- **Pluggable backend** (`Backend` interface): the port is a single toolkit-neutral `render` call that receives the screen's components; textures, fonts and input capture belong to the backend and its host. The ImGui/OpenGL backend is included. Each component type has its own renderer and each style type has its own style applier, both looked up in registries with superclass fallback; custom components and styles plug in by registering their renderers and style appliers on the backend or on a host display, which also replaces built-in ones.
- **Rich image handling**: a sealed `ImageMode` (repeat, fit, sub-image, sprite, nine-slice-style resize variants) and RGB-channel colour masking for recolourable textures.
- **i18n**: text is resolved through `BundleKeyResolver` using `ResourceBundle`s.
- **Engine and loop integration**: `ImGuiJmeDisplay` plugs into jMonkeyEngine as a `SceneProcessor`, and `ImGuiLwjglDisplay` embeds into an application's own LWJGL SDL3 window and loop. Both provide input routing, screen lifecycle hooks (through the host-neutral `ScreenManager`), font registration, and a release-and-reuse lifecycle: a jME display can be removed and attached again, and either display can be torn down safely from inside a frame or a screen hook.

## Success Criteria
- The author's game(s) use LorisUI as their only UI layer, without workarounds.
- The public API is stable, documented (Javadoc) and covered by automated tests.
- The library is published to a Maven repository and can be consumed with a standard dependency declaration.
- At least one additional backend or host is implemented without changing `loris-ui-api`. This shows the abstraction holds.

## Differentiators
- A strict API/implementation split: `loris-ui-api` has no dependencies and is a JPMS module (`lorisui.api`), with internal packages unexported and a documented public API.
- Built on the latest LTS Java (currently Java 25): records, sealed types, pattern matching and JPMS.
- ImGui's performance and maturity, with a declarative, styleable, game-oriented component model on top.

## Constraints
- **`loris-ui-api` must stay dependency-free** and toolkit-agnostic. All toolkit, engine and graphics dependencies belong in backend or host modules.
- **Track the latest Java LTS release** (currently 25). Adopt new language features, but don't target non-LTS releases.
- **Solo developer**: keep process, tooling and CI lightweight.

---
*Generated during Maister initialization on 2026-10-10 from codebase analysis and maintainer input; updated 2026-10-11.*
