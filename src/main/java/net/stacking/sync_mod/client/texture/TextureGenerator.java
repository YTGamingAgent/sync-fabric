package net.stacking.sync_mod.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;

@Environment(EnvType.CLIENT)
@FunctionalInterface
public interface TextureGenerator {
    /**
     * Generate or modify texture data.
     * @param image The image to generate/modify
     * @return The modified image (can be the same instance or a new one)
     */
    NativeImage generate(NativeImage image);
}