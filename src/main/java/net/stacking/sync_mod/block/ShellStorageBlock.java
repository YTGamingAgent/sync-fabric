package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.Direction;
import net.stacking.sync_mod.block.entity.ShellStorageBlockEntity;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;

@SuppressWarnings("deprecation")
public class ShellStorageBlock extends AbstractShellContainerBlock {

    public static final MapCodec<ShellStorageBlock> CODEC = createCodec(ShellStorageBlock::new);
    public static final BooleanProperty ENABLED = Properties.ENABLED;
    public static final BooleanProperty POWERED  = Properties.POWERED;

    // ── Pre-computed open collision shapes ────────────────────────────────────
    // When the doors are open the front face has NO collision (player can walk in).
    // The back and two side walls are 1/8-block thick to block entry from wrong sides.
    // Interior is empty so the player can stand inside freely.
    private static final double W = 0.125; // wall thickness

    // SOUTH = open on +Z face (front faces south)
    private static final VoxelShape OPEN_SOUTH = VoxelShapes.union(
            VoxelShapes.cuboid(0,   0, 0,   1,   1, W  ),   // back  (north wall)
            VoxelShapes.cuboid(0,   0, 0,   W,   1, 1  ),   // left  (west wall)
            VoxelShapes.cuboid(1-W, 0, 0,   1,   1, 1  )    // right (east wall)
    );
    // NORTH = open on -Z face
    private static final VoxelShape OPEN_NORTH = VoxelShapes.union(
            VoxelShapes.cuboid(0,   0, 1-W, 1,   1, 1  ),   // back  (south wall)
            VoxelShapes.cuboid(0,   0, 0,   W,   1, 1  ),   // left  (west wall)
            VoxelShapes.cuboid(1-W, 0, 0,   1,   1, 1  )    // right (east wall)
    );
    // EAST = open on +X face
    private static final VoxelShape OPEN_EAST = VoxelShapes.union(
            VoxelShapes.cuboid(0,   0, 0,   W,   1, 1  ),   // back  (west wall)
            VoxelShapes.cuboid(0,   0, 0,   1,   1, W  ),   // side  (north wall)
            VoxelShapes.cuboid(0,   0, 1-W, 1,   1, 1  )    // side  (south wall)
    );
    // WEST = open on -X face
    private static final VoxelShape OPEN_WEST = VoxelShapes.union(
            VoxelShapes.cuboid(1-W, 0, 0,   1,   1, 1  ),   // back  (east wall)
            VoxelShapes.cuboid(0,   0, 0,   1,   1, W  ),   // side  (north wall)
            VoxelShapes.cuboid(0,   0, 1-W, 1,   1, 1  )    // side  (south wall)
    );

