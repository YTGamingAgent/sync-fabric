package net.stacking.sync_mod.client.gui.widget;

import net.stacking.sync_mod.util.ChatUtil;

import net.stacking.sync_mod.util.client.render.PoseUtil;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
//? if <1.21.2
import net.stacking.sync_mod.client.gl.MSAAFramebuffer;
import net.stacking.sync_mod.util.client.render.ColorUtil;
import net.stacking.sync_mod.util.client.render.RenderSystemUtil;
import net.stacking.sync_mod.api.shell.ClientShell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.entity.ShellEntity;
import net.stacking.sync_mod.client.render.entity.ShellEntityRenderer;
import net.stacking.sync_mod.util.math.Radians;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.stacking.sync_mod.client.gui.GuiContext;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
//? if >=1.19.3 {
import org.joml.Vector3f;
//?} else
/*import com.mojang.math.Vector3f;*/

import java.util.function.BiPredicate;

@Environment(EnvType.CLIENT)
public class ShellSelectorButtonWidget extends AbstractWidget {
    private static final DyeColor DEFAULT_COLOR = DyeColor.BLACK;
    private static final float DEFAULT_ALPHA = 0.6F;
    private static final float DEFAULT_HOVERED_ALPHA = 0.8F;
    private static final float DEFAULT_PRESSED_ALPHA = 1F;
    private static final double DEFAULT_STEP = Radians.R_PI_128;

    private final double cX;
    private final double cY;
    private final double majorR;
    private final double minorR;
    private final double diffR;
    private final double minorRBorder;
    private final double from;
    private final double to;
    private final double step;
    private final DyeColor color;
    private final float alpha;
    private final float hoveredAlpha;
    private final float pressedAlpha;
    private final BiPredicate<Double, Double> belongsToSectorPredicate;

    public ShellState shell;

    public ShellSelectorButtonWidget(double cX, double cY, double majorR, double minorR, double borderWidth, double from, double to) {
        this(cX, cY, majorR, minorR, borderWidth, from, to, DEFAULT_STEP, DEFAULT_COLOR, DEFAULT_ALPHA, DEFAULT_HOVERED_ALPHA, DEFAULT_PRESSED_ALPHA);
    }

    public ShellSelectorButtonWidget(double cX, double cY, double majorR, double minorR, double borderWidth, double from, double to, DyeColor color) {
        this(cX, cY, majorR, minorR, borderWidth, from, to, DEFAULT_STEP, color, DEFAULT_ALPHA, DEFAULT_HOVERED_ALPHA, DEFAULT_PRESSED_ALPHA);
    }

    public ShellSelectorButtonWidget(double cX, double cY, double majorR, double minorR, double borderWidth, double from, double to, DyeColor color, float alpha, float hoveredAlpha, float pressedAlpha) {
        this(cX, cY, majorR, minorR, borderWidth, from, to, DEFAULT_STEP, color, alpha, hoveredAlpha, pressedAlpha);
    }

