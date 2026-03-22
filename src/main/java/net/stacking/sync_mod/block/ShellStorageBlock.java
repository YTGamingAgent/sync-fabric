package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.stacking.sync_mod.block.ComparatorOutputType;
import net.stacking.sync_mod.block.entity.ShellStorageBlockEntity;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;

@SuppressWarnings("deprecation")
public class ShellStorageBlock extends AbstractShellContainerBlock {
    public static final MapCodec<ShellStorageBlock> CODEC = createCodec(ShellStorageBlock::new);
    public static final BooleanProperty ENABLED = Properties.ENABLED;
    public static final BooleanProperty POWERED = Properties.POWERED;

    public ShellStorageBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(OPEN,    false)
                .with(ENABLED, false)
                .with(POWERED, false)
                .with(HALF,    DoubleBlockHalf.LOWER)
                .with(FACING,  net.minecraft.util.math.Direction.NORTH)
                .with(OUTPUT,  ComparatorOutputType.PROGRESS));
    }

    @Override
    protected MapCodec<? extends ShellStorageBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ENABLED, POWERED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER
                ? new ShellStorageBlockEntity(pos, state)
                : null;
    }

    @Override
    protected BlockEntityType<?> getExpectedBlockEntityType() {
        return SyncBlockEntities.SHELL_STORAGE;
    }

    // ── Static helpers used by the block entity ────────────────────────────

    public static boolean isEnabled(BlockState state) {
        return state.get(ENABLED);
    }

    public static boolean isPowered(BlockState state) {
        return state.get(POWERED);
    }

    public static void setPowered(BlockState state, World world, BlockPos pos, boolean powered) {
        world.setBlockState(pos, state.with(POWERED, powered), 3);
    }

    // ── Entity collision (client-side trigger for the GUI) ─────────────────

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient && world.getBlockEntity(pos) instanceof ShellStorageBlockEntity be) {
            be.onEntityCollisionClient(entity, state);
        }
    }

    // ── Scheduled-tick for redstone comparator output ─────────────────────

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ShellStorageBlockEntity be) {
            return switch (state.get(OUTPUT)) {
                case PROGRESS  -> be.getProgressComparatorOutput();
                case INVENTORY -> be.getInventoryComparatorOutput();
            };
        }
        return 0;
    }
}