package com.ivan1pl.lorisui.imgui.font;

import com.ivan1pl.lorisui.api.font.Font;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;

import java.io.IOException;

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

    public static ImGuiFont loadFromFile(String file, int size) {
        return doLoadFromFile(file, size, DEFAULT_GLYPH_RANGES);
    }

    public static ImGuiFont loadFromFile(String file, int size, short[] glyphRanges) {
        var builder = new ImFontGlyphRangesBuilder();
        builder.addRanges(glyphRanges);
        return doLoadFromFile(file, size, builder.buildRanges());
    }

    private static ImGuiFont doLoadFromFile(String file, int size, short[] glyphRanges) {
        return new ImGuiFont(ImGui.getIO().getFonts().addFontFromFileTTF(
                file, size, new ImFontConfig(), glyphRanges));
    }

    public static ImGuiFont loadFromResource(String resource, int size) throws IOException {
        return doLoadFromResource(resource, size, DEFAULT_GLYPH_RANGES);
    }

    public static ImGuiFont loadFromResource(String resource, int size, short[] glyphRanges) throws IOException {
        var builder = new ImFontGlyphRangesBuilder();
        builder.addRanges(glyphRanges);
        return doLoadFromResource(resource, size, builder.buildRanges());
    }

    private static ImGuiFont doLoadFromResource(String resource, int size, short[] glyphRanges) throws IOException {
        byte[] data = ImGuiFont.class.getResourceAsStream(resource).readAllBytes();
        return new ImGuiFont(ImGui.getIO().getFonts().addFontFromMemoryTTF(
                data, size, new ImFontConfig(), glyphRanges));
    }

    public void useFont(Runnable runnable) {
        ImGui.pushFont(this.font, this.font.getLegacySize());
        try {
            runnable.run();
        } finally {
            ImGui.popFont();
        }
    }
}