    public ShellSelectorButtonWidget(double cX, double cY, double majorR, double minorR, double borderWidth, double from, double to, double step, DyeColor color, float alpha, float hoveredAlpha, float pressedAlpha) {
        boolean isFullCircle = Math.abs(Radians.R_2_PI - to + from) < step;
        if (isFullCircle) {
            double tmp = from;
            from = -to;
            to = -tmp;
        }

        this.cX = cX;
        this.cY = cY;
        this.majorR = majorR;
        this.minorR = minorR;
        this.diffR = majorR - minorR;
        this.minorRBorder = majorR - borderWidth;
        this.from = from;
        this.to = to;
        this.step = step;
        this.color = color;
        this.alpha = alpha;
        this.hoveredAlpha = hoveredAlpha;
        this.pressedAlpha = pressedAlpha;

        double x0 = majorR * Math.cos(from) + cX;
        double y0 = majorR * Math.sin(from) + cY;
        double x1 = majorR * Math.cos(to) + cX;
        double y1 = majorR * Math.sin(to) + cY;

        double a0 = (y0 - cY) / (x0 - cX);
        double b0 = -a0 * cX + cY;
        double a1 = (y1 - cY) / (x1 - cX);
        double b1 = -a1 * cX + cY;

        if (isFullCircle) {
            this.belongsToSectorPredicate = (x, y) -> true;
        } else if (from < Radians.R_PI_2 && to > Radians.R_PI_2) {
            this.belongsToSectorPredicate = (x, y) -> liesOnOrAboveLine(x, y, a0, b0) && liesOnOrAboveLine(x, y, a1, b1);
        } else if (from < Radians.R_3_PI_2 && to > Radians.R_3_PI_2) {
            this.belongsToSectorPredicate = (x, y) -> liesOnOrBelowLine(x, y, a0, b0) && liesOnOrBelowLine(x, y, a1, b1);
        } else if (y1 > y0) {
            this.belongsToSectorPredicate = (x, y) -> liesOnOrAboveLine(x, y, a0, b0) && liesOnOrBelowLine(x, y, a1, b1);
        } else {
            this.belongsToSectorPredicate = (x, y) -> liesOnOrBelowLine(x, y, a0, b0) && liesOnOrAboveLine(x, y, a1, b1);
        }
    }


    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.shell != null && liesOnCircle(mouseX, mouseY, this.cX, this.cY, this.majorR) && !liesOnCircle(mouseX, mouseY, this.cX, this.cY, this.minorR) && this.belongsToSectorPredicate.test(mouseX, mouseY);
    }

    //? if >=1.19.4
    @Override
    public void setFocused(boolean focused) {

    }

    //? if >=1.19.4
    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    protected void renderContent(GuiContext drawContext, int mouseX, int mouseY, float delta) {
        this.renderSector(drawContext);

        if (this.shell != null) {
            if (this.shell.getProgress() < ShellState.PROGRESS_PRINTING) {
                this.renderShellAndProgress(drawContext);
            } else {
                //? if >=1.21.2 {
                /*this.renderShellAndProgress(drawContext);
                *///?} else
                MSAAFramebuffer.renderAfterUsage(() -> this.renderShellAndProgress(drawContext));
            }
        }
    }

    @Override
    protected void onMouseClick(double mouseX, double mouseY, int button) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || this.shell == null || this.shell.getProgress() < ShellState.PROGRESS_DONE) {
            return;
        }

        PlayerSyncEvents.SyncFailureReason failureReason = ((ClientShell) client.player).beginSync(this.shell);
        if (failureReason != null) {
            if (client.screen != null) {
                client.screen.onClose();
            }
            ChatUtil.message(client.player, failureReason.toText(), true);
        }
    }

    private void renderSector(GuiContext drawContext) {
        float alpha = this.isPressed() ? this.pressedAlpha : this.isHovered() ? this.hoveredAlpha : this.alpha;
        DyeColor borderColor = this.shell == null ? null : this.shell.getColor();
        DyeColor color = borderColor != null && (this.isHovered() || this.isPressed()) ? borderColor : this.color;
        float[] rgb = net.stacking.sync_mod.util.client.render.ColorUtil.getColorComponents(color);
        RenderSystemUtil.drawAnnulusSector(drawContext, this.cX, this.cY, this.majorR, this.minorR, this.from, this.to, this.step, rgb[0], rgb[1], rgb[2], alpha);

        if (borderColor != null) {
            rgb = net.stacking.sync_mod.util.client.render.ColorUtil.getColorComponents(borderColor);
            RenderSystemUtil.drawAnnulusSector(drawContext, this.cX, this.cY, this.majorR, this.minorRBorder, this.from, this.to, this.step, rgb[0], rgb[1], rgb[2], 1F);
        }
    }

    private void renderShellAndProgress(GuiContext drawContext) {
        this.renderShell(drawContext);
        if (this.shell.getProgress() < ShellState.PROGRESS_DONE) {
            this.renderProgress(drawContext);
        }
    }

    private void renderShell(GuiContext drawContext) {
        final double SHELL_WIDTH_HALF = 0.26F;
        final double SHELL_HEIGHT_HALF = 0.35F;
        final float SHELL_SCALE = 0.365F;

        ShellEntity shellEntity = this.shell.asEntity();
        shellEntity.pitchProgress = 0;
        shellEntity.isActive = this.shell.getProgress() >= ShellState.PROGRESS_DONE;

        double r = this.minorR + this.diffR / 2;
        double tAngle = (this.to + this.from) / 2;
        double shellCX = r * Math.cos(tAngle) + this.cX;
        double shellCY = r * Math.sin(tAngle) + this.cY;
        double tX = shellCX - this.diffR * SHELL_WIDTH_HALF;
        double tY = shellCY + this.diffR * SHELL_HEIGHT_HALF;
        float scale = (float)this.diffR * SHELL_SCALE;

        //? if >=1.21.6 {
        /*// Since 1.21.6 entities in GUIs are drawn by a picture-in-picture renderer into a box.
        int boxSize = (int)this.diffR;
        int x0 = (int)(shellCX - boxSize / 2.0), y0 = (int)(shellCY - boxSize / 2.0);
        ShellEntityRenderer.submitShellToGui(drawContext.graphics(), shellEntity, x0, y0, x0 + boxSize, y0 + boxSize, scale);
        *///?} else {
        PoseStack matrices = drawContext.pose();

        matrices.pushPose();
        matrices.translate(tX, tY, this.majorR);
        matrices.scale(scale, -scale, 1F);
        PoseUtil.rotateXDegrees(matrices, 15);
        PoseUtil.rotateYDegrees(matrices, 40);

        RenderSystem.setupGui3DDiffuseLighting(new Vector3f((float)this.cX * 2, (float)this.cY * 2, -1), new Vector3f(0, 0, 1));
        MultiBufferSource.BufferSource immediate = RenderSystemUtil.getEntityVertexConsumerProvider();
        ShellEntityRenderer.renderShell(shellEntity, 0, 0, matrices, immediate, RenderSystemUtil.MAX_LIGHT_LEVEL);
        immediate.endBatch();

        matrices.popPose();
        //?}
    }

    private void renderProgress(GuiContext drawContext) {
        final float FONT_SCALE = 0.15F;
        final float BOX_SCALE = 1.6F;

        Font textRenderer = RenderSystemUtil.getTextRenderer();
        double r = this.minorR + this.diffR / 2;
        double tAngle = (this.to + this.from) / 2;
        float shellCX = (float)(r * Math.cos(tAngle) + this.cX);
        float shellCY = (float)(r * Math.sin(tAngle) + this.cY);
        float fontHeight = (float)this.diffR * FONT_SCALE;
        float fontScale = fontHeight / textRenderer.lineHeight;

        int progress = (int)Math.floor(this.shell.getProgress() * 100);
        Component progressText = Component.nullToEmpty(String.format("%s%%", progress));
        float boxHeight = fontHeight * BOX_SCALE;
        float progressTextWidth = textRenderer.width(progressText) * fontScale;
        float progressBoxWidth = Math.max(boxHeight * 2F, progressTextWidth);
        float boxTop = shellCY - boxHeight / 2 - textRenderer.lineHeight * fontScale * 0.125F;
        float boxLeft = shellCX - progressBoxWidth / 2F;

        PoseStack matrices = drawContext.pose();

        matrices.pushPose();
        matrices.translate(0, 0, this.majorR * 2);
        RenderSystemUtil.drawRectangle(drawContext, boxLeft, boxTop, progressBoxWidth, boxHeight, boxHeight * 0.25F, 1F, 0, (float)this.step, 0F, 0F, 0F, 0.8F);
        RenderSystemUtil.drawCenteredText(drawContext, progressText, shellCX, shellCY, fontScale, ColorUtil.fromDyeColor(DyeColor.WHITE), true);
        matrices.popPose();
    }

    @Override
    protected Component getWidgetDescription() {
        if (this.shell == null) {
            return null;
        }

        BlockPos pos = this.shell.getPos();
        return Component.nullToEmpty(String.format("%s, %s, %s", pos.getX(), pos.getY(), pos.getZ()));
    }

    private static boolean liesOnCircle(double x, double y, double cX, double cY, double r) {
        return Mth.square(cX - x) + Mth.square(cY - y) <= r * r;
    }

    private static boolean liesOnOrAboveLine(double x, double y, double a, double b) {
        return y >= a * x + b;
    }

    private static boolean liesOnOrBelowLine(double x, double y, double a, double b) {
        return y <= a * x + b;
    }
}
