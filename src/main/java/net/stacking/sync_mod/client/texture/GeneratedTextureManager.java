package net.stacking.sync_mod.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class GeneratedTextureManager {
    private static final Map<TextureGenerator, Identifier> CACHE = new HashMap<>();
    private static int textureCounter = 0;

    public static void clearCache() {
        MinecraftClient client = MinecraftClient.getInstance();
        CACHE.values().forEach(client.getTextureManager()::destroyTexture);
        CACHE.clear();
    }

    public static Identifier getOrCreate(int width, int height, TextureGenerator generator) {
        return getOrCreate(String.format("%sx%s", width, height), width, height, generator);
    }

    public static Identifier getOrCreate(String format, int width, int height, TextureGenerator generator) {
        return CACHE.computeIfAbsent(generator, x -> {
            Identifier id = Identifier.of("__dynamic", format + (++textureCounter));
            NativeImage image = new NativeImage(width, height, true);
            generator.generate(image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
            return id;
        });
    }

    /**
     * Compatibility helper for shader mods that expect an array of generated textures.
     *
     * The original Sync client generated multiple variants; for modern versions we
     * generate a single texture and return it as a 1-element array.
     */
    public static Identifier[] getTextures(TextureGenerator generator) {
        return new Identifier[] { getOrCreate(64, 64, generator) };
    }

    private GeneratedTextureManager() { }
}