package net.stacking.sync_mod.client.model;

import net.stacking.sync_mod.api.shell.ShellState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
public class ShellConstructorModel extends AbstractShellContainerModel {
    public final ModelPart floor;
    public final ModelPart floorRimF;
    public final ModelPart floorRimL;
    public final ModelPart floorRimR;
    public final ModelPart floorRimB;

    public final ModelPart ceiling;
    public final ModelPart ceilingRimF;
    public final ModelPart ceilingRimL;
    public final ModelPart ceilingRimR;
    public final ModelPart ceilingRimB;

    public final ModelPart wallLL;
    public final ModelPart wallLU;
    public final ModelPart wallRL;
    public final ModelPart wallRU;
    public final ModelPart wallBL;
    public final ModelPart wallBU;

    public final ModelPart pillarBLU;
    public final ModelPart pillarBLL;
    public final ModelPart pillarBRU;
    public final ModelPart pillarBRL;
    public final ModelPart pillarFLU;
    public final ModelPart pillarFLL;
    public final ModelPart pillarFRU;
    public final ModelPart pillarFRL;

    public final ModelPart sprayerR;
    public final ModelPart sprayerG;
    public final ModelPart sprayerB;
    public final ModelPart sprayerRMast;
    public final ModelPart sprayerGMast;
    public final ModelPart sprayerBMast;

    public final ModelPart printerL;
    public final ModelPart printerR;

    private final ModelPart doorLL;
    private final ModelPart doorLU;
    private final ModelPart doorRL;
    private final ModelPart doorRU;

    public float buildProgress;
    public boolean showInnerParts;
    public float sprayerProgress;

    public ShellConstructorModel() {
        super(256, 256);

        ModelPart rightPart = this.createRotationTemplate(0, 0.7853982F, 0);
        ModelPart leftPart = this.createRotationTemplate(0, -0.7853982F, 0);

        this.floor = this.addCuboid(BOTTOM, 64, 62, -16, 23, -16, 32, 1, 32);
        this.floorRimF = this.addCuboid(BOTTOM, 70, 34, -13, 22, -14, 26, 1, 3);
        this.floorRimL = this.addCuboid(BOTTOM, 0, 38, 13, 22, -14, 3, 1, 27);
        this.floorRimR = this.addCuboid(BOTTOM, 0, 38, -16, 22, -14, 3, 1, 27);
        this.floorRimB = this.addCuboid(BOTTOM, 0, 34, -16, 22, 13, 32, 1, 3);

        this.ceiling = this.addCuboid(TOP, 0, 0, -16, -8, -16, 32, 2, 32);
        this.ceilingRimF = this.addCuboid(TOP, 70, 34, -13, -6, -14, 26, 1, 3);
        this.ceilingRimL = this.addCuboid(TOP, 0, 38, 13, -6, -14, 3, 1, 27);
        this.ceilingRimR = this.addCuboid(TOP, 0, 38, -16, -6, -14, 3, 1, 27);
        this.ceilingRimB = this.addCuboid(TOP, 0, 34, -16, -6, 13, 32, 1, 3);

        this.wallLL = this.addCuboid(BOTTOM, 200, 122, 15.05F, -8, -14, 1, 30, 27);
        this.wallLU = this.addCuboid(TOP, 200, 66, 15.05F, -5, -14, 1, 29, 27);

        this.wallRL = this.addCuboid(BOTTOM, 200, 122, -16.05F, -8, -14, 1, 30, 27);
        this.wallRU = this.addCuboid(TOP, 200, 66, -16.05F, -5, -14, 1, 29, 27);

        this.wallBL = this.addCuboid(BOTTOM, 101, 188, -13, -8, 14, 26, 30, 1);
        this.wallBU = this.addCuboid(TOP, 101, 156, -13, -5, 14, 26, 29, 1);

        this.pillarFLL = this.addCuboid(BOTTOM, 60, 68, -16, -8, -15, 1, 31, 1);
        this.pillarFLU = this.addCuboid(TOP, 60, 38, -16, -6, -15, 1, 30, 1);

        this.pillarFRL = this.addCuboid(BOTTOM, 60, 68, 15, -8, -15, 1, 31, 1);
        this.pillarFRU = this.addCuboid(TOP, 60, 38, 15, -6, -15, 1, 30, 1);

        this.pillarBLL = this.addCuboid(BOTTOM, 0, 155, -16, -8, 13, 3, 30, 3);
        this.pillarBLU = this.addCuboid(TOP, 0, 126, -16, -5, 13, 3, 29, 3);

        this.pillarBRL = this.addCuboid(BOTTOM, 0, 155, 13, -8, 13, 3, 30, 3);
        this.pillarBRU = this.addCuboid(TOP, 0, 126, 13, -5, 13, 3, 29, 3);

        this.sprayerRMast = this.createCuboid(150, 0, 7.5F, -6, 4, 1, 24, 1, leftPart);
        this.sprayerGMast = this.createCuboid(150, 0, 0, -6, 7.5F, 1, 24, 1);
        this.sprayerBMast = this.createCuboid(150, 0, -8.5F, -6, 4, 1, 24, 1, rightPart);

        this.sprayerR = this.createCuboid(150, 31, 7.5F, 5, 4, 1, 3, 1, leftPart);
        this.sprayerG = this.createCuboid(150, 31, 0, 5, 7.5F, 1, 3, 1);
        this.sprayerB = this.createCuboid(150, 31, -8.5F, 5, 4, 1, 3, 1, rightPart);

        this.printerL = this.createCuboid(150, 25, 11, -6, -4, 1, 5, 2, leftPart);
        this.printerR = this.createCuboid(150, 25, -12, -6, -4, 1, 5, 2, rightPart);

        this.doorLL = this.createCuboid(150, 0, 0, -6, 7.5F, 1, 24, 1);
        this.doorLU = this.createCuboid(150, 0, 0, -6, 7.5F, 1, 24, 1);
        this.doorRL = this.createCuboid(150, 0, 0, -6, 7.5F, 1, 24, 1);
        this.doorRU = this.createCuboid(150, 0, 0, -6, 7.5F, 1, 24, 1);
    }

