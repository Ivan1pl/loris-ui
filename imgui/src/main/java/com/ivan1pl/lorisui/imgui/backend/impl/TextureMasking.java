package com.ivan1pl.lorisui.imgui.backend.impl;

import com.ivan1pl.lorisui.api.core.Color;

import java.nio.ByteBuffer;

public final class TextureMasking {
    private static final int CHANNELS = 4;

    private TextureMasking() {}

    public static void apply(ByteBuffer rgbaPixels, int width, int height, Color maskR, Color maskG, Color maskB) {
        int byteCount = width * height * CHANNELS;
        for (int i = 0; i < byteCount; i += CHANNELS) {
            Color source = new Color(toUnitFloat(rgbaPixels.get(i)), toUnitFloat(rgbaPixels.get(i + 1)),
                    toUnitFloat(rgbaPixels.get(i + 2)), toUnitFloat(rgbaPixels.get(i + 3)));
            RgbaPixels.put(rgbaPixels, i, source.mask(maskR, maskG, maskB).getRGBA());
        }
    }

    private static float toUnitFloat(byte channel) {
        return Byte.toUnsignedInt(channel) / 255f;
    }
}
