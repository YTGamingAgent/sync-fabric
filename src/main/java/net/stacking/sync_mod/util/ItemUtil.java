package net.stacking.sync_mod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.config.SyncConfig;
//? if >=1.21.2 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;
*///?} else if >=1.19.4 {
import net.minecraft.world.item.Equipable;
//?} else {
/*import net.minecraft.world.item.ArmorItem;
*///?}

public final class ItemUtil {
    private static final TagKey<Item> WRENCHES = TagKey.create(GameRegistries.ITEM_KEY, IdUtil.of("c", "wrenches"));

    /**
     * An item works as a wrench if it's tagged c:wrenches or matches the "wrench" config option.
     */
    public static boolean isWrench(ItemStack itemStack) {
        if (itemStack.is(WRENCHES)) {
            return true;
        }

        SyncConfig config = Sync.getConfig();
        ResourceLocation wrenchId = config.wrench() == null || config.wrench().isBlank() ? null : ResourceLocation.tryParse(config.wrench());
        if (wrenchId == null) {
            return false;
        }
        Item wrench = GameRegistries.get(GameRegistries.ITEM, wrenchId);
        return wrench != Items.AIR && itemStack.is(wrench);
    }

    public static boolean isArmor(ItemStack itemStack) {
        return getPreferredEquipmentSlot(itemStack).getType() == armorSlotType();
    }

    /**
     * The slot an item would be worn in (armor slots, offhand for shields, etc.), or the main hand.
     */
    public static EquipmentSlot getPreferredEquipmentSlot(ItemStack itemStack) {
        //? if >=1.21.2 {
        /*Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        return equippable == null ? EquipmentSlot.MAINHAND : equippable.slot();
        *///?} else if >=1.19.4 {
        Equipable equipable = Equipable.get(itemStack);
        return equipable == null ? EquipmentSlot.MAINHAND : equipable.getEquipmentSlot();
        //?} else {
        /*return itemStack.getItem() instanceof ArmorItem armorItem ? armorItem.getSlot() : EquipmentSlot.MAINHAND;
        *///?}
    }

    public static EquipmentSlot.Type armorSlotType() {
        //? if >=1.21 {
        return EquipmentSlot.Type.HUMANOID_ARMOR;
        //?} else {
        /*return EquipmentSlot.Type.ARMOR;
        *///?}
    }

    private ItemUtil() { }
}
