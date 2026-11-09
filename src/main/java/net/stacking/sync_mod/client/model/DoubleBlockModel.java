package net.stacking.sync_mod.client.model;

import net.stacking.sync_mod.util.client.render.ModelUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public abstract class DoubleBlockModel {
    /**
     * Opaque white in ARGB. Since 1.21, model colors are packed ARGB ints,
     * so plain {@code 0xFFFFFF} would mean "fully transparent".
     */
    public static final int WHITE = 0xFFFFFFFF;

    private final Function<ResourceLocation, RenderType> layerFactory;
    private final Map<DoubleBlockCombiner.BlockType, List<ModelPart>> parts;
    protected final int textureWidth;
    protected final int textureHeight;

    public DoubleBlockModel(int textureWidth, int textureHeight) {
        this(RenderType::entityCutout, textureWidth, textureHeight);
    }

    public DoubleBlockModel(Function<ResourceLocation, RenderType> layerFactory, int textureWidth, int textureHeight) {
        this.layerFactory = layerFactory;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.parts = new HashMap<>();
    }


    public RenderType renderType(ResourceLocation textureId) {
        return this.layerFactory.apply(textureId);
    }

    protected ModelPart createCuboid(int textureOffsetU, int textureOffsetV, float x, float y, float z, float xSize, float ySize, float zSize) {
        ModelPart.Cube cuboid = new ModelPart.Cube(textureOffsetU, textureOffsetV, 0, 0, 0, xSize, ySize, zSize, 0, 0, 0, true, this.textureWidth, this.textureHeight/*? if >=1.19.4 {*/, Arrays.stream(Direction.values()).collect(Collectors.toSet())/*?}*/);
        ModelPart part = new ModelPart(List.of(cuboid), Map.of());
        part.setPos(x, y, z);
        return part;
    }

    protected ModelPart createCuboid(int textureOffsetU, int textureOffsetV, float x, float y, float z, float xSize, float ySize, float zSize, ModelPart template) {
        ModelPart part = this.createCuboid(textureOffsetU, textureOffsetV, x, y, z, xSize, ySize, zSize);
        part.loadPose(template.storePose());
        part.setPos(x, y, z);
        return part;
    }

    protected ModelPart addCuboid(DoubleBlockCombiner.BlockType type, int textureOffsetU, int textureOffsetV, float x, float y, float z, float xSize, float ySize, float zSize) {
        ModelPart part = this.createCuboid(textureOffsetU, textureOffsetV, x, y, z, xSize, ySize, zSize);
        this.parts.computeIfAbsent(type, k -> new ArrayList<>()).add(part);
        return part;
    }

    protected ModelPart addCuboid(DoubleBlockCombiner.BlockType type, int textureOffsetU, int textureOffsetV, float x, float y, float z, float xSize, float ySize, float zSize, ModelPart template) {
        ModelPart part = this.addCuboid(type, textureOffsetU, textureOffsetV, x, y, z, xSize, ySize, zSize);
        part.loadPose(template.storePose());
        part.setPos(x, y, z);
        return part;
    }


    protected ModelPart createTemplate() {
        return new ModelPart(List.of(), Map.of());
    }

    protected ModelPart createRotationTemplate(float pitch, float yaw, float roll) {
        ModelPart template = this.createTemplate();
        template.xRot = pitch;
        template.yRot = yaw;
        template.zRot = roll;
        return template;
    }

    /**
     * Renders both halves of the model (used for item rendering, where there is no world).
     */
    public void renderModel(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        matrices.pushPose();
        this.render(DoubleBlockCombiner.BlockType.FIRST, matrices, vertices, light, overlay, color);
        this.translate(matrices);
        this.render(DoubleBlockCombiner.BlockType.SECOND, matrices, vertices, light, overlay, color);
        matrices.popPose();
    }

    public void render(DoubleBlockCombiner.BlockType type, PoseStack matrices, VertexConsumer vertices, int light, int overlay) {
        this.render(type, matrices, vertices, light, overlay, WHITE);
    }

    public void render(DoubleBlockCombiner.BlockType type, PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        List<ModelPart> currentParts = this.parts.get(type);
        if (currentParts == null) {
            return;
        }

        for (ModelPart part : currentParts) {
            ModelUtil.renderPart(part, matrices, vertices, light, overlay, color);
        }
    }

    protected abstract void translate(PoseStack matrices);
}
