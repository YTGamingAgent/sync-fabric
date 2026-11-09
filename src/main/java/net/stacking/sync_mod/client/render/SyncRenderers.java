package net.stacking.sync_mod.client.render;

import net.stacking.sync_mod.util.GameRegistries;

import net.stacking.sync_mod.block.entity.SyncBlockEntities;
import net.stacking.sync_mod.client.render.block.entity.ShellConstructorBlockEntityRenderer;
import net.stacking.sync_mod.client.render.block.entity.ShellStorageBlockEntityRenderer;
import net.stacking.sync_mod.client.render.block.entity.TreadmillBlockEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
//? if <1.21.4
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

@Environment(EnvType.CLIENT)
public final class SyncRenderers {
    public static void initClient() {
        register(ShellStorageBlockEntityRenderer::new, SyncBlockEntities.SHELL_STORAGE);
        register(ShellConstructorBlockEntityRenderer::new, SyncBlockEntities.SHELL_CONSTRUCTOR);
        register(TreadmillBlockEntityRenderer::new, SyncBlockEntities.TREADMILL);
        // Since 1.21.4, items are drawn through item model definitions (assets/sync/items).
        //? if >=1.21.4
        /*net.stacking.sync_mod.client.render.item.BlockEntityItemRenderer.register();*/
    }

    //? if >=1.21.9 {
    /*private static <E extends BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void register(BlockEntityRendererProvider<E, S> rendererFactory, BlockEntityType<E> blockEntityType) {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(blockEntityType, rendererFactory);
    *///?} else {
    private static <E extends BlockEntity> void register(BlockEntityRendererProvider<? super E> rendererFactory, BlockEntityType<E> blockEntityType) {
        BlockEntityRendererRegistry.register(blockEntityType, rendererFactory);
    //?}

        ResourceLocation id = GameRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType);
        Block block = GameRegistries.get(GameRegistries.BLOCK, id);
        Item item = GameRegistries.get(GameRegistries.ITEM, id);
        if (GameRegistries.BLOCK.getKey(block).equals(GameRegistries.BLOCK.getDefaultKey()) || GameRegistries.ITEM.getKey(item).equals(GameRegistries.ITEM.getDefaultKey())) {
            return;
        }

        // A world-less block entity: the renderers detect this and draw the whole model for the item.
        BlockEntity renderEntity = blockEntityType.create(BlockPos.ZERO, block.defaultBlockState());
        //? if <1.21.4 {
        BuiltinItemRendererRegistry.INSTANCE.register(item, (stack, mode, matrices, vertexConsumers, light, overlay) ->
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(renderEntity, matrices, vertexConsumers, light, overlay)
        );
        //?}
    }
}
