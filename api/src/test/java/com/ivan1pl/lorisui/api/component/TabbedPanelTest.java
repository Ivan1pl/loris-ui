package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

class TabbedPanelTest {
    private static final BiFunction<Integer, Integer, Component> LABEL = (_, _) -> Spacer.builder().build();

    @Test
    void shouldRejectBuildWithoutTabs() {
        TabbedPanel.Builder builder = TabbedPanel.builder().size(300, 200);

        Assertions.assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void shouldSwitchTabWhenTabButtonClicked() {
        TabbedPanel tabbedPanel = threeTabs();

        tabButton(tabbedPanel, 2).click();
        Assertions.assertEquals(2, tabbedPanel.getTabIndex());
        tabButton(tabbedPanel, 1).click();
        Assertions.assertEquals(1, tabbedPanel.getTabIndex());
    }

    @Test
    void shouldSelectTabAsClickingItsButtonWouldWhenTabIndexSet() {
        TabbedPanel tabbedPanel = threeTabs();

        tabbedPanel.setTabIndex(2);
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, tabbedPanel.getTabIndex()),
                () -> Assertions.assertTrue(tabButton(tabbedPanel, 0).isEnabled()),
                () -> Assertions.assertFalse(tabButton(tabbedPanel, 2).isEnabled()));
        tabbedPanel.setTabIndex(2);
        Assertions.assertEquals(2, tabbedPanel.getTabIndex());
        tabbedPanel.setTabIndex(0);
        Assertions.assertEquals(0, tabbedPanel.getTabIndex());
    }

    @Test
    void shouldRejectTabIndexOutOfRangeWithoutChangingSelection() {
        TabbedPanel tabbedPanel = threeTabs();
        tabbedPanel.setTabIndex(1);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Tab index -1 is outside the range 0 to 2",
                        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> tabbedPanel.setTabIndex(-1)).getMessage()),
                () -> Assertions.assertEquals("Tab index 3 is outside the range 0 to 2",
                        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> tabbedPanel.setTabIndex(3)).getMessage()),
                () -> Assertions.assertEquals(1, tabbedPanel.getTabIndex()));
    }

    @Test
    void shouldDisableSelectedTabButton() {
        TabbedPanel tabbedPanel = threeTabs();

        Assertions.assertAll(
                () -> Assertions.assertFalse(tabButton(tabbedPanel, 0).isEnabled()),
                () -> Assertions.assertTrue(tabButton(tabbedPanel, 1).isEnabled()),
                () -> Assertions.assertTrue(tabButton(tabbedPanel, 2).isEnabled()));
        tabButton(tabbedPanel, 1).click();
        Assertions.assertAll(
                () -> Assertions.assertTrue(tabButton(tabbedPanel, 0).isEnabled()),
                () -> Assertions.assertFalse(tabButton(tabbedPanel, 1).isEnabled()));
    }

    @Test
    void shouldGiveRemainderOfTabBarWidthToFirstTabsOnePixelEach() {
        List<Integer> labelWidths = new ArrayList<>();
        BiFunction<Integer, Integer, Component> label = (width, _) -> {
            labelWidths.add(width);
            return Spacer.builder().build();
        };
        Component contents = Spacer.builder().build();
        TabbedPanel tabbedPanel = TabbedPanel.builder()
                .size(570, 200)
                .addTab(label, null, null, null, null, contents)
                .addTab(label, null, null, null, null, contents)
                .addTab(label, null, null, null, null, contents)
                .addTab(label, null, null, null, null, contents)
                .build();

        List<Integer> buttonWidths = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            buttonWidths.add(tabButton(tabbedPanel, i).getWidth(800, 600));
        }
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(143, 143, 142, 142), buttonWidths),
                () -> Assertions.assertEquals(List.of(143, 143, 142, 142), labelWidths));
    }

    @Test
    void shouldRejectNullTabLabelOrContents() {
        TabbedPanel.Builder builder = TabbedPanel.builder().size(300, 200);
        Component contents = Spacer.builder().build();

        Assertions.assertAll(
                () -> assertNullPointer("tabLabel",
                        () -> builder.addTab(null, null, null, null, null, contents)),
                () -> assertNullPointer("tabLabel",
                        () -> builder.addTab(null, null, null, null, null, () -> contents)),
                () -> assertNullPointer("tabContents",
                        () -> builder.addTab(LABEL, null, null, null, null, (Component) null)),
                () -> assertNullPointer("tabContents",
                        () -> builder.addTab(LABEL, null, null, null, null, (Supplier<Component>) null)));
    }

    private static TabbedPanel threeTabs() {
        Component contents = Spacer.builder().build();
        return TabbedPanel.builder()
                .size(300, 200)
                .addTab(LABEL, null, null, null, null, contents)
                .addTab(LABEL, null, null, null, null, contents)
                .addTab(LABEL, null, null, null, null, contents)
                .build();
    }

    private static Button tabButton(TabbedPanel tabbedPanel, int index) {
        Component tab = tabbedPanel.getTabsPanel().getContent().get(index).apply(300, 200);
        if (tab instanceof Button button) {
            return button;
        }
        throw new AssertionError("Tab " + index + " is not a Button: " + tab);
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
