package net.stacking.sync_mod.client.render.block.entity;

import net.stacking.sync_mod.block.entity.DoubleBlockEntity;
import net.stacking.sync_mod.client.model.DoubleBlockModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public abstract class DoubleBlockEntityRenderer<T extends BlockEntity & DoubleBlockEntity>
        implements BlockEntityRenderer<T> {

    protected final BlockEntityRendererFactory.Context context;

    public DoubleBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.context = context;
    }

    @Override
    public void render(T blockEntity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        BlockState blockState = this.getBlockState(blockEntity);

        matrices.push();

        Direction face     = this.getFacing(blockState);
        float     rotation = face.asRotation();

        matrices.translate(0.5D, 0.75D + 0.002D, 0.5D);
        matrices.scale(-0.5F, -0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

        DoubleBlockModel model    = this.getModel(blockEntity, blockState, tickDelta);
        VertexConsumer   consumer = vertexConsumers.getBuffer(model.getLayer(this.getTextureId()));

        if (blockEntity.hasWorld() && !this.shouldRenderAllParts()) {
            // Treadmill-style: each block entity renders only its own half
            model.render(blockEntity.getBlockType(blockState), matrices, consumer, light, overlay);
        } else {
            // Shell container-style: lower block entity renders the full 2-block model
            model.render(matrices, consumer, light, overlay);
        }

        matrices.pop();
    }

    protected BlockState getBlockState(T blockEntity) {
        return blockEntity.hasWorld()
                ? blockEntity.getCachedState()
                : this.getDefaultState();
    }

    protected Direction getFacing(BlockState blockState) {
        return blockState.get(Properties.HORIZONTAL_FACING);
    }

    /**
     * When {@code true}, always renders ALL parts (FIRST + translate + SECOND)
     * from the single lower block entity.  Use for vertically-stacked machines
     * (Shell Constructor, Shell Storage).
     * <p>
     * When {@code false} (default), each block entity renders only its own half —
     * correct for horizontally-placed double blocks like the Treadmill.
     */
    protected boolean shouldRenderAllParts() {
        return false;
    }

    protected abstract DoubleBlockModel getModel(T blockEntity,
                                                 BlockState blockState,
                                                 float tickDelta);

    protected abstract BlockState getDefaultState();

    protected abstract Identifier getTextureId();
}