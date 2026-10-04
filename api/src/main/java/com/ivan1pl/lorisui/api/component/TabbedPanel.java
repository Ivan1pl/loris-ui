package com.ivan1pl.lorisui.api.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class TabbedPanel implements Component {
    private final int width;
    private final int height;
    private final int tabBarHeight;
    private final Panel tabsPanel;
    private final List<Supplier<Component>> tabsContents;
    private int tabIndex = 0;

    private TabbedPanel(int width, int height, int tabBarHeight, List<BiFunction<Integer, Integer, Component>> tabs,
                        List<Background> tabsBackgrounds, List<Background> tabsHoveredBackgrounds,
                        List<Background> tabsActiveBackgrounds, List<Background> tabsSelectedBackground,
                        List<Supplier<Component>> tabsContents) {
        this.width = width;
        this.height = height;
        this.tabBarHeight = tabBarHeight;

        int tabWidth = width / tabs.size();
        var builder = Panel.panelBuilder(this::toString)
                .size(width, tabBarHeight)
                .layout(Layout.HORIZONTAL);
        for (int i = 0; i < tabs.size(); ++i) {
            int tabIndex = i;
            builder.addContent(Button.buttonBuilder(() -> "btn_" + this + "_" + tabIndex)
                    .size(tabWidth, tabBarHeight)
                    .background(tabsBackgrounds.get(i))
                    .hoveredBackground(tabsHoveredBackgrounds.get(i))
                    .activeBackground(tabsActiveBackgrounds.get(i))
                    .disabledBackground(tabsSelectedBackground.get(i))
                    .addContent(tabs.get(i).apply(tabWidth, tabBarHeight))
                    .enabled(() -> this.tabIndex != tabIndex)
                    .action(() -> this.tabIndex = tabIndex)
                    .build());
        }
        this.tabsPanel = builder.build();

        this.tabsContents = tabsContents;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTabBarHeight() {
        return tabBarHeight;
    }

    public Panel getTabsPanel() {
        return tabsPanel;
    }

    public List<Supplier<Component>> getTabsContents() {
        return tabsContents;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int width;
        private int height = 30;
        private int tabBarHeight = 30;
        private final List<BiFunction<Integer, Integer, Component>> tabs = new ArrayList<>();
        private final List<Background> tabsBackgrounds = new ArrayList<>();
        private final List<Background> tabsHoveredBackgrounds = new ArrayList<>();
        private final List<Background> tabsActiveBackgrounds = new ArrayList<>();
        private final List<Background> tabsSelectedBackground = new ArrayList<>();
        private final List<Supplier<Component>> tabsContents = new ArrayList<>();

        private Builder() {}

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder tabBarHeight(int tabBarHeight) {
            this.tabBarHeight = tabBarHeight;
            return this;
        }

        public Builder addTab(BiFunction<Integer, Integer, Component> tabLabel, Background tabLabelBackground,
                              Background tabLabelHoveredBackground, Background tabLabelActiveBackground,
                              Background tabLabelSelectedBackground, Component tabContents) {
            return addTab(tabLabel, tabLabelBackground, tabLabelHoveredBackground, tabLabelActiveBackground,
                    tabLabelSelectedBackground, () -> tabContents);
        }

        public Builder addTab(BiFunction<Integer, Integer, Component> tabLabel, Background tabLabelBackground,
                              Background tabLabelHoveredBackground, Background tabLabelActiveBackground,
                              Background tabLabelSelectedBackground, Supplier<Component> tabContents) {
            this.tabs.add(tabLabel);
            this.tabsBackgrounds.add(tabLabelBackground);
            this.tabsHoveredBackgrounds.add(tabLabelHoveredBackground);
            this.tabsActiveBackgrounds.add(tabLabelActiveBackground);
            this.tabsSelectedBackground.add(tabLabelSelectedBackground);
            this.tabsContents.add(tabContents);
            return this;
        }

        public TabbedPanel build() {
            return new TabbedPanel(width, height, tabBarHeight, tabs, tabsBackgrounds, tabsHoveredBackgrounds,
                    tabsActiveBackgrounds, tabsSelectedBackground, tabsContents);
        }
    }
}
