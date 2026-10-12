package com.ivan1pl.lorisui.example.jme3;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.example.shared.ExampleScreen;
import com.ivan1pl.lorisui.example.shared.ExampleSettings;
import com.ivan1pl.lorisui.jme3.ImGuiJmeDisplay;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.system.AppSettings;

import java.io.IOException;
import java.util.Locale;

public class LorisJme3Example extends SimpleApplication {
    private static final String TOGGLE_UI_MAPPING = "toggleUi";

    private ImGuiJmeDisplay imGuiJmeDisplay;

    @Override
    public void simpleInitApp() {
        getFlyByCamera().setEnabled(false);
        getInputManager().setCursorVisible(true);

        imGuiJmeDisplay = new ImGuiJmeDisplay(context, inputManager, guiViewPort);
        imGuiJmeDisplay.setResourceBundle(ExampleSettings.BUNDLE_NAME, ExampleSettings.BUNDLE_PATH);
        Font font;
        try {
            font = imGuiJmeDisplay.registerFontFromResources(ExampleSettings.FONT_PATH, ExampleSettings.FONT_SIZE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ExampleScreen exampleScreen = new ExampleScreen(font, () -> guiViewPort.removeProcessor(imGuiJmeDisplay));
        imGuiJmeDisplay.addScreen(ExampleSettings.SCREEN_NAME, exampleScreen);
        imGuiJmeDisplay.setCurrentScreen(ExampleSettings.SCREEN_NAME);
        guiViewPort.addProcessor(imGuiJmeDisplay);

        inputManager.addMapping(TOGGLE_UI_MAPPING, new KeyTrigger(KeyInput.KEY_F1));
        inputManager.addListener((ActionListener) (_, isPressed, _) -> {
            if (isPressed) {
                toggleDisplay();
            }
        }, TOGGLE_UI_MAPPING);
    }

    @Override
    public void destroy() {
        if (isDisplayAttached()) {
            guiViewPort.removeProcessor(imGuiJmeDisplay);
        }
        super.destroy();
    }

    private void toggleDisplay() {
        if (isDisplayAttached()) {
            guiViewPort.removeProcessor(imGuiJmeDisplay);
        } else {
            guiViewPort.addProcessor(imGuiJmeDisplay);
        }
    }

    // ViewPort.removeProcessor calls cleanup even for a processor that is not attached
    private boolean isDisplayAttached() {
        return imGuiJmeDisplay != null && guiViewPort.getProcessors().contains(imGuiJmeDisplay);
    }

    static AppSettings createDefaultSettings() {
        AppSettings settings = new AppSettings(true);
        settings.setHeight(ExampleSettings.WINDOW_HEIGHT);
        settings.setWidth(ExampleSettings.WINDOW_WIDTH);
        settings.setFullscreen(false);
        settings.setTitle("LorisUI jME3 Example (OpenGL ES 3 / ANGLE)");
        return settings;
    }

    static void start(AppSettings settings) {
        Locale.setDefault(ExampleSettings.LOCALE);
        var app = new LorisJme3Example();
        app.setShowSettings(false);
        app.setSettings(settings);
        app.start();
    }

    static void main() {
        start(createDefaultSettings());
    }
}
