package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.core.IntBiFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

class PanelTest {
    @Test
    void shouldRejectNullIdWhenAnyPanelFactoryCalled() {
        Assertions.assertAll(
                () -> assertNullPointer("id", () -> Panel.panelBuilder((String) null)),
                () -> assertNullPointer("id", () -> Panel.panelBuilder((Supplier<String>) null)),
                () -> assertNullPointer("id", () -> Button.buttonBuilder((String) null)),
                () -> assertNullPointer("id", () -> Button.buttonBuilder((Supplier<String>) null)),
                () -> assertNullPointer("id", () -> Popup.popupBuilder((String) null)),
                () -> assertNullPointer("id", () -> Popup.popupBuilder((Supplier<String>) null)),
                () -> assertNullPointer("id", () -> ModalPopup.popupBuilder(null)));
    }

    @Test
    void shouldRejectBuildWhenSizeNeverSet() {
        Assertions.assertAll(
                () -> assertNullPointer("width", () -> Panel.panelBuilder("panel").build()),
                () -> assertNullPointer("width", () -> Button.buttonBuilder("button").build()),
                () -> assertNullPointer("width", () -> Popup.popupBuilder("popup").build()),
                () -> assertNullPointer("width", () -> ModalPopup.popupBuilder("modal").build()));
    }

    @Test
    void shouldApplyDefaultsWhenOptionalPropertiesUnset() {
        Panel panel = Panel.panelBuilder("panel").size(10, 10).build();

        Assertions.assertAll(
                () -> Assertions.assertEquals("panel", panel.getId()),
                () -> Assertions.assertTrue(panel.isOpen()),
                () -> Assertions.assertEquals(Layout.VERTICAL, panel.getLayout()),
                () -> Assertions.assertTrue(panel.isScrollable()),
                () -> Assertions.assertNull(panel.getBackground()),
                () -> Assertions.assertNull(panel.getTooltip()),
                () -> Assertions.assertEquals(0, panel.getX(800, 600)),
                () -> Assertions.assertEquals(0, panel.getY(800, 600)));
    }

    @Test
    void shouldHaveNoBackgroundWhenBackgroundColorIsNull() {
        AtomicReference<Color> color = new AtomicReference<>();
        Panel constantNull = Panel.panelBuilder("constant")
                .size(10, 10)
                .backgroundColor((Color) null)
                .build();
        Panel dynamic = Panel.panelBuilder("dynamic")
                .size(10, 10)
                .backgroundColor(color::get)
                .build();

        Assertions.assertNull(constantNull.getBackground());
        Assertions.assertNull(dynamic.getBackground());
        color.set(Color.BLUE);
        if (!(dynamic.getBackground() instanceof Background.Color background)) {
            Assertions.fail("backgroundColor() did not produce a Background.Color once the colour was set");
            return;
        }
        Assertions.assertSame(Color.BLUE, background.getColor());
    }

    @Test
    void shouldRejectNullRequiredArgumentsAtSetter() {
        Panel.Builder builder = Panel.panelBuilder("panel");
        IntSupplier zero = () -> 0;
        IntBinaryOperator origin = (_, _) -> 0;
        ImageMode mode = ImageMode.fit();

        Assertions.assertAll(
                () -> assertNullPointer("x", () -> builder.position(null, zero)),
                () -> assertNullPointer("y", () -> builder.position(zero, null)),
                () -> assertNullPointer("x", () -> builder.position(null, origin)),
                () -> assertNullPointer("y", () -> builder.position(origin, null)),
                () -> assertNullPointer("width", () -> builder.size(null, zero)),
                () -> assertNullPointer("height", () -> builder.size(zero, null)),
                () -> assertNullPointer("width", () -> builder.size(null, origin)),
                () -> assertNullPointer("height", () -> builder.size(origin, null)),
                () -> assertNullPointer("component", () -> builder.addContent((Component) null)),
                () -> assertNullPointer("component", () -> builder.addContent((Supplier<Component>) null)),
                () -> assertNullPointer("component", () -> builder.addContent((IntBiFunction<Component>) null)),
                () -> assertNullPointer("image", () -> builder.backgroundImage(null, mode)),
                () -> assertNullPointer("imageMode", () -> builder.backgroundImage("bg.png", null)),
                () -> assertNullPointer("open", () -> builder.open(null)),
                () -> assertNullPointer("layout", () -> builder.layout(null)),
                () -> assertNullPointer("style", () -> builder.addStyle(null)));
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
