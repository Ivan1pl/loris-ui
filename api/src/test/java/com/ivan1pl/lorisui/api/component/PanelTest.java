package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.core.IntBiFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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
                () -> Assertions.assertEquals(0, panel.getY(800, 600)),
                () -> Assertions.assertEquals(0, panel.getPaddingTop()),
                () -> Assertions.assertEquals(0, panel.getPaddingBottom()),
                () -> Assertions.assertEquals(0, panel.getPaddingLeft()),
                () -> Assertions.assertEquals(0, panel.getPaddingRight()));
    }

    @Test
    void shouldSetEverySideWhenPaddingSet() {
        assertPadding(buildEachType(builder -> builder.padding(7)), 7, 7, 7, 7);
    }

    @Test
    void shouldKeepEachSideWhenSidesSetSeparately() {
        List<Panel> panels = buildEachType(builder -> builder
                .paddingTop(1)
                .paddingBottom(2)
                .paddingLeft(3)
                .paddingRight(4));

        assertPadding(panels, 1, 2, 3, 4);
    }

    @Test
    void shouldApplyPaddingSettersInCallOrderWhenCombined() {
        List<Panel> sideAfterAll = buildEachType(builder -> builder
                .padding(5)
                .paddingLeft(2));
        List<Panel> allAfterSides = buildEachType(builder -> builder
                .paddingTop(1)
                .paddingBottom(2)
                .paddingLeft(3)
                .paddingRight(4)
                .padding(6));

        Assertions.assertAll(
                () -> assertPadding(sideAfterAll, 5, 5, 2, 5),
                () -> assertPadding(allAfterSides, 6, 6, 6, 6));
    }

    @Test
    void shouldRejectNegativePaddingWhenSet() {
        Panel.Builder builder = Panel.panelBuilder("panel")
                .size(10, 10)
                .paddingTop(1)
                .paddingBottom(2)
                .paddingLeft(3)
                .paddingRight(4);

        Assertions.assertAll(
                () -> assertNegativePadding("Padding must not be negative: -1", () -> builder.padding(-1)),
                () -> assertNegativePadding("Top padding must not be negative: -1", () -> builder.paddingTop(-1)),
                () -> assertNegativePadding("Bottom padding must not be negative: -1",
                        () -> builder.paddingBottom(-1)),
                () -> assertNegativePadding("Left padding must not be negative: -1", () -> builder.paddingLeft(-1)),
                () -> assertNegativePadding("Right padding must not be negative: -1",
                        () -> builder.paddingRight(-1)),
                () -> assertNegativePadding("Padding must not be negative: -1",
                        () -> Button.buttonBuilder("button").padding(-1)),
                () -> assertNegativePadding("Padding must not be negative: -1",
                        () -> Popup.popupBuilder("popup").padding(-1)),
                () -> assertNegativePadding("Padding must not be negative: -1",
                        () -> ModalPopup.popupBuilder("modal").padding(-1)));
        assertPadding(List.of(builder.build()), 1, 2, 3, 4);
    }

    @Test
    void shouldAcceptZeroPaddingWhenSet() {
        List<Panel> sidesZeroed = buildEachType(builder -> builder
                .padding(5)
                .paddingTop(0)
                .paddingBottom(0)
                .paddingLeft(0)
                .paddingRight(0));
        List<Panel> allZeroed = buildEachType(builder -> builder
                .padding(5)
                .padding(0));

        Assertions.assertAll(
                () -> assertPadding(sidesZeroed, 0, 0, 0, 0),
                () -> assertPadding(allZeroed, 0, 0, 0, 0));
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

    private static List<Panel> buildEachType(UnaryOperator<Panel.BaseBuilder<?, ?>> configure) {
        return List.of(
                configure.apply(Panel.panelBuilder("panel").size(10, 10)).build(),
                configure.apply(Button.buttonBuilder("button").size(10, 10)).build(),
                configure.apply(Popup.popupBuilder("popup").size(10, 10)).build(),
                configure.apply(ModalPopup.popupBuilder("modal").size(10, 10)).build());
    }

    private static void assertPadding(List<Panel> panels, int top, int bottom, int left, int right) {
        List<Executable> checks = new ArrayList<>();
        for (Panel panel : panels) {
            checks.add(() -> Assertions.assertEquals(top, panel.getPaddingTop(), panel.getId() + " top"));
            checks.add(() -> Assertions.assertEquals(bottom, panel.getPaddingBottom(), panel.getId() + " bottom"));
            checks.add(() -> Assertions.assertEquals(left, panel.getPaddingLeft(), panel.getId() + " left"));
            checks.add(() -> Assertions.assertEquals(right, panel.getPaddingRight(), panel.getId() + " right"));
        }
        Assertions.assertAll(checks);
    }

    private static void assertNegativePadding(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(IllegalArgumentException.class, executable).getMessage());
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
