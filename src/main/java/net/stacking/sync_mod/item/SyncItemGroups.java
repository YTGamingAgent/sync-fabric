package net.stacking.sync_mod.item;

import net.stacking.sync_mod.Sync;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SyncItemGroups {
    public static final ItemGroup MAIN = FabricItemGroup.builder().noScrollbar().icon(() ->new ItemStack(SyncItems.SYNC_CORE)).displayName(Text.literal("Sync")).entries((displayContext, entries) -> {
        entries.add(new ItemStack(SyncItems.SYNC_CORE));
        entries.add(new ItemStack(SyncItems.SHELL_STORAGE));
        entries.add(new ItemStack(SyncItems.SHELL_CONSTRUCTOR));
        entries.add(new ItemStack(SyncItems.TREADMILL));
    }).build();
}