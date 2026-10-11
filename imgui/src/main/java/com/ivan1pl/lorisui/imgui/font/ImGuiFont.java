package com.ivan1pl.lorisui.imgui.font;

import com.ivan1pl.lorisui.api.font.Font;
import com.ivan1pl.lorisui.imgui.impl.ClassPathResources;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;

import java.io.IOException;
import java.io.InputStream;

/**
 * A TrueType font for the Dear ImGui backend, loaded from a file or a class-path resource.
 *
 * <p>Fonts work only with the Dear ImGui backend; components drawn by any other backend cannot use them. Hosts usually
 * load fonts on the application's behalf (the jMonkeyEngine host through its {@code registerFont*} methods). A host
 * that drives {@link com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer} itself loads fonts on its UI thread, after creating
 * the renderer and before {@linkplain com.ivan1pl.lorisui.imgui.ImGuiOpenGlRenderer#initialize() initializing} it.
 *
 * <p>Glyph ranges select the characters a font includes. They use Dear ImGui's zero-terminated range-pair format: pairs
 * of first and last code points (both inclusive), followed by a {@code 0}. For example,
 * {@code new short[] { 0x0020, 0x00FF, 0x0100, 0x017F, 0 }} includes Basic Latin, Latin-1 Supplement and Latin
 * Extended-A. Without explicit glyph ranges a font includes the default glyph ranges, Basic Latin and Latin-1
 * Supplement ({@code U+0020} to {@code U+00FF}).
 */
public final class ImGuiFont implements Font {
    private static final short[] DEFAULT_GLYPH_RANGES;

    static {
        var builder = new ImFontGlyphRangesBuilder();
        builder.addRanges(new short[] { 0x0020, 0x00FF });
        DEFAULT_GLYPH_RANGES = builder.buildRanges();
    }

    private final ImFont font;

    private ImGuiFont(ImFont font) {
        this.font = font;
    }

    /**
     * Loads a font from a TrueType file with the default glyph ranges, Basic Latin and Latin-1 Supplement
     * ({@code U+0020} to {@code U+00FF}).
     *
     * @param file path of the TrueType file on the file system; must not be null
     * @param size font size in pixels
     * @return the loaded font
     */
    public static ImGuiFont loadFromFile(String file, int size) {
        return doLoadFromFile(file, size, DEFAULT_GLYPH_RANGES);
    }

    /**
     * Loads a font from a TrueType file with the given glyph ranges.
     *
     * @param file        path of the TrueType file on the file system; must not be null
     * @param size        font size in pixels
     * @param glyphRanges characters to include, in Dear ImGui's zero-terminated range-pair format; must not be null
     * @return the loaded font
     */
    public static ImGuiFont loadFromFile(String file, int size, short[] glyphRanges) {
        var builder = new ImFontGlyphRangesBuilder();
        builder.addRanges(glyphRanges);
        return doLoadFromFile(file, size, builder.buildRanges());
    }

    private static ImGuiFont doLoadFromFile(String file, int size, short[] glyphRanges) {
        return new ImGuiFont(ImGui.getIO().getFonts().addFontFromFileTTF(
                file, size, new ImFontConfig(), glyphRanges));
    }

    /**
     * Loads a font from a TrueType class-path resource with the default glyph ranges, Basic Latin and Latin-1
     * Supplement ({@code U+0020} to {@code U+00FF}).
     *
     * <p>{@code resource} is a class-path resource name, where a leading {@code /} is optional. On the module path the
     * resource must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * @param resource class-path resource name of the TrueType font; must not be null
     * @param size     font size in pixels
     * @return the loaded font
     * @throws IOException if the resource does not exist (the message names {@code resource}) or cannot be read
     */
    public static ImGuiFont loadFromResource(String resource, int size) throws IOException {
        return doLoadFromResource(resource, size, DEFAULT_GLYPH_RANGES);
    }

    /**
     * Loads a font from a TrueType class-path resource with the given glyph ranges.
     *
     * <p>{@code resource} is a class-path resource name, where a leading {@code /} is optional. On the module path the
     * resource must be outside any Java package of a named module, or in a package that the application module opens
     * unconditionally ({@code opens pkg;}). A qualified {@code opens pkg to ...} is not enough.
     *
     * @param resource    class-path resource name of the TrueType font; must not be null
     * @param size        font size in pixels
     * @param glyphRanges characters to include, in Dear ImGui's zero-terminated range-pair format; must not be null
     * @return the loaded font
     * @throws IOException if the resource does not exist (the message names {@code resource}) or cannot be read
     */
    public static ImGuiFont loadFromResource(String resource, int size, short[] glyphRanges) throws IOException {
        var builder = new ImFontGlyphRangesBuilder();
        builder.addRanges(glyphRanges);
        return doLoadFromResource(resource, size, builder.buildRanges());
    }

    private static ImGuiFont doLoadFromResource(String resource, int size, short[] glyphRanges) throws IOException {
        try (InputStream inputStream = ClassPathResources.open(resource)) {
            if (inputStream == null) {
                throw new IOException("Resource '" + resource + "' not found");
            }
            byte[] data = inputStream.readAllBytes();
            return new ImGuiFont(ImGui.getIO().getFonts().addFontFromMemoryTTF(
                    data, size, new ImFontConfig(), glyphRanges));
        }
    }

    /**
     * Runs the given drawing code with this font as the current font, and restores the previous font afterwards, even
     * when the code throws. Intended for renderer authors: call it on the host's UI thread from
     * {@link com.ivan1pl.lorisui.imgui.renderer.Renderer#render Renderer.render}.
     *
     * @param runnable drawing code to run with this font; must not be null
     */
    public void useFont(Runnable runnable) {
        ImGui.pushFont(this.font, this.font.getLegacySize());
        try {
            runnable.run();
        } finally {
            ImGui.popFont();
        }
    }
}
