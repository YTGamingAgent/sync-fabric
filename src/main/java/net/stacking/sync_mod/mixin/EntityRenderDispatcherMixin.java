package net.stacking.sync_mod.mixin;

import com.google.common.collect.ImmutableMap;
import net.stacking.sync_mod.client.render.entity.ShellEntityRenderer;
import net.stacking.sync_mod.entity.ShellEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
*///?} else
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.impl.client.rendering.RegistrationHelperImpl;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
//? if <26.1
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? if <1.21.4
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
    //? if <1.21.4 {
    @Shadow
    private @Final ItemRenderer itemRenderer;
    //?}

    @Shadow
    private @Final Font font;

    //? if >=1.21.4 {
    /*@Shadow
    private @Final java.util.function.Supplier<EntityModelSet> entityModels;
    *///?} else {
    @Shadow
    private @Final EntityModelSet entityModels;
    //?}

    @Shadow
    private @Final ItemInHandRenderer itemInHandRenderer;

    //? if >=26.1 {
    /*@Shadow
    private @Final net.minecraft.client.renderer.block.BlockModelResolver blockModelResolver;
    *///?} else {
    @Shadow
    private @Final BlockRenderDispatcher blockRenderDispatcher;
    //?}

    //? if >=1.21.9 {
    /*@Shadow
    private @Final net.minecraft.client.resources.model.AtlasManager atlasManager;

    @Shadow
    private @Final net.minecraft.client.renderer.PlayerSkinRenderCache playerSkinRenderCache;
    *///?}

    //? if >=1.21.4 {
    /*@Shadow
    private @Final net.minecraft.client.renderer.MapRenderer mapRenderer;

    @Shadow
    private @Final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;

    @Shadow
    private @Final net.minecraft.client.resources.model.EquipmentAssetManager equipmentAssets;
    *///?} else if >=1.21.2 {
    /*@Shadow
    private @Final net.minecraft.client.renderer.MapRenderer mapRenderer;

    @Shadow
    private @Final net.minecraft.client.resources.model.EquipmentModelSet equipmentModels;
    *///?}

    @Unique
    private Map<String, ShellEntityRenderer> shellRenderers = ImmutableMap.of();

    @SuppressWarnings("unchecked")
    @Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    //? if >=1.21.2 {
    /*private <T extends Entity> void getRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T, ?>> cir) {
    *///?} else
    private <T extends Entity> void getRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (entity instanceof ShellEntity shell) {
            cir.setReturnValue((/*? if >=1.21.2 {*//*EntityRenderer<? super T, ?>*//*?} else {*/EntityRenderer<? super T>/*?}*/)(Object)this.shellRenderers.get(shell.getModel()));

        }
    }

    //? if >=1.21.6 {
    /*// GUIs (picture-in-picture) look renderers up by render state instead of by entity.
    @SuppressWarnings("unchecked")
    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    private <S extends net.minecraft.client.renderer.entity.state.EntityRenderState> void getRendererForState(S state, CallbackInfoReturnable<EntityRenderer<?, ? super S>> cir) {
        if ((Object)state instanceof ShellEntityRenderer.ShellRenderState shellState && shellState.shell != null) {
            cir.setReturnValue((EntityRenderer<?, ? super S>)(Object)this.shellRenderers.get(shellState.shellModel));
        }
    }
    *///?}

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void reload(ResourceManager manager, CallbackInfo ci) {
        //? if >=26.1 {
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context((EntityRenderDispatcher)(Object)this, this.blockModelResolver, this.itemModelResolver, this.mapRenderer, manager, this.entityModels.get(), this.equipmentAssets, this.atlasManager, this.font, this.playerSkinRenderCache);
        *///?} else if >=1.21.9 {
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context((EntityRenderDispatcher)(Object)this, this.itemModelResolver, this.mapRenderer, this.blockRenderDispatcher, manager, this.entityModels.get(), this.equipmentAssets, this.atlasManager, this.font, this.playerSkinRenderCache);
        *///?} else if >=1.21.4 {
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context((EntityRenderDispatcher)(Object)this, this.itemModelResolver, this.mapRenderer, this.blockRenderDispatcher, manager, this.entityModels.get(), this.equipmentAssets, this.font);
        *///?} else if >=1.21.2 {
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context((EntityRenderDispatcher)(Object)this, this.itemRenderer, this.mapRenderer, this.blockRenderDispatcher, manager, this.entityModels, this.equipmentModels, this.font);
        *///?} else
        EntityRendererProvider.Context context = new EntityRendererProvider.Context((EntityRenderDispatcher)(Object)this, this.itemRenderer, this.blockRenderDispatcher, this.itemInHandRenderer, manager, this.entityModels, this.font);
        this.shellRenderers = ImmutableMap.of(
            "default", createShellEntityRenderer(context, false),
            "slim", createShellEntityRenderer(context, true)
        );
    }

    @SuppressWarnings({"unchecked", "ConstantConditions", "rawtypes"})
    private static ShellEntityRenderer createShellEntityRenderer(EntityRendererProvider.Context context, boolean slim) {
        ShellEntityRenderer shellEntityRenderer = new ShellEntityRenderer(context, slim);
        LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor)shellEntityRenderer;
        //? if >=26.1 {
        /*LivingEntityRenderLayerRegistrationCallback.EVENT.invoker().registerLayers(EntityType.PLAYER, shellEntityRenderer, new RegistrationHelperImpl(accessor::executeAddFeature), context);
        *///?} else
        LivingEntityFeatureRendererRegistrationCallback.EVENT.invoker().registerRenderers(EntityType.PLAYER, shellEntityRenderer, new RegistrationHelperImpl(accessor::executeAddFeature), context);
        return shellEntityRenderer;
    }
}