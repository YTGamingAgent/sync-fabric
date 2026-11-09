package net.stacking.sync_mod.client.gui;

import net.stacking.sync_mod.util.WorldUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else
/*import com.mojang.blaze3d.vertex.PoseStack;*/
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.stacking.sync_mod.api.shell.Shell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.client.gui.TooltipProvider;
//? if <1.21.2
import net.stacking.sync_mod.client.gl.MSAAFramebuffer;
import net.stacking.sync_mod.client.gui.controller.HudController;
import net.stacking.sync_mod.client.gui.widget.ArrowButtonWidget;
import net.stacking.sync_mod.client.gui.widget.CrossButtonWidget;
import net.stacking.sync_mod.client.gui.widget.PageDisplayWidget;
import net.stacking.sync_mod.client.gui.widget.ShellSelectorButtonWidget;
import net.stacking.sync_mod.util.IdUtil;
import net.stacking.sync_mod.util.client.render.ColorUtil;
import net.stacking.sync_mod.util.math.Radians;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@SuppressWarnings("FieldCanBeLocal")
@Environment(EnvType.CLIENT)
public class ShellSelectorGUI extends Screen {
    private static final int MAX_SLOTS = 8;
    private static final double MENU_RADIUS = 0.3F;
    private static final int BACKGROUND_COLOR = ColorUtil.getRgb(DyeColor.BLACK, 76);
    private static final Component TITLE = Component.translatable("gui.sync.default.cross_button.title");
    private static final Collection<Component> ARROW_TITLES = List.of(
            Component.translatable("gui.sync.shell_selector.up.title"),
            Component.translatable("gui.sync.shell_selector.right.title"),
            Component.translatable("gui.sync.shell_selector.down.title"),
            Component.translatable("gui.sync.shell_selector.left.title")
    );

    private final Runnable onCloseCallback;
    private final Runnable onRemovedCallback;
    private boolean wasClosed;
    private List<ShellSelectorButtonWidget> shellButtons;
    private List<ArrowButtonWidget> arrowButtons;
    private CrossButtonWidget crossButton;
    private PageDisplayWidget<ResourceLocation, ShellState> pageDisplay;

    public ShellSelectorGUI(Runnable onCloseCallback, Runnable onRemovedCallback) {
        super(TITLE);
        this.onCloseCallback = onCloseCallback;
        this.onRemovedCallback = onRemovedCallback;
    }

    @Override
    public void init() {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        Stream<ShellState> data = ((Shell)player).getAvailableShellStates();
        ResourceLocation selectedWorld = WorldUtil.level(player).dimension().location();

        this.wasClosed = false;
        this.arrowButtons = createArrowButtons(this.width, this.height, ARROW_TITLES,
                List.of(this::previousSection, this::nextPage, this::nextSection, this::previousPage));
        this.crossButton = createCrossButton(this.width, this.height, this::onClose);
        this.pageDisplay = createPageDisplay(this.width, this.height, data, selectedWorld, MAX_SLOTS, this::onPageChange);
        Stream.concat(this.arrowButtons.stream(), Stream.of(this.crossButton, this.pageDisplay)).forEach(this::addRenderableWidget);

        HudController.hide();
    }

    private static List<ShellSelectorButtonWidget> createShellButtons(int screenWidth, int screenHeight, int count) {
        final double HOLLOW_R = MENU_RADIUS * 0.6;
        final double BORDER_WIDTH = 0.0033;
        final double SECTOR_SPACING = 0.01;

        double cX = screenWidth / 2.0;
        double cY = screenHeight / 2.0;
        double majorR = screenHeight * MENU_RADIUS;
        double minorR = screenHeight * HOLLOW_R;
        double spacing = count > 1 ? SECTOR_SPACING : 0;
        double sector = Radians.R_2_PI / count - spacing;
        double borderWidth = screenHeight * BORDER_WIDTH;
        double pos = -sector / (2 << (count % 2));
        List<ShellSelectorButtonWidget> shellButtons = new ArrayList<>();

        for (int i = 0; i < count; ++i) {
            ShellSelectorButtonWidget button = new ShellSelectorButtonWidget(cX, cY, majorR, minorR, borderWidth, pos, pos + sector);
            pos += sector + spacing;
            shellButtons.add(button);
        }

        return shellButtons;
    }

