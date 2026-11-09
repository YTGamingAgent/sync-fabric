package net.stacking.sync_mod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface TickableBlockEntity {
    static <T extends BlockEntity> void clientTicker(Level world, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof TickableBlockEntity tickable) {
            tickable.onClientTick(world, pos, state);
        }
    }

    static <T extends BlockEntity> void serverTicker(Level world, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof TickableBlockEntity tickable) {
            tickable.onServerTick(world, pos, state);
        }
    }

    default void onClientTick(Level world, BlockPos pos, BlockState state) {
        this.onTick(world, pos, state);
    }

    default void onServerTick(Level world, BlockPos pos, BlockState state) {
        this.onTick(world, pos, state);
    }

    @SuppressWarnings("unused")
    default void onTick(Level world, BlockPos pos, BlockState state) { }
}