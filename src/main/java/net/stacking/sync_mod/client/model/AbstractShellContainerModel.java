package net.stacking.sync_mod.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
public abstract class AbstractShellContainerModel extends DoubleBlockModel {
    protected static final DoubleBlockProperties.Type BOTTOM = DoubleBlockProperties.Type.FIRST;
    protected static final DoubleBlockProperties.Type TOP    = DoubleBlockProperties.Type.SECOND;

    public float doorOpenProgress;

    public AbstractShellContainerModel(int textureWidth, int textureHeight) {
        // getEntityTranslucent renders the semi-transparent glass walls at their
        // true alpha value (e.g. 30% opacity = clearly glassy).
        // getEntityCutoutNoCull was discarding those pixels entirely (cutoff at 50%),
        // which is why the glass appeared nearly invisible.
        // Now that the renderer is AbstractShellContainerBlockEntityRenderer (not
        // GeoBlockRenderer), translucent rendering works correctly.
        super(RenderLayer::getEntityTranslucent, textureWidth, textureHeight);
    }

    @Override
    protected void translate(MatrixStack matrices) {
        // The renderer applies scale(-0.5, -0.5, 0.5), so a Y translate of -2
        // in model space produces a world-space offset of (-0.5 * -2) = +1.0 block,
        // which is exactly the gap between the lower and upper block.
        matrices.translate(0, -2, 0);
    }

    @Override
    public void render(DoubleBlockProperties.Type type, MatrixStack matrices,
                       VertexConsumer vertices, int light, int overlay, int color) {
        if (this.doorOpenProgress > 0) {
            this.renderDoors(type, matrices, vertices, light, overlay, color);
        }
        super.render(type, matrices, vertices, light, overlay, color);
    }

    protected abstract void renderDoors(DoubleBlockProperties.Type type,
                                        MatrixStack matrices,
                                        VertexConsumer vertices,
                                        int light, int overlay, int color);
}