package com.ivan1pl.lorisui.jme3;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.api.screen.LorisScreen;
import com.ivan1pl.lorisui.imgui.backend.ImGuiBackend;
import com.ivan1pl.lorisui.imgui.backend.ImGuiOpenGlBackend;
import com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer;
import com.ivan1pl.lorisui.imgui.font.ImGuiFont;
import com.jme3.input.InputManager;
import com.jme3.input.lwjgl.SdlEventListener;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.JmeContext;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.texture.FrameBuffer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ImGuiJmeDisplay implements SceneProcessor, BundleKeyResolver {
    private final ImGuiBackend backend = new ImGuiOpenGlBackend();
    private final LwjglWindow window;
    private final SdlEventListener sdlEventListener;
    private final InputManager inputManager;
    private final ImGuiOpenGlRenderer renderer;
    private RenderManager renderManager;
    private ViewPort vp;
    private int w;
    private int h;
    private boolean initialized = false;
    private final ImGuiInputSystem inputSys = new ImGuiInputSystem();
    private final Map<String, LorisScreen> screens = new HashMap<>();
    private final Map<String, ResourceBundle> resourceBundles = new HashMap<>();
    private String currentScreen;

    public ImGuiJmeDisplay(JmeContext context, InputManager inputManager, ViewPort viewport) {
        this.window = (LwjglWindow) context;
        this.inputManager = inputManager;
        this.renderer = new ImGuiOpenGlRenderer(backend, this.window.getWindowHandle());
        this.sdlEventListener = event -> this.renderer.processSdlEvent(event.address());
        this.w = viewport.getCamera().getWidth();
        this.h = viewport.getCamera().getHeight();
        this.vp = viewport;
    }

    public Font registerFontFromFile(String ttfFilePath, int size) {
        if (initialized) {
            throw new IllegalStateException("Already initialized!");
        }
        return ImGuiFont.loadFromFile(ttfFilePath, size);
    }

    public Font registerFontFromFile(String ttfFilePath, int size, short[] glyphRanges) {
        if (initialized) {
            throw new IllegalStateException("Already initialized!");
        }
        return ImGuiFont.loadFromFile(ttfFilePath, size, glyphRanges);
    }

    public Font registerFontFromResources(String ttfResourcePath, int size) throws IOException {
        if (initialized) {
            throw new IllegalStateException("Already initialized!");
        }
        return ImGuiFont.loadFromResource(ttfResourcePath, size);
    }

    public Font registerFontFromResources(String ttfResourcePath, int size, short[] glyphRanges) throws IOException {
        if (initialized) {
            throw new IllegalStateException("Already initialized!");
        }
        return ImGuiFont.loadFromResource(ttfResourcePath, size, glyphRanges);
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        renderer.initialize();
        this.window.registerSdlEventListener(sdlEventListener);
        this.renderManager = rm;

        if (inputManager != null) {
            inputManager.addRawInputListener(inputSys);
        }
        initialized = true;
        this.vp = vp;

        Camera camera = vp.getCamera();
        this.w = camera.getWidth();
        this.h = camera.getHeight();

        this.screens.get(currentScreen).onStartScreen();
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        this.w = w;
        this.h = h;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void preFrame(float tpf) {
    }

    @Override
    public void postQueue(RenderQueue rq) {
        renderManager.setCamera(vp.getCamera(), true);
        renderer.render(screens.get(currentScreen), w, h, this);
        renderManager.setCamera(vp.getCamera(), false);
    }

    @Override
    public void postFrame(FrameBuffer out) {
    }

    @Override
    public void cleanup() {
        initialized = false;
        if (inputManager != null) {
            inputManager.removeRawInputListener(inputSys);
        }
        this.window.removeSdlEventListener(sdlEventListener);
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
    }

    public void addScreen(String name, LorisScreen screen) {
        screens.put(name, screen);
    }

    public void setCurrentScreen(String name) {
        this.currentScreen = name;
    }

    public void setResourceBundle(String name, String path) {
        resourceBundles.put(name, ResourceBundle.getBundle(path));
    }

    @Override
    public String resolveResourceKey(String key, Object... args) {
        String[] parts = key.split("\\.", 2);
        String result = resourceBundles.get(parts[0]).getString(parts[1]);
        if (args.length > 0) {
            result = result.formatted(args);
        }
        return result;
    }
}