    private static PageDisplayWidget<ResourceLocation, ShellState> createPageDisplay(int screenWidth, int screenHeight, Stream<ShellState> data, ResourceLocation defaultPage, int entriesPerPage, BiConsumer<PageDisplayWidget<ResourceLocation, ShellState>, PageDisplayWidget<ResourceLocation, ShellState>.Page> onChange) {
        final float FONT_HEIGHT = 1 / 30F;

        float cX = screenWidth / 2F;
        float cY = screenHeight / 2F;
        float scale = screenHeight * FONT_HEIGHT / Minecraft.getInstance().font.lineHeight;

        return new PageDisplayWidget<>(cX, cY, scale, data, ShellState::getWorld, IdUtil::prettifyAsText, defaultPage, entriesPerPage, onChange);
    }

    private static List<ArrowButtonWidget> createArrowButtons(int screenWidth, int screenHeight, Iterable<Component> arrowTitles, Iterable<Runnable> arrowActions) {
        final float ARROW_HEIGHT = 2 / 75F;
        final float ARROW_WIDTH = 57 / 32F;
        final float ARROW_THICKNESS = 1 / 240F;
        final float ARROW_SPACING = 1 / 14F;

        float cX = screenWidth / 2F;
        float cY = screenHeight / 2F;
        float r = screenHeight * (float)MENU_RADIUS * (1F + ARROW_SPACING);
        float arrowHeight = screenHeight * ARROW_HEIGHT;
        float arrowWidth = arrowHeight * ARROW_WIDTH;
        float thickness = screenHeight * ARROW_THICKNESS;
        Iterator<Runnable> actions = arrowActions.iterator();
        Iterator<Component> descriptions = arrowTitles.iterator();
        List<ArrowButtonWidget> arrowButtons = new ArrayList<>();

        for (ArrowButtonWidget.ArrowType arrowType : ArrowButtonWidget.ArrowType.values()) {
            float x;
            float y;
            if (arrowType.isVertical()) {
                x = screenWidth / 2F - arrowWidth / 2F;
                y = cY + r * (arrowType.isDown() ? 1 : -1) + (arrowType.isDown() ? 0 : -arrowHeight);
            } else {
                x = cX + r * (arrowType.isRight() ? 1 : -1) + (arrowType.isRight() ? 0 : -arrowHeight);
                y = screenHeight / 2F - arrowWidth / 2F;
            }
            arrowButtons.add(new ArrowButtonWidget(x, y, arrowWidth, arrowHeight, arrowType, thickness, descriptions.next(), actions.next()));
        }

        return arrowButtons;
    }

    private static CrossButtonWidget createCrossButton(int screenWidth, int screenHeight, Runnable onClose) {
        final float CROSS_MARGIN = 1 / 15F;
        final float CROSS_WIDTH = 2 / 75F;
        final float CROSS_THICKNESS = 1 / 240F;

        float width = screenHeight * CROSS_WIDTH;
        float y = screenHeight * CROSS_MARGIN;
        float x = screenWidth - y - width;
        float thickness = screenHeight * CROSS_THICKNESS;

        return new CrossButtonWidget(x, y, width, width, thickness, onClose);
    }

