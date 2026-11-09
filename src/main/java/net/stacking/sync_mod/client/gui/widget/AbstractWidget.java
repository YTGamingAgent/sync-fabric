package net.stacking.sync_mod.client.gui.widget;

import net.stacking.sync_mod.client.gui.TooltipProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else
/*import com.mojang.blaze3d.vertex.PoseStack;*/
import net.stacking.sync_mod.client.gui.GuiContext;
//? if >=1.19.3 {
import net.minecraft.client.gui.components.Renderable;
//?} else
/*import net.minecraft.client.gui.components.Widget;*/
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import java.util.Date;

@Environment(EnvType.CLIENT)
public abstract class AbstractWidget implements /*? if >=1.19.3 {*/Renderable/*?} else {*//*Widget*//*?}*/, NarratableEntry, TooltipProvider, GuiEventListener {
    public boolean visible = true;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private double lastMouseX = -1;
    private double lastMouseY = -1;
    private long lastMovementTime = 0;

    public boolean isHovered() {
        return this.isHovered;
    }

    public boolean isPressed() {
        return this.isPressed;
    }

    @Override
    //? if >=26.1 {
    /*public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        GuiContext drawContext = new GuiContext(graphics);
    *///?} else if >=1.20 {
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        GuiContext drawContext = new GuiContext(graphics);
    //?} else {
    /*public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        GuiContext drawContext = new GuiContext(matrices, null);
    *///?}
        if (this.visible) {
            this.mouseMoved(mouseX, mouseY);
            this.renderContent(drawContext, mouseX, mouseY, delta);
        }
    }

    protected abstract void renderContent(GuiContext drawContext, int mouseX, int mouseY, float delta);

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.isHovered = this.visible && this.isMouseOver(mouseX, mouseY);
        this.isPressed &= this.isHovered;
        if (!this.isHovered || this.lastMouseX != mouseX || this.lastMouseY != mouseY) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            this.lastMovementTime = new Date().getTime();
        }
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }
    *///?}

    //? if <1.21.9
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.visible && this.isHovered) {
            this.isPressed = true;
            return true;
        }
        return false;
    }

    //? if <1.21.9
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.visible && this.isPressed) {
            this.onMouseClick(mouseX, mouseY, button);
            this.isPressed = false;
            return true;
        }
        return false;
    }

    protected void onMouseClick(double mouseX, double mouseY, int button) { }

    @Override
    public NarrationPriority narrationPriority() {
        return this.isPressed() ? NarrationPriority.FOCUSED : this.isHovered() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public Component getTooltip() {
        if (this.isHovered && (new Date().getTime() - this.lastMovementTime) >= this.getTooltipDelay()) {
            return this.getWidgetDescription();
        }

        return null;
    }

    protected long getTooltipDelay() {
        return 500;
    }

    protected Component getWidgetDescription() {
        return null;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        Component description = this.getWidgetDescription();
        if (description != null) {
            builder.add(NarratedElementType.TITLE, description);
        }
    }
}
