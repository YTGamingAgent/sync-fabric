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

    @Override
    public void preRender(MatrixStack poseStack, TreadmillBlockEntity animatable,
                          BakedGeoModel model, VertexConsumerProvider bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, int color) {

        // Scale to 0 on the FRONT block — only BACK renders the full model.
        if (!TreadmillBlock.isBack(animatable.getCachedState())) {
            poseStack.scale(0f, 0f, 0f);
            return;
        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, color);

        Direction facing = animatable.getCachedState().get(TreadmillBlock.FACING);

        // Treadmill geo model spans Z: -8..+24 Blockbench units (= -0.5..+1.5 blocks).
        // The model origin [0,0,0] maps to the BACK block's centre in Minecraft.
        // Correct transform: shift to block centre then rotate — no un-translate needed.
        float yRot = switch (facing) {
            case NORTH -> 180f;
            case SOUTH ->   0f;
            case WEST  ->  90f;
            case EAST  -> 270f;
            default    ->   0f;
        };

        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRot));
        // NO translate(-0.5, 0, -0.5) — that was the source of the offset bug.
    }
}
