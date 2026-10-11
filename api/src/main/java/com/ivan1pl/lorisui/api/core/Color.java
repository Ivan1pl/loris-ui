package com.ivan1pl.lorisui.api.core;

/**
 * An immutable RGBA color.
 * <p>
 * Each channel is a float in the range {@code 0..1}, where alpha {@code 0} is fully transparent and {@code 1} fully
 * opaque. A color can also be created from, and converted to, an {@code int} packed as {@code 0xAARRGGBB}.
 */
public final class Color {
    /** Opaque red. */
    public static final Color RED = new Color(1f, 0f, 0f, 1f);
    /** Opaque green. */
    public static final Color GREEN = new Color(0f, 1f, 0f, 1f);
    /** Opaque blue. */
    public static final Color BLUE = new Color(0f, 0f, 1f, 1f);
    /** Opaque black. */
    public static final Color BLACK = new Color(0f, 0f, 0f, 1f);
    /** Opaque white. */
    public static final Color WHITE = new Color(1f, 1f, 1f, 1f);
    /** Opaque yellow. */
    public static final Color YELLOW = new Color(1f, 1f, 0f, 1f);
    /** Fully transparent black. */
    public static final Color EMPTY = new Color(0f, 0f, 0f, 0f);

    private final float r;
    private final float g;
    private final float b;
    private final float a;
    private final int rgba;

    /**
     * Creates a color from float channels.
     *
     * @param r red channel, in the range {@code 0..1}
     * @param g green channel, in the range {@code 0..1}
     * @param b blue channel, in the range {@code 0..1}
     * @param a alpha channel, in the range {@code 0..1}
     */
    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.rgba = calculateRGBA();
    }

    /**
     * Creates a color from an {@code int} packed as {@code 0xAARRGGBB}: alpha in the highest byte, then red, green
     * and blue in the lowest byte.
     *
     * @param argb the color packed as {@code 0xAARRGGBB}
     */
    public Color(int argb) {
        this.r = (argb >> 16 & 255) / 255f;
        this.g = (argb >> 8 & 255) / 255f;
        this.b = (argb & 255) / 255f;
        this.a = (argb >> 24 & 255) / 255f;
        this.rgba = argb;
    }

    /**
     * Returns the red channel.
     *
     * @return the red channel, in the range {@code 0..1}
     */
    public float getR() {
        return r;
    }

    /**
     * Returns the green channel.
     *
     * @return the green channel, in the range {@code 0..1}
     */
    public float getG() {
        return g;
    }

    /**
     * Returns the blue channel.
     *
     * @return the blue channel, in the range {@code 0..1}
     */
    public float getB() {
        return b;
    }

    /**
     * Returns the alpha channel.
     *
     * @return the alpha channel, in the range {@code 0..1}
     */
    public float getA() {
        return a;
    }

    /**
     * Returns this color packed as {@code 0xAARRGGBB}, despite the method name: alpha in the highest byte, then
     * red, green and blue in the lowest byte. For a color created from an {@code int}, this is the argument as
     * given; for a color created from float channels, each channel is rounded to 8 bits.
     *
     * @return the color packed as {@code 0xAARRGGBB}
     */
    public int getRGBA() {
        return rgba;
    }

    /**
     * Returns a copy of this color with a different alpha channel.
     *
     * @param a the new alpha channel, in the range {@code 0..1}
     * @return a new color with the red, green and blue channels of this color and the given alpha
     */
    public Color withA(float a) {
        return new Color(this.r, this.g, this.b, a);
    }

    /**
     * Recolors this color by replacing each of its red, green and blue channels with a mask color.
     * <p>
     * The result's RGB is the average of the three mask colors, weighted by this color's red, green and blue
     * channels. A {@code null} mask leaves the contribution of its channel unchanged (it acts as this color), so
     * masking with three {@code null} masks returns an equivalent color.
     * <p>
     * The result's alpha is this color's alpha multiplied by the mask alphas weighted the same way, where a
     * {@code null} mask counts as alpha 1. A transparent color therefore stays transparent, and {@code null} masks
     * never change the alpha.
     *
     * @param red   color that replaces the red channel, or {@code null} to keep it
     * @param green color that replaces the green channel, or {@code null} to keep it
     * @param blue  color that replaces the blue channel, or {@code null} to keep it
     * @return the masked color, or this instance when its red, green and blue channels are all zero
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
