package net.stacking.sync_mod.item;

import net.stacking.sync_mod.util.nbt.ItemStackNbt;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SimpleInventory implements Container {
    public final NonNullList<ItemStack> main;
    public final NonNullList<ItemStack> armor;
    public final NonNullList<ItemStack> offHand;
    private final NonNullList<ItemStack> combinedInventory;

    public SimpleInventory() {
        this(36, 4, 1);
    }

    public SimpleInventory(int mainSize, int armorSize, int offHandSize) {
        this.main = NonNullList.withSize(mainSize, ItemStack.EMPTY);
        this.armor = NonNullList.withSize(armorSize, ItemStack.EMPTY);
        this.offHand = NonNullList.withSize(offHandSize, ItemStack.EMPTY);
        this.combinedInventory = NonNullList.withSize(mainSize + armorSize + offHandSize, ItemStack.EMPTY);
    }

    // Player inventory slots: 0-35 main, 36-39 armor (feet to head), 40 offhand.
    private static final int PLAYER_ARMOR_START = 36;
    private static final int PLAYER_OFFHAND_START = 40;

    public void clone(Inventory inventory) {
        for (int i = 0; i < this.main.size(); ++i) {
            this.main.set(i, inventory.getItem(i).copy());
        }
        for (int i = 0; i < this.armor.size(); ++i) {
            this.armor.set(i, inventory.getItem(PLAYER_ARMOR_START + i).copy());
        }
        for (int i = 0; i < this.offHand.size(); ++i) {
            this.offHand.set(i, inventory.getItem(PLAYER_OFFHAND_START + i).copy());
        }
        this.updateCombinedInventory();
    }

    public void copyTo(Inventory inventory) {
        for (int i = 0; i < this.main.size(); ++i) {
            inventory.setItem(i, this.main.get(i).copy());
        }
        for (int i = 0; i < this.armor.size(); ++i) {
            inventory.setItem(PLAYER_ARMOR_START + i, this.armor.get(i).copy());
        }
        for (int i = 0; i < this.offHand.size(); ++i) {
            inventory.setItem(PLAYER_OFFHAND_START + i, this.offHand.get(i).copy());
        }
    }

    private void copyFrom(NonNullList<ItemStack> from, NonNullList<ItemStack> to) {
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
    public int getContainerSize() {
        return this.combinedInventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.combinedInventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.combinedInventory.size() ? this.combinedInventory.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.combinedInventory, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
            this.updateFromCombined(slot);
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.combinedInventory, slot);
        this.updateFromCombined(slot);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.combinedInventory.size()) {
            this.combinedInventory.set(slot, stack);
            if (stack.getCount() > this.getMaxStackSize()) {
                stack.setCount(this.getMaxStackSize());
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
    public void setChanged() { }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public void clearContent() {
        this.main.clear();
        this.armor.clear();
        this.offHand.clear();
        this.combinedInventory.clear();
    }

    // Registry-aware NBT (1.21 API)
    public void writeNbt(ListTag list, HolderLookup.Provider registryLookup) {
        this.writeListToNbt(list, this.main, registryLookup);
        this.writeListToNbt(list, this.armor, registryLookup);
        this.writeListToNbt(list, this.offHand, registryLookup);
    }

    public void readNbt(ListTag list, HolderLookup.Provider registryLookup) {
        int index = 0;
        index = this.readListFromNbt(list, this.main, index, registryLookup);
        index = this.readListFromNbt(list, this.armor, index, registryLookup);
        this.readListFromNbt(list, this.offHand, index, registryLookup);
        this.updateCombinedInventory();
    }

    private void writeListToNbt(ListTag list, NonNullList<ItemStack> items, HolderLookup.Provider registryLookup) {
        for (ItemStack stack : items) {
            list.add(ItemStackNbt.save(stack, registryLookup));
        }
    }

    private int readListFromNbt(ListTag list, NonNullList<ItemStack> items, int startIndex, HolderLookup.Provider registryLookup) {
        for (int i = 0; i < items.size() && startIndex < list.size(); i++, startIndex++) {
            CompoundTag compound = net.stacking.sync_mod.util.nbt.Nbt.getCompound(list, startIndex);
            items.set(i, ItemStackNbt.load(compound, registryLookup));
        }
        return startIndex;
    }
}