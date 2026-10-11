package com.ivan1pl.lorisui.api.resolver;

/**
 * Resolves localization keys into display text.
 * <p>
 * Hosts implement this interface on top of their resource bundles and pass it to
 * {@link com.ivan1pl.lorisui.api.screen.LorisScreen#render}. Components with localized text call it each time they
 * are drawn. The format of the keys is defined by the host.
 */
public interface BundleKeyResolver {
    /**
     * Returns the localized text for a key, formatted with the given arguments.
     *
     * @param key  localization key, in the format defined by the host; must not be null
     * @param args format arguments for the resolved text; empty when the text is used as is
     * @return the localized text
     */
    String resolveResourceKey(String key, Object... args);
}
