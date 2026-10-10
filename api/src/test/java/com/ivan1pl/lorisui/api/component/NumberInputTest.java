package com.ivan1pl.lorisui.api.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

class NumberInputTest {
    @Test
    void shouldReturnDecimalValueOrEmptyWhenGetterYieldsNull() {
        AtomicReference<Integer> value = new AtomicReference<>();
        NumberInput numberInput = NumberInput.builder()
                .value(value::get, value::set)
                .build();

        value.set(1234567890);
        Assertions.assertEquals("1234567890", numberInput.getValue());
        value.set(-987654321);
        Assertions.assertEquals("-987654321", numberInput.getValue());
        value.set(Integer.MIN_VALUE);
        Assertions.assertEquals("-2147483648", numberInput.getValue());
        value.set(Integer.MAX_VALUE);
        Assertions.assertEquals("2147483647", numberInput.getValue());
        value.set(null);
        Assertions.assertEquals("", numberInput.getValue());
    }

    @Test
    void shouldClampInputToDefaultAndCustomBounds() {
        Consumer<Integer> defaultSetter = mockSetter();
        NumberInput defaultBounds = NumberInput.builder()
                .value(() -> 0, defaultSetter)
                .build();
        Consumer<Integer> customSetter = mockSetter();
        NumberInput customBounds = NumberInput.builder()
                .minValue(10)
                .maxValue(99)
                .value(() -> 10, customSetter)
                .build();

        defaultBounds.validateInput("-5");
        defaultBounds.validateInput("2147483647");
        defaultBounds.validateInput("42");
        customBounds.validateInput("5");
        customBounds.validateInput("150");
        customBounds.validateInput("42");

        Mockito.verify(defaultSetter).accept(0);
        Mockito.verify(defaultSetter).accept(Integer.MAX_VALUE);
        Mockito.verify(defaultSetter).accept(42);
        Mockito.verify(customSetter).accept(10);
        Mockito.verify(customSetter).accept(99);
        Mockito.verify(customSetter).accept(42);
    }

    @Test
    void shouldIgnoreNonNumericInput() {
        Consumer<Integer> setter = mockSetter();
        NumberInput numberInput = NumberInput.builder()
                .value(() -> 0, setter)
                .build();

        numberInput.validateInput("abc");
        numberInput.validateInput("12a");
        numberInput.validateInput("");

        Mockito.verifyNoInteractions(setter);
    }

    @Test
    void shouldRejectMissingValueBinding() {
        Consumer<Integer> setter = mockSetter();
        NumberInput.Builder builder = NumberInput.builder();

        Assertions.assertAll(
                () -> assertNullPointer("getter", () -> builder.value((Supplier<Integer>) null, setter)),
                () -> assertNullPointer("setter", () -> builder.value(() -> 0, null)),
                () -> assertNullPointer("valueGetter", () -> NumberInput.builder().build()));
    }

    @Test
    void shouldClampToDynamicBoundsReadWhenInputValidated() {
        AtomicReference<Integer> min = new AtomicReference<>(0);
        AtomicReference<Integer> max = new AtomicReference<>(10);
        Consumer<Integer> setter = mockSetter();
        NumberInput numberInput = NumberInput.builder()
                .minValue(min::get)
                .maxValue(max::get)
                .value(() -> 0, setter)
                .build();

        numberInput.validateInput("50");
        max.set(20);
        min.set(5);
        numberInput.validateInput("50");
        numberInput.validateInput("1");

        Mockito.verify(setter).accept(10);
        Mockito.verify(setter).accept(20);
        Mockito.verify(setter).accept(5);
    }

    @Test
    void shouldRejectNullBoundsAndStyleAtSetter() {
        NumberInput.Builder builder = NumberInput.builder();

        Assertions.assertAll(
                () -> assertNullPointer("minValue", () -> builder.minValue((Supplier<Integer>) null)),
                () -> assertNullPointer("maxValue", () -> builder.maxValue((Supplier<Integer>) null)),
                () -> assertNullPointer("style", () -> builder.style(null)));
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Integer> mockSetter() {
        return Mockito.mock(Consumer.class);
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
