package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.util.GameRegistries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.stacking.sync_mod.block.SyncBlocks;

public final class SyncBlockEntities {
    public static final BlockEntityType<ShellStorageBlockEntity> SHELL_STORAGE;
    public static final BlockEntityType<ShellConstructorBlockEntity> SHELL_CONSTRUCTOR;
    public static final BlockEntityType<TreadmillBlockEntity> TREADMILL;

    static {
        SHELL_STORAGE = register(ShellStorageBlockEntity::new, SyncBlocks.SHELL_STORAGE);
        SHELL_CONSTRUCTOR = register(ShellConstructorBlockEntity::new, SyncBlocks.SHELL_CONSTRUCTOR);
        TREADMILL = register(TreadmillBlockEntity::new, SyncBlocks.TREADMILL);
    }

    public static void init() { }

    private static <T extends BlockEntity> BlockEntityType<T> register(BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        ResourceLocation id = GameRegistries.BLOCK.getKey(block);
        //? if >=1.21.2 {
        /*BlockEntityType<T> type = net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(factory::create, block).build();
        *///?} else
        BlockEntityType<T> type = BlockEntityType.Builder.of(factory, block).build(null);
        return Registry.register(GameRegistries.BLOCK_ENTITY_TYPE, id, type);
    }
}
