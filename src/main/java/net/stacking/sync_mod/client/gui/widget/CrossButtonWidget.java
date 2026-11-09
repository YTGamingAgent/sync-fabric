package net.stacking.sync_mod.client.gui.widget;

import net.stacking.sync_mod.util.client.render.ColorUtil;
import net.stacking.sync_mod.util.client.render.RenderSystemUtil;
import net.stacking.sync_mod.util.math.QuarticFunction;
import net.stacking.sync_mod.util.math.Radians;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.stacking.sync_mod.client.gui.GuiContext;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

@Environment(EnvType.CLIENT)
public class CrossButtonWidget extends AbstractButtonWidget {
    private static final Component DEFAULT_DESCRIPTION = Component.translatable("gui.sync.default.cross_button.title");
    private static final int DEFAULT_COLOR = ColorUtil.fromDyeColor(DyeColor.WHITE);
    private static final float DEFAULT_STEP = Radians.R_PI_32;

    private final float step;
    private final float[] color;
    private final Component description;
    private final float x0;
    private final float y0;
    private final float x1;
    private final float y1;
    private final float borderRadius;
    private final float stickWidth;
    private final float stickHeight;
    private final float angle;

    public CrossButtonWidget(float x, float y, float width, float height, float thickness, Runnable onClick) {
        this(x, y, width, height, thickness, DEFAULT_STEP, DEFAULT_COLOR, DEFAULT_DESCRIPTION, onClick);
    }

    public CrossButtonWidget(float x, float y, float width, float height, float thickness, int color, Runnable onClick) {
        this(x, y, width, height, thickness, DEFAULT_STEP, color, DEFAULT_DESCRIPTION, onClick);
    }

    public CrossButtonWidget(float x, float y, float width, float height, float thickness, Component description, Runnable onClick) {
        this(x, y, width, height, thickness, DEFAULT_STEP, DEFAULT_COLOR, description, onClick);
    }

    public CrossButtonWidget(float x, float y, float width, float height, float thickness, int color, Component description, Runnable onClick) {
        this(x, y, width, height, thickness, DEFAULT_STEP, color, description, onClick);
    }

    public CrossButtonWidget(float x, float y, float width, float height, float thickness, float step, int color, Component description, Runnable onClick) {
        super(x, y, width, height, onClick);
        this.step = step;
        this.color = ColorUtil.toRGBA(color);
        this.description = description;

        float shiftY = (float)new QuarticFunction(4, -4 * height, height * height + width * width - 4 * thickness * thickness, 2 * height * thickness * thickness, thickness * thickness * (thickness * thickness - width * width)).getRoot(1);
        float shiftX = (float)Math.sqrt(thickness * thickness - shiftY * shiftY);
        this.angle = (float)Math.acos(shiftY / thickness);
        this.x0 = x;
        this.y0 = y + height - shiftY;
        this.x1 = x + shiftX;
        this.y1 = y;
        this.borderRadius = thickness * 0.5F;
        this.stickHeight = thickness;
        this.stickWidth = Mth.sqrt(Mth.square(width - shiftX) + Mth.square(height - shiftY));
    }

    @Override
    protected void renderContent(GuiContext drawContext, int mouseX, int mouseY, float delta) {
        RenderSystemUtil.drawRectangle(drawContext, this.x0, this.y0, this.stickWidth, this.stickHeight, this.borderRadius, 1, -this.angle, this.step, this.color[0], this.color[1], this.color[2], this.color[3]);
        RenderSystemUtil.drawRectangle(drawContext, this.x1, this.y1, this.stickWidth, this.stickHeight, this.borderRadius, 1, this.angle, this.step, this.color[0], this.color[1], this.color[2], this.color[3]);
    }

    @Override
    protected Component getWidgetDescription() {
        return this.description;
    }

    private boolean focused;
    //? if >=1.19.4
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    //? if >=1.19.4
    @Override
    public boolean isFocused() {
        return focused;
    }
}