package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

class TextInputTest {
    @Test
    void shouldReturnValueOrEmptyWhenGetterYieldsNull() {
        String longUtf8Value = "zażółć gęślą jaźń 漢字 ".repeat(200);
        AtomicReference<String> value = new AtomicReference<>(longUtf8Value);
        TextInput textInput = TextInput.builder()
                .maxLength(10000)
                .value(value::get, value::set)
                .build();

        Assertions.assertEquals(longUtf8Value, textInput.getValue());
        value.set(null);
        Assertions.assertEquals("", textInput.getValue());
    }

    @Test
    void shouldTruncateToDefaultAndCustomMaxLength() {
        Consumer<String> defaultSetter = mockSetter();
        TextInput defaultMaxLength = TextInput.builder()
                .value(() -> "", defaultSetter)
                .build();
        Consumer<String> customSetter = mockSetter();
        TextInput customMaxLength = TextInput.builder()
                .maxLength(5)
                .value(() -> "", customSetter)
                .build();

        defaultMaxLength.validateInput("a".repeat(150));
        defaultMaxLength.validateInput("short");
        customMaxLength.validateInput("abcdefgh");
        customMaxLength.validateInput("abc");

        Mockito.verify(defaultSetter).accept("a".repeat(100));
        Mockito.verify(defaultSetter).accept("short");
        Mockito.verify(customSetter).accept("abcde");
        Mockito.verify(customSetter).accept("abc");
    }

    @Test
    void shouldRequireValueBindingWhenBuilt() {
        NullPointerException exception = Assertions.assertThrows(NullPointerException.class,
                () -> TextInput.builder().build());

        Assertions.assertEquals("valueGetter", exception.getMessage());
    }

    @Test
    void shouldTruncateToDynamicMaxLengthReadWhenInputValidated() {
        AtomicReference<Integer> maxLength = new AtomicReference<>(3);
        Consumer<String> setter = mockSetter();
        TextInput textInput = TextInput.builder()
                .maxLength(maxLength::get)
                .value(() -> "", setter)
                .build();

        textInput.validateInput("abcdef");
        maxLength.set(5);
        textInput.validateInput("abcdef");

        Mockito.verify(setter).accept("abc");
        Mockito.verify(setter).accept("abcde");
    }

    @Test
    void shouldRejectNullBindingMaxLengthAndStyleAtSetter() {
        Consumer<String> setter = mockSetter();
        TextInput.Builder builder = TextInput.builder();

        Assertions.assertAll(
                () -> assertNullPointer("getter", () -> builder.value(null, setter)),
                () -> assertNullPointer("setter", () -> builder.value(() -> "", null)),
                () -> assertNullPointer("maxLength", () -> builder.maxLength((Supplier<Integer>) null)),
                () -> assertNullPointer("style", () -> builder.style(null)));
    }

    @SuppressWarnings("unchecked")
    private static Consumer<String> mockSetter() {
        return Mockito.mock(Consumer.class);
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
