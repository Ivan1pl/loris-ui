package com.ivan1pl.lorisui.api.resolver;

public interface BundleKeyResolver {
    String resolveResourceKey(String key, Object... args);
}
