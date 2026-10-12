package com.ivan1pl.lorisui.example.jme3;

import com.jme3.system.AppSettings;

public class LorisJme3Gl3Example {
    static void main() {
        AppSettings settings = LorisJme3Example.createDefaultSettings();
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setTitle("LorisUI jME3 Example (OpenGL 3.3)");
        LorisJme3Example.start(settings);
    }
}