    //? if >=26.1 {
    /*@Override
    public void extractBackground(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (Objects.requireNonNull(this.minecraft).level != null) {
            drawContext.fillGradient(0, 0, this.width, this.height, BACKGROUND_COLOR, BACKGROUND_COLOR);
        } else {
            super.extractBackground(drawContext, mouseX, mouseY, delta);
        }
    }
    *///?} else if >=1.20.2 {
    @Override
    public void renderBackground(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (Objects.requireNonNull(this.minecraft).level != null) {
            drawContext.fillGradient(0, 0, this.width, this.height, BACKGROUND_COLOR, BACKGROUND_COLOR);
        } else {
            super.renderBackground(drawContext, mouseX, mouseY, delta);
        }
    }
    //?} else if >=1.20 {
    /*@Override
    public void renderBackground(GuiGraphics drawContext) {
        if (Objects.requireNonNull(this.minecraft).level != null) {
            drawContext.fillGradient(0, 0, this.width, this.height, BACKGROUND_COLOR, BACKGROUND_COLOR);
        } else {
            super.renderBackground(drawContext);
        }
    }
    *///?} else {
    /*@Override
    public void renderBackground(PoseStack matrices) {
        if (Objects.requireNonNull(this.minecraft).level != null) {
            new GuiContext(matrices, this).fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
        } else {
            super.renderBackground(matrices);
        }
    }
    *///?}

    @Override
    //? if >=26.1 {
    /*public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        GuiContext drawContext = new GuiContext(graphics);
    *///?} else if >=1.20 {
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        GuiContext drawContext = new GuiContext(graphics);
    //?} else {
    /*public void render(PoseStack graphics, int mouseX, int mouseY, float delta) {
        GuiContext drawContext = new GuiContext(graphics, this);
    *///?}
        // Since 1.20.2 Screen#render draws the background itself.
        //? if <1.20.2
        /*this.renderBackground(graphics);*/
        //? if >=26.1 {
        /*super.extractRenderState(graphics, mouseX, mouseY, delta);
        *///?} else if >=1.21.2 {
        /*super.render(graphics, mouseX, mouseY, delta);
        *///?} else
        MSAAFramebuffer.use(MSAAFramebuffer.MAX_SAMPLES, () -> super.render(graphics, mouseX, mouseY, delta));
        this.renderTooltips(drawContext, mouseX, mouseY);
    }

    protected void renderTooltips(GuiContext drawContext, int mouseX, int mouseY) {
        for (GuiEventListener child : this.children()) {
            if (child instanceof NarratableEntry selectable && selectable.narrationPriority() != NarratableEntry.NarrationPriority.NONE) {
                Component tooltipText = selectable instanceof TooltipProvider tooltipProvider ? tooltipProvider.getTooltip() : null;
                if (tooltipText != null) {
                    drawContext.renderTooltip(font, tooltipText, mouseX, mouseY);
                }
                return;
            }
        }
    }

    @Override
    //? if >=1.21.9 {
    /*public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }

        for (GuiEventListener child : this.children()) {
            if (child.keyPressed(event)) {
    *///?} else {
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        for (GuiEventListener child : this.children()) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) {
    //?}
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void onPageChange(PageDisplayWidget<ResourceLocation, ShellState> pageDisplay, PageDisplayWidget<ResourceLocation, ShellState>.Page page) {
        for (ArrowButtonWidget arrow : this.arrowButtons) {
            arrow.visible = arrow.type.isVertical() ? pageDisplay.hasMoreSections() : pageDisplay.hasMorePages();
        }

        if (this.shellButtons != null) {
            this.shellButtons.forEach(this::removeWidget);
        }

        List<ShellState> content = page.content;
        this.shellButtons = createShellButtons(this.width, this.height, Math.max(content.size(), 1));
        this.shellButtons.forEach(this::addRenderableWidget);

        for (int i = 0; i < content.size(); ++i) {
            this.shellButtons.get(i).shell = content.get(i);
        }
    }

    private void nextSection() {
        this.pageDisplay.nextSection();
    }

    private void previousSection() {
        this.pageDisplay.previousSection();
    }

    private void nextPage() {
        this.pageDisplay.nextPage();
    }

    private void previousPage() {
        this.pageDisplay.previousPage();
    }

    @Override
    public void onClose() {
        HudController.restore();
        if (this.onCloseCallback != null) {
            this.onCloseCallback.run();
        }
        this.wasClosed = true;
        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
        if (!this.wasClosed && this.onRemovedCallback != null) {
            this.onRemovedCallback.run();
        }
    }
}