package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

class ButtonTest {
    @Test
    void shouldResolveBackgroundByStatePriority() {
        Background base = Background.color(Color.BLUE);
        Background active = Background.color(Color.RED);
        Background hovered = Background.color(Color.GREEN);
        Background disabled = Background.color(Color.BLACK);
        AtomicBoolean enabled = new AtomicBoolean(true);
        Button allStates = Button.buttonBuilder("all")
                .size(10, 10)
                .background(base)
                .activeBackground(active)
                .hoveredBackground(hovered)
                .disabledBackground(disabled)
                .enabled(enabled::get)
                .build();
        Button baseOnly = Button.buttonBuilder("base")
                .size(10, 10)
                .background(base)
                .hoveredBackgroundColor((Color) null)
                .enabled(false)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertSame(active, allStates.getBackground(true, true)),
                () -> Assertions.assertSame(hovered, allStates.getBackground(true, false)),
                () -> Assertions.assertSame(base, allStates.getBackground(false, false)),
                () -> Assertions.assertSame(base, baseOnly.getBackground(true, true)));
        enabled.set(false);
        Assertions.assertSame(disabled, allStates.getBackground(true, true));
    }

    @Test
    void shouldRejectNullActionEnabledAndStateBackgroundImage() {
        Button.Builder builder = Button.buttonBuilder("button");
        ImageMode mode = ImageMode.fit();

        Assertions.assertAll(
                () -> assertNullPointer("action", () -> builder.action(null)),
                () -> assertNullPointer("enabled", () -> builder.enabled((Supplier<Boolean>) null)),
                () -> assertNullPointer("image", () -> builder.activeBackgroundImage(null, mode)),
                () -> assertNullPointer("imageMode", () -> builder.activeBackgroundImage("bg.png", null)),
                () -> assertNullPointer("image", () -> builder.hoveredBackgroundImage(null, mode)),
                () -> assertNullPointer("imageMode", () -> builder.hoveredBackgroundImage("bg.png", null)),
                () -> assertNullPointer("image", () -> builder.disabledBackgroundImage(null, mode)),
                () -> assertNullPointer("imageMode", () -> builder.disabledBackgroundImage("bg.png", null)),
                () -> assertNullPointer("background", () -> builder.activeBackground((Supplier<Background>) null)),
                () -> assertNullPointer("background", () -> builder.hoveredBackground((Supplier<Background>) null)),
                () -> assertNullPointer("background",
                        () -> builder.disabledBackground((Supplier<Background>) null)));
    }

    @Test
    void shouldRunActionWhenClickedWhileEnabled() {
        Runnable action = Mockito.mock(Runnable.class);
        Button button = Button.buttonBuilder("button")
                .size(10, 10)
                .action(action)
                .build();

        button.click();

        Assertions.assertTrue(button.isEnabled());
        Mockito.verify(action, Mockito.times(1)).run();
    }

    @Test
    void shouldNotRunActionWhenClickedWhileDisabled() {
        Runnable action = Mockito.mock(Runnable.class);
        Button button = Button.buttonBuilder("button")
                .size(10, 10)
                .enabled(() -> false)
                .action(action)
                .build();

        button.click();

        Assertions.assertFalse(button.isEnabled());
        Mockito.verifyNoInteractions(action);
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