    @Override
    protected void renderDoors(DoubleBlockProperties.Type type, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        float angle = (float)(Math.PI / 2) * this.doorOpenProgress;
        if (type == BOTTOM) {
            this.doorLL.yaw = -angle;
            this.doorRL.yaw =  angle;
            this.doorLL.render(matrices, vertices, light, overlay, color);
            this.doorRL.render(matrices, vertices, light, overlay, color);
        } else {
            this.doorLU.yaw = -angle;
            this.doorRU.yaw =  angle;
            this.doorLU.render(matrices, vertices, light, overlay, color);
            this.doorRU.render(matrices, vertices, light, overlay, color);
        }
    }

    @Override
    public void render(DoubleBlockProperties.Type type, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        super.render(type, matrices, vertices, light, overlay, color);

        if (type != TOP || !this.showInnerParts) {
            return;
        }

        // Sprayers descend during construction
        float sprayerY = -this.sprayerProgress * 22F / 16F;
        matrices.push();
        matrices.translate(0, sprayerY, 0);

        this.sprayerRMast.render(matrices, vertices, light, overlay, color);
        this.sprayerGMast.render(matrices, vertices, light, overlay, color);
        this.sprayerBMast.render(matrices, vertices, light, overlay, color);

        if (this.buildProgress >= ShellState.PROGRESS_PRINTING) {
            float coloredProgress = (this.buildProgress - ShellState.PROGRESS_PRINTING) / ShellState.PROGRESS_PAINTING;
            if (coloredProgress < 1F / 3F) {
                this.sprayerR.render(matrices, vertices, light, overlay, 0xFFFF0000);
            } else if (coloredProgress < 2F / 3F) {
                this.sprayerG.render(matrices, vertices, light, overlay, 0xFF00FF00);
            } else if (coloredProgress <= 1F) {
                this.sprayerB.render(matrices, vertices, light, overlay, 0xFF0000FF);
            }
        }

        matrices.pop();

        if (this.buildProgress < ShellState.PROGRESS_PRINTING) {
            this.printerL.render(matrices, vertices, light, overlay, color);
            this.printerR.render(matrices, vertices, light, overlay, color);
        }
    }
}