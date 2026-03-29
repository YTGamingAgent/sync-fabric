package net.stacking.sync_mod.client.render.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Box;
import net.stacking.sync_mod.block.TreadmillBlock;
import net.stacking.sync_mod.block.entity.TreadmillBlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TreadmillBlockEntityRenderer extends GeoBlockRenderer<TreadmillBlockEntity> {

    public TreadmillBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(new GeoModel<TreadmillBlockEntity>() {
            @Override
            public Identifier getModelResource(TreadmillBlockEntity a) {
                return Identifier.of("sync", "geo/treadmill.geo.json");
            }

            @Override
            public Identifier getTextureResource(TreadmillBlockEntity a) {
                return Identifier.of("sync", "textures/block/treadmill.png");
            }

            @Override
            public Identifier getAnimationResource(TreadmillBlockEntity a) {
                return null;
            }
        });
    }

    @Override
    public RenderLayer getRenderType(TreadmillBlockEntity animatable, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityCutoutNoCull(texture);
    }

    // Suppress GeckoLib's built-in block rotation — we handle it ourselves below.
    @Override
    protected void rotateBlock(Direction facing, MatrixStack poseStack) {
    }

    // Always render regardless of frustum culling — treadmills disappear
    // when the camera is inside the constructor otherwise.
    @Override
    public boolean rendersOutsideBoundingBox(TreadmillBlockEntity blockEntity) {
        return true;
    }

    @Override
    public void preRender(MatrixStack poseStack,
                          TreadmillBlockEntity animatable,
                          BakedGeoModel model, VertexConsumerProvider bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, int color) {

        BlockState state = animatable.getCachedState();

        // FRONT block renders nothing — the BACK block draws the full 2-block model.
        if (!TreadmillBlock.isBack(state)) {
            poseStack.scale(0f, 0f, 0f);
            return;
        }

        // Item / inventory rendering — no world context, just center it.
        if (!animatable.hasWorld()) {
            poseStack.translate(0.5, 0.0, 0.5);
            return;
        }

        Direction facing = state.get(TreadmillBlock.FACING);
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
    }

    @Override
    public float getMotionAnimThreshold(TreadmillBlockEntity animatable) {
        return -1f;  // Negative value disables motion-based culling
    }
}