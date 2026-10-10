package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Consumer;

class PopupTest {
    @Test
    void shouldStartClosedWhenPopupOrModalPopupBuiltWithoutOpen() {
        Popup popup = Popup.popupBuilder("popup").size(10, 10).build();
        ModalPopup modalPopup = ModalPopup.popupBuilder("modal").size(10, 10).build();

        Assertions.assertAll(
                () -> Assertions.assertFalse(popup.isOpen()),
                () -> Assertions.assertFalse(popup.isShouldOpen()),
                () -> Assertions.assertFalse(modalPopup.isOpen()));
        popup.open();
        Assertions.assertTrue(popup.isShouldOpen());
    }

    @Test
    void shouldRejectNullOpenSetterAndKeepProvidedOne() {
        @SuppressWarnings("unchecked")
        Consumer<Boolean> openSetter = Mockito.mock(Consumer.class);
        Popup.Builder builder = Popup.popupBuilder("popup").size(10, 10);

        NullPointerException exception = Assertions.assertThrows(NullPointerException.class,
                () -> builder.open((Consumer<Boolean>) null));
        Popup popup = builder.open(openSetter).build();

        Assertions.assertEquals("openSetter", exception.getMessage());
        Assertions.assertSame(openSetter, popup.getOpenSetter());
    }
}
