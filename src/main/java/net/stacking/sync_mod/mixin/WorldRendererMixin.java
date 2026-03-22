package net.stacking.sync_mod.mixin;

import net.stacking.sync_mod.client.render.MatrixStackStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = WorldRenderer.class, priority = 1010)
abstract class WorldRendererMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    /**
     * This method forces renderer to render the player when they aren't a camera entity.
     *
     * In 1.21+: WorldRenderer.render signature changed to:
     * render(RenderTickCounter, boolean, Camera, GameRenderer, LightmapTextureManager, Matrix4f, Matrix4f)
     */
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;getFocusedEntity()Lnet/minecraft/entity/Entity;",
                    ordinal = 3
            ),
            require = 0  // Changed from 1 to 0 to make it optional
    )
    private Entity getFocusedEntity(Camera camera) {
        ClientPlayerEntity player = this.client.player;
        if (player != null && player != this.client.getCameraEntity() && !player.isSpectator()) {
            return player;
        }
        return camera.getFocusedEntity();
    }

    /**
     * Updated for 1.21+ render signature.
     * NEW SIGNATURE: render(RenderTickCounter tickCounter, boolean renderBlockOutline,
     *                       Camera camera, GameRenderer gameRenderer,
     *                       LightmapTextureManager lightmapTextureManager,
     *                       Matrix4f positionMatrix, Matrix4f projectionMatrix)
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void render(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        // Create a MatrixStack from the position matrix for compatibility
        MatrixStack matrices = new MatrixStack();
        matrices.peek().getPositionMatrix().set(positionMatrix);
        MatrixStackStorage.saveModelMatrixStack(matrices);
    }
}