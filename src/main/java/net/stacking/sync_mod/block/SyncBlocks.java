package net.stacking.sync_mod.block;

import net.minecraft.resources.ResourceKey;

import java.util.function.Function;

import net.stacking.sync_mod.util.GameRegistries;

import net.stacking.sync_mod.Sync;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class SyncBlocks {
    public static final Block SHELL_STORAGE;
    public static final Block SHELL_CONSTRUCTOR;
    public static final Block TREADMILL;

    static {
        SHELL_STORAGE = register("shell_storage", ShellStorageBlock::new, grayCopyOf(Blocks.GLASS).requiresCorrectToolForDrops().strength(1.8F).sound(SoundType.GLASS).noOcclusion().isValidSpawn(SyncBlocks::never).isRedstoneConductor(SyncBlocks::never).isSuffocating(SyncBlocks::never).isViewBlocking(SyncBlocks::never));
        SHELL_CONSTRUCTOR = register("shell_constructor", ShellConstructorBlock::new, grayCopyOf(Blocks.GLASS).requiresCorrectToolForDrops().strength(1.8F).sound(SoundType.GLASS).noOcclusion().isValidSpawn(SyncBlocks::never).isRedstoneConductor(SyncBlocks::never).isSuffocating(SyncBlocks::never).isViewBlocking(SyncBlocks::never));
        TREADMILL = register("treadmill", TreadmillBlock::new, grayCopyOf(Blocks.STONE).requiresCorrectToolForDrops().strength(1.8F).isValidSpawn(SyncBlocks::never).isRedstoneConductor(SyncBlocks::never).isSuffocating(SyncBlocks::never).isViewBlocking(SyncBlocks::never));
    }

    public static void init() { }

    private static Block register(String id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceLocation trueId = Sync.locate(id);
        // Since 1.21.2 a block must know its id before it's created.
        //? if >=1.21.2
        /*properties.setId(ResourceKey.create(GameRegistries.BLOCK_KEY, trueId));*/
        return Registry.register(GameRegistries.BLOCK, trueId, factory.apply(properties));
    }

    /**
     * Copies a vanilla block's properties and makes it show up gray on maps.
     */
    private static BlockBehaviour.Properties grayCopyOf(Block block) {
        //? if >=1.20.3 {
        return BlockBehaviour.Properties.ofFullCopy(block).mapColor(DyeColor.GRAY);
        //?} else if >=1.20 {
        /*return BlockBehaviour.Properties.copy(block).mapColor(DyeColor.GRAY);
        *///?} else {
        /*return BlockBehaviour.Properties.copy(block).color(DyeColor.GRAY.getMaterialColor());
        *///?}
    }

    private static boolean never(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    private static Boolean never(BlockState state, BlockGetter world, BlockPos pos, EntityType<?> type) {
        return false;
    }
}
