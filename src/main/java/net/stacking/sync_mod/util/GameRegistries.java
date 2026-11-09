package net.stacking.sync_mod.util;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if >=1.19.3 {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//?}

/**
 * Minecraft's built-in registries. They moved from Registry to BuiltInRegistries in 1.19.3.
 */
public final class GameRegistries {
    //? if >=1.19.3 {
    public static final DefaultedRegistry<Item> ITEM = BuiltInRegistries.ITEM;
    public static final DefaultedRegistry<Block> BLOCK = BuiltInRegistries.BLOCK;
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE = BuiltInRegistries.BLOCK_ENTITY_TYPE;
    public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = BuiltInRegistries.ENTITY_TYPE;
    public static final ResourceKey<Registry<Item>> ITEM_KEY = Registries.ITEM;
    public static final ResourceKey<Registry<Block>> BLOCK_KEY = Registries.BLOCK;
    //?} else {
    /*public static final DefaultedRegistry<Item> ITEM = Registry.ITEM;
    public static final DefaultedRegistry<Block> BLOCK = Registry.BLOCK;
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE = Registry.BLOCK_ENTITY_TYPE;
    public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = Registry.ENTITY_TYPE;
    public static final ResourceKey<Registry<Item>> ITEM_KEY = Registry.ITEM_REGISTRY;
    public static final ResourceKey<Registry<Block>> BLOCK_KEY = Registry.BLOCK_REGISTRY;
    *///?}

    /**
     * The registered value for {@code id}, or the registry's default value.
     */
    public static <T> T get(DefaultedRegistry<T> registry, ResourceLocation id) {
        //? if >=1.21.2 {
        /*return registry.getValue(id);
        *///?} else
        return registry.get(id);
    }

    private GameRegistries() { }
}
