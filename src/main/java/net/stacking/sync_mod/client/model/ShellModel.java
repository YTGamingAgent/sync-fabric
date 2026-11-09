package net.stacking.sync_mod.client.model;

import net.stacking.sync_mod.util.client.render.ModelUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.stacking.sync_mod.api.shell.ShellState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ShellModel {
    public final Model parentModel;
    private float buildProgress;
    private final VoxelModel voxelModel;

    public ShellModel(Model parentModel) {
        this(parentModel, new Random());
    }

    public ShellModel(Model parentModel, Random random) {
        this.parentModel = parentModel;
        this.buildProgress = 0;
        this.voxelModel = VoxelModel.fromModel(parentModel, random);
    }

    public void setBuildProgress(float buildProgress) {
        this.buildProgress = buildProgress;
        this.voxelModel.completeness = buildProgress / ShellState.PROGRESS_PRINTING;
    }

    public void setDestructionProgress(float destructionProgress) {
        this.voxelModel.destructionProgress = destructionProgress;
    }

    public RenderType renderType(ResourceLocation textureId) {
        if (this.isBeingPrinted() || this.isBeingDestroyed()) {
            return this.voxelModel.renderType();
        }

        // While being painted, the skin is drawn normally and CustomRenderLayer.getPaintingMask
        // is drawn over it (see ShellEntityRenderer).
        return RenderType.entityTranslucent(textureId);
    }

    public void renderModel(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        if (this.isBeingPrinted() || this.isBeingDestroyed()) {
            this.voxelModel.renderModel(matrices, vertices, light, overlay, color);
        } else {
            ModelUtil.renderModel(this.parentModel, matrices, vertices, light, overlay, color);
        }
    }

    private boolean isBeingPrinted() {
        return this.buildProgress >= ShellState.PROGRESS_START && this.buildProgress < ShellState.PROGRESS_PRINTING;
    }

    /**
     * @return 0 (nothing painted) to 1 (fully painted) while the shell is being painted.
     */
    public float getPaintingProgress() {
        return (this.buildProgress - ShellState.PROGRESS_PRINTING) / ShellState.PROGRESS_PAINTING;
    }

    public boolean isBeingPainted() {
        return this.buildProgress >= ShellState.PROGRESS_PRINTING && this.buildProgress < ShellState.PROGRESS_DONE;
    }

    private boolean isBeingDestroyed() {
        return this.voxelModel.destructionProgress > 0F;
    }
}