# Manual Testing

The manual verification procedures for the parts of LorisUI that unit tests cannot reach (no native context, GPU device or `VkDevice` in tests); the architectural reasons and the accepted gaps are in `architecture.md` › Known Architectural Concerns.

## Running the Examples

The `examples` project is the manual test bed. It has six run tasks, one per host and graphics API:

| Task | Host and graphics API |
|------|-----------------------|
| `./gradlew runJme3Example` | jME, default `ANGLE_GLES3` renderer (OpenGL ES 3 pair) |
| `./gradlew runJme3Gl3Example` | jME, `LWJGL_OPENGL33` (OpenGL 3 pair) |
| `./gradlew runLwjglSdlGl3Example` | LWJGL, SDL3 + OpenGL 3 |
| `./gradlew runLwjglGlfwGl3Example` | LWJGL, GLFW + OpenGL 3 |
| `./gradlew runLwjglSdlGpuExample` | LWJGL, SDL3 + SDL_GPU |
| `./gradlew runLwjglSdlVulkanExample` | LWJGL, SDL3 + Vulkan |

- Run them one at a time, or all six at once with `./gradlew runAllExamples` (each in its own JVM; the task fails naming every failed example once all have exited).
- For SDL_GPU and Vulkan, pass `-Dlorisui.example.gpuDebug=true` (also to `runAllExamples`): SDL_GPU creates its device in debug mode with validation, Vulkan enables `VK_LAYER_KHRONOS_validation` (from the Vulkan SDK) with a debug messenger. Checks run with it must produce no validation errors.
- The three shared screens are used: `ExampleScreen` (components, the "Canvas" tab, "Hide UI", the "Focus" button), `ExampleFocusScreen` (keyboard navigation and focusable components, reached through "Focus", left through "Back") and `ExampleScrollScreen` (the panel scroll position, reached through "Scroll" on the focus screen, left through "Back" to the focus screen).
- In the jME examples F1 toggles the UI (removing and re-adding the display, so it also exercises re-attach); "Hide UI" removes the display from inside a frame. In the LWJGL examples "Hide UI" closes the display for good.

## Native Lifecycle

Verified by hand with the six examples, one at a time or all at once with `runAllExamples`:

