package net.stacking.sync_mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.stacking.sync_mod.block.SyncBlocks;
import net.stacking.sync_mod.block.entity.SyncBlockEntities;
import net.stacking.sync_mod.client.render.CustomGameRenderer;
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

    public static Identifier locate(String location) {
        return Identifier.of(MOD_ID, location);
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
        Registry.register(Registries.ITEM_GROUP, Identifier.of("sync", "sync"), SyncItemGroups.MAIN);

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            net.stacking.sync_mod.compat.trinkets.TrinketsCompat.init();
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onInitializeClient() {
        CustomGameRenderer.initClient();
        SyncRenderers.initClient();
        SyncPackets.initClient();
    }
}