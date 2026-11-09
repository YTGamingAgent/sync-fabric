package net.stacking.sync_mod.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class TextureGenerators {
    /**
     * Masks for painting a shell: white over the part of a player skin that hasn't been painted yet.
     * Mask {@code i} of {@code n} corresponds to painting progress {@code i / n}; the top of the
     * body gets painted first and the legs last.
     */
    public static final TextureGenerator PlayerEntityPartiallyTexturedTextureGenerator = new PlayerEntityPartiallyTexturedTextureGenerator();

    private static class PlayerEntityPartiallyTexturedTextureGenerator implements TextureGenerator {
        private static final int TEXTURE_SIZE = 64;
        private static final int BLOCKS = 32;
        private static final int TRANSPARENT = 0x00000000;
        private static final int WHITE = 0xFFFFFFFF;

        /**
         * Skin regions as {paint from, paint until, x, y, width, height}: a region is painted
         * once the paint line (0..32, top to bottom of the body) passes "paint until".
         */
        private static final int[][] REGIONS = {
            // top
            { 0, 0, 8, 0, 8, 8 },
            // face
            { 1, 8, 0, 8, 32, 8 },
            // neck
            { 9, 9, 16, 0, 8, 8 },
            { 9, 9, 20, 16, 8, 4 },
            // shoulders
            { 9, 9, 44, 16, 4, 4 },
            { 9, 9, 36, 48, 4, 4 },
            // body
            { 9, 20, 16, 20, 24, 12 },
            // arms
            { 9, 20, 40, 20, 16, 12 },
            { 9, 20, 32, 52, 16, 12 },
            // body bottom
            { 21, 21, 28, 16, 8, 4 },
            // fists
            { 21, 21, 48, 16, 4, 4 },
            { 21, 21, 40, 48, 4, 4 },
            // legs' top
            { 21, 21, 4, 16, 4, 4 },
            { 21, 21, 20, 48, 4, 4 },
            // legs
            { 21, 32, 0, 20, 16, 12 },
            { 21, 32, 16, 52, 16, 12 },
        };

        @Override
        public List<NativeImage> generateImages() {
            int textureCount = BLOCKS;
            List<NativeImage> images = new ArrayList<>(textureCount + 1);
            for (int i = 0; i <= textureCount; ++i) {
                images.add(generateImage(i / (float)textureCount));
            }
            return images;
        }

        private static NativeImage generateImage(float emptiness) {
            NativeImage img = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, false);
            float lastBlock = BLOCKS * emptiness;
            img.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TRANSPARENT);

            for (int i = REGIONS.length - 1; i >= 0; --i) {
                int[] region = REGIONS[i];
                int from = region[0], until = region[1], x = region[2], y = region[3], width = region[4], height = region[5];
                if (lastBlock > until) {
                    break;
                }

                if (lastBlock > from) {
                    int paintedY = (int)(y + lastBlock - from);
                    img.fillRect(x, paintedY, width, height - (paintedY - y), WHITE);
                } else {
                    img.fillRect(x, y, width, height, WHITE);
                }
            }
            return img;
        }
    }

    private TextureGenerators() { }
}
