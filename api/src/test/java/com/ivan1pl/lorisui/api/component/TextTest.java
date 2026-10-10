package com.ivan1pl.lorisui.api.component;

import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.function.Supplier;

class TextTest {
    private static final BundleKeyResolver RESOLVER = (key, _) -> "<" + key + ">";

    @Test
    void shouldComposeTextAppendAndLocalizedPieces() {
        Text text = Text.builder()
                .text("Hello")
                .append(", ")
                .appendLocalized("name")
                .append(() -> "!")
                .width(100)
                .build();

        Assertions.assertEquals("Hello, <name>!", text.getText(RESOLVER));
    }

    @Test
    void shouldTreatNullPiecesAndUnsetTextAsEmpty() {
        Text withNullPiece = Text.builder()
                .text("a")
                .append(() -> null)
                .append(_ -> null)
                .append("b")
                .width(100)
                .build();
        Text unset = Text.builder()
                .width(100)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals("ab", withNullPiece.getText(RESOLVER)),
                () -> Assertions.assertEquals("", unset.getText(RESOLVER)),
                () -> Assertions.assertNull(unset.getColor()));
    }

    @Test
    void shouldRejectNullConstantTextAtSetter() {
        Text.Builder builder = Text.builder();

        Assertions.assertAll(
                () -> assertNullPointer("text", () -> builder.text((String) null)),
                () -> assertNullPointer("text", () -> builder.append((String) null)),
                () -> assertNullPointer("text", () -> builder.text((Supplier<String>) null)),
                () -> assertNullPointer("key", () -> builder.localizedText((String) null)),
                () -> assertNullPointer("key", () -> builder.appendLocalized((String) null)),
                () -> assertNullPointer("align", () -> builder.align(null)));
    }

    @Test
    void shouldRequireWidthWhenBuilt() {
        Text.Builder builder = Text.builder().text("no width");

        assertNullPointer("width", builder::build);
    }

    private static void assertNullPointer(String expectedMessage, Executable executable) {
        Assertions.assertEquals(expectedMessage,
                Assertions.assertThrows(NullPointerException.class, executable).getMessage());
    }
}
