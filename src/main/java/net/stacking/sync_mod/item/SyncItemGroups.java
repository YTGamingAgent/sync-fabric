package net.stacking.sync_mod.item;

import net.stacking.sync_mod.Sync;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
*///?} else if >=1.20
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
//? if >=1.20 {
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//?} else if >=1.19.3 {
/*import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
*///?} else {
/*import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
*///?}

import java.util.function.Consumer;

public final class SyncItemGroups {
    public static CreativeModeTab MAIN;

    public static void init() {
        //? if >=1.20 {
        MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Sync.locate("sync"), /*? if >=26.1 {*//*FabricCreativeModeTab*//*?} else {*/FabricItemGroup/*?}*/.builder()
                .noScrollBar()
                .icon(() -> new ItemStack(SyncItems.SYNC_CORE))
                .title(Component.literal("Sync"))
                .displayItems((parameters, entries) -> addItems(entries::accept))
                .build());
        //?} else if >=1.19.4 {
        /*MAIN = FabricItemGroup.builder(Sync.locate("sync"))
                .noScrollBar()
                .icon(() -> new ItemStack(SyncItems.SYNC_CORE))
                .title(Component.literal("Sync"))
                .displayItems((parameters, entries) -> addItems(entries::accept))
                .build();
        *///?} else if >=1.19.3 {
        /*MAIN = FabricItemGroup.builder(Sync.locate("sync"))
                .noScrollBar()
                .icon(() -> new ItemStack(SyncItems.SYNC_CORE))
                .title(Component.literal("Sync"))
                .displayItems((features, entries, operator) -> addItems(entries::accept))
                .build();
        *///?} else {
        /*MAIN = FabricItemGroupBuilder.create(Sync.locate("sync"))
                .icon(() -> new ItemStack(SyncItems.SYNC_CORE))
                .appendItems(list -> addItems(list::add))
                .build();
        *///?}
    }

    private static void addItems(Consumer<ItemStack> entries) {
        entries.accept(new ItemStack(SyncItems.SYNC_CORE));
        entries.accept(new ItemStack(SyncItems.SHELL_STORAGE));
        entries.accept(new ItemStack(SyncItems.SHELL_CONSTRUCTOR));
        entries.accept(new ItemStack(SyncItems.TREADMILL));
    }

    private SyncItemGroups() { }
}
