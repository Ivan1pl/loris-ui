/**
 * Provides {@link com.ivan1pl.lorisui.api.resolver.BundleKeyResolver}, which turns localization keys into text.
 * <p>
 * Components that display localized text look up their keys through a resolver supplied by the host, so the same
 * component tree can be shown in any language the host has resources for.
 * <p>
 * {@link com.ivan1pl.lorisui.api.resolver.ResourceBundleKeyResolver} is a ready-made resolver that looks keys up in
 * named {@link java.util.ResourceBundle}s.
 */
package com.ivan1pl.lorisui.api.resolver;