    public ShellStorageBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(OPEN,    false)
                .with(ENABLED, false)
                .with(POWERED, false)
                .with(HALF,    DoubleBlockHalf.LOWER)
                .with(FACING,  Direction.NORTH)
                .with(OUTPUT,  ComparatorOutputType.PROGRESS));
    }

    @Override protected MapCodec<ShellStorageBlock> getCodec() { return CODEC; }

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
    protected BlockEntityType<?> getExpectedBlockEntityType() { return SyncBlockEntities.SHELL_STORAGE; }

    // ── Static helpers

    public static boolean isEnabled(BlockState state) { return state.get(ENABLED); }
    public static boolean isPowered(BlockState state)  { return state.get(POWERED);  }

    // ── Collision shape

    public static void setPowered(BlockState state, World world, BlockPos pos, boolean powered) {
        // pos is always the LOWER half (ticker only runs on LOWER half).
        // We must update BOTH halves so their states stay in sync.
        BlockState liveState = world.getBlockState(pos);
        if (liveState.isOf(SyncBlocks.SHELL_STORAGE) && liveState.get(POWERED) != powered) {
            world.setBlockState(pos, liveState.with(POWERED, powered), Block.NOTIFY_ALL);
        }
        BlockPos upperPos = pos.up();
        BlockState upperState = world.getBlockState(upperPos);
        if (upperState.isOf(SyncBlocks.SHELL_STORAGE) && upperState.get(POWERED) != powered) {
            world.setBlockState(upperPos, upperState.with(POWERED, powered), Block.NOTIFY_ALL);
        }
    }

    public static void setOpen(BlockState state, World world, BlockPos pos, boolean open) {
        // pos is always the LOWER half (ticker only runs on LOWER half).
        // BOTH halves need OPEN=true so the upper half's getCollisionShape() also
        // returns the open shape, allowing the player to physically walk inside.
        BlockState liveState = world.getBlockState(pos);
        if (liveState.isOf(SyncBlocks.SHELL_STORAGE) && liveState.get(OPEN) != open) {
            world.setBlockState(pos, liveState.with(OPEN, open), Block.NOTIFY_ALL);
        }
        BlockPos upperPos = pos.up();
        BlockState upperState = world.getBlockState(upperPos);
        if (upperState.isOf(SyncBlocks.SHELL_STORAGE) && upperState.get(OPEN) != open) {
            world.setBlockState(upperPos, upperState.with(OPEN, open), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world,
                                           BlockPos pos, ShapeContext context) {
        if (!isOpen(state)) return VoxelShapes.fullCube();
        // Open: keep thin walls on back + sides, leave front face open
        return switch (state.get(FACING)) {
            case SOUTH -> OPEN_SOUTH;
            case NORTH -> OPEN_NORTH;
            case EAST  -> OPEN_EAST;
            case WEST  -> OPEN_WEST;
            default    -> VoxelShapes.fullCube();
        };
    }

    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockPos targetPos = pos;
        BlockState targetState = state;

        // If TOP half, get the BOTTOM half
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            targetPos = pos.offset(Direction.DOWN);
            targetState = world.getBlockState(targetPos);
        }

        // Must be LOWER half now
        if (targetState.get(HALF) != DoubleBlockHalf.LOWER) return ActionResult.PASS;
        if (!(world.getBlockEntity(targetPos) instanceof ShellStorageBlockEntity be)) return ActionResult.PASS;
        return be.onUse(world, targetPos, player, hand);
    }

    // ── Redstone detection

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos,
                             BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        updateEnabled(world, pos, state);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos,
                               Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        updateEnabled(world, pos, state);
    }

    // ── FIX for vertical double-blocks (LOWER/UPPER)
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

    private static void updateEnabled(World world, BlockPos pos, BlockState state) {
        if (world.isClient) return;
        BlockPos bottomPos = state.get(HALF) == DoubleBlockHalf.LOWER
                ? pos : pos.offset(Direction.DOWN);
        BlockPos topPos = bottomPos.offset(Direction.UP);

        boolean powered =
                world.isReceivingRedstonePower(bottomPos) ||
                        world.isReceivingRedstonePower(topPos)    ||
                        world.getReceivedRedstonePower(bottomPos) > 0 ||
                        world.getReceivedRedstonePower(topPos)    > 0;

        BlockState bottomState = world.getBlockState(bottomPos);
        if (bottomState.isOf(SyncBlocks.SHELL_STORAGE) && bottomState.get(ENABLED) != powered) {
            world.setBlockState(bottomPos, bottomState.with(ENABLED, powered), Block.NOTIFY_ALL);
            BlockState topState = world.getBlockState(topPos);
            if (topState.isOf(SyncBlocks.SHELL_STORAGE) && topState.get(ENABLED) != powered) {
                world.setBlockState(topPos, topState.with(ENABLED, powered), Block.NOTIFY_ALL);
            }
        }
    }

    // ── Entity collision

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient || !(entity instanceof PlayerEntity)) return;

        // Determine which block to use for entity collision
        BlockPos targetPos = pos;
        BlockState targetState = state;

        // If we're on the TOP half, get the BOTTOM half
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            targetPos = pos.offset(Direction.DOWN);
            targetState = world.getBlockState(targetPos);
        }

        // Verify we have the BOTTOM half and its block entity
        if (!targetState.isOf(this) || targetState.get(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        BlockEntity blockEntity = world.getBlockEntity(targetPos);
        if (!(blockEntity instanceof ShellStorageBlockEntity be)) {
            return;
        }

        // Now call the client-side logic
        be.onEntityCollisionClient(entity, targetState);
    }

    // ── Comparator output

    @Override public boolean hasComparatorOutput(BlockState state) { return true; }

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