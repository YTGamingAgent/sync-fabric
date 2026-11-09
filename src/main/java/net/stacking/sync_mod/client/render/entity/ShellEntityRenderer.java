package net.stacking.sync_mod.client.render.entity;

import net.stacking.sync_mod.util.client.render.ModelUtil;
import net.stacking.sync_mod.util.client.render.PoseUtil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.client.model.ShellModel;
import net.stacking.sync_mod.client.render.CustomRenderLayer;
import net.stacking.sync_mod.entity.ShellEntity;
//? if >=1.21.2
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;*/

/**
 * Renders shells: the regular player model once a shell is finished and in use, and the
 * printing/painting effect while it's being built (see {@link ShellModel}).
 */
public class ShellEntityRenderer extends PlayerRenderer {
    private final ShellModel shellModel;

    public ShellEntityRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context, slim);
        this.shellModel = new ShellModel(this.model);
        this.shadowRadius = 0;
        this.shadowStrength = 0;
    }

    //? if <1.21.9 {
    /**
     * Renders a shell standing in a shell container (or in the shell selector).
     *
     * @param yaw Direction the shell faces, in degrees.
     */
    public static void renderShell(ShellEntity shell, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        //? if >=1.21.2 {
        /*if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(shell) instanceof ShellEntityRenderer renderer) {
            PlayerRenderState state = renderer.createRenderState(shell, tickDelta);
            if (state instanceof ShellRenderState shellState) {
                shellState.shellYaw = yaw;
            }
            renderer.render(state, matrices, vertexConsumers, light);
        }
        *///?} else {
        EntityRenderer<? super ShellEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(shell);
        renderer.render(shell, yaw, tickDelta, matrices, vertexConsumers, light);
        //?}
    }
    //?}

    //? if >=1.21.6 {
    /*/^*
     * Draws a shell in a GUI box (the shell selector), turned slightly towards the viewer.
     ^/
    public static void submitShellToGui(net.minecraft.client.gui.GuiGraphics graphics, ShellEntity shell, int x0, int y0, int x1, int y1, float scale) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(shell) instanceof ShellEntityRenderer renderer)) {
            return;
        }
        PlayerRenderState state = renderer.createRenderState(shell, 0);
        if (state instanceof ShellRenderState shellState) {
            shellState.centered = true;
        }
        org.joml.Quaternionf rotation = new org.joml.Quaternionf().rotateZ((float)Math.PI).rotateX((float)Math.toRadians(15)).rotateY((float)Math.toRadians(40));
        graphics.submitEntityRenderState(state, scale, new org.joml.Vector3f(0, shell.getBbHeight() / 2, 0), rotation, null, x0, y0, x1, y1);
    }
    *///?}

    //? if <1.21.9 {
    /**
     * Draws a shell that isn't in use: printed voxels, a shell being painted, or a finished shell waiting in a container.
     */
    private void renderInactiveShell(ShellState state, ResourceLocation skin, float yaw, Runnable applyEntityScale, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        PoseUtil.rotateYDegrees(matrices, 180F);
        matrices.scale(-1.0F, -1.0F, 1.0F);
        applyEntityScale.run();
        matrices.translate(0.0D, -1.5010000467300415D, 0.0D);
        PoseUtil.rotateYDegrees(matrices, yaw);

        ModelUtil.resetPose(this.shellModel.parentModel);
        this.shellModel.setBuildProgress(state.getProgress());

        // While printing (< PROGRESS_PRINTING) ShellModel draws untextured white voxels. While painting,
        // the skin is drawn and then a mask covers the part that hasn't been painted yet with white.
        VertexConsumer consumer = vertexConsumers.getBuffer(this.shellModel.renderType(skin));
        // Colors are packed ARGB: -1 (0xFFFFFFFF) is opaque white.
        this.shellModel.renderModel(matrices, consumer, light, overlay, -1);
        if (this.shellModel.isBeingPainted()) {
            VertexConsumer maskConsumer = vertexConsumers.getBuffer(CustomRenderLayer.getPaintingMask(this.shellModel.getPaintingProgress()));
            this.shellModel.renderModel(matrices, maskConsumer, light, overlay, -1);
        }
    }
    //?}

    /**
     * Rotation for a shell that's in use, standing in a container facing {@code yaw}.
     */
    private static void applyActiveShellRotation(float yaw, PoseStack matrices) {
        Direction direction = Direction.fromYRot(yaw);
        PoseUtil.rotateYDegrees(matrices, yaw);
        if (direction == Direction.WEST || direction == Direction.EAST) {
            PoseUtil.rotateYDegrees(matrices, 180F);
        }
    }

    private static float getActiveShellPitch(boolean hasChestItem, float pitchProgress) {
        float maxPitch = hasChestItem ? 5 : 15;
        return maxPitch * pitchProgress;
    }

    //? if >=1.21.2 {
    /*/^*
     * Render state with what's needed to draw a shell.
     ^/
    public static class ShellRenderState extends PlayerRenderState {
        public ShellState shell;
        public ResourceLocation shellSkin;
        public boolean isActive;
        public boolean hasChestItem;
        public float pitchProgress;
        public float shellYaw;
        public String shellModel = "default";
        /^* Drawn centered on the origin (in a GUI) instead of in a block space. ^/
        public boolean centered;
    }

    @Override
    public PlayerRenderState createRenderState() {
        return new ShellRenderState();
    }

    @Override
    public void extractRenderState(AbstractClientPlayer player, PlayerRenderState state, float tickDelta) {
        super.extractRenderState(player, state, tickDelta);
        if (state instanceof ShellRenderState shellState) {
            shellState.shell = player instanceof ShellEntity shell ? shell.getState() : null;
            shellState.shellSkin = player instanceof ShellEntity shell ? shell.getSkinTexture() : null;
            shellState.isActive = !(player instanceof ShellEntity shell) || shell.isActive;
            shellState.pitchProgress = player instanceof ShellEntity shell ? shell.pitchProgress : 0;
            shellState.hasChestItem = !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
            shellState.shellModel = player instanceof ShellEntity shell ? shell.getModel() : "default";
        }
    }

    @Override
    protected boolean shouldShowName(AbstractClientPlayer player, double distance) {
        return player.shouldShowName() && super.shouldShowName(player, distance);
    }
    *///?}

    //? if >=1.21.2 && <1.21.9 {
    /*@Override
    public void render(PlayerRenderState state, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if (!(state instanceof ShellRenderState shellState) || shellState.shell == null) {
            super.render(state, matrices, vertexConsumers, light);
            return;
        }

        matrices.pushPose();
        if (!shellState.centered) {
            matrices.translate(0.5, 0, 0.5);
        }
        if (!shellState.isActive) {
            int overlay = getOverlayCoords(state, 0);
            this.renderInactiveShell(shellState.shell, shellState.shellSkin, shellState.shellYaw, () -> this.scale(state, matrices), matrices, vertexConsumers, light, overlay);
            if (shellState.shell.getProgress() >= ShellState.PROGRESS_DONE) {
                for (RenderLayer<PlayerRenderState, PlayerModel> layer : this.layers) {
                    layer.render(matrices, vertexConsumers, light, state, 0, 0);
                }
            }
        } else {
            applyActiveShellRotation(shellState.shellYaw, matrices);
            state.xRot = getActiveShellPitch(shellState.hasChestItem, shellState.pitchProgress);
            super.render(state, matrices, vertexConsumers, light);
        }
        matrices.popPose();
    }
    *///?}

    //? if >=1.21.9 {
    /*/^*
     * Queues a shell standing in a shell container for drawing (1.21.9+ submit-based rendering).
     ^/
    public static void submitShell(ShellEntity shell, float yaw, float tickDelta, PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera) {
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(shell) instanceof ShellEntityRenderer renderer) {
            PlayerRenderState state = renderer.createRenderState(shell, tickDelta);
            if (state instanceof ShellRenderState shellState) {
                shellState.shellYaw = yaw;
            }
            renderer.submit(state, matrices, collector, camera);
        }
    }

    @Override
    public void submit(PlayerRenderState state, PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera) {
        if (!(state instanceof ShellRenderState shellState) || shellState.shell == null) {
            super.submit(state, matrices, collector, camera);
            return;
        }

        matrices.pushPose();
        if (!shellState.centered) {
            matrices.translate(0.5, 0, 0.5);
        }
        if (!shellState.isActive) {
            int overlay = getOverlayCoords(state, 0);
            int light = state.lightCoords;
            float progress = shellState.shell.getProgress();
            PoseUtil.rotateYDegrees(matrices, 180F);
            matrices.scale(-1.0F, -1.0F, 1.0F);
            this.scale(state, matrices);
            matrices.translate(0.0D, -1.5010000467300415D, 0.0D);
            PoseUtil.rotateYDegrees(matrices, shellState.shellYaw);

            // Drawing is deferred, so the shared model is set up again inside each callback.
            this.shellModel.setBuildProgress(progress);
            collector.submitCustomGeometry(matrices, this.shellModel.renderType(shellState.shellSkin), (pose, vertices) -> this.drawShellModel(pose, vertices, progress, light, overlay));
            if (this.shellModel.isBeingPainted()) {
                collector.submitCustomGeometry(matrices, CustomRenderLayer.getPaintingMask(this.shellModel.getPaintingProgress()), (pose, vertices) -> this.drawShellModel(pose, vertices, progress, light, overlay));
            }
            if (progress >= ShellState.PROGRESS_DONE) {
                for (RenderLayer<PlayerRenderState, PlayerModel> layer : this.layers) {
                    layer.submit(matrices, collector, light, state, 0, 0);
                }
            }
        } else {
            applyActiveShellRotation(shellState.shellYaw, matrices);
            state.xRot = getActiveShellPitch(shellState.hasChestItem, shellState.pitchProgress);
            super.submit(state, matrices, collector, camera);
        }
        matrices.popPose();
    }

    private void drawShellModel(PoseStack.Pose pose, VertexConsumer vertices, float progress, int light, int overlay) {
        PoseStack matrices = new PoseStack();
        matrices.last().set(pose);
        ModelUtil.resetPose(this.shellModel.parentModel);
        this.shellModel.setBuildProgress(progress);
        this.shellModel.renderModel(matrices, vertices, light, overlay, -1);
    }
    *///?}

    //? if <1.21.2 {
    @Override
    public void render(AbstractClientPlayer player, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        matrices.pushPose();
        matrices.translate(0.5, 0, 0.5);
        if (player instanceof ShellEntity shell && !shell.isActive) {
            int overlay = getOverlayCoords(player, tickDelta);
            this.renderInactiveShell(shell.getState(), shell.getSkinTexture(), yaw, () -> this.scale(player, matrices, yaw), matrices, vertexConsumers, light, overlay);
            if (shell.getState().getProgress() >= ShellState.PROGRESS_DONE) {
                for (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> feature : this.layers) {
                    feature.render(matrices, vertexConsumers, light, player, 0, 0, tickDelta, 0, 0, 0);
                }
            }
        } else {
            applyActiveShellRotation(yaw, matrices);
            float pitch = getActiveShellPitch(!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), player instanceof ShellEntity shell ? shell.pitchProgress : 0);
            player.setXRot(pitch);
            player.xRotO = pitch;
            super.render(player, yaw, tickDelta, matrices, vertexConsumers, light);
        }
        matrices.popPose();
    }

    @Override
    protected boolean shouldShowName(AbstractClientPlayer player) {
        return player.shouldShowName() && super.shouldShowName(player);
    }
    //?}
}
