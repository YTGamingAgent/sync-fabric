package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.stacking.sync_mod.block.entity.ShellConstructorBlockEntity;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class ShellConstructorBlock extends AbstractShellContainerBlock {
    public static final MapCodec<ShellConstructorBlock> CODEC = createCodec(ShellConstructorBlock::new);

    public ShellConstructorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends ShellConstructorBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;

        // If TOP half, forward to BOTTOM half
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            pos = pos.offset(Direction.DOWN);
            state = world.getBlockState(pos);
        }

        if (!(world.getBlockEntity(pos) instanceof ShellConstructorBlockEntity be)) return ActionResult.PASS;
        return be.onUse(world, pos, player, hand);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        // Only the LOWER half gets a BlockEntity
        return state.get(HALF) == DoubleBlockHalf.LOWER
                ? new ShellConstructorBlockEntity(pos, state)
                : null;
    }

    @Override
    protected BlockEntityType<?> getExpectedBlockEntityType() {
        return SyncBlockEntities.SHELL_CONSTRUCTOR;
    }
}