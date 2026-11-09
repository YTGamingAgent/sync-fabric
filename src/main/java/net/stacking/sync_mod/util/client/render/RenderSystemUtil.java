package net.stacking.sync_mod.util.client.render;

import net.stacking.sync_mod.util.client.render.PoseUtil;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
//? if <1.21.5
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
//? if <26.2
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.stacking.sync_mod.client.gui.GuiContext;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.*;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
//? if >=1.21.6 {
/*import java.util.ArrayList;
import java.util.List;
import org.joml.Vector4f;
*///?}
//? if >=1.19.3 {
import org.joml.Matrix4f;
//?} else
/*import com.mojang.math.Matrix4f;*/

@Environment(EnvType.CLIENT)
public final class RenderSystemUtil {
    public static final int MAX_LIGHT_LEVEL = 15728880;

    public static Font getTextRenderer() {
        return Minecraft.getInstance().font;
    }

    //? if <26.2 {
    public static MultiBufferSource.BufferSource getEntityVertexConsumerProvider() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }
    //?}

    public static void drawCenteredText(GuiContext context, Component text, float x, float y, float scale, int color, boolean shadow) {
        Font textRenderer = getTextRenderer();
        float textWidth = textRenderer.width(text) * scale;
        float textX = x - textWidth / 2;
        float textY = y - (textRenderer.lineHeight * scale) / 2;

        PoseStack matrices = context.pose();
        matrices.pushPose();
        matrices.translate(textX, textY, 0);
        matrices.scale(scale, scale, 1.0F);
        context.drawString(textRenderer, text, 0, 0, color, shadow);
        matrices.popPose();
    }

    public static void drawRectangle(GuiContext context, float x, float y, float width, float height, float borderRadius, float scale, float angle, float step, float r, float g, float b, float a) {
        PoseStack matrices = context.pose();
        matrices.pushPose();
        matrices.translate(x + width / 2, y + height / 2, 0);
        matrices.scale(scale, scale, 1.0F);
        PoseUtil.rotateZ(matrices, (float)Math.toRadians(angle));
        matrices.translate(-(width / 2), -(height / 2), 0);

        Matrix4f matrix = matrices.last().pose();
        Quads vertices = begin(context);

        vertex(vertices, matrix, 0, 0, r, g, b, a);
        vertex(vertices, matrix, 0, height, r, g, b, a);
        vertex(vertices, matrix, width, height, r, g, b, a);
        vertex(vertices, matrix, width, 0, r, g, b, a);

        draw(vertices);
        matrices.popPose();
    }

    /**
     * Draws a ring segment between {@code minorR} and {@code majorR}, from angle {@code from} to {@code to} (radians).
     */
    public static void drawAnnulusSector(GuiContext context, double cX, double cY, double majorR, double minorR, double from, double to, double step, float r, float g, float b, float a) {
        Matrix4f matrix = context.pose().last().pose();
        Quads vertices = begin(context);

        for (double angle = from; angle < to; angle += step) {
            double nextAngle = Math.min(angle + step, to);
            float cos0 = (float)Math.cos(angle), sin0 = (float)Math.sin(angle);
            float cos1 = (float)Math.cos(nextAngle), sin1 = (float)Math.sin(nextAngle);

            vertex(vertices, matrix, (float)(cX + minorR * cos0), (float)(cY + minorR * sin0), r, g, b, a);
            vertex(vertices, matrix, (float)(cX + majorR * cos0), (float)(cY + majorR * sin0), r, g, b, a);
            vertex(vertices, matrix, (float)(cX + majorR * cos1), (float)(cY + majorR * sin1), r, g, b, a);
            vertex(vertices, matrix, (float)(cX + minorR * cos1), (float)(cY + minorR * sin1), r, g, b, a);
        }

        draw(vertices);
    }

    /**
     * Untextured, colored quads being built. Before 1.21.5 they're uploaded directly, in 1.21.5 they go
     * through the vanilla GUI render layer, and since 1.21.6 they're submitted as a GUI element.
     */
    private static final class Quads {
        final GuiContext context;
        //? if >=1.21.6 {
        /*final List<Float> xy = new ArrayList<>();
        final List<Integer> colors = new ArrayList<>();
        *///?} else
        VertexConsumer vertices;

        Quads(GuiContext context) {
            this.context = context;
        }
    }

    private static Quads begin(GuiContext context) {
        Quads quads = new Quads(context);
        //? if >=1.21.6 {
        //?} else if >=1.21.5 {
        /*quads.vertices = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.gui());
        *///?} else if >=1.21 {
        quads.vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        //?} else {
        /*BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        quads.vertices = bufferBuilder;
        *///?}
        return quads;
    }

    private static void vertex(Quads quads, Matrix4f matrix, float x, float y, float r, float g, float b, float a) {
        //? if >=1.21.6 {
        /*Vector4f position = matrix.transform(new Vector4f(x, y, 0, 1));
        quads.xy.add(position.x());
        quads.xy.add(position.y());
        quads.colors.add(((int)(a * 255) & 0xFF) << 24 | ((int)(r * 255) & 0xFF) << 16 | ((int)(g * 255) & 0xFF) << 8 | ((int)(b * 255) & 0xFF));
        *///?} else if >=1.21 {
        quads.vertices.addVertex(matrix, x, y, 0).setColor(r, g, b, a);
        //?} else {
        /*quads.vertices.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
        *///?}
    }

    private static void draw(Quads quads) {
        //? if >=1.21.6 {
        /*float[] xy = new float[quads.xy.size()];
        for (int i = 0; i < xy.length; ++i) {
            xy[i] = quads.xy.get(i);
        }
        quads.context.submitQuads(xy, quads.colors.stream().mapToInt(Integer::intValue).toArray());
        *///?} else if >=1.21.5 {
        /*Minecraft.getInstance().renderBuffers().bufferSource().endBatch(RenderType.gui());
        *///?} else if >=1.21 {
        BufferUploader.drawWithShader(((BufferBuilder)quads.vertices).buildOrThrow());
        //?} else {
        /*BufferUploader.drawWithShader(((BufferBuilder)quads.vertices).end());
        *///?}
    }

    private RenderSystemUtil() { }
}