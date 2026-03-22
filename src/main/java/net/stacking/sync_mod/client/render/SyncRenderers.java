package net.stacking.sync_mod.client.render;

import net.stacking.sync_mod.block.entity.SyncBlockEntities;
import net.stacking.sync_mod.client.render.block.entity.ShellConstructorBlockEntityRenderer;
import net.stacking.sync_mod.client.render.block.entity.ShellStorageBlockEntityRenderer;
import net.stacking.sync_mod.client.render.block.entity.TreadmillBlockEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class SyncRenderers {
    public static void initClient() {
        register(ShellConstructorBlockEntityRenderer::new, SyncBlockEntities.SHELL_CONSTRUCTOR);
        register(ShellStorageBlockEntityRenderer::new,    SyncBlockEntities.SHELL_STORAGE);
        register(TreadmillBlockEntityRenderer::new,       SyncBlockEntities.TREADMILL);
    }

    private static <E extends BlockEntity> void register(
            net.minecraft.client.render.block.entity.BlockEntityRendererFactory<? super E> factory,
            BlockEntityType<E> type) {

        BlockEntityRendererRegistry.register(type, factory);

        // Also register an item renderer so the block shows as a 3D model in inventory.
        Identifier id    = Registries.BLOCK_ENTITY_TYPE.getId(type);
        Block      block = Registries.BLOCK.get(id);
        Item       item  = Registries.ITEM.get(id);

        // Skip if no matching block or item was found.
        if (Registries.BLOCK.getId(block).equals(Registries.BLOCK.getDefaultId())) return;
        if (Registries.ITEM.getId(item).equals(Registries.ITEM.getDefaultId()))  return;

        BlockEntity renderEntity = type.instantiate(BlockPos.ORIGIN, block.getDefaultState());
        if (renderEntity == null) return;

        BuiltinItemRendererRegistry.INSTANCE.register(item,
                (stack, mode, matrices, vertexConsumers, light, overlay) ->
                        MinecraftClient.getInstance()
                                .getBlockEntityRenderDispatcher()
                                .renderEntity(renderEntity, matrices, vertexConsumers, light, overlay));
    }
}