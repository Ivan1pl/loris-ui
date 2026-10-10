package com.ivan1pl.lorisui.api.backend;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.ImageMode;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.io.IOException;
import java.util.List;

public interface Backend {
    void render(int w, int h, List<Component> components, BundleKeyResolver bundleKeyResolver);
    void renderTexture(int id, ImageMode imageMode, int width, int height, int textureWidth, int textureHeight);
    TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) throws IOException;
}
