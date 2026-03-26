package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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

    // ── Static helpers ────────────────────────────────────────────────────────

    public static boolean isEnabled(BlockState state) { return state.get(ENABLED); }
    public static boolean isPowered(BlockState state)  { return state.get(POWERED);  }

    // ── Collision shape ───────────────────────────────────────────────────────

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

    // ── Redstone detection ────────────────────────────────────────────────────

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

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
                                                BlockState neighborState, WorldAccess world,
                                                BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.getStateForNeighborUpdate(
                state, direction, neighborState, world, pos, neighborPos);
        if (world instanceof World w) updateEnabled(w, pos, updated);
        return updated;
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

    // ── Entity collision ──────────────────────────────────────────────────────

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) return;
        if (state.get(HALF) != DoubleBlockHalf.LOWER) return;
        if (!(world.getBlockEntity(pos) instanceof ShellStorageBlockEntity be)) return;
        be.onEntityCollisionClient(entity, state);
    }

    // ── Comparator output ─────────────────────────────────────────────────────

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