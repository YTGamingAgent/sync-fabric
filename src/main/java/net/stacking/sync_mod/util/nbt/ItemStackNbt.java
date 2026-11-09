package net.stacking.sync_mod.util.nbt;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Saves and loads item stacks as NBT on every supported Minecraft version.
 * The registry lookup is only used on 1.20.5+ (it may be null on older versions).
 */
public final class ItemStackNbt {
    /**
     * Saves a stack. Empty stacks are allowed and load back as {@link ItemStack#EMPTY}.
     */
    public static Tag save(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        //? if >=1.21.5 {
        /*return stack.isEmpty() ? new CompoundTag() : ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack).getOrThrow();
        *///?} else if >=1.20.5 {
        return stack.saveOptional(registries);
        //?} else {
        /*return stack.save(new CompoundTag());
        *///?}
    }

    public static ItemStack load(CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        //? if >=1.21.5 {
        /*return nbt.isEmpty() ? ItemStack.EMPTY : ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), nbt).result().orElse(ItemStack.EMPTY);
        *///?} else if >=1.20.5 {
        return ItemStack.parseOptional(registries, nbt);
        //?} else {
        /*return ItemStack.of(nbt);
        *///?}
    }

    /**
     * Whether two stacks hold the same item with the same data (so they could merge).
     */
    public static boolean isSameItemSameData(ItemStack a, ItemStack b) {
        //? if >=1.20.5 {
        return ItemStack.isSameItemSameComponents(a, b);
        //?} else {
        /*return ItemStack.isSameItemSameTags(a, b);
        *///?}
    }

    private ItemStackNbt() { }
}
