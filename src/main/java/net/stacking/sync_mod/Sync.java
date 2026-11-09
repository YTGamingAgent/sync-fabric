package net.stacking.sync_mod;

import net.stacking.sync_mod.util.IdUtil;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.stacking.sync_mod.block.SyncBlocks;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;
import net.stacking.sync_mod.client.render.SyncRenderers;
import net.stacking.sync_mod.command.SyncCommands;
import net.stacking.sync_mod.config.SyncConfig;
import net.stacking.sync_mod.item.SyncItemGroups;
import net.stacking.sync_mod.item.SyncItems;
import net.stacking.sync_mod.networking.SyncPackets;

public class Sync implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "sync";
    public static final String PROJECT_ID = MOD_ID + "-fabric";
    private static final SyncConfig CONFIG = SyncConfig.resolve();

    public static ResourceLocation locate(String location) {
        return IdUtil.of(MOD_ID, location);
    }

    public static SyncConfig getConfig() {
        return CONFIG;
    }

    @Override
    public void onInitialize() {
        SyncBlocks.init();
        SyncBlockEntities.init();
        SyncItems.init();
        SyncPackets.init();
        SyncCommands.init();
        SyncItemGroups.init();

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            net.stacking.sync_mod.compat.trinkets.TrinketsCompat.init();
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onInitializeClient() {
        SyncRenderers.initClient();
        SyncPackets.initClient();
    }
}