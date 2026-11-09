package net.stacking.sync_mod.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.stacking.sync_mod.util.IdUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers textures produced by {@link TextureGenerator}s, once, the first time they're needed.
 * Must be used from the render thread.
 */
@Environment(EnvType.CLIENT)
public final class GeneratedTextureManager {
    private static final Map<TextureGenerator, ResourceLocation[]> GENERATED_TEXTURES = new HashMap<>();

    public static ResourceLocation[] getTextures(TextureGenerator generator) {
        return GENERATED_TEXTURES.computeIfAbsent(generator, x -> register(x, GENERATED_TEXTURES.size()));
    }

    private static ResourceLocation[] register(TextureGenerator generator, int generatorId) {
        List<NativeImage> images = generator.generateImages();
        ResourceLocation[] ids = new ResourceLocation[images.size()];
        for (int i = 0; i < ids.length; ++i) {
            ResourceLocation id = IdUtil.of("sync", "dynamic/generated_" + generatorId + "_" + i);
            NativeImage image = images.get(i);
            //? if >=1.21.5 {
            /*DynamicTexture texture = new DynamicTexture(id::toString, image);
            *///?} else
            DynamicTexture texture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            ids[i] = id;
        }
        return ids;
    }

    private GeneratedTextureManager() { }
}
