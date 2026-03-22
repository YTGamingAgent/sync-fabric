package net.stacking.sync_mod.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;

/**
 * Built-in {@link TextureGenerator} implementations.
 */
@Environment(EnvType.CLIENT)
public final class TextureGenerators {

    /**
     * Used by shader compatibility (e.g. Iris) to obtain a set of skin-like textures
     * that can be used for the printing mask / partially textured entity layer.
     */
    public static final TextureGenerator PlayerEntityPartiallyTexturedTextureGenerator = new PlayerEntityPartiallyTexturedTextureGeneratorImpl();

    private TextureGenerators() {
    }

    private static final class PlayerEntityPartiallyTexturedTextureGeneratorImpl implements TextureGenerator {
        @Override
        public NativeImage generate(NativeImage base) {
            // Keep this intentionally simple and robust across Minecraft / loader changes.
            // Consumers can use the returned image as-is, or apply additional processing.
            return base;
        }
    }
}
