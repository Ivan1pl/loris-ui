package com.ivan1pl.lorisui.api.resolver;

import java.util.Objects;

/**
 * Resolves localization keys into display text.
 * <p>
 * Hosts supply an implementation, such as {@link ResourceBundleKeyResolver}, and pass it to
 * {@link com.ivan1pl.lorisui.api.screen.LorisScreen#render}. Components with localized text call it each time they
 * are drawn. The format of the keys is defined by the implementation.
 */
public interface BundleKeyResolver {
    /**
     * Returns the localized text for a key, formatted with the given arguments.
     *
     * @param key  localization key, in the format defined by the implementation; must not be null
     * @param args format arguments for the resolved text; empty when the text is used as is
     * @return the localized text
     */
    String resolveResourceKey(String key, Object... args);

    /**
     * Returns whether a key can be resolved into text.
     * <p>
     * By default, the key is resolved with {@link #resolveResourceKey} without arguments, and the key counts as
     * present when that returns without throwing a {@link RuntimeException}. Implementations that can check a key
     * without resolving it should override this method.
     *
     * @param key localization key, in the format defined by the implementation; must not be null
     * @return {@code true} if {@link #resolveResourceKey} returns text for the key, {@code false} otherwise
     * @throws NullPointerException if {@code key} is null
     */
    default boolean containsKey(String key) {
        Objects.requireNonNull(key, "key");
        try {
            resolveResourceKey(key);
            return true;
        } catch (RuntimeException _) {
            return false;
        }
    }
}
