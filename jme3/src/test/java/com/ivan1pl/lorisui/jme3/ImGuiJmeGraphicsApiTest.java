package com.ivan1pl.lorisui.jme3;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImGuiJmeGraphicsApiTest {
    @Test
    void shouldSelectOpenGlEsForAngle() {
        Assertions.assertEquals(ImGuiJmeGraphicsApi.OPENGL_ES, ImGuiJmeGraphicsApi.forRenderer("ANGLE_GLES3"));
    }

    @Test
    void shouldSelectOpenGlForLwjglOpenGlRenderers() {
        Assertions.assertEquals(ImGuiJmeGraphicsApi.OPENGL, ImGuiJmeGraphicsApi.forRenderer("LWJGL-OpenGL32"));
        Assertions.assertEquals(ImGuiJmeGraphicsApi.OPENGL, ImGuiJmeGraphicsApi.forRenderer("LWJGL-OpenGL33"));
        Assertions.assertEquals(ImGuiJmeGraphicsApi.OPENGL, ImGuiJmeGraphicsApi.forRenderer("LWJGL-OpenGL45"));
    }

    @Test
    void shouldSelectOpenGlForLwjglOpenGl2Renderer() {
        Assertions.assertEquals(ImGuiJmeGraphicsApi.OPENGL, ImGuiJmeGraphicsApi.forRenderer("LWJGL-OpenGL2"));
    }

    @Test
    void shouldRejectUnsupportedRendererNamingIt() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> ImGuiJmeGraphicsApi.forRenderer("JOGL_OPENGL_FORWARD_COMPATIBLE"));
        Assertions.assertTrue(exception.getMessage().contains("JOGL_OPENGL_FORWARD_COMPATIBLE"));
    }

    @Test
    void shouldRejectEmptyRendererNamingIt() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> ImGuiJmeGraphicsApi.forRenderer(""));
        Assertions.assertTrue(exception.getMessage().contains("''"));
    }

    @Test
    void shouldRejectNullRenderer() {
        Assertions.assertThrows(IllegalStateException.class, () -> ImGuiJmeGraphicsApi.forRenderer(null));
    }
}
