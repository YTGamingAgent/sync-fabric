package net.stacking.sync_mod.client.render.block.entity;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.SyncBlocks;
import net.stacking.sync_mod.block.entity.ShellConstructorBlockEntity;
import net.stacking.sync_mod.client.model.AbstractShellContainerModel;
import net.stacking.sync_mod.client.model.ShellConstructorModel;
import net.stacking.sync_mod.entity.ShellEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

@Environment(EnvType.CLIENT)
public class ShellConstructorBlockEntityRenderer extends AbstractShellContainerBlockEntityRenderer<ShellConstructorBlockEntity> {
    private static final ResourceLocation SHELL_CONSTRUCTOR_TEXTURE_ID = Sync.locate("textures/block/shell_constructor.png");
    private static final BlockState DEFAULT_STATE = SyncBlocks.SHELL_CONSTRUCTOR.defaultBlockState().setValue(AbstractShellContainerBlock.HALF, DoubleBlockHalf.LOWER).setValue(AbstractShellContainerBlock.FACING, Direction.SOUTH);

    private final ShellConstructorModel model;

    public ShellConstructorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.model = new ShellConstructorModel();
    }

    @Override
    protected AbstractShellContainerModel getShellContainerModel(ShellConstructorBlockEntity blockEntity, BlockState blockState, float tickDelta) {
        this.model.buildProgress = blockEntity.getShellState() == null ? 0F : blockEntity.getShellState().getProgress();
        this.model.showInnerParts = blockEntity.hasLevel();
        return this.model;
    }

    @Override
    protected ShellEntity createEntity(ShellState shellState, ShellConstructorBlockEntity blockEntity, float tickDelta) {
        ShellEntity entity = shellState.asEntity();
        entity.isActive = false;
        entity.pitchProgress = 0;
        return entity;
    }

    @Override
    protected BlockState getDefaultState() {
        return DEFAULT_STATE;
    }

    @Override
    protected ResourceLocation getTextureId() {
        return SHELL_CONSTRUCTOR_TEXTURE_ID;
    }
}
