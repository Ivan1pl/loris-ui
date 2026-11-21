package com.ivan1pl.lorisui.api.resolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

class BundleKeyResolverTest {
    @Test
    void shouldContainKeyByDefaultWhenItResolvesWithoutArguments() {
        List<Integer> argumentCounts = new ArrayList<>();
        BundleKeyResolver resolver = (key, args) -> {
            argumentCounts.add(args.length);
            return key;
        };

        Assertions.assertTrue(resolver.containsKey("menu.start"));
        Assertions.assertEquals(List.of(0), argumentCounts);
    }

    @Test
    void shouldNotContainKeyByDefaultWhenResolvingThrows() {
        BundleKeyResolver resolver = (key, _) -> {
            throw new MissingResourceException("missing", "bundle", key);
        };

        Assertions.assertFalse(resolver.containsKey("menu.start"));
    }

    @Test
    void shouldRejectNullKeyByDefaultWithoutResolving() {
        BundleKeyResolver resolver = (_, _) -> {
            throw new AssertionError("must not resolve");
        };

        Assertions.assertEquals("key",
                Assertions.assertThrows(NullPointerException.class, () -> resolver.containsKey(null)).getMessage());
    }
}
