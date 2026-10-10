package com.ivan1pl.lorisui.imgui.renderer.impl;

import com.ivan1pl.lorisui.api.backend.Backend;
import com.ivan1pl.lorisui.api.backend.TextureData;
import com.ivan1pl.lorisui.api.component.Image;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

import java.io.IOException;

public class ImageRenderer extends Renderer<Image> {
    private static final int FLAGS_CHILD =
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoTitleBar |
            ImGuiWindowFlags.NoResize |
            ImGuiWindowFlags.NoBackground;
    private final Backend backend;

    public ImageRenderer(Backend backend) {
        this.backend = backend;
    }

    @Override
    public void render(Image component, int w, int h, RendererProvider rendererProvider,
                       BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {
        try {
            int flags = enableInput ? FLAGS_CHILD : (FLAGS_CHILD | ImGuiWindowFlags.NoInputs);
            String id = component.toString() + "_img";
            ImGui.beginChild(id, component.getWidth(), component.getHeight(), false, flags);
            TextureData texture = loadTexture(backend, component.getImage());
            backend.renderTexture(texture.id(), component.getImageMode(), component.getWidth(), component.getHeight(),
                    texture.width(), texture.height());
            ImGui.endChild();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
