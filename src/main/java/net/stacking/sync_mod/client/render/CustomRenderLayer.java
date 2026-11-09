package net.stacking.sync_mod.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.client.texture.GeneratedTextureManager;
import net.stacking.sync_mod.client.texture.TextureGenerators;

/**
 * Render layers for shells under construction. They only use vanilla shaders, so they work
 * the same on every Minecraft version and with shader packs.
 */
@Environment(EnvType.CLIENT)
public final class CustomRenderLayer {
    private static final ResourceLocation WHITE = Sync.locate("textures/misc/white.png");

    /**
     * Untextured white voxels, used while a shell is being printed.
     */
    public static RenderType getVoxels() {
        return RenderType.entitySolid(WHITE);
    }

    /**
     * White over the part of the skin that hasn't been painted yet; drawn on top of the
     * shell's skin while it's being painted.
     *
     * @param paintingProgress 0 (nothing painted) to 1 (fully painted).
     */
    public static RenderType getPaintingMask(float paintingProgress) {
        ResourceLocation[] masks = GeneratedTextureManager.getTextures(TextureGenerators.PlayerEntityPartiallyTexturedTextureGenerator);
        int index = Math.max(0, Math.min((int)(masks.length * paintingProgress), masks.length - 1));
        return RenderType.entityTranslucent(masks[index]);
    }

    private CustomRenderLayer() { }
}
