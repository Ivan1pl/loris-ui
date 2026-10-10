package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.ComboBoxItemStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxPopupStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxStyle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ComboBox<ItemType> implements Component {
    private final int width;
    private final int height;
    private final Item<ItemType>[] items;
    private final Panel emptySelectionLabel;
    private final ComboBoxStyle style;
    private final ComboBoxPopupStyle popupStyle;
    private final ComboBoxItemStyle itemStyle;
    private ItemType selectedItem;

    private ComboBox(int width, int height, Item<ItemType>[] items, Panel emptySelectionLabel, ComboBoxStyle style,
                     ComboBoxPopupStyle popupStyle, ComboBoxItemStyle itemStyle) {
        this.width = width;
        this.height = height;
        this.items = items;
        this.emptySelectionLabel = emptySelectionLabel;
        this.style = style;
        this.popupStyle = popupStyle;
        this.itemStyle = itemStyle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Item<ItemType>[] getItems() {
        return items;
    }

    public Panel getEmptySelectionLabel() {
        return emptySelectionLabel;
    }

    public ItemType getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(ItemType selectedItem) {
        this.selectedItem = selectedItem;
    }

    public Panel getSelectedItemLabel() {
        for (Item<ItemType> item : items) {
            if (Objects.equals(item.item(), selectedItem)) {
                return item.label();
            }
        }
        return emptySelectionLabel;
    }

    public ComboBoxStyle getStyle() {
        return style;
    }

    public ComboBoxPopupStyle getPopupStyle() {
        return popupStyle;
    }

    public ComboBoxItemStyle getItemStyle() {
        return itemStyle;
    }

    public static <ItemType> Builder<ItemType> builder(Class<ItemType> ignored) {
        return new Builder<>();
    }

    public record Item<ItemType>(ItemType item, Panel label) {
    }

    /**
     * Builder for {@link ComboBox}. Required arguments are checked eagerly; Supplier setters reject null; constant
     * overloads of optional properties accept null meaning unset/toolkit default.
     */
    public static class Builder<ItemType> {
        private int width = 0;
        private int height = 0;
        private List<Item<ItemType>> items = Collections.emptyList();
        private Panel emptySelectionLabel = Panel.panelBuilder("##ComboBox#EmptySelectionLabel").size(0, 0).build();
        private ComboBoxStyle style = ComboBoxStyle.builder().build();
        private ComboBoxPopupStyle popupStyle = ComboBoxPopupStyle.builder().build();
        private ComboBoxItemStyle itemStyle = ComboBoxItemStyle.builder().build();

        public Builder<ItemType> items(List<Item<ItemType>> items) {
            this.items = Objects.requireNonNull(items, "items");
            return this;
        }

        @SafeVarargs
        public final Builder<ItemType> items(Item<ItemType>... items) {
            return items(Arrays.asList(Objects.requireNonNull(items, "items")));
        }

        public Builder<ItemType> emptySelectionLabel(Panel emptySelectionLabel) {
            this.emptySelectionLabel = Objects.requireNonNull(emptySelectionLabel, "emptySelectionLabel");
            return this;
        }

        public Builder<ItemType> size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder<ItemType> style(ComboBoxStyle style) {
            this.style = Objects.requireNonNull(style, "style");
            return this;
        }

        public Builder<ItemType> popupStyle(ComboBoxPopupStyle popupStyle) {
            this.popupStyle = Objects.requireNonNull(popupStyle, "popupStyle");
            return this;
        }

        public Builder<ItemType> itemStyle(ComboBoxItemStyle itemStyle) {
            this.itemStyle = Objects.requireNonNull(itemStyle, "itemStyle");
            return this;
        }

        @SuppressWarnings("unchecked")
        public ComboBox<ItemType> build() {
            int height = this.height == 0 ? emptySelectionLabel.getHeight(0, 0) : this.height;
            int width = this.width == 0 ? emptySelectionLabel.getWidth(0, 0) + height : this.width;
            return new ComboBox<>(width, height, items.toArray(Item[]::new), emptySelectionLabel, style, popupStyle,
                    itemStyle);
        }
    }
}
