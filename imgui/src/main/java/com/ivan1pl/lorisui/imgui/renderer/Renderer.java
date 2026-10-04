package com.ivan1pl.lorisui.imgui.renderer;

import com.ivan1pl.lorisui.api.component.Component;
import com.ivan1pl.lorisui.api.core.Color;
import com.ivan1pl.lorisui.api.resolver.BundleKeyResolver;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public abstract class Renderer<ComponentType extends Component> {
    // Complex key represented as nested maps to prevent object allocation just to look up a key
    private static final Map<String, Map<Object, Map<Object, Map<Object, TextureData>>>> TEXTURES = new HashMap<>();
    private static final Object NO_COLOR_KEY = new Object();

    public abstract void render(ComponentType component, int w, int h, RendererProvider rendererProvider,
                                BundleKeyResolver bundleKeyResolver, boolean topLevel, boolean enableInput);

    protected static TextureData loadTexture(String path) throws IOException {
        return loadTexture(path, null, null, null);
    }

    protected static TextureData loadTexture(String path, Color maskedR, Color maskedG, Color maskedB)
            throws IOException {
        TextureData textureData = getFromCache(path, maskedR, maskedG, maskedB);
        if (textureData != null) {
            return textureData;
        }
        BufferedImage image = ImageIO.read(Renderer.class.getResource(path));
        if (maskedR != null || maskedG != null || maskedB != null) {
            for (int x = 0; x < image.getWidth(); ++x) {
                for (int y = 0; y < image.getHeight(); ++y) {
                    Color color = new Color(image.getRGB(x, y));
                    image.setRGB(x, y, color.mask(maskedR, maskedG, maskedB).getRGBA());
                }
            }
        }
        ByteBuffer buffer = imageToBuffer(image);
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL21.GL_SRGB_ALPHA, image.getWidth(), image.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        textureData = new TextureData(texture, image.getWidth(), image.getHeight());
        putToCache(path, maskedR, maskedG, maskedB, textureData);
        return textureData;
    }

    private static ByteBuffer imageToBuffer(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }

    private static TextureData getFromCache(String path, Color maskedR, Color maskedG, Color maskedB) {
        var map = TEXTURES.get(path);
        if (map == null) {
            return null;
        }
        var innerR = map.get(maskedR == null ? NO_COLOR_KEY : maskedR);
        if (innerR == null) {
            return null;
        }
        var innerG = innerR.get(maskedG == null ? NO_COLOR_KEY : maskedG);
        if (innerG == null) {
            return null;
        }
        return innerG.get(maskedB == null ? NO_COLOR_KEY : maskedB);
    }

    private static synchronized void putToCache(String path, Color maskedR, Color maskedG, Color maskedB,
                                                TextureData textureData) {
        var map = TEXTURES.get(path);
        if (map == null) {
            TEXTURES.put(path, map = new HashMap<>());
        }
        Object key = maskedR == null ? NO_COLOR_KEY : maskedR;
        var innerR = map.get(key);
        if (innerR == null) {
            map.put(key, innerR = new HashMap<>());
        }
        key = maskedG == null ? NO_COLOR_KEY : maskedG;
        var innerG = innerR.get(key);
        if (innerG == null) {
            innerR.put(key, innerG = new HashMap<>());
        }
        innerG.put(maskedB == null ? NO_COLOR_KEY : maskedB, textureData);
    }

    protected record TextureData(int id, int width, int height) {
    }
}
