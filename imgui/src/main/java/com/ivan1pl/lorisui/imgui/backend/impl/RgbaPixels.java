package com.ivan1pl.lorisui.imgui.backend.impl;

import java.nio.ByteBuffer;

public final class RgbaPixels {
    private static final int CHANNELS = 4;

    private RgbaPixels() {}

    public static void put(ByteBuffer rgbaPixels, int byteIndex, int argb) {
        rgbaPixels.put(byteIndex, (byte) (argb >>> 16));
        rgbaPixels.put(byteIndex + 1, (byte) (argb >>> 8));
        rgbaPixels.put(byteIndex + 2, (byte) argb);
        rgbaPixels.put(byteIndex + 3, (byte) (argb >>> 24));
    }

    public static void pack(int[] argb, ByteBuffer rgbaPixels) {
        for (int i = 0; i < argb.length; i++) {
            put(rgbaPixels, i * CHANNELS, argb[i]);
        }
    }
}
