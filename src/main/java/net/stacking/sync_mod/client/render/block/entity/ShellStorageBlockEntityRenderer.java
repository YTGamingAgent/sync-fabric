package net.stacking.sync_mod.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.SyncBlocks;
import net.stacking.sync_mod.block.entity.ShellStorageBlockEntity;
import net.stacking.sync_mod.entity.ShellEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@Environment(EnvType.CLIENT)
public class ShellStorageBlockEntityRenderer
        extends GeoBlockRenderer<ShellStorageBlockEntity> {

    public ShellStorageBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(new GeoModel<ShellStorageBlockEntity>() {
            @Override public Identifier getModelResource(ShellStorageBlockEntity a) {
                return Identifier.of("sync", "geo/block/shell_storage.geo.json");
            }
            @Override public Identifier getTextureResource(ShellStorageBlockEntity a) {
                return Identifier.of("sync", "textures/block/shell_storage.png");
            }
            @Override public Identifier getAnimationResource(ShellStorageBlockEntity a) {
                return Identifier.of("sync", "animations/block/shell_storage.animation.json");
            }
        });
    }

    @Override
    protected void rotateBlock(Direction facing, MatrixStack poseStack) {}

    @Override
    public void preRender(MatrixStack poseStack,
                          ShellStorageBlockEntity animatable,
                          BakedGeoModel model, VertexConsumerProvider bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, int color) {

        BlockState state = animatable.hasWorld()
                ? animatable.getCachedState()
                : SyncBlocks.SHELL_STORAGE.getDefaultState()
                        .with(AbstractShellContainerBlock.HALF, DoubleBlockHalf.LOWER)
                        .with(AbstractShellContainerBlock.FACING, Direction.SOUTH)
                        .with(AbstractShellContainerBlock.OPEN, false);

        if (!AbstractShellContainerBlock.isBottom(state)) {
            poseStack.scale(0f, 0f, 0f);
            return;
        }

        // Item rendering: JSON gui transform already applied — just centre model.
        if (!animatable.hasWorld()) {
            poseStack.translate(0.5, 0.0, 0.5);
            return;
        }

        // World rendering: centre + rotate to facing.
        Direction facing = state.get(AbstractShellContainerBlock.FACING);
        float yRot = 180f - facing.asRotation();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRot));
    }

    @Override
    public void render(ShellStorageBlockEntity blockEntity, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {
        super.render(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay);
        if (!blockEntity.hasWorld()) return;
        BlockState state = blockEntity.getCachedState();
        if (!AbstractShellContainerBlock.isBottom(state)) return;
        ShellState shellState = blockEntity.getShellState();
        if (shellState == null) return;
        float yaw = state.get(AbstractShellContainerBlock.FACING).getOpposite().asRotation();
        ShellEntity shell = shellState.asEntity();
        shell.isActive      = shellState.getProgress() >= ShellState.PROGRESS_DONE;
        shell.pitchProgress = shell.isActive ? blockEntity.getConnectorProgress(tickDelta) : 0;
        EntityRenderer<? super ShellEntity> renderer =
                MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(shell);
        renderer.render(shell, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
