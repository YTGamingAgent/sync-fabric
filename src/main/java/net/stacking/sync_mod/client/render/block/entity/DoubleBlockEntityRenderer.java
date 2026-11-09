package net.stacking.sync_mod.client.render.block.entity;

import net.stacking.sync_mod.util.client.render.PoseUtil;

import net.stacking.sync_mod.block.entity.DoubleBlockEntity;
import net.stacking.sync_mod.client.model.DoubleBlockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@Environment(EnvType.CLIENT)
public abstract class DoubleBlockEntityRenderer<T extends BlockEntity & DoubleBlockEntity> implements BlockEntityRenderer<T/*? if >=1.21.9 {*//*, DoubleBlockEntityRenderer.State*//*?}*/> {
    protected final BlockEntityRendererProvider.Context context;

    public DoubleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    //? if >=1.21.9 {
    /*/^*
     * Since 1.21.9 block entities are drawn from a render state, after the world has been "extracted".
     * The block entity is kept so the (shared) model can be set up for it right before drawing.
     ^/
    public static class State extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        public BlockEntity blockEntity;
        public float tickDelta;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T blockEntity, State state, float tickDelta, net.minecraft.world.phys.Vec3 cameraPos, @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickDelta, cameraPos, crumbling);
        state.blockEntity = blockEntity;
        state.tickDelta = tickDelta;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void submit(State state, PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera) {
        T blockEntity = (T)state.blockEntity;
        BlockState blockState = this.getBlockState(blockEntity);
        int light = state.lightCoords;

        matrices.pushPose();
        this.applyTransform(blockState, matrices);
        DoubleBlockModel model = this.getModel(blockEntity, blockState, state.tickDelta);
        collector.submitCustomGeometry(matrices, model.renderType(this.getTextureId()), (pose, vertices) -> {
            PoseStack poseStack = new PoseStack();
            poseStack.last().set(pose);
            this.drawParts(blockEntity, blockState, this.getModel(blockEntity, blockState, state.tickDelta), poseStack, vertices, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        });
        matrices.popPose();
    }
    *///?} else {
    @Override
    //? if >=1.21.5 {
    /*public void render(T blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, net.minecraft.world.phys.Vec3 cameraPos) {
    *///?} else
    public void render(T blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        BlockState blockState = this.getBlockState(blockEntity);

        matrices.pushPose();
        this.applyTransform(blockState, matrices);
        DoubleBlockModel model = this.getModel(blockEntity, blockState, tickDelta);
        VertexConsumer consumer = vertexConsumers.getBuffer(model.renderType(this.getTextureId()));
        this.drawParts(blockEntity, blockState, model, matrices, consumer, light, overlay);
        matrices.popPose();
    }
    //?}

    private void applyTransform(BlockState blockState, PoseStack matrices) {
        matrices.translate(0.5D, 0.75D, 0.5D);
        matrices.scale(-0.5F, -0.5F, 0.5F);
        PoseUtil.rotateYDegrees(matrices, this.getFacing(blockState).toYRot());
    }

    private void drawParts(T blockEntity, BlockState blockState, DoubleBlockModel model, PoseStack matrices, VertexConsumer consumer, int light, int overlay) {
        if (blockEntity.hasLevel()) {
            // In the world, every half has its own block entity and renders only its own part.
            model.render(blockEntity.getBlockType(blockState), matrices, consumer, light, overlay);
        } else {
            // Item rendering: draw both halves.
            model.renderModel(matrices, consumer, light, overlay, DoubleBlockModel.WHITE);
        }
    }

    protected BlockState getBlockState(T blockEntity) {
        return blockEntity.hasLevel() ? blockEntity.getBlockState() : this.getDefaultState();
    }

    protected Direction getFacing(BlockState blockState) {
        return blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    protected abstract DoubleBlockModel getModel(T blockEntity, BlockState blockState, float tickDelta);

    protected abstract BlockState getDefaultState();

    protected abstract ResourceLocation getTextureId();
}
