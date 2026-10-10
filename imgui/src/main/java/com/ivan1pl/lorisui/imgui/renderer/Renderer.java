package com.ivan1pl.lorisui.imgui.renderer;

import com.ivan1pl.lorisui.api.backend.Backend;
import com.ivan1pl.lorisui.api.backend.TextureData;
import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class Renderer<ComponentType extends Component> {
    // Complex key represented as nested maps to prevent object allocation just to look up a key
    private static final Map<String, Map<Object, Map<Object, Map<Object, TextureData>>>> TEXTURES = new HashMap<>();
    private static final Object NO_COLOR_KEY = new Object();

    public abstract void render(ComponentType component, int w, int h, RendererProvider rendererProvider,
                                BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput);

    protected static TextureData loadTexture(Backend backend, String path) throws IOException {
        return loadTexture(backend, path, null, null, null);
    }

    protected static TextureData loadTexture(Backend backend, String path, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        TextureData textureData = getFromCache(path, maskedR, maskedG, maskedB);
        if (textureData != null) {
            return textureData;
        }

        textureData = backend.loadTexture(path, maskedR, maskedG, maskedB);
        putToCache(path, maskedR, maskedG, maskedB, textureData);
        return textureData;
    }

    private static TextureData getFromCache(String path, Color maskedR, Color maskedG, Color maskedB) {
        var map = TEXTURES.get(path);
        if (map == null) {
            return null;
        }
        var innerR = map.get(maskedR == null ? NO_COLOR_KEY : maskedR);
        if (innerR == null) {
            return null;
        }
        var innerG = innerR.get(maskedG == null ? NO_COLOR_KEY : maskedG);
        if (innerG == null) {
            return null;
        }
        return innerG.get(maskedB == null ? NO_COLOR_KEY : maskedB);
    }

    private static synchronized void putToCache(String path, Color maskedR, Color maskedG, Color maskedB,
                                                TextureData textureData) {
        var map = TEXTURES.get(path);
        if (map == null) {
            TEXTURES.put(path, map = new HashMap<>());
        }
        Object key = maskedR == null ? NO_COLOR_KEY : maskedR;
        var innerR = map.get(key);
        if (innerR == null) {
            map.put(key, innerR = new HashMap<>());
        }
        key = maskedG == null ? NO_COLOR_KEY : maskedG;
        var innerG = innerR.get(key);
        if (innerG == null) {
            innerR.put(key, innerG = new HashMap<>());
        }
        innerG.put(maskedB == null ? NO_COLOR_KEY : maskedB, textureData);
    }
}
