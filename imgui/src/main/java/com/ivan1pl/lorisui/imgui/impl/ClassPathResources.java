package com.ivan1pl.lorisui.imgui.impl;

import java.io.InputStream;

public final class ClassPathResources {
    private ClassPathResources() {}

    public static InputStream open(String resourcePath) {
        String name = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        return ClassPathResources.class.getClassLoader().getResourceAsStream(name);
    }
}
