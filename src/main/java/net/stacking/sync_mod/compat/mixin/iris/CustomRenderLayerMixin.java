package net.stacking.sync_mod.compat.mixin.iris;

import net.stacking.sync_mod.client.render.CustomRenderLayer;
import net.stacking.sync_mod.compat.iris.IrisRenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Environment(EnvType.CLIENT)
@Mixin(CustomRenderLayer.class)
final class CustomRenderLayerMixin {
    /**
     * @author Me
     * @reason Iris compatibility for custom render layers
     */
    @Overwrite
    public static RenderLayer getVoxels() {
        return IrisRenderLayer.getVoxels();
    }

    /**
     * @author Me
     * @reason Iris compatibility for partially textured entities
     */
    @Overwrite
    public static RenderLayer getEntityTranslucentPartiallyTextured(Identifier textureId, float cutoutY, boolean affectsOutline) {
        return IrisRenderLayer.getEntityTranslucentPartiallyTextured(textureId, cutoutY, affectsOutline);
    }
}