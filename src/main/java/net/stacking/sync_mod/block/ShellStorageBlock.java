package net.stacking.sync_mod.block;

//? if >=1.20.3
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.stacking.sync_mod.block.entity.ShellStorageBlockEntity;

@SuppressWarnings("deprecation")
public class ShellStorageBlock extends AbstractShellContainerBlock {
    //? if >=1.20.3
    public static final MapCodec<ShellStorageBlock> CODEC = simpleCodec(ShellStorageBlock::new);
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ShellStorageBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(OPEN, false).setValue(ENABLED, false).setValue(POWERED, false));
    }

    //? if >=1.20.3 {
    @Override
    public MapCodec<ShellStorageBlock> codec() {
        return CODEC;
    }
    //?}

    public static boolean isEnabled(BlockState state) {
        return state.getValue(ENABLED);
    }

    public static boolean isPowered(BlockState state) {
        return state.getValue(POWERED);
    }

    public static void setPowered(BlockState state, Level world, BlockPos pos, boolean powered) {
        if (state.getValue(POWERED) != powered) {
            world.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);

            BlockPos secondPos = pos.relative(getDirectionTowardsAnotherPart(state));
            BlockState secondState = world.getBlockState(secondPos);
            if (secondState.is(state.getBlock())) {
                world.setBlock(secondPos, secondState.setValue(POWERED, powered), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShellStorageBlockEntity(pos, state);
    }

    @Override
    //? if >=1.21.2 {
    /*public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, @org.jetbrains.annotations.Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean notify) {
    *///?} else
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (!world.isClientSide()) {
            boolean enabled = state.getValue(ENABLED);
            boolean shouldBeEnabled = shouldBeEnabled(state, world, pos);
            if (enabled != shouldBeEnabled) {
                BlockPos secondPartPos = pos.relative(getDirectionTowardsAnotherPart(state));
                if (enabled) {
                    world.scheduleTick(pos, this, 4);
                    world.scheduleTick(secondPartPos, this, 4);
                } else {
                    world.setBlock(pos, state.setValue(ENABLED, true), Block.UPDATE_CLIENTS);
                    BlockState secondPartState = world.getBlockState(secondPartPos);
                    if (secondPartState.is(this)) {
                        world.setBlock(secondPartPos, secondPartState.setValue(ENABLED, true), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(ENABLED) && !shouldBeEnabled(state, world, pos)) {
            world.setBlock(pos, state.setValue(ENABLED, false), Block.UPDATE_CLIENTS);
        }
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
        if (world.isClientSide() && entity instanceof Player && isBottom(state)) {
            if (world.getBlockEntity(pos) instanceof ShellStorageBlockEntity shellStorage) {
                shellStorage.onEntityCollisionClient(entity, state);
            }
        }
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ENABLED);
        builder.add(POWERED);
    }

    private static boolean shouldBeEnabled(BlockState state, Level world, BlockPos pos) {
        return world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.relative(getDirectionTowardsAnotherPart(state)));
    }
}
