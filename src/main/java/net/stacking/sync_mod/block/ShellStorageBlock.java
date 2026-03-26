package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.stacking.sync_mod.block.entity.ShellStorageBlockEntity;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;

@SuppressWarnings("deprecation")
public class ShellStorageBlock extends AbstractShellContainerBlock {

    public static final MapCodec<ShellStorageBlock> CODEC = createCodec(ShellStorageBlock::new);
    public static final BooleanProperty ENABLED = Properties.ENABLED;
    public static final BooleanProperty POWERED  = Properties.POWERED;

    public ShellStorageBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(OPEN,    false)
                .with(ENABLED, false)
                .with(POWERED, false)
                .with(HALF,    DoubleBlockHalf.LOWER)
                .with(FACING,  Direction.NORTH)
                .with(OUTPUT,  ComparatorOutputType.PROGRESS));
    }

    @Override
    protected MapCodec<ShellStorageBlock> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ENABLED, POWERED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER
                ? new ShellStorageBlockEntity(pos, state) : null;
    }

    @Override
    protected BlockEntityType<?> getExpectedBlockEntityType() {
        return SyncBlockEntities.SHELL_STORAGE;
    }

    // ── Static helpers used by the block entity

    public static boolean isEnabled(BlockState state) { return state.get(ENABLED); }
    public static boolean isPowered(BlockState state)  { return state.get(POWERED);  }

    public static void setPowered(BlockState state, World world, BlockPos pos, boolean powered) {
        world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_ALL);
    }

    // ── Redstone detection
    // Called whenever a neighbouring block changes (block placed, torch added,
    // lever toggled, redstone wire updated, etc.).  We check BOTH halves so that
    // a signal delivered to the top block also enables the storage.

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos,
                             BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        updateEnabled(world, pos, state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
                                                BlockState neighborState, WorldAccess world,
                                                BlockPos pos, BlockPos neighborPos) {
        // Let the parent handle the double-block seam, then refresh ENABLED.
        BlockState updated = super.getStateForNeighborUpdate(
                state, direction, neighborState, world, pos, neighborPos);
        if (world instanceof World w) {
            updateEnabled(w, pos, updated);
        }
        return updated;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos,
                               Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        updateEnabled(world, pos, state);
    }

    /**
     * Checks whether either half of the storage is receiving a direct redstone
     * signal (strong or weak power from any adjacent face) and updates ENABLED
     * on the BOTTOM block state accordingly.
     *
     * Rules:
     *  - Redstone torch or block placed directly adjacent = powered ✓
     *  - Redstone wire powered by a lever running into it  = powered ✓
     *  - Lever attached directly to the block              = powered ✓
     *  - Signal more than 1 block away (not adjacent)      = NOT powered ✗
     */
    private static void updateEnabled(World world, BlockPos pos, BlockState state) {
        if (world.isClient) return;

        // Resolve BOTTOM block position regardless of which half was notified.
        BlockPos bottomPos = state.get(HALF) == DoubleBlockHalf.LOWER
                ? pos : pos.offset(Direction.DOWN);
        BlockPos topPos = bottomPos.offset(Direction.UP);

        // isReceivingRedstonePower checks all 6 adjacent faces for strong power.
        // getReceivedRedstonePower also includes weak power through blocks.
        boolean powered =
                world.isReceivingRedstonePower(bottomPos) ||
                        world.isReceivingRedstonePower(topPos)    ||
                        world.getReceivedRedstonePower(bottomPos) > 0 ||
                        world.getReceivedRedstonePower(topPos)    > 0;

        BlockState bottomState = world.getBlockState(bottomPos);
        if (bottomState.isOf(SyncBlocks.SHELL_STORAGE)
                && bottomState.get(ENABLED) != powered) {
            world.setBlockState(bottomPos,
                    bottomState.with(ENABLED, powered), Block.NOTIFY_ALL);

            // Also update the TOP half so both blocks reflect the same state.
            BlockState topState = world.getBlockState(topPos);
            if (topState.isOf(SyncBlocks.SHELL_STORAGE)
                    && topState.get(ENABLED) != powered) {
                world.setBlockState(topPos,
                        topState.with(ENABLED, powered), Block.NOTIFY_ALL);
            }
        }
    }

    // ── Entity collision (client-side trigger for the GUI) ────────────────────

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient
                && world.getBlockEntity(pos) instanceof ShellStorageBlockEntity be) {
            be.onEntityCollisionClient(entity, state);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world,
                                           BlockPos pos, ShapeContext context) {
        // When the door is open, remove all collision so the player can walk in
        return isOpen(state) ? VoxelShapes.empty() : VoxelShapes.fullCube();
    }

    // ── Comparator output ─────────────────────────────────────────────────────

    @Override
    public boolean hasComparatorOutput(BlockState state) { return true; }

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