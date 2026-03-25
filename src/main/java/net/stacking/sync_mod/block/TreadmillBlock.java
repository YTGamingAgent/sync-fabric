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
import net.stacking.sync_mod.block.entity.TreadmillBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class TreadmillBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    public static final MapCodec<TreadmillBlock> CODEC = createCodec(TreadmillBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.of("treadmill_part", Part.class);

    // ── Collision shapes (physical — what you stand on / bump into) ─────────────
    // Belt surface at 8/16 = 0.5 blocks tall.  Minecraft's step-up threshold is
    // 0.6 blocks, so a player or animal walking up to the treadmill steps straight
    // onto the belt without needing to jump.  The handlebar console is intentionally
    // excluded from collision so it does not block the runner.
    private static final VoxelShape COL_NS = Block.createCuboidShape(1, 0, 0, 15, 8, 16);
    private static final VoxelShape COL_EW = Block.createCuboidShape(0, 0, 1, 16, 8, 15);

    // ── Outline shapes (selection highlight — includes handlebar on BACK half) ──
    private static final VoxelShape OUTLINE_BACK_SOUTH = VoxelShapes.union(
            Block.createCuboidShape(1, 0, 0, 15, 8, 16),
            Block.createCuboidShape(1, 8, 0, 15, 16, 3)
    );
    private static final VoxelShape OUTLINE_BACK_NORTH = VoxelShapes.union(
            Block.createCuboidShape(1, 0, 0, 15, 8, 16),
            Block.createCuboidShape(1, 8, 13, 15, 16, 16)
    );
    private static final VoxelShape OUTLINE_BACK_EAST = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 1, 16, 8, 15),
            Block.createCuboidShape(0, 8, 1, 3, 16, 15)
    );
    private static final VoxelShape OUTLINE_BACK_WEST = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 1, 16, 8, 15),
            Block.createCuboidShape(13, 8, 1, 16, 16, 15)
    );

    public TreadmillBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(PART, Part.BACK));
    }

    @Override
    protected MapCodec<TreadmillBlock> getCodec() { return CODEC; }

    public static boolean isBack(BlockState state) {
        return state.get(PART) == Part.BACK;
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
        Direction facing = state.get(FACING);
        if (state.get(PART) == Part.BACK) {
            return switch (facing) {
                case SOUTH -> OUTLINE_BACK_SOUTH;
                case NORTH -> OUTLINE_BACK_NORTH;
                case EAST  -> OUTLINE_BACK_EAST;
                case WEST  -> OUTLINE_BACK_WEST;
                default    -> OUTLINE_BACK_SOUTH;
            };
        }
        // FRONT half — belt only, no handlebars
        return (facing == Direction.EAST || facing == Direction.WEST) ? COL_EW : COL_NS;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world,
                                           BlockPos pos, ShapeContext context) {
        // Belt only — handlebars are purely visual and never block movement.
        Direction facing = state.get(FACING);
        return (facing == Direction.EAST || facing == Direction.WEST) ? COL_EW : COL_NS;
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        BlockPos pos     = ctx.getBlockPos();
        BlockPos front   = pos.offset(facing);
        World world      = ctx.getWorld();
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
        return super.getStateForNeighborUpdate(state, direction, neighborState,
                world, pos, neighborPos);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            BlockPos other = pos.offset(
                    getDirectionTowardsOtherPart(state.get(PART), state.get(FACING)));
            BlockState otherState = world.getBlockState(other);
            if (otherState.isOf(this) && otherState.get(PART) != state.get(PART)) {
                world.setBlockState(other, Blocks.AIR.getDefaultState(),
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
        Part part        = state.get(PART);
        BlockPos backPos = (part == Part.BACK) ? pos : pos.offset(facing.getOpposite());
        BlockState backState = world.getBlockState(backPos);
        BlockEntity be   = world.getBlockEntity(backPos);
        if (!(be instanceof TreadmillBlockEntity treadmill)) return ActionResult.PASS;

        Box search = new Box(backPos).expand(10.0);
        List<MobEntity> mobs = world.getEntitiesByClass(
                MobEntity.class, search,
                m -> m.isLeashed()
                        && m.getLeashHolder() instanceof PlayerEntity lp
                        && lp.getUuid().equals(player.getUuid()));
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
        Part part        = state.get(PART);
        Direction facing = state.get(FACING);
        BlockEntity first  = world.getBlockEntity(pos);
        BlockEntity second = world.getBlockEntity(
                pos.offset(getDirectionTowardsOtherPart(part, facing)));
        if (!(first  instanceof TreadmillBlockEntity firstT)
                || !(second instanceof TreadmillBlockEntity secondT)) return;
        TreadmillBlockEntity back  = (part == Part.BACK)  ? firstT  : secondT;
        TreadmillBlockEntity front = (part == Part.BACK)  ? secondT : firstT;
        if (back.isOverheated()) {
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (!isBack(state)) return null;
        if (type != SyncBlockEntities.TREADMILL) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<TreadmillBlockEntity>)
                (w, pos, st, be) -> {
                    if (w.isClient) be.onClientTick(w, pos, st);
                    else            be.onServerTick(w, pos, st);
                };
    }

    public enum Part implements StringIdentifiable {
        FRONT("front"),
        BACK("back");

        private final String name;
        Part(String name) { this.name = name; }

        @Override public String toString()  { return name; }
        @Override public String asString()  { return name; }
    }
}