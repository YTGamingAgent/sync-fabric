package net.stacking.sync_mod.client.render.block.entity;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.block.SyncBlocks;
import net.stacking.sync_mod.block.TreadmillBlock;
import net.stacking.sync_mod.block.entity.TreadmillBlockEntity;
import net.stacking.sync_mod.client.model.DoubleBlockModel;
import net.stacking.sync_mod.client.model.TreadmillModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public class TreadmillBlockEntityRenderer extends DoubleBlockEntityRenderer<TreadmillBlockEntity> {
    private static final ResourceLocation TREADMILL_TEXTURE_ID = Sync.locate("textures/block/treadmill.png");
    private static final BlockState DEFAULT_STATE = SyncBlocks.TREADMILL.defaultBlockState().setValue(TreadmillBlock.PART, TreadmillBlock.Part.FRONT).setValue(TreadmillBlock.FACING, Direction.SOUTH);

    private final DoubleBlockModel model;

    public TreadmillBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.model = new TreadmillModel();
    }

    @Override
    protected DoubleBlockModel getModel(TreadmillBlockEntity blockEntity, BlockState blockState, float tickDelta) {
        return this.model;
    }

    @Override
    protected BlockState getDefaultState() {
        return DEFAULT_STATE;
    }

    @Override
    protected ResourceLocation getTextureId() {
        return TREADMILL_TEXTURE_ID;
    }
}
