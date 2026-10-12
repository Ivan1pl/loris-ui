package com.ivan1pl.lorisui.jme3;

import com.jme3.system.AppSettings;

// References only AppSettings' compile-time String constants, which javac inlines, so no jME class is loaded
enum ImGuiJmeGraphicsApi {
    OPENGL,
    OPENGL_ES;

    private static final String LWJGL_OPENGL_PREFIX = "LWJGL-OpenGL";

    static ImGuiJmeGraphicsApi forRenderer(String jmeRenderer) {
        if (AppSettings.ANGLE_GLES3.equals(jmeRenderer)) {
            return OPENGL_ES;
        }
        if (jmeRenderer != null && jmeRenderer.startsWith(LWJGL_OPENGL_PREFIX)) {
            return OPENGL;
        }
        throw new IllegalStateException("Unsupported jME renderer '" + jmeRenderer + "'; ImGuiJmeDisplay supports "
                + "AppSettings.ANGLE_GLES3 and the AppSettings.LWJGL_OPENGL* renderers");
    }
}
