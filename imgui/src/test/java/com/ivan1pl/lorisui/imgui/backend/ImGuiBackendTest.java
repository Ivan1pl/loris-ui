package com.ivan1pl.lorisui.imgui.backend;

import com.ivan1pl.lorisui.api.backend.TextureData;
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
import com.ivan1pl.lorisui.imgui.renderer.Renderer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ImGuiBackendTest {
    private final ImGuiBackend backend = new ImGuiBackend() {
        @Override
        public TextureData loadTexture(String resourcePath, Color maskedR, Color maskedG, Color maskedB) {
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
}
