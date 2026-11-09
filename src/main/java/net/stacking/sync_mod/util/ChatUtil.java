package net.stacking.sync_mod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class ChatUtil {
    /**
     * Shows a message to a player, either above the hotbar ({@code overlay}) or in the chat.
     */
    public static void message(Player player, Component text, boolean overlay) {
        //? if >=26.1 {
        /*if (overlay) {
            player.sendOverlayMessage(text);
        } else {
            player.sendSystemMessage(text);
        }
        *///?} else
        player.displayClientMessage(text, overlay);
    }

    private ChatUtil() { }
}