1. Context creation and destruction.
2. Impl shutdown.
3. Font replay.
4. GL and GLES texture upload and deletion.
5. GPU texture upload and release, and the SDL_GPU frame.
6. The GLES draw path under ANGLE (see [OpenGL ES under ANGLE](#opengl-es-under-angle)).
7. The Vulkan pipeline, uploads, synchronization, swapchain recreation and teardown (see [Vulkan](#vulkan)).
8. SDL text-input delivery.
9. Native GLFW callback chaining.
10. Re-attach (jME, F1).

## OpenGL ES under ANGLE

The GLES3 draw path under jME's ANGLE renderer (and the ANGLE drivers per platform) is verified only by running:

1. `runJme3Example`,
2. side by side with `runJme3Gl3Example` and the LWJGL examples, comparing the output.

Last checked with jME 3.10.0-beta3.

## SDL_GPU

1. GPU release before submit: in the SDL_GPU example, press "Hide UI" (ideally with `-Dlorisui.example.gpuDebug=true`). Closing the display inside a frame releases GPU resources at the end of `render`, before the application submits that frame's command buffer; there must be no validation error or crash.

## Vulkan

Run `runLwjglSdlVulkanExample` with `-Dlorisui.example.gpuDebug=true`; every check must pass without validation errors. The Vulkan classes cannot be constructed in tests, so these paths behind a successful construction are checked only by hand (the shared display base behavior stays covered by the SDL3 + OpenGL 3 tests):

1. The display's close-during-frame logic: `skipFrameDraw`, the restored frame flag and the deferred release ("Hide UI").
2. Its render check order: its own state checks, then the renderer's command-buffer checks.
3. Calls before `initialize()`.
4. `close()`, and registration after `close()`.
5. The pipeline, uploads, synchronization, swapchain recreation (resize the window) and teardown (see [Native Lifecycle](#native-lifecycle)).

On macOS additionally:

6. Loader sharing: SDL and LWJGL must load the same Vulkan library (the example passes LWJGL's loaded library path to `SDL_Vulkan_LoadLibrary`).
7. Vulkan 1.3 (dynamic rendering) support in the MoltenVK build shipped with LWJGL 3.4.3, which is unverified so far.

## Keyboard Navigation

What this covers: the focusable renderers (registration, focus looks, popup and combo scopes, the internal combo popup ID, the one-frame input activation, `clearActiveID` after the widget and before a disabled input's widget, `beginDisabled` around inputs and the whole combo, `AllowWhenDisabled` clicks), the always-on focus frame in `ImGuiRenderer`, the Tab diversion in the five variants (`SdlKeyEvents` reads native event memory; foreign-window Tab) and the host capture.

The pass uses both example screens. On `ExampleScreen`:

1. Tab and Shift+Tab through the screen:
   1. the disabled `progress_minus` is skipped;
   2. inputs get a caret and keep their edit when tabbed off;
   3. Enter opens the combo, Tab and Up/Down move through its items, Enter selects and returns to the combo, Escape closes and returns;
   4. the selected tab keeps its anchor;
   5. a button re-enabled while focused runs its hover action again.
2. Held Tab across inputs at a low frame rate: no stray caret, the focus matches the active input.
3. Ctrl+Tab does nothing.

On `ExampleFocusScreen`:

4. The two inputs in one panel keep separate edits.
5. The modal traps Tab, and Escape does not close it.
6. A click only sets the anchor.

Host keys:

7. While a button is focused, Escape is consumed: no jME exit and no GLFW close.
8. After the focus is cleared, F1 and Escape reach the game.

The keyboard-navigation checks have not been verified yet (pending the first manual pass).

## Focusable Components

On `ExampleFocusScreen`, on all six examples:

1. Disabled look: the disabled `TextInput` is dimmed with its disabled style; the `NumberInput` and `ComboBox` are dimmed with their normal styles.
2. Tab and Shift+Tab skip all three.
3. A click on one neither edits nor opens it, and the next Tab continues after it.
4. "Toggle (3 s)" makes them reachable again.
5. Disabling while in use: the row starts disabled, so first re-enable it as in check 4. Press "Toggle (3 s)" again and, within the 3 s, start editing one of its two inputs, or expand its combo with Enter or open it with a click, and let the toggle disable it: editing stops with no stray caret, no typed character and no captured keys (in the jME examples F1 toggles the UI), or the list closes. Press "Toggle (3 s)" once more to re-enable the row: Tab works, a combo expanded with Enter gets its look back without expanding, and one opened with a click stays closed.
6. A request releases an edited input: click "Focus number (3 s)", tab to the text input and type until the request lands. The text input loses its caret and keeps its edit, the number input starts editing, and further keys reach only the number input.
7. "Focus number" gives the number input the caret.
8. "Focus modal" opens the modal with the caret in its input.
9. "Focus hidden tab" focuses nothing, and switching to tab 2 a moment later does not focus its button.
10. On the first switch to the focus screen in a run, the text input gets the caret from `onStartScreen`, and again on later visits.
11. The status text follows Tab, shows an input edited after a mouse click but not a clicked button, shows the combo while Up/Down moves through its items, and clears on Escape.
12. Navigation off: add a temporary, uncommitted `imGuiJmeDisplay.setKeyboardNavigationEnabled(false);` right after the display is constructed in `LorisJme3Example.simpleInitApp()` (shared by both jME examples; in an LWJGL example, `display.setKeyboardNavigationEnabled(false);` after the display is constructed in `runUi`), run a jME example and revert the call afterwards:
    1. Tab and Enter or Space do nothing for LorisUI;
    2. the request buttons and `onStartScreen` still focus their targets;
    3. the status text still follows a mouse-edited input;
    4. keys are not captured while a button is focused: select tab 2 and press "Focus hidden tab", so the status text shows "button 2", then press F1, which hides the UI (F1 again shows it);
    5. last, Escape is not taken by LorisUI either: with a button still focused, it reaches jME and exits the example (in the GLFW example it closes the UI).

The focusable-components checks have not been verified yet (pending the first manual pass).

## Panel Scroll Position

What this covers: the `PanelRenderer` scroll hooks and `PanelScrollTracker` against a real Dear ImGui context (the scroll range, the whole-pixel scroll and the scrollbar), the padded and unpadded nested paths, the unpadded `ModalPopup` path, and requests from another thread (see [Not Covered](#not-covered-by-the-examples) for the top-level and non-modal `Popup` paths).

On `ExampleScrollScreen` (reached through "Scroll" on `ExampleFocusScreen`), on all six examples (the Vulkan one also with `-Dlorisui.example.gpuDebug=true`, without validation errors):

1. On every visit the panel ends at the bottom ("Line 30" visible) and the label reads 1.00.
2. "Top", "Middle" and "Bottom" move the content, and the label reads 0.00, 0.50 and 1.00. "Middle" may read 0.49 to 0.51, because the panel scrolls by whole pixels.
3. The wheel and a scrollbar drag update the label live.
4. Press "Bottom (3 s)", then scroll to the top: the panel jumps to the bottom after 3 s (a request from another thread).
5. Press "Bottom (3 s)", then drag the scrollbar grab to the top and hold it still until well after the 3 s: the panel does not jump. After the release it stays at the top and the label reads 0.00, because the drag dropped the request.
6. In both jME examples, F1 twice ends at the bottom with the label at 1.00 (a new Dear ImGui context, and `onStartScreen()` again).
7. Unpadded panel and content that fits: make a temporary, uncommitted change to the scroll panel (`scroll_content`) in `ExampleScrollScreen`, in this order, and revert it afterwards:
    1. padding 0: re-run checks (1) to (5);
    2. still padding 0, with the content cut to 3 lines: the buttons do nothing, and the label stays at 0.00;
    3. padding 10 restored, still 3 lines: the same.
8. Growing content: make a temporary, uncommitted change that replaces the 30 lines by one `Text`, whose supplier joins as many numbered lines as a counter holds (starting at 3) with line breaks, and makes "Bottom" increment the counter before it requests 1.0. Press "Bottom" until the content overflows, and a few times more: every press, including the first one that makes the content overflow, ends with the newest line visible and the label at 1.00. Revert the change afterwards.
9. Modal popup (the unpadded `ModalPopup` path, where the popup window itself scrolls): press "Modal":
    1. the modal opens at the bottom ("Modal line 30" and "Close" visible), and the label reads "Modal: 1.00";
    2. the wheel over the modal then scrolls it and updates the modal value;
    3. scroll it up a little, keeping "Close" visible (the modal value below 1.00), press "Close", then "Modal" again: it reopens at the bottom with the modal value at 1.00, because every open requests the bottom again (the old position may show for up to two frames).

The panel scroll position checks have not been verified yet (pending the first manual pass).

## Canvas

What this covers: `CanvasRenderer` (hit-testing, `itemHoverable`, the wheel-consuming child), the native canvas texture paths of the four backends (create, partial update, GL state restore, SDL_GPU and Vulkan uploads, eviction and release) and the wheel withholding in the hosts (see [Not Covered](#not-covered-by-the-examples)).

On all six examples (also with `runAllExamples`, and the Vulkan one with `-Dlorisui.example.gpuDebug=true` without validation errors), on `ExampleScreen`'s "Canvas" tab:

1. The tab shows the canvas with its background.
2. Painted pixels match the swatch exactly.
3. Paint, erase, pan and zoom work.
4. A left, right or middle drag that leaves the canvas keeps painting, erasing or panning until release, and another button pressed mid-gesture changes nothing.
5. In both jME examples, F1 pressed during a left, right and middle canvas drag toggles the UI (no keyboard capture).
6. A drag passing over "Clear" may show its hover look but never clears on release.
7. Zoomed pixels are sharp, except blurred on SDL_GPU.
8. The wheel over the canvas zooms without scrolling the tab content, while the wheel elsewhere in the tab scrolls it, also right after the content was scrolled (wheel lock) and on the first frames after the tab opens.
9. Switching tabs away and back keeps the drawing (eviction and re-upload).
10. Clear works.
11. "Hide UI" and showing it again, and jME re-attach, keep the drawing.
12. No GL state corruption in either jME example.

The canvas wheel take-over was last verified with Dear ImGui 1.92.7 on all six examples (2026-11-19).

## imgui-java Upgrade

LorisUI relies on Dear ImGui internals, and the runtime range `[1.92.7.1,1.93)` admits any later 1.92.x, so on every imgui-java upgrade (also within the range):

1. Re-check `BeginCombo`'s `"##ComboPopup"` popup-ID seed.
2. Re-check the `itemHoverable` signature and the early return of the empty-space move path on a hovered item (F1 must still reach the jME example during a left canvas drag).
3. Re-check the canvas wheel take-over: the wheeling window chosen from the hovered window, `ScrollMax` from the previous frame's content size, `SetNextWindowScroll` overriding the wheel's scroll target.
4. Re-check the `clearActiveID` and `SetKeyboardFocusHere` semantics: the request is activated in the next `NewFrame`, and an input requested by navigation is not re-activated while it is active; `clearActiveID` before a disabled input's widget ends the edit cleanly.
5. Re-check the `BeginDisabled` interplay: a block spanning the combo popup and preview child, `AllowWhenDisabled` hover on a disabled input and combo frame, a combo closing while its items are drawn disabled.
6. Re-check the panel scroll assumptions: `ScrollMax` is computed from the previous frame's content size (0 in a window's first frame); `SetNextWindowScroll` is applied in that window's `Begin`, rounded to whole pixels and clamped to `ScrollMax` (the 0.51 px tolerance of `PanelScrollTracker` and the two-decimal label of `ExampleScrollScreen` depend on the rounding); the wheel and keyboard scroll targets are set before `Begin` and replaced by it; the scrollbar is the only thing that moves the scroll inside `Begin` after the requested scroll (the drag rule depends on it, and anything else would drop requests as if the user had dragged); `BeginPopup` and `BeginPopupModal` discard the next-window data while the popup is closed.
7. Re-run the manual canvas checks:
   1. wheel take-over: the wheel over a canvas in a scrollable panel runs `wheel()` without scrolling the panel;
   2. no keyboard capture during canvas drags: F1 pressed during a left, right or middle canvas drag still toggles the jME examples' UI.
8. Re-run the manual keyboard checks:
   1. inputs: Tab into a text or number input shows the caret, typing and Tab off keep the edit and deactivate it; holding Tab across inputs at a low frame rate leaves no stray caret;
   2. combo boxes: a focused combo opens on Enter at its selected item, Tab, Shift+Tab, Up and Down move with the highlight, Enter selects, closes and returns the focus.
9. Re-run the [focusable-components checks](#focusable-components) (1) to (12) on `ExampleFocusScreen`.
10. Re-run the [panel scroll position checks](#panel-scroll-position) (1) to (9) on `ExampleScrollScreen`.

## Not Covered by the Examples

These paths have no example control or are not observable in the examples; they are covered only by unit tests and review (accepted gaps, see `architecture.md`):

1. A display's navigation setter reaching its private backend: verified by review and with the examples.
2. The navigation switch and the `SPACE` activation key end to end: unit tests and review only, apart from the temporary local setter call of focusable-components check (12).
3. The Tab trap of a non-modal `Popup` (no example contains one): `KeyboardFocusTest` and review only.
4. The wheel withholding from the game over an enabled canvas: the jME examples disable the fly camera and no LWJGL example reads the wheel, so it is verified by review only.
5. The scroll paths of top-level panels and non-modal `Popup`s (no example has an overflowing one): `PanelScrollTrackerTest` and review only; the nested paths and the unpadded `ModalPopup` path are covered by the panel scroll position checks, the unpadded nested one through the temporary change of check (7) and the modal through check (9).
6. Content or a panel that changes height in every frame, so a scroll request is never reached and stays applied: `PanelScrollTrackerTest` and review only (the growing content of panel scroll position check (8) settles after each press).
