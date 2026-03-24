package net.stacking.sync_mod.client.render.block.entity;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.stacking.sync_mod.block.TreadmillBlock;
import net.stacking.sync_mod.block.entity.TreadmillBlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TreadmillBlockEntityRenderer extends GeoBlockRenderer<TreadmillBlockEntity> {

    public TreadmillBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(new GeoModel<TreadmillBlockEntity>() {
            @Override
            public Identifier getModelResource(TreadmillBlockEntity animatable) {
                return Identifier.of("sync", "geo/treadmill.geo.json");
            }
            @Override
            public Identifier getTextureResource(TreadmillBlockEntity animatable) {
                return Identifier.of("sync", "textures/block/treadmill.png");
            }
            @Override
            public Identifier getAnimationResource(TreadmillBlockEntity animatable) {
                return null;
            }
        });
    }

    @Override
    public RenderLayer getRenderType(TreadmillBlockEntity animatable, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityCutoutNoCull(texture);
    }

    // -----------------------------------------------------------------------
    // Treadmill model: root pivot at [0,0,0], cubes X:-6.75..+6.75, Z:-8..+24.
    // Belt runs toward +Z (SOUTH) in Blockbench.
    // Net transform needed: translate(0.5,0,0.5) + rotateY(facing.asRotation())
    //   SOUTH 0°  → belt faces +Z = SOUTH ✓
    //   WEST  90° → belt faces -X = WEST  ✓
    //   NORTH 180°→ belt faces -Z = NORTH ✓
    //   EAST  270°→ belt faces +X = EAST  ✓
    // -----------------------------------------------------------------------

    @Override
    protected void rotateBlock(Direction facing, MatrixStack poseStack) {
        // Intentionally empty — rotation handled in preRender().
    }

    @Override
    public void preRender(MatrixStack poseStack,
                          TreadmillBlockEntity animatable,
                          BakedGeoModel model,
                          VertexConsumerProvider bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          int color) {

        if (!TreadmillBlock.isBack(animatable.getCachedState())) {
            poseStack.scale(0f, 0f, 0f);
            return;
        }

        Direction facing = animatable.getCachedState().get(TreadmillBlock.FACING);

        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing.asRotation()));
    }
}
