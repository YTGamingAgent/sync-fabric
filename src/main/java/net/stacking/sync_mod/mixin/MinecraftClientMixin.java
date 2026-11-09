package net.stacking.sync_mod.mixin;

import net.stacking.sync_mod.client.gui.controller.DeathScreenController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//? if <26.2
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
//? if >=26.2 {
/*@Mixin(value = net.minecraft.client.gui.Gui.class, priority = 1001)
*///?} else
@Mixin(value = Minecraft.class, priority = 1001)
abstract class MinecraftClientMixin {
    /**
     * `setScreen(null)` opens DeathScreen when the player is dead.
     * This method can prevent this from happening.
     */
    @Redirect(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isDeadOrDying()Z", ordinal = 0), require = 1)
    private boolean isPlayerDead(LocalPlayer player) {
        return player.isDeadOrDying() && !DeathScreenController.isSuspended();
    }
}