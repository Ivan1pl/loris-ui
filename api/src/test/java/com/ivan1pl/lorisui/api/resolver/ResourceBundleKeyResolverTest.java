package com.ivan1pl.lorisui.api.resolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.MissingResourceException;

class ResourceBundleKeyResolverTest {
    private final ResourceBundleKeyResolver resolver = new ResourceBundleKeyResolver();

    @Test
    void shouldResolveKeyFromRegisteredBundle() {
        resolver.setResourceBundle("test", "resolver-test");

        Assertions.assertEquals("Start game", resolver.resolveResourceKey("test.menu.start"));
    }

    @Test
    void shouldFormatTextOnlyWhenArgumentsAreGiven() {
        resolver.setResourceBundle("test", "resolver-test");

        Assertions.assertEquals("Score: 3 of 5", resolver.resolveResourceKey("test.menu.score", 3, 5));
        Assertions.assertEquals("Score: %d of %d", resolver.resolveResourceKey("test.menu.score"));
    }

    @Test
    void shouldRejectKeyWithoutBundleName() {
        resolver.setResourceBundle("test", "resolver-test");

        Assertions.assertThrows(IllegalArgumentException.class, () -> resolver.resolveResourceKey("menu"));
    }

    @Test
    void shouldRejectKeyOfUnregisteredBundleNamingBundleAndKey() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> resolver.resolveResourceKey("missing.menu.start"));

        Assertions.assertTrue(exception.getMessage().contains("missing"), exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("missing.menu.start"), exception.getMessage());
    }

    @Test
    void shouldThrowMissingResourceExceptionForUnknownKey() {
        resolver.setResourceBundle("test", "resolver-test");

        Assertions.assertThrows(MissingResourceException.class, () -> resolver.resolveResourceKey("test.menu.quit"));
    }

    @Test
    void shouldReplaceBundleRegisteredUnderSameName() {
        resolver.setResourceBundle("test", "resolver-test");
        resolver.setResourceBundle("test", "resolver-test-other");

        Assertions.assertEquals("Begin", resolver.resolveResourceKey("test.menu.start"));
    }

    @Test
    void shouldKeepRegisteredBundleWhenReplacementBundleIsMissing() {
        resolver.setResourceBundle("test", "resolver-test");

        Assertions.assertThrows(MissingResourceException.class,
                () -> resolver.setResourceBundle("test", "resolver-test-missing"));
        Assertions.assertThrows(NullPointerException.class, () -> resolver.setResourceBundle(null, "resolver-test"));
        Assertions.assertThrows(NullPointerException.class, () -> resolver.setResourceBundle("test", null));

        Assertions.assertEquals("Start game", resolver.resolveResourceKey("test.menu.start"));
    }
}
