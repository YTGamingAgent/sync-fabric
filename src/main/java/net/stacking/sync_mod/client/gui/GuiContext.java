package net.stacking.sync_mod.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.6 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
*///?} else if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
*///?}

/**
 * What the shell selector GUI draws with. Minecraft's GUI drawing API differs between versions
 * (GuiGraphics since 1.20, a bare PoseStack before), so the GUI code only talks to this class.
 */
@Environment(EnvType.CLIENT)
public final class GuiContext {
    //? if >=1.21.6 {
    /*private final GuiGraphics graphics;
    // The GUI is drawn in 2D since 1.21.6; our widgets still describe their geometry with a 3D pose stack.
    private final PoseStack pose = new PoseStack();

    public GuiContext(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public GuiGraphics graphics() {
        return this.graphics;
    }

    public PoseStack pose() {
        return this.pose;
    }

    public void fill(int x1, int y1, int x2, int y2, int argb) {
        this.graphics.fill(x1, y1, x2, y2, argb);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        Matrix4f m = this.pose.last().pose();
        this.graphics.pose().pushMatrix();
        this.graphics.pose().mul(new Matrix3x2f(m.m00(), m.m01(), m.m10(), m.m11(), m.m30(), m.m31()));
        this.graphics.drawString(font, text, x, y, color, shadow);
        this.graphics.pose().popMatrix();
    }

    public void renderTooltip(Font font, Component text, int x, int y) {
        this.graphics.setTooltipForNextFrame(font, text, x, y);
    }

    /^*
     * Submits untextured quads: {@code xy} holds already-transformed screen coordinates (x0, y0, x1, y1, ...),
     * four vertices per quad, with one ARGB color per vertex.
     ^/
    public void submitQuads(float[] xy, int[] colors) {
        if (colors.length == 0) {
            return;
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < colors.length; ++i) {
            minX = Math.min(minX, xy[2 * i]);
            maxX = Math.max(maxX, xy[2 * i]);
            minY = Math.min(minY, xy[2 * i + 1]);
            maxY = Math.max(maxY, xy[2 * i + 1]);
        }
        ScreenRectangle scissor = this.graphics.scissorStack.peek();
        ScreenRectangle bounds = new ScreenRectangle((int)Math.floor(minX), (int)Math.floor(minY), (int)Math.ceil(maxX - minX) + 1, (int)Math.ceil(maxY - minY) + 1);
        this.graphics.guiRenderState.submitGuiElement(new QuadsRenderState(xy, colors, scissor, scissor == null ? bounds : scissor.intersection(bounds)));
    }

    private record QuadsRenderState(float[] xy, int[] colors, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vertices/^? if <1.21.9 {^/, float z/^?}^/) {
            for (int i = 0; i < this.colors.length; ++i) {
                vertices.addVertex(this.xy[2 * i], this.xy[2 * i + 1], /^? if >=1.21.9 {^//^0^//^?} else {^/z/^?}^/).setColor(this.colors[i]);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
    *///?} else if >=1.20 {
    private final GuiGraphics graphics;

    public GuiContext(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public GuiGraphics graphics() {
        return this.graphics;
    }

    public PoseStack pose() {
        return this.graphics.pose();
    }

    public void fill(int x1, int y1, int x2, int y2, int argb) {
        this.graphics.fill(x1, y1, x2, y2, argb);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void renderTooltip(Font font, Component text, int x, int y) {
        this.graphics.renderTooltip(font, text, x, y);
    }
    //?} else {
    /*private final PoseStack pose;
    @Nullable
    private final Screen screen;

    public GuiContext(PoseStack pose, @Nullable Screen screen) {
        this.pose = pose;
        this.screen = screen;
    }

    public PoseStack pose() {
        return this.pose;
    }

    public void fill(int x1, int y1, int x2, int y2, int argb) {
        GuiComponent.fill(this.pose, x1, y1, x2, y2, argb);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(this.pose, text, x, y, color);
        } else {
            font.draw(this.pose, text, x, y, color);
        }
    }

    public void renderTooltip(Font font, Component text, int x, int y) {
        if (this.screen != null) {
            this.screen.renderTooltip(this.pose, text, x, y);
        }
    }
    *///?}
}
