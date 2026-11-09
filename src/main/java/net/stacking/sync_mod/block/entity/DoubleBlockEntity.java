package net.stacking.sync_mod.block.entity;

import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;

public interface DoubleBlockEntity {
    DoubleBlockCombiner.BlockType getBlockType(BlockState state);
}
