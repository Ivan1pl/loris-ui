package com.ivan1pl.lorisui.api.backend;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;

import java.util.List;

/**
 * Draws LorisUI components with a concrete UI toolkit.
 * <p>
 * A backend is the port between the toolkit-agnostic component model and a rendering toolkit. Screens pass their
 * component tree to it through {@link com.ivan1pl.lorisui.api.screen.LorisScreen#render}, and host integrations
 * create the backend and drive rendering.
 */
public interface Backend {
    /**
     * Draws the given top-level components. Called on the host's UI thread each time the host renders, with the
     * display size in pixels and the list most recently returned by
     * {@link com.ivan1pl.lorisui.api.screen.LorisScreen#layout() LorisScreen.layout()} (an empty list before the
     * first layout completes). Backends must not modify the list.
     *
     * @param w                 display width in pixels
     * @param h                 display height in pixels
     * @param components        top-level components to draw, in order; must not be null
     * @param bundleKeyResolver resolver for localized text; must not be null
     */
    void render(int w, int h, List<Component> components, BundleKeyResolver bundleKeyResolver);
}
