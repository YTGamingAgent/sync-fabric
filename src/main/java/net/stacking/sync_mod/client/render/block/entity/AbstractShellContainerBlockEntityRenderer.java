package net.stacking.sync_mod.client.render.block.entity;

import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.block.entity.AbstractShellContainerBlockEntity;
import net.stacking.sync_mod.client.model.AbstractShellContainerModel;
import net.stacking.sync_mod.client.model.DoubleBlockModel;
import net.stacking.sync_mod.entity.ShellEntity;
import net.stacking.sync_mod.client.render.entity.ShellEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public abstract class AbstractShellContainerBlockEntityRenderer<T extends AbstractShellContainerBlockEntity> extends DoubleBlockEntityRenderer<T> {
    public AbstractShellContainerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    //? if >=1.21.9 {
    /*@Override
    @SuppressWarnings("unchecked")
    public void submit(DoubleBlockEntityRenderer.State state, PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera) {
        super.submit(state, matrices, collector, camera);
        T blockEntity = (T)state.blockEntity;
        if (blockEntity.getShellState() != null) {
            float yaw = this.getFacing(this.getBlockState(blockEntity)).getOpposite().toYRot();
            ShellEntityRenderer.submitShell(this.createEntity(blockEntity.getShellState(), blockEntity, state.tickDelta), yaw, 0, matrices, collector, camera);
        }
    }
    *///?} else {
    @Override
    //? if >=1.21.5 {
    /*public void render(T blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, net.minecraft.world.phys.Vec3 cameraPos) {
        super.render(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay, cameraPos);
    *///?} else {
    public void render(T blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        super.render(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay);
    //?}
        if (blockEntity.getShellState() != null) {
            this.renderShell(blockEntity.getShellState(), blockEntity, tickDelta, this.getBlockState(blockEntity), matrices, vertexConsumers, light);
        }
    }

    protected void renderShell(ShellState shellState, T blockEntity, float tickDelta, BlockState blockState, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        float yaw = this.getFacing(blockState).getOpposite().toYRot();
        ShellEntity shellEntity = this.createEntity(shellState, blockEntity, tickDelta);

        ShellEntityRenderer.renderShell(shellEntity, yaw, 0, matrices, vertexConsumers, light);
    }
    //?}

    @Override
    protected DoubleBlockModel getModel(T blockEntity, BlockState blockState, float tickDelta) {
        AbstractShellContainerModel model = this.getShellContainerModel(blockEntity, blockState, tickDelta);
        model.doorOpenProgress = blockEntity.getDoorOpenProgress(tickDelta);
        return model;
    }

    protected abstract ShellEntity createEntity(ShellState shellState, T blockEntity, float tickDelta);

    protected abstract AbstractShellContainerModel getShellContainerModel(T blockEntity, BlockState blockState, float tickDelta);
}
