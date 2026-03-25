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
            @Override public Identifier getModelResource(ShellConstructorBlockEntity a) {
                return Identifier.of("sync", "geo/block/shell_constructor.geo.json");
            }
            @Override public Identifier getTextureResource(ShellConstructorBlockEntity a) {
                return Identifier.of("sync", "textures/block/shell_constructor.png");
            }
            @Override public Identifier getAnimationResource(ShellConstructorBlockEntity a) {
                return Identifier.of("sync", "animations/block/shell_constructor.animation.json");
            }
        });
    }

    // -----------------------------------------------------------------------
    // rotateBlock() → empty. We own all rotation in preRender().
    // -----------------------------------------------------------------------
    @Override
    protected void rotateBlock(Direction facing, MatrixStack poseStack) {}

    @Override
    public void preRender(MatrixStack poseStack,
                          ShellConstructorBlockEntity animatable,
                          BakedGeoModel model, VertexConsumerProvider bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, int color) {

        BlockState state = animatable.hasWorld()
                ? animatable.getCachedState()
                : SyncBlocks.SHELL_CONSTRUCTOR.getDefaultState()
                        .with(AbstractShellContainerBlock.HALF, DoubleBlockHalf.LOWER)
                        .with(AbstractShellContainerBlock.FACING, Direction.SOUTH)
                        .with(AbstractShellContainerBlock.OPEN, false);

        if (!AbstractShellContainerBlock.isBottom(state)) {
            poseStack.scale(0f, 0f, 0f);
            return;
        }

        // ---- ITEM RENDERING (hotbar / inventory) ----------------------------
        // Minecraft has ALREADY applied the gui display transform from the item
        // model JSON (rotation [30, 225, 0], scale [0.625, 0.625, 0.625]) before
        // calling this renderer.  We must NOT add our own rotation here or it
        // double-rotates.  All we need is to centre the model so its X/Z pivot
        // aligns with the display transform's origin.
        if (!animatable.hasWorld()) {
            poseStack.translate(0.5, 0.0, 0.5);
            return;
        }

        // ---- WORLD RENDERING ------------------------------------------------
        // translate(0.5, 0, 0.5) → move model origin from SW block-corner to centre.
        // rotateY(yRot)           → spin to match FACING direction.
        // Model doors face -Z (NORTH) in Blockbench.  Using 180° - facing.asRotation()
        // turns the -Z front toward the correct world direction.
        Direction facing = state.get(AbstractShellContainerBlock.FACING);
        float yRot = 180f - facing.asRotation();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRot));
    }

    @Override
    public void render(ShellConstructorBlockEntity blockEntity, float tickDelta,
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
        shell.isActive = false;
        shell.pitchProgress = 0;
        EntityRenderer<? super ShellEntity> renderer =
                MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(shell);
        renderer.render(shell, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
