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

    // -----------------------------------------------------------------------
    // Strategy: take FULL control of every transform in preRender() and
    // cancel GeckoLib's auto-rotation via rotateBlock() → do nothing.
    //
    // Model cubes span X/Z: -8..+8 Blockbench units = -0.5..+0.5 blocks.
    // The MatrixStack origin when render() is first called is at the SW
    // corner of the block (0,0,0 relative to the block).
    //
    // Required net transform:
    //   translate(0.5, 0, 0.5)     → move origin from SW corner to block centre
    //   rotateY(yRot)              → spin model to match FACING
    //
    // Model front (doors) faces -Z (NORTH) in Blockbench.
    // facing.asRotation(): SOUTH=0°, WEST=90°, NORTH=180°, EAST=270°
    // So 180° - facing.asRotation() turns the -Z front toward FACING:
    //   placed SOUTH → yRot=180°  → model rotated 180° → doors face +Z = SOUTH ✓
    //   placed NORTH → yRot=0°   → doors face -Z = NORTH ✓
    //   placed WEST  → yRot=90°  → doors face -X = WEST  ✓
    //   placed EAST  → yRot=270° → doors face +X = EAST  ✓
    // -----------------------------------------------------------------------

    @Override
    protected void rotateBlock(Direction facing, MatrixStack poseStack) {
        // Intentionally empty — we handle all rotation in preRender().
    }

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

        if (!AbstractShellContainerBlock.isBottom(state)) {
            poseStack.scale(0f, 0f, 0f);
            return;
        }

        if (!animatable.hasWorld()) {
            // Hotbar / inventory item rendering.
            // Model is 2 blocks tall; scale + shift so it sits centred in the slot.
            poseStack.translate(0.5, 0.15, 0.5);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(225f));
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30f));
            poseStack.scale(0.4f, 0.4f, 0.4f);
            return;
        }

        // World rendering — centre on block then rotate to face the right direction.
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
        ShellEntity shellEntity = shellState.asEntity();
        shellEntity.isActive = false;
        shellEntity.pitchProgress = 0;
        EntityRenderer<? super ShellEntity> renderer =
                MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(shellEntity);
        renderer.render(shellEntity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
