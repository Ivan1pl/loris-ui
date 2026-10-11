package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.component.Button;
import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.component.ModalPopup;
import com.ivan1pl.lorisui.api.component.Panel;
import com.ivan1pl.lorisui.api.component.Popup;
import com.ivan1pl.lorisui.api.component.style.ComboBoxItemStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxPopupStyle;
import com.ivan1pl.lorisui.api.component.style.ComboBoxStyle;
import com.ivan1pl.lorisui.api.component.style.ScrollbarStyle;
import com.ivan1pl.lorisui.api.component.style.Style;
import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import com.ivan1pl.lorisui.imgui.renderer.RendererProvider;
import com.ivan1pl.lorisui.imgui.renderer.style.StyleApplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ImGuiBackendTest {
    private final ImGuiBackend backend = new ImGuiBackend() {
        @Override
        protected TextureData createTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void deleteTexture(TextureData texture) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void shouldResolvePanelRendererForButtonPopupAndModalPopup() {
        Renderer<?> panelRenderer = backend.getRenderer(Panel.panelBuilder("panel").size(10, 10).build());

        Assertions.assertSame(panelRenderer, backend.getRenderer(Button.buttonBuilder("button").size(10, 10).build()));
        Assertions.assertSame(panelRenderer, backend.getRenderer(Popup.popupBuilder("popup").size(10, 10).build()));
        Assertions.assertSame(panelRenderer,
                backend.getRenderer(ModalPopup.popupBuilder("modal").size(10, 10).build()));
    }

    @Test
    void shouldThrowWhenComponentHasNoRenderer() {
        Component unknown = new Component() {};

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> backend.getRenderer(unknown));

        Assertions.assertTrue(thrown.getMessage().startsWith("Missing renderer for class"), thrown.getMessage());
    }

    @Test
    void shouldThrowWhenStyleHasNoApplier() {
        Style unknown = new Style() {};

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> backend.getStyleApplier(unknown));

        Assertions.assertTrue(thrown.getMessage().startsWith("Missing style applier for class"), thrown.getMessage());
    }

    @Test
    void shouldResolveApplierForEveryRegisteredStyle() {
        List<Style> styles = List.of(
                ScrollbarStyle.builder().build(),
                ComboBoxStyle.builder().build(),
                ComboBoxPopupStyle.builder().build(),
                ComboBoxItemStyle.builder().build(),
                TextInputStyle.builder().build());

        for (Style style : styles) {
            Assertions.assertNotNull(backend.getStyleApplier(style), style.getClass().getName());
        }
    }

    @Test
    void shouldReturnRendererRegisteredForCustomComponent() {
        Renderer<Gauge> gaugeRenderer = new NoOpRenderer<>();

        backend.registerRenderer(Gauge.class, gaugeRenderer);

        Assertions.assertSame(gaugeRenderer, backend.getRenderer(new Gauge()));
    }

    @Test
    void shouldReplaceBuiltInPanelRendererAlsoForSubclasses() {
        Renderer<Panel> panelRenderer = new NoOpRenderer<>();

        backend.registerRenderer(Panel.class, panelRenderer);

        Assertions.assertSame(panelRenderer, backend.getRenderer(Panel.panelBuilder("panel").size(10, 10).build()));
        Assertions.assertSame(panelRenderer, backend.getRenderer(Button.buttonBuilder("button").size(10, 10).build()));
    }

    @Test
    void shouldReturnRegisteredStyleApplierAndReplaceBuiltIn() {
        StyleApplier<GaugeStyle> gaugeStyleApplier = new NoOpStyleApplier<>();
        StyleApplier<ScrollbarStyle> scrollbarStyleApplier = new NoOpStyleApplier<>();

        backend.registerStyleApplier(GaugeStyle.class, gaugeStyleApplier);
        backend.registerStyleApplier(ScrollbarStyle.class, scrollbarStyleApplier);

        Assertions.assertSame(gaugeStyleApplier, backend.getStyleApplier(new GaugeStyle()));
        Assertions.assertSame(scrollbarStyleApplier, backend.getStyleApplier(ScrollbarStyle.builder().build()));
    }

    @Test
    void shouldRejectNullRegistrationArguments() {
        Assertions.assertThrows(NullPointerException.class,
                () -> backend.registerRenderer(null, new NoOpRenderer<Gauge>()));
        Assertions.assertThrows(NullPointerException.class, () -> backend.registerRenderer(Gauge.class, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> backend.registerStyleApplier(null, new NoOpStyleApplier<GaugeStyle>()));
        Assertions.assertThrows(NullPointerException.class,
                () -> backend.registerStyleApplier(GaugeStyle.class, null));
    }

    @Test
    void shouldRejectRegistrationForInterfaceClassesNamingTheClass() {
        IllegalArgumentException rendererFailure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> backend.registerRenderer(Component.class, new NoOpRenderer<Component>()));
        IllegalArgumentException styleApplierFailure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> backend.registerStyleApplier(Style.class, new NoOpStyleApplier<Style>()));

        Assertions.assertTrue(rendererFailure.getMessage().contains(Component.class.getName()),
                rendererFailure.getMessage());
        Assertions.assertTrue(styleApplierFailure.getMessage().contains(Style.class.getName()),
                styleApplierFailure.getMessage());
    }

    private static final class Gauge implements Component {}

    private static final class GaugeStyle implements Style {}

    private static final class NoOpRenderer<ComponentType extends Component> extends Renderer<ComponentType> {
        @Override
        public void render(ComponentType component, int w, int h, RendererProvider rendererProvider,
                           BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput) {}
    }

    private static final class NoOpStyleApplier<StyleType extends Style> implements StyleApplier<StyleType> {
        @Override
        public Styles applyStyles(StyleType style, RendererProvider rendererProvider) {
            return new Styles() {};
        }

        @Override
        public void popStyles(Styles styles) {}
    }
}
