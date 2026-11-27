package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.component.style.TextInputStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;
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
    void shouldRejectNullBindingMaxLengthStyleAndSubmitActionAtSetter() {
        Consumer<String> setter = mockSetter();
        TextInput.Builder builder = TextInput.builder();

        Assertions.assertAll(
                () -> assertNullPointer("getter", () -> builder.value(null, setter)),
                () -> assertNullPointer("setter", () -> builder.value(() -> "", null)),
                () -> assertNullPointer("maxLength", () -> builder.maxLength((Supplier<Integer>) null)),
                () -> assertNullPointer("style", () -> builder.style(null)),
                () -> assertNullPointer("submitAction", () -> builder.submitAction(null)));
    }

    @Test
    void shouldDefaultToEnabledAndEvaluateEnabledSupplierOnEachCall() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        TextInput defaultInput = TextInput.builder()
                .value(() -> "", mockSetter())
                .build();
        TextInput constantInput = TextInput.builder()
                .value(() -> "", mockSetter())
                .enabled(false)
                .build();
        TextInput dynamicInput = TextInput.builder()
                .value(() -> "", mockSetter())
                .enabled(enabled::get)
                .build();

        boolean dynamicBefore = dynamicInput.isEnabled();
        enabled.set(false);

        Assertions.assertAll(
                () -> Assertions.assertTrue(defaultInput.isEnabled()),
                () -> Assertions.assertFalse(constantInput.isEnabled()),
                () -> Assertions.assertTrue(dynamicBefore),
                () -> Assertions.assertFalse(dynamicInput.isEnabled()),
                () -> assertNullPointer("enabled", () -> TextInput.builder().enabled((Supplier<Boolean>) null)));
    }

    @Test
    void shouldReturnDisabledStyleOrNullWhenUnset() {
        TextInputStyle disabledStyle = TextInputStyle.builder().build();
        TextInput defaultInput = TextInput.builder()
                .value(() -> "", mockSetter())
                .build();
        TextInput styledInput = TextInput.builder()
                .value(() -> "", mockSetter())
                .disabledStyle(disabledStyle)
                .build();
        TextInput resetInput = TextInput.builder()
                .value(() -> "", mockSetter())
                .disabledStyle(disabledStyle)
                .disabledStyle(null)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertNull(defaultInput.getDisabledStyle()),
                () -> Assertions.assertSame(disabledStyle, styledInput.getDisabledStyle()),
                () -> Assertions.assertNull(resetInput.getDisabledStyle()));
    }

    @Test
    void shouldDelegateFocusStateToItsHolder() {
        Focusable input = TextInput.builder()
                .value(() -> "", mockSetter())
                .build();

        input.setFocused(true);
        long before = System.nanoTime();
        input.setRequestsFocus(true);
        long after = System.nanoTime();
        long stamp = input.getFocusRequestTime();
        boolean requested = input.requestsFocus();
        input.setRequestsFocus(false);

        Assertions.assertAll(
                () -> Assertions.assertTrue(input.isFocused()),
                () -> Assertions.assertTrue(requested),
                () -> Assertions.assertTrue(stamp - before >= 0 && after - stamp >= 0),
                () -> Assertions.assertFalse(input.requestsFocus()),
                () -> Assertions.assertEquals(stamp, input.getFocusRequestTime()));
    }

    @Test
    void shouldRunSubmitActionWhenSubmittedWhileEnabled() {
        Runnable action = Mockito.mock(Runnable.class);
        TextInput input = TextInput.builder()
                .value(() -> "", mockSetter())
                .submitAction(action)
                .build();

        input.submit();

        Mockito.verify(action, Mockito.times(1)).run();
    }

    @Test
    void shouldNotRunSubmitActionWhenSubmittedWhileDisabled() {
        Runnable action = Mockito.mock(Runnable.class);
        TextInput input = TextInput.builder()
                .value(() -> "", mockSetter())
                .enabled(false)
                .submitAction(action)
                .build();

        input.submit();

        Mockito.verifyNoInteractions(action);
    }

    @Test
    void shouldReadEnabledSupplierWhenSubmitted() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        Runnable action = Mockito.mock(Runnable.class);
        TextInput input = TextInput.builder()
                .value(() -> "", mockSetter())
                .enabled(enabled::get)
                .submitAction(action)
                .build();

        input.submit();
        enabled.set(false);
        input.submit();

        Mockito.verify(action, Mockito.times(1)).run();
    }

    @Test
    void shouldDoNothingWhenSubmittedWithDefaultAction() {
        TextInput input = TextInput.builder()
                .value(() -> "", mockSetter())
                .build();

        Assertions.assertDoesNotThrow(input::submit);
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
