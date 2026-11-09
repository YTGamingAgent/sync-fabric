package net.stacking.sync_mod.api.shell;

import net.stacking.sync_mod.util.nbt.Nbt;
import net.stacking.sync_mod.util.nbt.ItemStackNbt;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ShellStateComponent {
    private static final int INVENTORY_SIZE = 41; // 36 main + 4 armor + 1 offhand

    private NonNullList<ItemStack> items;
    private int level;
    private int levelTotal;
    private int levelProgress;
    private float exhaustion;
    private float saturation;
    private int food;
    private float health;

    public ShellStateComponent() {
        this.items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    public ShellStateComponent(ServerPlayer player) {
        this();
        // Copy player data
    }

    public static ShellStateComponent empty() {
        Collection<ShellStateComponentFactoryRegistry.ShellStateComponentFactory> factories = ShellStateComponentFactoryRegistry.getInstance().getValues();
        return factories.stream()
                .map(ShellStateComponentFactoryRegistry.ShellStateComponentFactory::empty)
                .reduce(new ShellStateComponent(), ShellStateComponent::combine);
    }

    public static ShellStateComponent of(ServerPlayer player) {
        Collection<ShellStateComponentFactoryRegistry.ShellStateComponentFactory> factories = ShellStateComponentFactoryRegistry.getInstance().getValues();
        return factories.stream()
                .map(f -> f.of(player))
                .reduce(new ShellStateComponent(), ShellStateComponent::combine);
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public int getXp() {
        return 0; // Override in subclasses if needed
    }

    public ShellStateComponent combine(ShellStateComponent other) {
        // Combine logic - for now just return this
        return this;
    }

    public ShellStateComponent clone() {
        ShellStateComponent copy = new ShellStateComponent();
        copy.items = this.items.stream().map(ItemStack::copy).collect(Collectors.toCollection(() -> NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)));
        copy.level = this.level;
        copy.levelTotal = this.levelTotal;
        copy.levelProgress = this.levelProgress;
        copy.exhaustion = this.exhaustion;
        copy.saturation = this.saturation;
        copy.food = this.food;
        copy.health = this.health;
        return copy;
    }

    public void applyToPlayer(ServerPlayer player, HolderLookup.Provider registryLookup) {
        // Apply component data to player
    }

    public CompoundTag writeNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        Objects.requireNonNull(nbt, "nbt");

        ListTag list = new ListTag();
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("slot", i);
                entry.put("stack", ItemStackNbt.save(stack, registryLookup));
                list.add(entry);
            }
        }
        nbt.put("items", list);

        nbt.putInt("level", this.level);
        nbt.putInt("levelTotal", this.levelTotal);
        nbt.putInt("levelProgress", this.levelProgress);

        nbt.putFloat("exhaustion", this.exhaustion);
        nbt.putFloat("saturation", this.saturation);
        nbt.putInt("food", this.food);
        nbt.putFloat("health", this.health);

        return nbt;
    }

    public void readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        Objects.requireNonNull(nbt, "nbt");

        this.items.clear();
        this.items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

        if (Nbt.contains(nbt, "items", Tag.TAG_LIST)) {
            ListTag list = Nbt.getList(nbt, "items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = Nbt.getCompound(list, i);
                int slot = Nbt.getInt(entry, "slot");
                if (slot >= 0 && slot < this.items.size() && Nbt.contains(entry, "stack", Tag.TAG_COMPOUND)) {
                    this.items.set(slot, ItemStackNbt.load(Nbt.getCompound(entry, "stack"), registryLookup));
                }
            }
        }

        this.level = Nbt.getInt(nbt, "level");
        this.levelTotal = Nbt.getInt(nbt, "levelTotal");
        this.levelProgress = Nbt.getInt(nbt, "levelProgress");

        this.exhaustion = Nbt.getFloat(nbt, "exhaustion");
        this.saturation = Nbt.getFloat(nbt, "saturation");
        this.food = Nbt.getInt(nbt, "food");
        this.health = Nbt.getFloat(nbt, "health");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShellStateComponent that)) return false;
        return levelProgress == that.levelProgress &&
                levelTotal == that.levelTotal &&
                level == that.level &&
                Float.compare(exhaustion, that.exhaustion) == 0 &&
                Float.compare(saturation, that.saturation) == 0 &&
                food == that.food &&
                Float.compare(health, that.health) == 0 &&
                Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, levelProgress, levelTotal, level, exhaustion, saturation, food, health);
    }
}