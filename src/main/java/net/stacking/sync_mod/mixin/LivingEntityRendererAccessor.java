package net.stacking.sync_mod.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
@SuppressWarnings("rawtypes")
interface LivingEntityRendererAccessor {
    @Invoker("addLayer")
    boolean executeAddFeature(RenderLayer featureRenderer);
}
