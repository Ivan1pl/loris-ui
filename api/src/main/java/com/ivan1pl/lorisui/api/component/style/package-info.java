/**
 * Provides styles that change how components look: colors, rounding, border thickness and similar visual properties.
 *
 * <p>Each style is an immutable object created with its {@code builder()}. Every property is optional: a property
 * that is not set, or is set to {@code null}, keeps the toolkit default. Properties can be given as constants or as
 * {@link java.util.function.Supplier}s; a supplier is evaluated each time the style is applied, so a style can follow
 * changing game state. Numeric values (rounding, thickness, sizes and padding, all in pixels) are passed to the
 * backend as given, without validation.
 *
 * <p>A style is used either by the component it is made for (for example
 * {@link com.ivan1pl.lorisui.api.component.ComboBox.Builder#style(ComboBoxStyle)}) or by adding it to a panel with
 * the panel builder's {@code addStyle(...)}, in which case it applies to the panel and everything inside it.
 *
 * <p>Style properties may be ignored by backends that lack the corresponding feature.
 */
package com.ivan1pl.lorisui.api.component.style;
