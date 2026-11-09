package net.stacking.sync_mod.item;

import net.minecraft.resources.ResourceKey;

import java.util.function.Function;

import java.util.function.BiFunction;

import net.stacking.sync_mod.util.GameRegistries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.block.SyncBlocks;

public final class SyncItems {
    public static final Item SYNC_CORE;
    public static final Item SHELL_STORAGE;
    public static final Item SHELL_CONSTRUCTOR;
    public static final Item TREADMILL;

    static {
        SYNC_CORE = register("sync_core", Item::new, new Item.Properties().stacksTo(16));
        SHELL_STORAGE = registerBlockItem(SyncBlocks.SHELL_STORAGE, BlockItem::new, new Item.Properties().stacksTo(1));
        SHELL_CONSTRUCTOR = registerBlockItem(SyncBlocks.SHELL_CONSTRUCTOR, BlockItem::new, new Item.Properties().stacksTo(1));
        TREADMILL = registerBlockItem(SyncBlocks.TREADMILL, TreadmillItem::new, new Item.Properties().stacksTo(1));
    }

    public static void init() { }

    private static Item register(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceLocation trueId = Sync.locate(id);
        // Since 1.21.2 an item must know its id before it's created.
        //? if >=1.21.2
        /*properties.setId(ResourceKey.create(GameRegistries.ITEM_KEY, trueId));*/
        return Registry.register(GameRegistries.ITEM, trueId, factory.apply(properties));
    }

    private static Item registerBlockItem(Block block, BiFunction<Block, Item.Properties, BlockItem> factory, Item.Properties properties) {
        ResourceLocation id = GameRegistries.BLOCK.getKey(block);
        //? if >=1.21.2
        /*properties.setId(ResourceKey.create(GameRegistries.ITEM_KEY, id)).useBlockDescriptionPrefix();*/
        BlockItem item = factory.apply(block, properties);
        item.registerBlocks(Item.BY_BLOCK, item);
        return Registry.register(GameRegistries.ITEM, id, item);
    }
}
