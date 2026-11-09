package net.stacking.sync_mod.client.gui.controller;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

@Environment(EnvType.CLIENT)
public final class HudController {
    private static final Options GAME_OPTIONS = Minecraft.getInstance().options;
    private static Boolean wasHudHidden;

    public static void show() {
        setHudHidden(false);
    }

    public static void hide() {
        setHudHidden(true);
    }

    private static void setHudHidden(boolean value) {
        if (wasHudHidden == null) {
            wasHudHidden = isHudHidden();
        }
        setHidden(value);
    }

    public static void restore() {
        if (wasHudHidden != null) {
            setHidden(wasHudHidden);
            wasHudHidden = null;
        }
    }

    private static boolean isHudHidden() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.isHidden();
        *///?} else
        return GAME_OPTIONS.hideGui;
    }

    private static void setHidden(boolean value) {
        //? if >=26.2 {
        /*if (isHudHidden() != value) {
            Minecraft.getInstance().gui.hud.toggle();
        }
        *///?} else
        GAME_OPTIONS.hideGui = value;
    }
}