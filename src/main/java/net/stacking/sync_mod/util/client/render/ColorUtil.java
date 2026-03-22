package net.stacking.sync_mod.util.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.DyeColor;

@Environment(EnvType.CLIENT)
public final class ColorUtil {
    public static int getRgb(DyeColor color) {
        return getRgb(color, 255);
    }

    public static int getRgb(DyeColor color, int alpha) {
        int rgb = color.getSignColor();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    public static int fromDyeColor(DyeColor color) {
        return getRgb(color, 255);
    }

    public static int fromDyeColor(DyeColor color, float alpha) {
        return getRgb(color, (int)(alpha * 255));
    }

    public static float[] toRGBA(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color >> 24) & 0xFF) / 255.0F;
        return new float[]{r, g, b, a};
    }

    public static int[] getRgba(DyeColor color) {
        return getRgba(color, 255);
    }

    public static int[] getRgba(DyeColor color, int alpha) {
        int rgb = color.getSignColor();
        return new int[] {
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF,
                alpha
        };
    }

    public static float[] getRgbaFloat(DyeColor color) {
        return getRgbaFloat(color, 1.0F);
    }

    public static float[] getRgbaFloat(DyeColor color, float alpha) {
        int rgb = color.getSignColor();
        return new float[] {
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F,
                alpha
        };
    }

    public static float[] getColorComponents(DyeColor color) {
        return getRgbaFloat(color);
    }

    private ColorUtil() { }
}