package net.stacking.sync_mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.stacking.sync_mod.block.entity.ShellConstructorBlockEntity;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;

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