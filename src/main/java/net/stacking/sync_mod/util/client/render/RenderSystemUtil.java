package net.stacking.sync_mod.util.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class RenderSystemUtil {
    public static final int MAX_LIGHT_LEVEL = 15728880;

    public static TextRenderer getTextRenderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static VertexConsumerProvider.Immediate getEntityVertexConsumerProvider() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }

    public static void drawCenteredText(DrawContext context, Text text, float x, float y, float scale, int color, boolean shadow) {
        TextRenderer textRenderer = getTextRenderer();
        float textWidth = textRenderer.getWidth(text) * scale;
        float textX = x - textWidth / 2;
        float textY = y - (textRenderer.fontHeight * scale) / 2;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(textX, textY, 0);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(textRenderer, text, 0, 0, color, shadow);
        matrices.pop();
    }

    public static void drawRectangle(MatrixStack matrices, float x, float y, float width, float height, float borderRadius, float scale, float angle, float step, float r, float g, float b, float a) {
        matrices.push();
        matrices.translate(x + width / 2, y + height / 2, 0);
        matrices.scale(scale, scale, 1.0F);
        matrices.multiply(new Quaternionf().rotationZ((float)Math.toRadians(angle)));
        matrices.translate(-(width / 2), -(height / 2), 0);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        bufferBuilder.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, 0, height, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, width, height, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, width, 0, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        matrices.pop();
    }

    public static void drawAnnulusSector(MatrixStack matrices, double cX, double cY, double majorR, double minorR, double from, double to, double step, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (double angle = from; angle <= to; angle += step) {
            float cosAngle = (float) Math.cos(angle);
            float sinAngle = (float) Math.sin(angle);

            float x1 = (float)(cX + minorR * cosAngle);
            float y1 = (float)(cY + minorR * sinAngle);
            float x2 = (float)(cX + majorR * cosAngle);
            float y2 = (float)(cY + majorR * sinAngle);

            bufferBuilder.vertex(matrix, x1, y1, 0).color(r, g, b, a);
            bufferBuilder.vertex(matrix, x2, y2, 0).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private RenderSystemUtil() { }
}