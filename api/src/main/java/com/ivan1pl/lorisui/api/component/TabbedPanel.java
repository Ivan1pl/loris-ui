package com.ivan1pl.lorisui.api.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A component that shows one of several tabs, selected with a bar of tab buttons along its top edge.
 *
 * <p>The tab bar spans the full width of the tabbed panel and splits it equally among the tab buttons, in the order
 * the tabs were added. The content of the selected tab is shown below the tab bar. The first tab is selected
 * initially; clicking another tab's button selects that tab.
 *
 * <p>Create a tabbed panel with {@link #builder()}.
 */
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

    /**
     * Returns the width of the tabbed panel.
     *
     * @return the width, in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the total height of the tabbed panel, including the tab bar.
     *
     * @return the height, in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the height of the tab bar.
     *
     * @return the tab bar height, in pixels
     */
    public int getTabBarHeight() {
        return tabBarHeight;
    }

    /**
     * Returns the panel that forms the tab bar, holding one button per tab.
     *
     * @return the tab bar panel
     */
    public Panel getTabsPanel() {
        return tabsPanel;
    }

    /**
     * Returns the content suppliers of the tabs, in the order the tabs were added. Each supplier is evaluated each
     * time its tab is drawn. Callers must not modify the list.
     *
     * @return the tab content suppliers
     */
    public List<Supplier<Component>> getTabsContents() {
        return tabsContents;
    }

    /**
     * Returns the index of the selected tab.
     *
     * @return the zero-based index of the selected tab
     */
    public int getTabIndex() {
        return tabIndex;
    }

    /**
     * Returns a new builder for a tabbed panel.
     *
     * @return a new tabbed panel builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link TabbedPanel}. Obtain one with {@link TabbedPanel#builder()}.
     *
     * <p>At least one tab must be added with {@code addTab(...)}; {@link #build()} checks this. Set the size with
     * {@link #size(int, int)}: the width defaults to 0 and the height to 30 pixels. The tab bar is 30 pixels high
     * unless set with {@link #tabBarHeight(int)}.
     */
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

        /**
         * Sets the size of the tabbed panel.
         *
         * @param width  the width, in pixels
         * @param height the total height, including the tab bar, in pixels
         * @return this builder
         */
        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * Sets the height of the tab bar. Defaults to 30 pixels.
         *
         * @param tabBarHeight the tab bar height, in pixels
         * @return this builder
         */
        public Builder tabBarHeight(int tabBarHeight) {
            this.tabBarHeight = tabBarHeight;
            return this;
        }

        /**
         * Adds a tab with fixed content, after any tabs added before.
         *
         * @param tabLabel                   creates the content of the tab's button, such as a {@link Text} label,
         *                                   from the button's width and height in pixels; called once, when the
         *                                   tabbed panel is built; must not be null
         * @param tabLabelBackground         background of the tab's button, or {@code null} for no background
         * @param tabLabelHoveredBackground  background of the button while the pointer is over it, or {@code null}
         *                                   to use {@code tabLabelBackground}
         * @param tabLabelActiveBackground   background of the button while it is being pressed, or {@code null} to
         *                                   use the hovered or base background
         * @param tabLabelSelectedBackground background of the button while its tab is selected, or {@code null} to
         *                                   use {@code tabLabelBackground}
         * @param tabContents                the content shown while the tab is selected; must not be null
         * @return this builder
         * @throws NullPointerException if {@code tabLabel} or {@code tabContents} is {@code null}
         */
        public Builder addTab(BiFunction<Integer, Integer, Component> tabLabel, Background tabLabelBackground,
                              Background tabLabelHoveredBackground, Background tabLabelActiveBackground,
                              Background tabLabelSelectedBackground, Component tabContents) {
            Objects.requireNonNull(tabContents, "tabContents");
            return addTab(tabLabel, tabLabelBackground, tabLabelHoveredBackground, tabLabelActiveBackground,
                    tabLabelSelectedBackground, () -> tabContents);
        }

        /**
         * Adds a tab whose content is supplied by a function, after any tabs added before. The supplier is evaluated
         * each time the tab is drawn.
         *
         * @param tabLabel                   creates the content of the tab's button, such as a {@link Text} label,
         *                                   from the button's width and height in pixels; called once, when the
         *                                   tabbed panel is built; must not be null
         * @param tabLabelBackground         background of the tab's button, or {@code null} for no background
         * @param tabLabelHoveredBackground  background of the button while the pointer is over it, or {@code null}
         *                                   to use {@code tabLabelBackground}
         * @param tabLabelActiveBackground   background of the button while it is being pressed, or {@code null} to
         *                                   use the hovered or base background
         * @param tabLabelSelectedBackground background of the button while its tab is selected, or {@code null} to
         *                                   use {@code tabLabelBackground}
         * @param tabContents                supplies the content shown while the tab is selected; must not be null
         *                                   and must not return {@code null}
         * @return this builder
         * @throws NullPointerException if {@code tabLabel} or {@code tabContents} is {@code null}
         */
        public Builder addTab(BiFunction<Integer, Integer, Component> tabLabel, Background tabLabelBackground,
                              Background tabLabelHoveredBackground, Background tabLabelActiveBackground,
                              Background tabLabelSelectedBackground, Supplier<Component> tabContents) {
            Objects.requireNonNull(tabLabel, "tabLabel");
            Objects.requireNonNull(tabContents, "tabContents");
            this.tabs.add(tabLabel);
            this.tabsBackgrounds.add(tabLabelBackground);
            this.tabsHoveredBackgrounds.add(tabLabelHoveredBackground);
            this.tabsActiveBackgrounds.add(tabLabelActiveBackground);
            this.tabsSelectedBackground.add(tabLabelSelectedBackground);
            this.tabsContents.add(tabContents);
            return this;
        }

        /**
         * Builds the tabbed panel. The first tab is selected initially.
         *
         * @return a new {@link TabbedPanel}
         * @throws IllegalStateException if no tab was added
         */
        public TabbedPanel build() {
            if (tabs.isEmpty()) {
                throw new IllegalStateException("TabbedPanel requires at least one tab");
            }
            return new TabbedPanel(width, height, tabBarHeight, tabs, tabsBackgrounds, tabsHoveredBackgrounds,
                    tabsActiveBackgrounds, tabsSelectedBackground, tabsContents);
        }
    }
}
