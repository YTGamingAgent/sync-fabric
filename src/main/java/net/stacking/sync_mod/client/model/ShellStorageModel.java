package net.stacking.sync_mod.client.model;

import net.stacking.sync_mod.util.client.render.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;

@Environment(EnvType.CLIENT)
public class ShellStorageModel extends AbstractShellContainerModel {

    // ---- Floor & Ceiling ----
    private final ModelPart floor;
    private final ModelPart ceiling;

    // ---- Side glass walls (left = +X side, right = -X side), split at block boundary ----
    private final ModelPart wallLL;
    private final ModelPart wallLU;
    private final ModelPart wallRL;
    private final ModelPart wallRU;

    // ---- Striped back wall (opaque, BOTTOM only — full height so the stripe
    //      texture covers the entire interior back face) ----
    private final ModelPart backWall;

    // ---- Corner pillars ----
    private final ModelPart pillarFLL;
    private final ModelPart pillarFLU;
    private final ModelPart pillarFRL;
    private final ModelPart pillarFRU;
    private final ModelPart pillarBLL;
    private final ModelPart pillarBLU;
    private final ModelPart pillarBRL;
    private final ModelPart pillarBRU;

    // ---- Connector post (createCuboid = not auto-rendered; animated in render()) ----
    private final ModelPart connectorBase;
    private final ModelPart connector;

    // ---- LED indicator — right-front inner wall, near floor
    //      createCuboid = only rendered with ledColor tint ----
    private final ModelPart ledPivot;
    private final ModelPart ledLight;

    private final ModelPart doorLL;
    private final ModelPart doorLU;
    private final ModelPart doorRL;
    private final ModelPart doorRU;

    public DyeColor ledColor;
    public float connectorProgress;

    public ShellStorageModel() {
        super(256, 256);

        // ---- Floor (BOTTOM) -------------------------------------------------------
        this.floor = this.addCuboid(BOTTOM, 64, 62, -16, 23, -16, 32, 1, 32);

        // ---- Ceiling (TOP) --------------------------------------------------------
        this.ceiling = this.addCuboid(TOP, 0, 0, -16, -8, -16, 32, 2, 32);

        // ---- Side glass walls, split at block boundary ----------------------------
        this.wallLL = this.addCuboid(BOTTOM, 200, 122,  15.05F, -8, -14, 1, 30, 27);
        this.wallLU = this.addCuboid(TOP,    200,  66,  15.05F, -5, -14, 1, 29, 27);
        this.wallRL = this.addCuboid(BOTTOM, 200, 122, -16.05F, -8, -14, 1, 30, 27);
        this.wallRU = this.addCuboid(TOP,    200,  66, -16.05F, -5, -14, 1, 29, 27);

        // ---- Back wall with black/white stripes (BOTTOM, spans full 2-block height) --
        // The FRONT face (facing the open doorway) carries the stripe UV at (102,189).
        // Height=62 covers both lower and upper halves from the BOTTOM origin:
        //   y=-8 to y-62  in model space  →  world_y ≈ 0.0 to 2.0 (full 2 blocks)
        this.backWall = this.addCuboid(BOTTOM, 101, 188, -13, -8, 14, 26, 62, 1);

        // ---- Corner pillars -------------------------------------------------------
        this.pillarFLL = this.addCuboid(BOTTOM, 60,  68, -16, -8, -15,  1, 31,  1);
        this.pillarFLU = this.addCuboid(TOP,    60,  38, -16, -6, -15,  1, 30,  1);
        this.pillarFRL = this.addCuboid(BOTTOM, 60,  68,  15, -8, -15,  1, 31,  1);
        this.pillarFRU = this.addCuboid(TOP,    60,  38,  15, -6, -15,  1, 30,  1);
        this.pillarBLL = this.addCuboid(BOTTOM,  0, 155, -16, -8,  13,  3, 31,  3);
        this.pillarBLU = this.addCuboid(TOP,     0, 126, -16, -6,  13,  3, 30,  3);
        this.pillarBRL = this.addCuboid(BOTTOM,  0, 155,  13, -8,  13,  3, 31,  3);
        this.pillarBRU = this.addCuboid(TOP,     0, 126,  13, -6,  13,  3, 30,  3);

        // ---- Connector post -------------------------------------------------------
        this.connectorBase = this.addCuboid(BOTTOM, 0, 50, -4, 16, -4, 8, 8, 8);
        this.connector     = this.createCuboid(      0, 60, -3,  8, -3, 6, 16, 6);

        // ---- LED indicator -------------------------------------------------------
        // Positioned on the right-front inner face (x=13, near front z=-13, y=18 ≈
        // world 0.19 above the floor).  Protrudes 1 unit inward so it's visible.
        // W=2, H=4, D=1  →  UV(0,70) front face at (1,71) 2×4.
        this.ledPivot = this.createRotationTemplate(0, 0, 0);
        this.ledLight = this.createCuboid(0, 70, 13, 18, -13, 2, 4, 1);
        this.doorLL = this.createCuboid(140, 0, 15F, -8F, -15F, -15F, 0F, 0F, 15, 30, 1);
        this.doorLU = this.createCuboid(140, 0, 15F, -5F, -15F, -15F, 0F, 0F, 15, 29, 1);
        this.doorRL = this.createCuboid(140, 0, -15F, -8F, -15F, 0F, 0F, 0F, 15, 30, 1);
        this.doorRU = this.createCuboid(140, 0, -15F, -5F, -15F, 0F, 0F, 0F, 15, 29, 1);
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
    public void render(DoubleBlockProperties.Type type, MatrixStack matrices,
                       VertexConsumer vertices, int light, int overlay, int color) {
        super.render(type, matrices, vertices, light, overlay, color);

        // Animated connector post.
        if (type == BOTTOM) {
            matrices.push();
            matrices.translate(0, this.connectorProgress * 8F / 16F, 0);
            this.connector.render(matrices, vertices, light, overlay, color);
            matrices.pop();
        }

        // LED — rendered only with the indicator colour (green/blue/red).
        if (type == BOTTOM && this.ledColor != null) {
            matrices.push();
            this.ledPivot.rotate(matrices);
            this.ledLight.render(matrices, vertices, light, overlay,
                    ColorUtil.fromDyeColor(this.ledColor));
            matrices.pop();
        }
    }
}