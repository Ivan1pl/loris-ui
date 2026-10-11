package com.ivan1pl.lorisui.imgui.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class ClassPathResourcesTest {
    private static final String EXPECTED_CONTENT = "class-path resource content";

    @Test
    void shouldOpenResourceByName() throws IOException {
        try (InputStream in = ClassPathResources.open("classpath-resources-test.txt")) {
            Assertions.assertNotNull(in);
            Assertions.assertEquals(EXPECTED_CONTENT, new String(in.readAllBytes(), StandardCharsets.UTF_8).strip());
        }
    }

    @Test
    void shouldOpenResourceByNameWithLeadingSlash() throws IOException {
        try (InputStream in = ClassPathResources.open("/classpath-resources-test.txt")) {
            Assertions.assertNotNull(in);
            Assertions.assertEquals(EXPECTED_CONTENT, new String(in.readAllBytes(), StandardCharsets.UTF_8).strip());
        }
    }

    @Test
    void shouldReturnNullForMissingResource() throws IOException {
        try (InputStream in = ClassPathResources.open("missing-resource.txt")) {
            Assertions.assertNull(in);
        }
    }
}
