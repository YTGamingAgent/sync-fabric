package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;
import net.stacking.sync_mod.block.entity.TickableBlockEntity;
import net.stacking.sync_mod.block.entity.TreadmillBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class TreadmillBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    public static final MapCodec<TreadmillBlock> CODEC = createCodec(TreadmillBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.of("treadmill_part", Part.class);

    // The treadmill model is ~13.5 Blockbench units wide (0.084 blocks of
    // margin on each side) and roughly 9.5 units tall at the handlebars.
    // It spans 2 blocks lengthwise (BACK + FRONT).
    //
    // Define shapes for FACING=SOUTH and will rotate them via the
    // FACING direction at runtime. The BACK block contains the standing
    // platform; the front block is the ramp end.

    private static final VoxelShape SHAPE_BACK_S = VoxelShapes.union(
            // Main flat belt/frame: full width, low height
            Block.createCuboidShape(1, 0, 0, 15, 10, 16),
            // Handlebar frame at the back (south face when FACING=SOUTH)
            Block.createCuboidShape(1, 0, 0,  15, 16,  3)
    );

    // FRONT block: ramp end of the treadmill
    private static final VoxelShape SHAPE_FRONT_S = VoxelShapes.union(
            Block.createCuboidShape(1, 0, 0, 15, 10, 16)
    );

    // FACING=NORTH (rotated 180°)
    private static final VoxelShape SHAPE_BACK_N = VoxelShapes.union(
            Block.createCuboidShape(1, 0, 0, 15, 10, 16),
            Block.createCuboidShape(1, 0, 13, 15, 16, 16)
    );
    private static final VoxelShape SHAPE_FRONT_N = VoxelShapes.union(
            Block.createCuboidShape(1, 0, 0, 15, 10, 16)
    );

    // FACING=EAST (model's +Z becomes +X in world)
    private static final VoxelShape SHAPE_BACK_E = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 1, 16, 10, 15),
            Block.createCuboidShape(0, 0, 1,  3, 16, 15)
    );
    private static final VoxelShape SHAPE_FRONT_E = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 1, 16, 10, 15)
    );

    // FACING=WEST
    private static final VoxelShape SHAPE_BACK_W = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 1, 16, 10, 15),
            Block.createCuboidShape(13, 0, 1, 16, 16, 15)
    );
    private static final VoxelShape SHAPE_FRONT_W = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 1, 16, 10, 15)
    );

    public TreadmillBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(PART, Part.BACK));
    }

    @Override
    protected MapCodec<TreadmillBlock> getCodec() {
        return CODEC;
    }

    public static boolean isBack(BlockState state) {
        return state.get(PART) == Part.BACK;
    }

    public static DoubleBlockProperties.Type getTreadmillPart(BlockState state) {
        return isBack(state) ? DoubleBlockProperties.Type.FIRST
                : DoubleBlockProperties.Type.SECOND;
    }

    public static Direction getDirectionTowardsOtherPart(Part part, Direction facing) {
        return part == Part.BACK ? facing : facing.getOpposite();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TreadmillBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world,
                                         BlockPos pos, ShapeContext context) {
        return getShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world,
                                           BlockPos pos, ShapeContext context) {
        return getShape(state);
    }

    private VoxelShape getShape(BlockState state) {
        Direction facing = state.get(FACING);
        Part part = state.get(PART);
        return switch (facing) {
            case SOUTH -> part == Part.BACK ? SHAPE_BACK_S : SHAPE_FRONT_S;
            case NORTH -> part == Part.BACK ? SHAPE_BACK_N : SHAPE_FRONT_N;
            case EAST  -> part == Part.BACK ? SHAPE_BACK_E : SHAPE_FRONT_E;
            case WEST  -> part == Part.BACK ? SHAPE_BACK_W : SHAPE_FRONT_W;
            default    -> VoxelShapes.fullCube();
        };
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        BlockPos pos   = ctx.getBlockPos();
        BlockPos front = pos.offset(facing);
        World world    = ctx.getWorld();

        if (!world.getBlockState(front).canReplace(ctx)) return null;

        return this.getDefaultState()
                .with(FACING, facing)
                .with(PART, Part.BACK);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.get(FACING);
        world.setBlockState(pos.offset(facing),
                state.with(PART, Part.FRONT), 3);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
                                                BlockState neighborState, WorldAccess world,
                                                BlockPos pos, BlockPos neighborPos) {
        if (direction == getDirectionTowardsOtherPart(state.get(PART), state.get(FACING))) {
            return neighborState.isOf(this) && neighborState.get(PART) != state.get(PART)
                    ? state
                    : Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state,
                              PlayerEntity player) {
        if (!world.isClient) {
            BlockPos otherPos = pos.offset(getDirectionTowardsOtherPart(
                    state.get(PART), state.get(FACING)));
            BlockState otherState = world.getBlockState(otherPos);
            if (otherState.isOf(this) && otherState.get(PART) != state.get(PART)) {
                world.setBlockState(otherPos, Blocks.AIR.getDefaultState(),
                        Block.NOTIFY_ALL | Block.SKIP_DROPS);
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        Direction facing = state.get(FACING);
        Part part = state.get(PART);
        BlockPos backPos = (part == Part.BACK) ? pos : pos.offset(facing.getOpposite());
        BlockState backState = world.getBlockState(backPos);
        BlockEntity be = world.getBlockEntity(backPos);

        if (!(be instanceof TreadmillBlockEntity treadmill)) return ActionResult.PASS;

        Box search = new Box(backPos).expand(10.0);
        List<MobEntity> mobs = world.getEntitiesByClass(
                MobEntity.class, search,
                m -> m.isLeashed()
                        && m.getLeashHolder() instanceof PlayerEntity lp
                        && lp.getUuid().equals(player.getUuid())
        );
        if (mobs.isEmpty()) return ActionResult.PASS;

        MobEntity mob = mobs.get(0);
        double x = switch (facing) {
            case WEST  -> backPos.getX();
            case EAST  -> backPos.getX() + 1;
            default    -> backPos.getX() + 0.5D;
        };
        double y = backPos.getY() + 0.175;
        double z = switch (facing) {
            case SOUTH -> backPos.getZ() + 1;
            case NORTH -> backPos.getZ();
            default    -> backPos.getZ() + 0.5D;
        };

        mob.updatePositionAndAngles(x, y, z, facing.asRotation(), 0);
        mob.detachLeash(true, false);
        if (!player.isCreative()) {
            player.getInventory().insertStack(new ItemStack(Items.LEAD));
        }
        treadmill.onSteppedOn(backPos, backState, mob);
        return ActionResult.SUCCESS;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        Part part     = state.get(PART);
        Direction facing = state.get(FACING);
        BlockEntity first  = world.getBlockEntity(pos);
        BlockEntity second = world.getBlockEntity(
                pos.offset(getDirectionTowardsOtherPart(part, facing)));

        if (!(first instanceof TreadmillBlockEntity firstT)
                || !(second instanceof TreadmillBlockEntity secondT)) return;

        TreadmillBlockEntity back = (part == Part.BACK) ? firstT : secondT;
        if (back.isOverheated()) {
            TreadmillBlockEntity front = (part == Part.BACK) ? secondT : firstT;
            double x = front.getPos().getX() + random.nextDouble();
            double y = front.getPos().getY() + 0.4;
            double z = front.getPos().getZ() + random.nextDouble();
            world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.1, 0);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return true;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof TreadmillBlockEntity t) {
            t.onSteppedOn(pos, state, entity);
        }
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends net.minecraft.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        // Only the BACK block's entity is ticked
        if (!isBack(state)) return null;
        if (type != SyncBlockEntities.TREADMILL) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<TreadmillBlockEntity>)
                (w, pos, st, be) -> {
                    if (w.isClient) be.onClientTick(w, pos, st);
                    else            be.onServerTick(w, pos, st);
                };
    }

    // ── Part enum ─────────────────────────────────────────────────────────────

    public enum Part implements StringIdentifiable {
        FRONT("front"),
        BACK("back");

        private final String name;

        Part(String name) { this.name = name; }

        @Override public String toString()   { return name; }
        @Override public String asString()   { return name; }
    }
}