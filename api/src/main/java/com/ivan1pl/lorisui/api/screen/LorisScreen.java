package com.ivan1pl.lorisui.api.screen;

import com.ivan1pl.lorisui.api.backend.Backend;
import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class LorisScreen {
    private static final ExecutorService LAYOUT_UPDATER = Executors.newSingleThreadExecutor();
    private List<Component> content = Collections.emptyList();
    private boolean layoutNeedsUpdate = true;

    protected LorisScreen() {}

    protected void setLayoutNeedsUpdate(boolean layoutNeedsUpdate) {
        this.layoutNeedsUpdate = layoutNeedsUpdate;
    }

    public void render(Backend backend, int w, int h, BundleKeyResolver bundleKeyResolver) {
        if (layoutNeedsUpdate) {
            layoutNeedsUpdate = false;
            LAYOUT_UPDATER.submit(this::updateLayout);
        }
        backend.render(w, h, content, bundleKeyResolver);
    }

    private List<Component> updateLayout() {
        List<Component> newLayout = layout();
        content = newLayout;
        return newLayout;
    }

    public abstract void onStartScreen();
    public abstract void onEndScreen();
    protected abstract List<Component> layout();
}
