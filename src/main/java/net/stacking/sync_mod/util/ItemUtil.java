package net.stacking.sync_mod.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.config.SyncConfig;

public final class ItemUtil {
    private static final TagKey<Item> WRENCHES = TagKey.of(Registries.ITEM.getKey(), Identifier.of("c", "wrenches"));

    public static boolean isWrench(ItemStack itemStack) {
        return itemStack.isIn(WRENCHES) || itemStack.isIn(ItemTags.PICKAXES) && itemStack.getName().getString().toLowerCase().contains(Sync.getConfig().wrench());
    }

    public static boolean isPickaxe(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.PICKAXES);
    }

    public static boolean isSword(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.SWORDS);
    }

    public static boolean isTool(ItemStack itemStack) {
        return itemStack.contains(DataComponentTypes.TOOL);
    }

    public static boolean isArmor(ItemStack itemStack) {
        return itemStack.getItem() instanceof ArmorItem;
    }

    public static boolean isValuable(ItemStack itemStack) {
        return Sync.getConfig().isValuable(itemStack);
    }

    public static EquipmentSlot getEquipmentSlot(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (item instanceof ArmorItem armorItem) {
            return armorItem.getSlotType();
        }
        return EquipmentSlot.MAINHAND;
    }

    public static EquipmentSlot getPreferredEquipmentSlot(ItemStack itemStack) {
        return getEquipmentSlot(itemStack);
    }

    private ItemUtil() { }
}