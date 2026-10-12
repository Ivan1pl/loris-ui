package com.ivan1pl.lorisui.imgui.backend.impl;

public final class TextureTiling {
    private TextureTiling() {}

    public static int[] spans(int length, int tileLength) {
        if (tileLength <= 0) {
            throw new IllegalArgumentException("Tile length must be positive: " + tileLength);
        }
        if (length <= 0) {
            return new int[0];
        }
        int fullTiles = length / tileLength;
        int remainder = length % tileLength;
        int[] spans = new int[remainder > 0 ? fullTiles + 1 : fullTiles];
        for (int i = 0; i < fullTiles; i++) {
            spans[i] = tileLength;
        }
        if (remainder > 0) {
            spans[fullTiles] = remainder;
        }
        return spans;
    }
}
