package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.stacking.sync_mod.block.entity.AbstractShellContainerBlockEntity;
import net.stacking.sync_mod.block.entity.TickableBlockEntity;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public abstract class AbstractShellContainerBlock extends BlockWithEntity {

    public static final EnumProperty<DoubleBlockHalf>       HALF    = Properties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction>             FACING  = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty                     OPEN    = BooleanProperty.of("open");
    public static final EnumProperty<ComparatorOutputType>  OUTPUT  =
            EnumProperty.of("output", ComparatorOutputType.class);

    // ── Hitbox shapes ─────────────────────────────────────────────────────────
    private static final VoxelShape SHAPE_LOWER = VoxelShapes.fullCube();
    private static final VoxelShape SHAPE_UPPER = VoxelShapes.fullCube();

    protected AbstractShellContainerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(HALF,   DoubleBlockHalf.LOWER)
                .with(FACING, Direction.NORTH)
                .with(OPEN,   false)
                .with(OUTPUT, ComparatorOutputType.PROGRESS));
    }

    @Override
    protected abstract MapCodec<? extends AbstractShellContainerBlock> getCodec();

    // ── Shape overrides ───────────────────────────────────────────────────────

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world,
                                         BlockPos pos, ShapeContext context) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? SHAPE_LOWER : SHAPE_UPPER;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world,
                                           BlockPos pos, ShapeContext context) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? SHAPE_LOWER : SHAPE_UPPER;
    }

    // ── Existing helpers ──────────────────────────────────────────────────────

    public static boolean isBottom(BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER;
    }

    public static boolean isOpen(BlockState state) {
        return state.get(OPEN);
    }

    public static DoubleBlockHalf getShellContainerHalf(BlockState state) {
        return state.get(HALF);
    }

    public static Direction getDirectionTowardsAnotherPart(BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
    }

    public static void setOpen(BlockState state, World world, BlockPos pos, boolean open) {
        // Update this half
        if (state.get(OPEN) != open) {
            world.setBlockState(pos, state.with(OPEN, open), 3);
        }
        // Also update the other half — both halves must agree on OPEN so the
        // upper half's collision shape matches and players can physically enter.
        Direction toOther = state.get(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        BlockPos otherPos = pos.offset(toOther);
        BlockState otherState = world.getBlockState(otherPos);
        if (otherState.isOf(state.getBlock()) && otherState.get(OPEN) != open) {
            world.setBlockState(otherPos, otherState.with(OPEN, open), 3);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OPEN, OUTPUT);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    // ── Proper 2-block placement ──────────────────────────────────────────────

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos   = ctx.getBlockPos();
        World    world = ctx.getWorld();

        if (pos.getY() >= world.getTopY() - 1)              return null;
        if (!world.getBlockState(pos.up()).canReplace(ctx))  return null;

        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        return this.getDefaultState()
                .with(FACING, facing)
                .with(HALF,   DoubleBlockHalf.LOWER);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
                                                   BlockState neighborState, WorldAccess world,
                                                   BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() != Direction.Axis.Y
                || (half == DoubleBlockHalf.LOWER) != (direction == Direction.UP)
                || (neighborState.isOf(this) && neighborState.get(HALF) != half)) {
            return half == DoubleBlockHalf.LOWER && direction == Direction.DOWN
                    && !state.canPlaceAt(world, pos)
                    ? Blocks.AIR.getDefaultState()
                    : super.getStateForNeighborUpdate(state, direction, neighborState,
                    world, pos, neighborPos);
        }
        return Blocks.AIR.getDefaultState();
    }

    // ── FIX #1 & #2: onBreak must return BlockState in MC 1.21 ───────────────
    /**
     * In Minecraft 1.21 the signature changed from:
     *   void onBreak(World, BlockPos, BlockState, PlayerEntity)
     * to:
     *   BlockState onBreak(World, BlockPos, BlockState, PlayerEntity)
     *
     * We must return the (possibly modified) BlockState.
     * We also break the other half of the two-block structure here.
     */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state,
                              PlayerEntity player) {
        if (!world.isClient) {
            BlockPos otherPos = (state.get(HALF) == DoubleBlockHalf.LOWER)
                    ? pos.up() : pos.down();
            BlockState otherState = world.getBlockState(otherPos);

            if (otherState.isOf(this) && otherState.get(HALF) != state.get(HALF)) {
                // Notify the block entity before removing the other half
                if (world.getBlockEntity(pos) instanceof AbstractShellContainerBlockEntity be) {
                    be.onBreak(world, pos);
                }
                world.setBlockState(otherPos, Blocks.AIR.getDefaultState(),
                        Block.NOTIFY_ALL | Block.SKIP_DROPS);
            }
        }
        // MUST call super and return its result — it handles drops, stats, etc.
        return super.onBreak(world, pos, state, player);
    }

    // ── FIX #3: Hand comes from the method parameter, NOT from BlockHitResult ─
    /**
     * BlockHitResult does NOT have a getHand() method.
     * The Hand is passed as a separate parameter in onUse().
     */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        // Always delegate to the LOWER half's block entity
        BlockPos bePos = (state.get(HALF) == DoubleBlockHalf.LOWER) ? pos : pos.down();

        if (world.getBlockEntity(bePos) instanceof AbstractShellContainerBlockEntity be) {
            // Pass Hand.MAIN_HAND as the interaction hand — if your
            // AbstractShellContainerBlockEntity.onUse() needs the actual hand,
            // add Hand as a parameter to that method instead.
            return be.onUse(world, bePos, player, Hand.MAIN_HAND);
        }
        return ActionResult.PASS;
    }

    // ── FIX #4: Ticker — avoid wildcard capture problem
    /**
     * createTickerHelper() cannot infer types when getExpectedBlockEntityType()
     * returns BlockEntityType<?>. The solution is to use a generic helper method
     * that captures the wildcard properly, avoiding the raw-type inference failure.
     */
    // ── Ticker
    /**
     * We bypass createTickerHelper() entirely because it cannot resolve the
     * wildcard capture from getExpectedBlockEntityType() returning BlockEntityType<?>.
     * Instead we do the type equality check manually and suppress the unchecked
     * cast — it is safe because we verify the type matches before casting.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {

        // Only the LOWER half owns a BlockEntity — skip the upper half entirely
        if (state.get(HALF) != DoubleBlockHalf.LOWER) return null;

        // If the provided type doesn't match what we expect, return null (vanilla contract)
        if (type != getExpectedBlockEntityType()) return null;

        // Safe unchecked cast: we just confirmed type == getExpectedBlockEntityType()
        return (BlockEntityTicker<T>) (BlockEntityTicker<?>) (w, p, s, be) -> {
            if (be instanceof TickableBlockEntity tbe) {
                if (w.isClient) tbe.onClientTick(w, p, s);
                else            tbe.onServerTick(w, p, s);
            }
        };
    }

    /** Subclasses return their specific BlockEntityType so getTicker() can match it. */
    protected abstract BlockEntityType<?> getExpectedBlockEntityType();
}