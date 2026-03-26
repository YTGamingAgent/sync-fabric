package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.stacking.sync_mod.block.entity.ShellConstructorBlockEntity;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class ShellConstructorBlock extends AbstractShellContainerBlock {

    public static final MapCodec<ShellConstructorBlock> CODEC = createCodec(ShellConstructorBlock::new);

    public ShellConstructorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(OPEN,    false)
                .with(HALF,    DoubleBlockHalf.LOWER)
                .with(FACING,  Direction.NORTH)
                .with(OUTPUT,  ComparatorOutputType.PROGRESS));
    }

    @Override
    protected MapCodec<ShellConstructorBlock> getCodec() { return CODEC; }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER
                ? new ShellConstructorBlockEntity(pos, state) : null;
    }

    @Override
    protected BlockEntityType<?> getExpectedBlockEntityType() { return SyncBlockEntities.SHELL_CONSTRUCTOR; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
    }

    // ── FIX for vertical double-blocks (LOWER/UPPER) ────────────────────────
    // Override to use Direction.UP/DOWN instead of facing-based directions
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
                                                BlockState neighborState, WorldAccess world,
                                                BlockPos pos, BlockPos neighborPos) {
        // For vertical double-blocks, the other half is always UP or DOWN
        Direction directionToOtherPart = state.get(HALF) == DoubleBlockHalf.LOWER
                ? Direction.UP : Direction.DOWN;

        if (direction == directionToOtherPart) {
            // Check if the neighbor is the correct type with the OTHER half
            return neighborState.isOf(this) && neighborState.get(HALF) != state.get(HALF)
                    ? state
                    : Blocks.AIR.getDefaultState();
        }

        // For other directions, delegate to parent for open/comparator logic
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    // ── Override onUse to handle TOP → BOTTOM forwarding ─────────────────────
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              BlockHitResult hit, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockPos targetPos = pos;
        BlockState targetState = state;

        // If TOP half, forward to BOTTOM half
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            targetPos = pos.offset(Direction.DOWN);
            targetState = world.getBlockState(targetPos);
        }

        // Must be LOWER half
        if (targetState.get(HALF) != DoubleBlockHalf.LOWER) return ActionResult.PASS;
        if (!(world.getBlockEntity(targetPos) instanceof ShellConstructorBlockEntity be)) {
            return ActionResult.PASS;
        }

        return be.onUse(world, targetPos, player, hand);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) { return true; }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ShellConstructorBlockEntity be) {
            return switch (state.get(OUTPUT)) {
                case PROGRESS  -> be.getProgressComparatorOutput();
                case INVENTORY -> be.getInventoryComparatorOutput();
            };
        }
        return 0;
    }
}