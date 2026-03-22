package net.stacking.sync_mod.block;

import net.stacking.sync_mod.Sync;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registry;
import net.minecraft.world.BlockView;

public final class SyncBlocks {
    public static final Block SHELL_STORAGE;
    public static final Block SHELL_CONSTRUCTOR;
    public static final Block TREADMILL;

    static {
        SHELL_STORAGE = register("shell_storage", new ShellStorageBlock(AbstractBlock.Settings.copy(Blocks.GLASS).mapColor(DyeColor.GRAY).requiresTool().strength(1.8F).sounds(BlockSoundGroup.GLASS).nonOpaque().allowsSpawning(SyncBlocks::never).solidBlock(SyncBlocks::never).suffocates(SyncBlocks::never).blockVision(SyncBlocks::never)));
        SHELL_CONSTRUCTOR = register("shell_constructor", new ShellConstructorBlock(AbstractBlock.Settings.copy(Blocks.GLASS).mapColor(DyeColor.GRAY).requiresTool().strength(1.8F).sounds(BlockSoundGroup.GLASS).nonOpaque().allowsSpawning(SyncBlocks::never).solidBlock(SyncBlocks::never).suffocates(SyncBlocks::never).blockVision(SyncBlocks::never)));
        TREADMILL = register("treadmill", new TreadmillBlock(AbstractBlock.Settings.copy(Blocks.STONE).mapColor(DyeColor.GRAY).requiresTool().strength(1.8F).allowsSpawning(SyncBlocks::never).solidBlock(SyncBlocks::never).suffocates(SyncBlocks::never).blockVision(SyncBlocks::never)));
    }

    public static void init() { }

    private static Block register(String id, Block block) {
        Identifier trueId = Sync.locate(id);
        return Registry.register(Registries.BLOCK, trueId, block);
    }

    private static boolean never(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    private static Boolean never(BlockState state, BlockView world, BlockPos pos, EntityType<?> type) {
        return false;
    }
}
