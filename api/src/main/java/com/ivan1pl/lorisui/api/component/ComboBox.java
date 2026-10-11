package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.ComboBoxItemStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxPopupStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxStyle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A drop-down list from which the user selects one item.
 * <p>
 * Each {@link Item} pairs a value with the {@link Panel} drawn for it in the list. The closed combo box shows the label
 * of the selected item, or the empty-selection label when no item is selected. Items are matched to the selected value
 * with {@link Object#equals(Object)}. The selection is state of the combo box instance: it changes when the user picks
 * an item and can be read and set with {@link #getSelectedItem()} and {@link #setSelectedItem(Object)}. Create
 * instances with {@link #builder(Class)}.
 *
 * @param <ItemType> type of the item values
 */
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

    /**
     * Returns the width of the combo box.
     *
     * @return the width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the combo box.
     *
     * @return the height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the items in the order they are listed. The returned array is shared with this combo box, so callers
     * must not modify it.
     *
     * @return the items; never {@code null}
     */
    public Item<ItemType>[] getItems() {
        return items;
    }

    /**
     * Returns the label shown when no item is selected.
     *
     * @return the empty-selection label; never {@code null}
     */
    public Panel getEmptySelectionLabel() {
        return emptySelectionLabel;
    }

    /**
     * Returns the selected value.
     *
     * @return the selected value, or {@code null} when nothing is selected
     */
    public ItemType getSelectedItem() {
        return selectedItem;
    }

    /**
     * Sets the selected value. A value that matches no item, including {@code null}, shows the empty-selection
     * label.
     *
     * @param selectedItem the value to select, or {@code null} to clear the selection
     */
    public void setSelectedItem(ItemType selectedItem) {
        this.selectedItem = selectedItem;
    }

    /**
     * Returns the label shown for the current selection: the label of the first item whose value equals the selected
     * value, or the empty-selection label when no item matches.
     *
     * @return the label of the current selection; never {@code null}
     */
    public Panel getSelectedItemLabel() {
        for (Item<ItemType> item : items) {
            if (Objects.equals(item.item(), selectedItem)) {
                return item.label();
            }
        }
        return emptySelectionLabel;
    }

    /**
     * Returns the style of the combo box itself.
     *
     * @return the combo box style; never {@code null}
     */
    public ComboBoxStyle getStyle() {
        return style;
    }

    /**
     * Returns the style of the drop-down list.
     *
     * @return the drop-down list style; never {@code null}
     */
    public ComboBoxPopupStyle getPopupStyle() {
        return popupStyle;
    }

    /**
     * Returns the style of the items in the drop-down list.
     *
     * @return the item style; never {@code null}
     */
    public ComboBoxItemStyle getItemStyle() {
        return itemStyle;
    }

    /**
     * Creates a builder for a combo box.
     *
     * @param itemType   class of the item values; used only to infer {@code ItemType}, its value is ignored
     * @param <ItemType> type of the item values
     * @return a new builder
     */
    public static <ItemType> Builder<ItemType> builder(Class<ItemType> itemType) {
        return new Builder<>();
    }

    /**
     * An entry of a combo box: a value and the panel drawn for it.
     *
     * @param item       the value selected by this entry
     * @param label      the panel drawn for this entry in the drop-down list and, when it is selected, in the closed
     *                   combo box
     * @param <ItemType> type of the value
     */
    public record Item<ItemType>(ItemType item, Panel label) {
    }

    /**
     * Builder for {@link ComboBox}, obtained from {@link ComboBox#builder(Class)}.
     * <p>
     * No property is required. Unless set, the combo box has no items, its empty-selection label is an empty panel of
     * size 0 &times; 0, and its styles keep the toolkit defaults. The size is derived from the empty-selection label
     * when it is not set (see {@link #build()}), so set either {@link #size(int, int)} or a sized empty-selection
     * label.
     *
     * @param <ItemType> type of the item values
     */
    public static class Builder<ItemType> {
        private int width = 0;
        private int height = 0;
        private List<Item<ItemType>> items = Collections.emptyList();
        private Panel emptySelectionLabel = Panel.panelBuilder("comboBox.emptySelectionLabel").size(0, 0).build();
        private ComboBoxStyle style = ComboBoxStyle.builder().build();
        private ComboBoxPopupStyle popupStyle = ComboBoxPopupStyle.builder().build();
        private ComboBoxItemStyle itemStyle = ComboBoxItemStyle.builder().build();

        private Builder() {}

        /**
         * Sets the items, replacing any set before. The list is copied when the combo box is built.
         *
         * @param items the items in the order they are listed; must not be null
         * @return this builder
         * @throws NullPointerException if {@code items} is null
         */
        public Builder<ItemType> items(List<Item<ItemType>> items) {
            this.items = Objects.requireNonNull(items, "items");
            return this;
        }

        /**
         * Sets the items, replacing any set before.
         *
         * @param items the items in the order they are listed; must not be null
         * @return this builder
         * @throws NullPointerException if {@code items} is null
         */
        @SafeVarargs
        public final Builder<ItemType> items(Item<ItemType>... items) {
            return items(Arrays.asList(Objects.requireNonNull(items, "items")));
        }

        /**
         * Sets the label shown when no item is selected. The default is an empty panel of size 0 &times; 0.
         *
         * @param emptySelectionLabel the empty-selection label; must not be null
         * @return this builder
         * @throws NullPointerException if {@code emptySelectionLabel} is null
         */
        public Builder<ItemType> emptySelectionLabel(Panel emptySelectionLabel) {
            this.emptySelectionLabel = Objects.requireNonNull(emptySelectionLabel, "emptySelectionLabel");
            return this;
        }

        /**
         * Sets the size of the combo box. A dimension set to 0 (the default) is derived from the empty-selection
         * label when the combo box is built (see {@link #build()}).
         *
         * @param width  the width in pixels, or 0 to derive it
         * @param height the height in pixels, or 0 to derive it
         * @return this builder
         */
        public Builder<ItemType> size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * Sets the style of the combo box itself. The default style keeps the toolkit defaults.
         *
         * @param style the combo box style; must not be null
         * @return this builder
         * @throws NullPointerException if {@code style} is null
         */
        public Builder<ItemType> style(ComboBoxStyle style) {
            this.style = Objects.requireNonNull(style, "style");
            return this;
        }

        /**
         * Sets the style of the drop-down list. The default style keeps the toolkit defaults.
         *
         * @param popupStyle the drop-down list style; must not be null
         * @return this builder
         * @throws NullPointerException if {@code popupStyle} is null
         */
        public Builder<ItemType> popupStyle(ComboBoxPopupStyle popupStyle) {
            this.popupStyle = Objects.requireNonNull(popupStyle, "popupStyle");
            return this;
        }

        /**
         * Sets the style of the items in the drop-down list. The default style keeps the toolkit defaults.
         *
         * @param itemStyle the item style; must not be null
         * @return this builder
         * @throws NullPointerException if {@code itemStyle} is null
         */
        public Builder<ItemType> itemStyle(ComboBoxItemStyle itemStyle) {
            this.itemStyle = Objects.requireNonNull(itemStyle, "itemStyle");
            return this;
        }

        /**
         * Builds the combo box.
         * <p>
         * When the size is unset or 0, the default size comes from the empty-selection label, measured for a screen
         * size of 0 &times; 0: a height of 0 becomes the label height, and a width of 0 becomes the label width plus
         * the height.
         *
         * @return a new {@link ComboBox}
         */
        @SuppressWarnings("unchecked")
        public ComboBox<ItemType> build() {
            int height = this.height == 0 ? emptySelectionLabel.getHeight(0, 0) : this.height;
            int width = this.width == 0 ? emptySelectionLabel.getWidth(0, 0) + height : this.width;
            return new ComboBox<>(width, height, items.toArray(Item[]::new), emptySelectionLabel, style, popupStyle,
                    itemStyle);
        }
    }
}
