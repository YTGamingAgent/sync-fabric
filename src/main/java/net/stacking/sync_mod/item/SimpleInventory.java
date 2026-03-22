package net.stacking.sync_mod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

public class SimpleInventory implements Inventory {
    public final DefaultedList<ItemStack> main;
    public final DefaultedList<ItemStack> armor;
    public final DefaultedList<ItemStack> offHand;
    private final DefaultedList<ItemStack> combinedInventory;

    public SimpleInventory() {
        this(36, 4, 1);
    }

    public SimpleInventory(int mainSize, int armorSize, int offHandSize) {
        this.main = DefaultedList.ofSize(mainSize, ItemStack.EMPTY);
        this.armor = DefaultedList.ofSize(armorSize, ItemStack.EMPTY);
        this.offHand = DefaultedList.ofSize(offHandSize, ItemStack.EMPTY);
        this.combinedInventory = DefaultedList.ofSize(mainSize + armorSize + offHandSize, ItemStack.EMPTY);
    }

    public void clone(PlayerInventory inventory) {
        this.copyFrom(inventory.main, this.main);
        this.copyFrom(inventory.armor, this.armor);
        this.copyFrom(inventory.offHand, this.offHand);
        this.updateCombinedInventory();
    }

    public void copyTo(PlayerInventory inventory) {
        this.copyFrom(this.main, inventory.main);
        this.copyFrom(this.armor, inventory.armor);
        this.copyFrom(this.offHand, inventory.offHand);
    }

    private void copyFrom(DefaultedList<ItemStack> from, DefaultedList<ItemStack> to) {
        int size = Math.min(from.size(), to.size());
        for (int i = 0; i < size; ++i) {
            to.set(i, from.get(i).copy());
        }
    }

    private void updateCombinedInventory() {
        int i = 0;
        for (ItemStack stack : this.main) this.combinedInventory.set(i++, stack);
        for (ItemStack stack : this.armor) this.combinedInventory.set(i++, stack);
        for (ItemStack stack : this.offHand) this.combinedInventory.set(i++, stack);
    }

    @Override
    public int size() {
        return this.combinedInventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.combinedInventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot >= 0 && slot < this.combinedInventory.size() ? this.combinedInventory.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.combinedInventory, slot, amount);
        if (!result.isEmpty()) {
            this.markDirty();
            this.updateFromCombined(slot);
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(this.combinedInventory, slot);
        this.updateFromCombined(slot);
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.combinedInventory.size()) {
            this.combinedInventory.set(slot, stack);
            if (stack.getCount() > this.getMaxCountPerStack()) {
                stack.setCount(this.getMaxCountPerStack());
            }
            this.updateFromCombined(slot);
        }
    }

    private void updateFromCombined(int slot) {
        int mainSize = this.main.size();
        int armorSize = this.armor.size();

        if (slot < mainSize) {
            this.main.set(slot, this.combinedInventory.get(slot));
        } else if (slot < mainSize + armorSize) {
            this.armor.set(slot - mainSize, this.combinedInventory.get(slot));
        } else {
            this.offHand.set(slot - mainSize - armorSize, this.combinedInventory.get(slot));
        }
    }

    @Override
    public void markDirty() { }

    @Override
    public boolean canPlayerUse(PlayerEntity player) { return true; }

    @Override
    public void clear() {
        this.main.clear();
        this.armor.clear();
        this.offHand.clear();
        this.combinedInventory.clear();
    }

    // Registry-aware NBT (1.21 API)
    public void writeNbt(NbtList list, RegistryWrapper.WrapperLookup registryLookup) {
        this.writeListToNbt(list, this.main, registryLookup);
        this.writeListToNbt(list, this.armor, registryLookup);
        this.writeListToNbt(list, this.offHand, registryLookup);
    }

    public void readNbt(NbtList list, RegistryWrapper.WrapperLookup registryLookup) {
        int index = 0;
        index = this.readListFromNbt(list, this.main, index, registryLookup);
        index = this.readListFromNbt(list, this.armor, index, registryLookup);
        this.readListFromNbt(list, this.offHand, index, registryLookup);
        this.updateCombinedInventory();
    }

    private void writeListToNbt(NbtList list, DefaultedList<ItemStack> items, RegistryWrapper.WrapperLookup registryLookup) {
        for (ItemStack stack : items) {
            list.add(stack.encodeAllowEmpty(registryLookup));
        }
    }

    private int readListFromNbt(NbtList list, DefaultedList<ItemStack> items, int startIndex, RegistryWrapper.WrapperLookup registryLookup) {
        for (int i = 0; i < items.size() && startIndex < list.size(); i++, startIndex++) {
            NbtCompound compound = list.getCompound(startIndex);
            items.set(i, ItemStack.fromNbtOrEmpty(registryLookup, compound));
        }
        return startIndex;
    }
}