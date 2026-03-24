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
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.SyncBlocks;
import net.stacking.sync_mod.block.entity.ShellConstructorBlockEntity;
import net.stacking.sync_mod.entity.ShellEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@Environment(EnvType.CLIENT)
public class ShellConstructorBlockEntityRenderer
        extends GeoBlockRenderer<ShellConstructorBlockEntity> {

    public ShellConstructorBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(new GeoModel<ShellConstructorBlockEntity>() {
            @Override
            public Identifier getModelResource(ShellConstructorBlockEntity animatable) {
                return Identifier.of("sync", "geo/block/shell_constructor.geo.json");
            }
            @Override
            public Identifier getTextureResource(ShellConstructorBlockEntity animatable) {
                return Identifier.of("sync", "textures/block/shell_constructor.png");
            }
            @Override
            public Identifier getAnimationResource(ShellConstructorBlockEntity animatable) {
                return Identifier.of("sync", "animations/block/shell_constructor.animation.json");
            }
        });
    }

    /**
     * preRender is called from INSIDE GeckoLib's own render(), so our transforms are
     * applied at exactly the right point in the pipeline — no double-translation.
     */
    @Override
    public void preRender(MatrixStack poseStack,
                          ShellConstructorBlockEntity animatable,
                          BakedGeoModel model,
                          VertexConsumerProvider bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          int color) {

        BlockState state = animatable.hasWorld()
                ? animatable.getCachedState()
                : SyncBlocks.SHELL_CONSTRUCTOR.getDefaultState()
                        .with(AbstractShellContainerBlock.HALF, DoubleBlockHalf.LOWER)
                        .with(AbstractShellContainerBlock.FACING, Direction.SOUTH)
                        .with(AbstractShellContainerBlock.OPEN, false);

        // Only the LOWER block entity renders the full 2-block model.
        if (!AbstractShellContainerBlock.isBottom(state)) {
            poseStack.scale(0f, 0f, 0f);
            return;
        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, color);

        Direction facing = state.get(AbstractShellContainerBlock.FACING);
        // Model front (doors) faces -Z (NORTH) in Blockbench space.
        // facing.asRotation(): SOUTH=0, WEST=90, NORTH=180, EAST=270.
        // 180f - facing.asRotation() rotates the -Z front to the correct facing direction.
        float yRot = 180f - facing.asRotation();

        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRot));
    }

    /**
     * Override render() only to add the shell-entity rendering on top of the
     * normal GeckoLib pass. We do NOT push any extra matrix transforms here —
     * that is preRender's job.
     */
    @Override
    public void render(ShellConstructorBlockEntity blockEntity, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        // Let GeckoLib render the main model (calls preRender internally).
        super.render(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay);

        // Render the shell entity that lives inside the constructor (if any).
        if (!blockEntity.hasWorld()) return;
        BlockState state = blockEntity.getCachedState();
        if (!AbstractShellContainerBlock.isBottom(state)) return;

        ShellState shellState = blockEntity.getShellState();
        if (shellState != null) {
            float yaw = state.get(AbstractShellContainerBlock.FACING).getOpposite().asRotation();
            ShellEntity shellEntity = shellState.asEntity();
            shellEntity.isActive = false;
            shellEntity.pitchProgress = 0;
            EntityRenderer<? super ShellEntity> renderer =
                    MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(shellEntity);
            renderer.render(shellEntity, yaw, tickDelta, matrices, vertexConsumers, light);
        }
    }
}
