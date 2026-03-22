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

    public TreadmillBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(PART, Part.BACK));
    }

    @Override
    protected MapCodec<? extends TreadmillBlock> getCodec() {
        return CODEC;
    }

    public static boolean isBack(BlockState state) {
        return state.get(PART) == Part.BACK;
    }

    public static DoubleBlockProperties.Type getTreadmillPart(BlockState state) {
        Part part = state.get(PART);
        return part == Part.BACK ? DoubleBlockProperties.Type.FIRST : DoubleBlockProperties.Type.SECOND;
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
                m -> m.isLeashed() && m.getLeashHolder() instanceof PlayerEntity lp
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
        Part part = state.get(PART);
        Direction facing = state.get(FACING);
        BlockEntity first  = world.getBlockEntity(pos);
        BlockEntity second = world.getBlockEntity(pos.offset(getDirectionTowardsOtherPart(part, facing)));
        if (!(first  instanceof TreadmillBlockEntity firstTreadmill)
                || !(second instanceof TreadmillBlockEntity secondTreadmill)) return;

        TreadmillBlockEntity back = part == Part.BACK ? firstTreadmill : secondTreadmill;
        if (back.isOverheated()) {
            TreadmillBlockEntity front = part == Part.BACK ? secondTreadmill : firstTreadmill;
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
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return true;
    }

    /**
     * Use a full cube for both halves so the selection/outline box perfectly
     * encloses the GeckoLib-rendered model regardless of facing direction.
     * The model spans two full block positions, so fullCube() on each is correct.
     */
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world,
                                         BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world,
                                           BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof TreadmillBlockEntity t) {
            t.onSteppedOn(pos, state, entity);
        }
    }

    @Override
    @Nullable
    public <T extends net.minecraft.block.entity.BlockEntity>
    net.minecraft.block.entity.BlockEntityTicker<T> getTicker(
            net.minecraft.world.World world,
            net.minecraft.block.BlockState state,
            net.minecraft.block.entity.BlockEntityType<T> type) {
        if (!isBack(state)) return null;
        if (type != SyncBlockEntities.TREADMILL) return null;
        return (w, pos, st, be) -> {
            TreadmillBlockEntity treadmill = (TreadmillBlockEntity) be;
            if (w.isClient) treadmill.onClientTick(w, pos, st);
            else            treadmill.onServerTick(w, pos, st);
        };
    }

    public static Direction getDirectionTowardsOtherPart(Part part, Direction direction) {
        return part == Part.BACK ? direction : direction.getOpposite();
    }

    public enum Part implements StringIdentifiable {
        FRONT("front"),
        BACK("back");
        private final String name;
        Part(String name) { this.name = name; }
        @Override public String toString()  { return this.name; }
        @Override public String asString()  { return this.name; }
    }
}