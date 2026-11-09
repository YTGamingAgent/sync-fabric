package net.stacking.sync_mod.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
//? if <26.2
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
//? if >=26.2 {
/*@Mixin(value = net.minecraft.client.renderer.extract.LevelExtractor.class, priority = 1010)
*///?} else
@Mixin(value = LevelRenderer.class, priority = 1010)
abstract class WorldRendererMixin {
    @Final
    @Shadow
    private Minecraft minecraft;

    /**
     * Makes the game draw the local player while they aren't the camera entity
     * (e.g. while the camera flies to another shell). The redirected call is the
     * "skip the local player unless it's the camera entity" check in the method that
     * collects the entities to draw, which moved out of renderLevel in 1.21.2.
     */
    @Redirect(
            //? if >=1.21.9 {
            /*method = "extractVisibleEntities",
            *///?} else if >=1.21.2 {
            /*method = "collectVisibleEntities",
            *///?} else
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    //? if >=1.21.11 {
                    /*target = "Lnet/minecraft/client/Camera;entity()Lnet/minecraft/world/entity/Entity;",
                    *///?} else
                    target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;",
                    ordinal = 3
            ),
            require = 1
    )
    private Entity getFocusedEntity(Camera camera) {
        LocalPlayer player = this.minecraft.player;
        if (player != null && player != this.minecraft.getCameraEntity() && !player.isSpectator()) {
            return player;
        }
        return camera./*? if >=1.21.11 {*//*entity()*//*?} else {*/getEntity()/*?}*/;
    }
}
