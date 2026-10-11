package com.ivan1pl.lorisui.api.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Resolves localization keys from named {@link ResourceBundle}s.
 * <p>
 * Bundles are registered under a name with {@link #setResourceBundle}. A key has the form {@code bundleName.key}:
 * the part before the first dot names a registered bundle, and the rest is the key within that bundle.
 *
 * <h2>Threading</h2>
 * The resolver is not thread-safe. Register bundles and resolve keys on the host's UI thread, where screens are
 * drawn.
 */
public final class ResourceBundleKeyResolver implements BundleKeyResolver {
    private final Map<String, ResourceBundle> resourceBundles = new HashMap<>();

    /**
     * Creates a resolver with no registered bundles.
     */
    public ResourceBundleKeyResolver() {}

    /**
     * Loads a resource bundle for the default locale and registers it under a name, for use by
     * {@link #resolveResourceKey}. A bundle already registered under the same name is replaced.
     * <p>
     * On the module path, a bundle in a named application module must be outside any package of that module, or in
     * a package that the module opens unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is
     * not enough.
     *
     * @param name the bundle name, used as the first part of localization keys; must not be null
     * @param path the base name of the bundle, as accepted by {@link ResourceBundle#getBundle(String)}, for example
     *             {@code "bundles.global"}; must not be null
     * @throws NullPointerException               if {@code name} or {@code path} is null
     * @throws java.util.MissingResourceException if no bundle is found for {@code path}
     */
    public void setResourceBundle(String name, String path) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(path, "path");
        resourceBundles.put(name, ResourceBundle.getBundle(path));
    }

    /**
     * Returns the localized text for a key from the resource bundles registered with {@link #setResourceBundle}.
     * <p>
     * The key has the form {@code bundleName.key}: the part before the first dot names a registered bundle, and the
     * rest is the key within that bundle. For example, {@code "global.menu.start"} looks up {@code menu.start} in the
     * bundle registered as {@code global}. When arguments are given, the text is formatted with
     * {@link String#formatted(Object...)}; otherwise it is returned as is.
     *
     * @param key  the key in the form {@code bundleName.key}, where {@code bundleName} is registered; must not be
     *             null
     * @param args format arguments for the text; empty when the text is used as is
     * @return the localized text
     * @throws NullPointerException               if {@code key} is null
     * @throws IllegalArgumentException           if {@code key} contains no dot, so it names no bundle
     * @throws IllegalStateException              if no bundle is registered under the bundle name of {@code key}
     * @throws java.util.MissingResourceException if the bundle has no text for the key
     */
    @Override
    public String resolveResourceKey(String key, Object... args) {
        Objects.requireNonNull(key, "key");
        int separator = key.indexOf('.');
        if (separator < 0) {
            throw new IllegalArgumentException("Localization key has no bundle name, expected bundleName.key: " + key);
        }
        String bundleName = key.substring(0, separator);
        ResourceBundle bundle = resourceBundles.get(bundleName);
        if (bundle == null) {
            throw new IllegalStateException(
                    "No resource bundle registered under name: " + bundleName + " (localization key: " + key + ")");
        }
        String result = bundle.getString(key.substring(separator + 1));
        if (args.length > 0) {
            result = result.formatted(args);
        }
        return result;
    }
}
