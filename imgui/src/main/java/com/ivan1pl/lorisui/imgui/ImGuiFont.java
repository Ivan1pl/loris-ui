package com.ivan1pl.lorisui.imgui;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.imgui.impl.ImGuiState;
import imgui.ImFont;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;

import java.util.Objects;

/**
 * A TrueType font registered with an {@link ImGuiOpenGlRenderer}, either directly or through a host display.
 *
 * <p>The font belongs to the renderer it was registered with and works only with that renderer. It stays valid across
 * {@linkplain ImGuiOpenGlRenderer#dispose() disposing} and {@linkplain ImGuiOpenGlRenderer#initialize() initializing}
 * the renderer again, so the same instance can be kept in components for the renderer's whole life. For that, the
 * renderer keeps the font's TrueType data in memory for its whole life, which matters for large fonts. The font can
 * be used only while that renderer is initialized. Fonts work only with the Dear ImGui backend; components drawn by
 * any other backend cannot use them.
 *
 * <p>Glyph ranges select the characters a font includes. They use Dear ImGui's zero-terminated range-pair format: pairs
 * of first and last code points (both inclusive), followed by a {@code 0}. For example,
 * {@code new short[] { 0x0020, 0x00FF, 0x0100, 0x017F, 0 }} includes Basic Latin, Latin-1 Supplement and Latin
 * Extended-A. Code points are read as unsigned 16-bit values, so the ranges cover {@code U+0001} to {@code U+FFFF};
 * code points above {@code U+7FFF} are written with a cast, as in {@code { 0x4E00, (short) 0x9FAF, 0 }} for the CJK
 * Unified Ideographs. Every range needs both code points, its first code point must not be above its last, and at least
 * one range must precede the terminating {@code 0}. Without explicit glyph ranges a font includes the default glyph
 * ranges, Basic Latin and Latin-1 Supplement ({@code U+0020} to {@code U+00FF}).
 */
public final class ImGuiFont implements Font {
    static final short[] DEFAULT_GLYPH_RANGES = buildGlyphRanges(new short[] { 0x0020, 0x00FF, 0 });

    private final byte[] data;
    private final String source;
    private final int size;
    // Dear ImGui keeps a raw pointer to the glyph ranges for the font's lifetime, but imgui-java passes it from a JNI
    // critical array that is released after the call, so a moving GC may relocate the array while Dear ImGui still
    // reads it. Keeping the array reachable does not prevent that; it is a known limitation of the imgui-java binding.
    private final short[] glyphRanges;
    private long contextPtr;
    private ImFont font;

    ImGuiFont(byte[] data, String source, int size, short[] glyphRanges) {
        this.data = data;
        this.source = source;
        this.size = size;
        this.glyphRanges = glyphRanges;
    }

    // ImFontGlyphRangesBuilder.addRanges reads code points as signed shorts and drops every range that ends above
    // U+7FFF, so the ranges are validated and added here as unsigned values
    static short[] buildGlyphRanges(short[] glyphRanges) {
        ImFontGlyphRangesBuilder builder = new ImFontGlyphRangesBuilder();
        int index = 0;
        while (index < glyphRanges.length && glyphRanges[index] != 0) {
            if (index + 1 >= glyphRanges.length) {
                throw new IllegalArgumentException("Glyph range at index " + index + " has no last code point");
            }
            int first = Short.toUnsignedInt(glyphRanges[index]);
            int last = Short.toUnsignedInt(glyphRanges[index + 1]);
            if (first > last) {
                throw new IllegalArgumentException(String.format(
                        "Glyph range at index %d starts above its end: U+%04X > U+%04X", index, first, last));
            }
            for (int codePoint = first; codePoint <= last; codePoint++) {
                builder.addChar((char) codePoint);
            }
            index += 2;
        }
        if (index >= glyphRanges.length) {
            throw new IllegalArgumentException("Glyph ranges must end with a terminating 0");
        }
        if (index == 0) {
            throw new IllegalArgumentException("Glyph ranges must contain at least one range before the terminating 0");
        }
        return builder.buildRanges();
    }

    byte[] getData() {
        return data;
    }

    String getSource() {
        return source;
    }

    int getSize() {
        return size;
    }

    short[] getGlyphRanges() {
        return glyphRanges;
    }

    void bind(long contextPtr, ImFont font) {
        this.contextPtr = contextPtr;
        this.font = font;
    }

    void unbind() {
        this.contextPtr = 0L;
        this.font = null;
    }

    /**
     * Runs the given drawing code with this font as the current font, and restores the previous font afterwards, even
     * when the code throws. Intended for renderer authors: call it on the host's UI thread from
     * {@link com.ivan1pl.lorisui.imgui.renderer.Renderer#render Renderer.render}.
     *
     * @param runnable drawing code to run with this font; must not be null
     * @throws NullPointerException  if {@code runnable} is {@code null}
     * @throws IllegalStateException if the renderer this font was registered with is not initialized, or if another
     *                               renderer's Dear ImGui context is current; the code does not run then
     */
    public void useFont(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (font == null || contextPtr != ImGuiState.currentContextPtr) {
            throw new IllegalStateException("Font cannot be used: the renderer it was registered with is not "
                    + "initialized, or another renderer's context is current");
        }
        ImGui.pushFont(font, font.getLegacySize());
        try {
            runnable.run();
        } finally {
            ImGui.popFont();
        }
    }
}
