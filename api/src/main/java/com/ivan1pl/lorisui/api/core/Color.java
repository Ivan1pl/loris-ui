package com.ivan1pl.lorisui.api.core;

public class Color {
    public static final Color RED = new Color(1f, 0f, 0f, 1f);
    public static final Color GREEN = new Color(0f, 1f, 0f, 1f);
    public static final Color BLUE = new Color(0f, 0f, 1f, 1f);
    public static final Color BLACK = new Color(0f, 0f, 0f, 1f);
    public static final Color WHITE = new Color(1f, 1f, 1f, 1f);
    public static final Color YELLOW = new Color(1f, 1f, 0f, 1f);
    public static final Color EMPTY = new Color(0f, 0f, 0f, 0f);

    private final float r;
    private final float g;
    private final float b;
    private final float a;
    private final int rgba;

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.rgba = calculateRGBA();
    }

    public Color(int rgba) {
        this.r = (rgba >> 16 & 255) / 255f;
        this.g = (rgba >> 8 & 255) / 255f;
        this.b = (rgba & 255) / 255f;
        this.a = (rgba >> 24 & 255) / 255f;
        this.rgba = rgba;
    }

    public float getR() {
        return r;
    }

    public float getG() {
        return g;
    }

    public float getB() {
        return b;
    }

    public float getA() {
        return a;
    }

    public int getRGBA() {
        return rgba;
    }

    public Color withA(float a) {
        return new Color(this.r, this.g, this.b, a);
    }

    /**
     * Recolours this colour by replacing each of its red, green and blue channels with a mask colour.
     * <p>
     * The result's RGB is the average of the three mask colours, weighted by this colour's red, green and blue
     * channels. A {@code null} mask leaves the contribution of its channel unchanged (it acts as this colour), so
     * masking with three {@code null} masks returns an equivalent colour.
     * <p>
     * The result's alpha is this colour's alpha multiplied by the mask alphas weighted the same way, where a
     * {@code null} mask counts as alpha 1. A transparent colour therefore stays transparent, and {@code null} masks
     * never change the alpha.
     *
     * @param red   colour that replaces the red channel, or {@code null} to keep it
     * @param green colour that replaces the green channel, or {@code null} to keep it
     * @param blue  colour that replaces the blue channel, or {@code null} to keep it
     * @return the masked colour, or this instance when its red, green and blue channels are all zero
     */
    public Color mask(Color red, Color green, Color blue) {
        float div = this.r + this.g + this.b;
        if (div == 0f) {
            return this;
        }
        float redAlpha = red == null ? 1f : red.a;
        float greenAlpha = green == null ? 1f : green.a;
        float blueAlpha = blue == null ? 1f : blue.a;
        red = red == null ? this : red;
        green = green == null ? this : green;
        blue = blue == null ? this : blue;
        float r = this.r * red.r + this.g * green.r + this.b * blue.r;
        float g = this.r * red.g + this.g * green.g + this.b * blue.g;
        float b = this.r * red.b + this.g * green.b + this.b * blue.b;
        float maskAlpha = this.r * redAlpha + this.g * greenAlpha + this.b * blueAlpha;
        return new Color(r / div, g / div, b / div, this.a * maskAlpha / div);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Color color = (Color) o;
        return Float.compare(r, color.r) == 0 && Float.compare(g, color.g) == 0 && Float.compare(b, color.b) == 0 &&
                Float.compare(a, color.a) == 0 && rgba == color.rgba;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(r);
        result = 31 * result + Float.hashCode(g);
        result = 31 * result + Float.hashCode(b);
        result = 31 * result + Float.hashCode(a);
        result = 31 * result + Integer.hashCode(rgba);
        return result;
    }

    private int calculateRGBA() {
        int r = (int)((double)(this.r * 255f) + (double)0.5f);
        int g = (int)((double)(this.g * 255f) + (double)0.5f);
        int b = (int)((double)(this.b * 255f) + (double)0.5f);
        int a = (int)((double)(this.a * 255f) + (double)0.5f);
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }
}
