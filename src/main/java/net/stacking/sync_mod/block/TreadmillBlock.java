package net.stacking.sync_mod.block;

import net.stacking.sync_mod.util.EntityUtil;

//? if >=1.21.2 {
/*import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
*///?}

//? if >=1.20.3
import com.mojang.serialization.MapCodec;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;
import net.stacking.sync_mod.block.entity.TickableBlockEntity;
import net.stacking.sync_mod.block.entity.TreadmillBlockEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class TreadmillBlock extends HorizontalDirectionalBlock implements EntityBlock {
    //? if >=1.20.3
    public static final MapCodec<TreadmillBlock> CODEC = simpleCodec(TreadmillBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("treadmill_part", Part.class);

    /** Same range as a vanilla lead. */
    private static final double LEASHED_MOB_SEARCH_RADIUS = 10;

    private static final VoxelShape NORTH_SHAPE_BACK;
    private static final VoxelShape NORTH_SHAPE_FRONT;
    private static final VoxelShape SOUTH_SHAPE_BACK;
    private static final VoxelShape SOUTH_SHAPE_FRONT;
    private static final VoxelShape EAST_SHAPE_BACK;
    private static final VoxelShape EAST_SHAPE_FRONT;
    private static final VoxelShape WEST_SHAPE_BACK;
    private static final VoxelShape WEST_SHAPE_FRONT;

    public TreadmillBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.BACK));
    }

    //? if >=1.20.3 {
    @Override
    public MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}

    public static boolean isBack(BlockState state) {
        return state.getValue(PART) == Part.BACK;
    }

    public static DoubleBlockCombiner.BlockType getTreadmillPart(BlockState state) {
        return isBack(state) ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TreadmillBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        //? if >=1.21.4 {
        /*return RenderShape.INVISIBLE;
        *///?} else
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    //? if >=1.21.2 {
    /*public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
    *///?} else
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == getDirectionTowardsOtherPart(state.getValue(PART), state.getValue(FACING))) {
            return neighborState.is(this) && neighborState.getValue(PART) != state.getValue(PART) ? state : Blocks.AIR.defaultBlockState();
        }
        //? if >=1.21.2 {
        /*return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
        *///?} else
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction direction = ctx.getHorizontalDirection();
        BlockPos blockPos = ctx.getClickedPos();
        BlockPos blockPos2 = blockPos.relative(direction);
        return ctx.getLevel().getBlockState(blockPos2).canBeReplaced(ctx) ? this.defaultBlockState().setValue(FACING, direction) : null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide()) {
            BlockPos blockPos = pos.relative(state.getValue(FACING));
            world.setBlock(blockPos, state.setValue(PART, Part.FRONT), Block.UPDATE_ALL);
            //? if >=1.21.5 {
            /*world.updateNeighborsAt(pos, Blocks.AIR, null);
            *///?} else
            world.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(world, pos, Block.UPDATE_ALL);
        }
    }

    @Override
    //? if >=1.20.3 {
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    //?} else
    /*public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {*/
        if (!world.isClientSide() && player.isCreative()) {
            // Same as vanilla beds: in creative, breaking the front part must not drop the back part.
            Part part = state.getValue(PART);
            if (part == Part.FRONT) {
                BlockPos blockPos = pos.relative(getDirectionTowardsOtherPart(part, state.getValue(FACING)));
                BlockState blockState = world.getBlockState(blockPos);
                if (blockState.getBlock() == this && blockState.getValue(PART) == Part.BACK) {
                    world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    world.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, blockPos, Block.getId(blockState));
                }
            }
        }
        //? if >=1.20.3 {
        return super.playerWillDestroy(world, pos, state, player);
        //?} else
        /*super.playerWillDestroy(world, pos, state, player);*/
    }

    /**
     * Right-click a treadmill while you have a mob on a lead to put that mob on the belt
     * (centred on the seam between the two parts, facing the console) and get the lead back.
     */
    //? if >=1.20.5 {
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return this.useLead(state, world, pos, player);
    }
    //?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return this.useLead(state, world, pos, player);
    }
    *///?}

    private InteractionResult useLead(BlockState state, Level world, BlockPos pos, Player player) {
        BlockPos backPos = isBack(state) ? pos : pos.relative(getDirectionTowardsOtherPart(state.getValue(PART), state.getValue(FACING)));
        Mob mob = world.getEntitiesOfClass(Mob.class, new AABB(backPos).inflate(LEASHED_MOB_SEARCH_RADIUS), x -> x.isLeashed() && x.getLeashHolder() == player)
                .stream()
                .findFirst()
                .orElse(null);
        if (mob == null) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(backPos) instanceof TreadmillBlockEntity treadmill) || treadmill.isRunning()) {
            return InteractionResult.PASS;
        }

        Direction facing = state.getValue(FACING);
        Vec3 pivot = TreadmillBlockEntity.computeTreadmillPivot(backPos, facing);
        //? if >=1.21.4 {
        /*mob.removeLeash();
        *///?} else
        mob.dropLeash(true, false);
        EntityUtil.moveTo(mob, pivot.x, pivot.y, pivot.z, facing.toYRot(), 0);
        if (!player.isCreative()) {
            ItemStack lead = new ItemStack(Items.LEAD);
            if (!player.addItem(lead)) {
                player.drop(lead, false);
            }
        }
        treadmill.onSteppedOn(backPos, world.getBlockState(backPos), mob);
        return InteractionResult.SUCCESS;
    }

    @Override
    //? if >=1.20.5 {
    public boolean isPathfindable(BlockState state, PathComputationType type) {
    //?} else
    /*public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {*/
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        boolean isBack = isBack(state);

        return switch (direction) {
            case NORTH -> isBack ? NORTH_SHAPE_BACK : NORTH_SHAPE_FRONT;
            case SOUTH -> isBack ? SOUTH_SHAPE_BACK : SOUTH_SHAPE_FRONT;
            case EAST -> isBack ? EAST_SHAPE_BACK : EAST_SHAPE_FRONT;
            case WEST -> isBack ? WEST_SHAPE_BACK : WEST_SHAPE_FRONT;
            default -> NORTH_SHAPE_FRONT;
        };
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof TreadmillBlockEntity treadmillBlockEntity) {
            treadmillBlockEntity.onSteppedOn(pos, state, entity);
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (type != SyncBlockEntities.TREADMILL || !isBack(state)) {
            return null;
        }

        return world.isClientSide() ? TickableBlockEntity::clientTicker : TickableBlockEntity::serverTicker;
    }

    public static Direction getDirectionTowardsOtherPart(Part part, Direction direction) {
        return part == Part.BACK ? direction : direction.getOpposite();
    }

    public enum Part implements StringRepresentable {
        FRONT("front"),
        BACK("back");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    static {
        final VoxelShape TRACK_NORTH_SOUTH = Block.box(1.5, 0, 0, 14.5, 4, 16);
        final VoxelShape TRACK_EAST_WEST = Block.box(0, 0, 1.5, 16, 4, 14.5);

        final VoxelShape LEFT_SIDE_GUARD_NORTH_BACK = Block.box(1.5, 0, 0, 1.6, 8.3, 7.4);
        final VoxelShape RIGHT_SIDE_GUARD_NORTH_BACK = Block.box(14.4, 0, 0, 14.5, 8.3, 7.4);

        final VoxelShape LEFT_SIDE_GUARD_NORTH_FRONT = Block.box(1.5, 0, 7.5, 1.6, 8.7, 16);
        final VoxelShape RIGHT_SIDE_GUARD_NORTH_FRONT = Block.box(14.4, 0, 7.5, 14.5, 8.7, 16);
        final VoxelShape DASHBOARD_NORTH_FRONT = Block.box(1.5, 0, 2, 14.5, 8.5, 3);

        final VoxelShape LEFT_SIDE_GUARD_SOUTH_BACK = Block.box(1.5, 0, 8.6, 1.6, 8.3, 16);
        final VoxelShape RIGHT_SIDE_GUARD_SOUTH_BACK = Block.box(14.4, 0, 8.6, 14.5, 8.3, 16);

        final VoxelShape LEFT_SIDE_GUARD_SOUTH_FRONT = Block.box(1.5, 0, 0, 1.6, 8.7, 8.5);
        final VoxelShape RIGHT_SIDE_GUARD_SOUTH_FRONT = Block.box(14.4, 0, 0, 14.5, 8.7, 8.5);
        final VoxelShape DASHBOARD_SOUTH_FRONT = Block.box(1.5, 0, 13, 14.5, 8.5, 14);

        final VoxelShape LEFT_SIDE_GUARD_EAST_BACK = Block.box(8.6, 0, 1.5, 16, 8.3, 1.6);
        final VoxelShape RIGHT_SIDE_GUARD_EAST_BACK = Block.box(8.6, 0, 14.4, 16, 8.3, 14.5);

        final VoxelShape LEFT_SIDE_GUARD_EAST_FRONT = Block.box(0, 0, 1.5, 8.5, 8.7, 1.6);
        final VoxelShape RIGHT_SIDE_GUARD_EAST_FRONT = Block.box(0, 0, 14.4, 8.5, 8.7, 14.5);
        final VoxelShape DASHBOARD_EAST_FRONT = Block.box(13, 0, 1.5, 14, 8.5, 14.5);

        final VoxelShape LEFT_SIDE_GUARD_WEST_BACK = Block.box(0, 0, 1.5, 7.4, 8.3, 1.6);
        final VoxelShape RIGHT_SIDE_GUARD_WEST_BACK = Block.box(0, 0, 14.4, 7.4, 8.3, 14.5);

        final VoxelShape LEFT_SIDE_GUARD_WEST_FRONT = Block.box(7.5, 0, 1.5, 16, 8.7, 1.6);
        final VoxelShape RIGHT_SIDE_GUARD_WEST_FRONT = Block.box(7.5, 0, 14.4, 16, 8.7, 14.5);
        final VoxelShape DASHBOARD_WEST_FRONT = Block.box(2, 0, 1.5, 3, 8.5, 14.5);

        NORTH_SHAPE_BACK = Shapes.or(TRACK_NORTH_SOUTH, LEFT_SIDE_GUARD_NORTH_BACK, RIGHT_SIDE_GUARD_NORTH_BACK).optimize();
        NORTH_SHAPE_FRONT = Shapes.or(TRACK_NORTH_SOUTH, LEFT_SIDE_GUARD_NORTH_FRONT, RIGHT_SIDE_GUARD_NORTH_FRONT, DASHBOARD_NORTH_FRONT).optimize();

        SOUTH_SHAPE_BACK = Shapes.or(TRACK_NORTH_SOUTH, LEFT_SIDE_GUARD_SOUTH_BACK, RIGHT_SIDE_GUARD_SOUTH_BACK).optimize();
        SOUTH_SHAPE_FRONT = Shapes.or(TRACK_NORTH_SOUTH, LEFT_SIDE_GUARD_SOUTH_FRONT, RIGHT_SIDE_GUARD_SOUTH_FRONT, DASHBOARD_SOUTH_FRONT).optimize();

        EAST_SHAPE_BACK = Shapes.or(TRACK_EAST_WEST, LEFT_SIDE_GUARD_EAST_BACK, RIGHT_SIDE_GUARD_EAST_BACK).optimize();
        EAST_SHAPE_FRONT = Shapes.or(TRACK_EAST_WEST, LEFT_SIDE_GUARD_EAST_FRONT, RIGHT_SIDE_GUARD_EAST_FRONT, DASHBOARD_EAST_FRONT).optimize();

        WEST_SHAPE_BACK = Shapes.or(TRACK_EAST_WEST, LEFT_SIDE_GUARD_WEST_BACK, RIGHT_SIDE_GUARD_WEST_BACK).optimize();
        WEST_SHAPE_FRONT = Shapes.or(TRACK_EAST_WEST, LEFT_SIDE_GUARD_WEST_FRONT, RIGHT_SIDE_GUARD_WEST_FRONT, DASHBOARD_WEST_FRONT).optimize();
    }
}
