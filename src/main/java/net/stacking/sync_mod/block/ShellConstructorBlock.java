package net.stacking.sync_mod.block;

//? if >=1.20.3
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.stacking.sync_mod.block.entity.ShellConstructorBlockEntity;

public class ShellConstructorBlock extends AbstractShellContainerBlock {
    //? if >=1.20.3
    public static final MapCodec<ShellConstructorBlock> CODEC = simpleCodec(ShellConstructorBlock::new);

    public ShellConstructorBlock(Properties settings) {
        super(settings);
    }

    //? if >=1.20.3 {
    @Override
    public MapCodec<ShellConstructorBlock> codec() {
        return CODEC;
    }
    //?}

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShellConstructorBlockEntity(pos, state);
    }
}
