package com.ivan1pl.lorisui.example.jme3;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

import java.io.IOException;
import java.util.Locale;

public class LorisJme3Example extends SimpleApplication {
    @Override
    public void simpleInitApp() {
        getFlyByCamera().setEnabled(false);
        getInputManager().setCursorVisible(true);

        ImGuiJmeDisplay imGuiJmeDisplay = new ImGuiJmeDisplay(context, inputManager, guiViewPort);
        imGuiJmeDisplay.setResourceBundle("global", "bundles.global");
        Font font;
        try {
            font = imGuiJmeDisplay.registerFontFromResources("/fonts/open-sans.regular.ttf", 20);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ExampleScreen exampleScreen = new ExampleScreen(font);
        imGuiJmeDisplay.addScreen("example", exampleScreen);
        imGuiJmeDisplay.setCurrentScreen("example");
        guiViewPort.addProcessor(imGuiJmeDisplay);
    }

    @Override
    public void destroy() {
        super.destroy();
        System.exit(0);
    }

    private static AppSettings createDefaultSettings() {
        AppSettings settings = new AppSettings(true);
        settings.setHeight(600);
        settings.setWidth(800);
        settings.setFullscreen(false);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        return settings;
    }

    static void main() {
        Locale.setDefault(Locale.ENGLISH);
        var app = new LorisJme3Example();
        app.setShowSettings(false);
        app.setSettings(createDefaultSettings());
        app.start();
    }
}
