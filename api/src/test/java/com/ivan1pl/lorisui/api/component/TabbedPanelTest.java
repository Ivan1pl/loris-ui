package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

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
