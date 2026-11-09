package net.stacking.sync_mod.block;

//? if >=1.20.3
import com.mojang.serialization.MapCodec;
import net.stacking.sync_mod.block.entity.AbstractShellContainerBlockEntity;
import net.stacking.sync_mod.block.entity.TickableBlockEntity;
import net.stacking.sync_mod.util.ItemUtil;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 && <1.21.2
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
//? if <1.21.2
import net.minecraft.world.level.block.state.properties.DirectionProperty;
//? if >=1.21.2 {
/*import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
*///?}
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public abstract class AbstractShellContainerBlock extends BaseEntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    //? if >=1.21.2 {
    /*public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    *///?} else
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<ComparatorOutputType> OUTPUT = EnumProperty.create("output", ComparatorOutputType.class);

    private static final VoxelShape SOLID_SHAPE_TOP;
    private static final VoxelShape SOLID_SHAPE_BOTTOM;
    private static final VoxelShape NORTH_SHAPE_TOP;
    private static final VoxelShape NORTH_SHAPE_BOTTOM;
    private static final VoxelShape SOUTH_SHAPE_TOP;
    private static final VoxelShape SOUTH_SHAPE_BOTTOM;
    private static final VoxelShape EAST_SHAPE_TOP;
    private static final VoxelShape EAST_SHAPE_BOTTOM;
    private static final VoxelShape WEST_SHAPE_TOP;
    private static final VoxelShape WEST_SHAPE_BOTTOM;

    protected AbstractShellContainerBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
            this.getStateDefinition().any()
                .setValue(OPEN, false)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH)
                .setValue(OUTPUT, ComparatorOutputType.PROGRESS)
        );
    }

    //? if >=1.20.3 {
    @Override
    public abstract MapCodec<? extends AbstractShellContainerBlock> codec();
    //?}


    public static void setOpen(BlockState state, Level world, BlockPos pos, boolean open) {
        if (state.getValue(OPEN) != open) {
            world.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);

            BlockPos secondPos = pos.relative(getDirectionTowardsAnotherPart(state));
            BlockState secondState = world.getBlockState(secondPos);
            if (secondState.is(state.getBlock())) {
                world.setBlock(secondPos, secondState.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
            }
        }
    }

    public static boolean isOpen(BlockState state) {
        return state.getValue(OPEN);
    }

    public static boolean isBottom(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    public static DoubleBlockCombiner.BlockType getShellContainerHalf(BlockState state) {
        return isBottom(state) ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
    }

    public static Direction getDirectionTowardsAnotherPart(BlockState state) {
        return isBottom(state) ? Direction.UP : Direction.DOWN;
    }

    @Override
    //? if >=1.21.2 {
    /*public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
    *///?} else
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (doubleBlockHalf == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighborState.is(this) && neighborState.getValue(HALF) != doubleBlockHalf ? state.setValue(FACING, neighborState.getValue(FACING)) : Blocks.AIR.defaultBlockState();
        }
        if (doubleBlockHalf == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(world, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        //? if >=1.21.2 {
        /*return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
        *///?} else
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos blockPos = ctx.getClickedPos();
        if (blockPos.getY() < /*? if >=1.21.2 {*//*world.getMaxY()*//*?} else {*/world.getMaxBuildHeight() - 1/*?}*/ && world.getBlockState(blockPos.above()).canBeReplaced(ctx)) {
            return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(HALF, DoubleBlockHalf.LOWER);
        }

        return null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        world.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    //? if >=1.21.9 {
    /*public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean pastEdges) {
        super.entityInside(state, world, pos, entity, effects, pastEdges);
    *///?} else if >=1.21.5 {
    /*public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier effects) {
        super.entityInside(state, world, pos, entity, effects);
    *///?} else {
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        super.entityInside(state, world, pos, entity);
    //?}
        if (!world.isClientSide() && entity instanceof Player && isBottom(state)) {
            setOpen(state, world, pos, true);
        }
    }

    @Override
    //? if >=1.20.3 {
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    //?} else
    /*public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {*/
        boolean bottom = isBottom(state);
        BlockPos bottomPos = bottom ? pos : pos.below();
        if (!world.isClientSide() && player.isCreative() && !bottom) {
            // Same as vanilla doors: in creative, breaking the top half must not drop the bottom half.
            BlockState blockState = world.getBlockState(bottomPos);
            if (blockState.getBlock() == state.getBlock() && blockState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                world.setBlock(bottomPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                world.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, bottomPos, Block.getId(blockState));
            }
        }
        //? if >=1.20.3 {
        return super.playerWillDestroy(world, pos, state, player);
        //?} else
        /*super.playerWillDestroy(world, pos, state, player);*/
    }

    // Since 1.21.5 block removal is handled by AbstractShellContainerBlockEntity#preRemoveSideEffects.
    //? if <1.21.5 {
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && isBottom(state) && world.getBlockEntity(pos) instanceof AbstractShellContainerBlockEntity shellContainer) {
            shellContainer.onBreak(world, pos);
        }
        super.onRemove(state, world, pos, newState, moved);
    }
    //?}

    /**
     * Right-click. The wrench cycles the comparator output; everything else
     * (dye, bed, totem, empty hand, ...) is handed to the block entity.
     */
    private static InteractionResult use(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand) {
        if (ItemUtil.isWrench(stack)) {
            if (!world.isClientSide()) {
                world.setBlock(pos, state.cycle(OUTPUT), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
                world.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            //? if >=1.21.2 {
            /*return InteractionResult.SUCCESS;
            *///?} else
            return InteractionResult.sidedSuccess(world.isClientSide());
        }

        AbstractShellContainerBlockEntity shellContainer = getBottomBlockEntity(state, world, pos);
        if (shellContainer == null) {
            return InteractionResult.PASS;
        }
        return shellContainer.onUse(world, shellContainer.getBlockPos(), player, hand);
    }

    //? if >=1.21.2 {
    /*@Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return use(stack, state, world, pos, player, hand);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return use(ItemStack.EMPTY, state, world, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?} else if >=1.20.5 {
    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = use(stack, state, world, pos, player, hand);
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            case PASS -> ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return use(ItemStack.EMPTY, state, world, pos, player, InteractionHand.MAIN_HAND);
    }
    //?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return use(player.getItemInHand(hand), state, world, pos, player, hand);
    }
    *///?}

    @Nullable
    private static AbstractShellContainerBlockEntity getBottomBlockEntity(BlockState state, Level world, BlockPos pos) {
        BlockPos bottomPos = isBottom(state) ? pos : pos.below();
        return world.getBlockEntity(bottomPos) instanceof AbstractShellContainerBlockEntity shellContainer ? shellContainer : null;
    }

    @Override
    //? if >=1.20.5 {
    public boolean isPathfindable(BlockState state, PathComputationType type) {
    //?} else
    /*public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {*/
        return false;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    //? if >=1.21.9 {
    /*public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
    *///?} else
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof AbstractShellContainerBlockEntity shellContainer
                ? state.getValue(OUTPUT) == ComparatorOutputType.PROGRESS
                    ? shellContainer.getProgressComparatorOutput()
                    : shellContainer.getInventoryComparatorOutput()
                : 0;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        //? if >=1.21.4 {
        /*return RenderShape.INVISIBLE;
        *///?} else
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public long getSeed(BlockState state, BlockPos pos) {
        return Mth.getSeed(pos.getX(), pos.below(state.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), pos.getZ());
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OPEN, OUTPUT);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (!isBottom(state)) {
            return null;
        }
        return world.isClientSide() ? TickableBlockEntity::clientTicker : TickableBlockEntity::serverTicker;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        boolean isBottom = isBottom(state);
        if (!isOpen(state)) {
            return isBottom ? SOLID_SHAPE_BOTTOM : SOLID_SHAPE_TOP;
        }

        Direction direction = state.getValue(FACING);
        return switch (direction) {
            case NORTH -> isBottom ? NORTH_SHAPE_BOTTOM : NORTH_SHAPE_TOP;
            case SOUTH -> isBottom ? SOUTH_SHAPE_BOTTOM : SOUTH_SHAPE_TOP;
            case EAST -> isBottom ? EAST_SHAPE_BOTTOM : EAST_SHAPE_TOP;
            case WEST -> isBottom ? WEST_SHAPE_BOTTOM : WEST_SHAPE_TOP;
            default -> throw new IllegalArgumentException();
        };
    }

    static {
        final VoxelShape ROOF = Block.box(0, 15, 0, 16, 16, 16);
        final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 1, 16);
        final VoxelShape NORTH_WALL = Block.box(0, 0, 0, 16, 16, 1);
        final VoxelShape SOUTH_WALL = Block.box(0, 0, 15, 16, 16, 16);
        final VoxelShape EAST_WALL = Block.box(15, 0, 0, 16, 16, 16);
        final VoxelShape WEST_WALL = Block.box(0, 0, 0, 1, 16, 16);

        final VoxelShape NORTH_SHAPE = Shapes.or(NORTH_WALL, EAST_WALL, WEST_WALL).optimize();
        final VoxelShape SOUTH_SHAPE = Shapes.or(SOUTH_WALL, EAST_WALL, WEST_WALL).optimize();
        final VoxelShape EAST_SHAPE = Shapes.or(NORTH_WALL, SOUTH_WALL, EAST_WALL).optimize();
        final VoxelShape WEST_SHAPE = Shapes.or(NORTH_WALL, SOUTH_WALL, WEST_WALL).optimize();

        SOLID_SHAPE_TOP = Shapes.or(NORTH_WALL, SOUTH_WALL, EAST_WALL, WEST_WALL, ROOF).optimize();
        SOLID_SHAPE_BOTTOM = Shapes.or(NORTH_WALL, SOUTH_WALL, EAST_WALL, WEST_WALL, FLOOR).optimize();

        NORTH_SHAPE_TOP = Shapes.or(NORTH_SHAPE, ROOF).optimize();
        NORTH_SHAPE_BOTTOM = Shapes.or(NORTH_SHAPE, FLOOR).optimize();
        SOUTH_SHAPE_TOP = Shapes.or(SOUTH_SHAPE, ROOF).optimize();
        SOUTH_SHAPE_BOTTOM = Shapes.or(SOUTH_SHAPE, FLOOR).optimize();
        EAST_SHAPE_TOP = Shapes.or(EAST_SHAPE, ROOF).optimize();
        EAST_SHAPE_BOTTOM = Shapes.or(EAST_SHAPE, FLOOR).optimize();
        WEST_SHAPE_TOP = Shapes.or(WEST_SHAPE, ROOF).optimize();
        WEST_SHAPE_BOTTOM = Shapes.or(WEST_SHAPE, FLOOR).optimize();
    }
}
