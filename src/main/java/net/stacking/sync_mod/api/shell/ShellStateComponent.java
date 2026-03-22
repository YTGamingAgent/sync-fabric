package net.stacking.sync_mod.api.shell;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class ShellStateComponent {
    private static final int INVENTORY_SIZE = 41; // 36 main + 4 armor + 1 offhand

    private DefaultedList<ItemStack> items;
    private int level;
    private int levelTotal;
    private int levelProgress;
    private float exhaustion;
    private float saturation;
    private int food;
    private float health;

    public ShellStateComponent() {
        this.items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    public ShellStateComponent(ServerPlayerEntity player) {
        this();
        // Copy player data
    }

    public static ShellStateComponent empty() {
        Collection<ShellStateComponentFactoryRegistry.ShellStateComponentFactory> factories = ShellStateComponentFactoryRegistry.getInstance().getValues();
        return factories.stream()
                .map(ShellStateComponentFactoryRegistry.ShellStateComponentFactory::empty)
                .reduce(new ShellStateComponent(), ShellStateComponent::combine);
    }

    public static ShellStateComponent of(ServerPlayerEntity player) {
        Collection<ShellStateComponentFactoryRegistry.ShellStateComponentFactory> factories = ShellStateComponentFactoryRegistry.getInstance().getValues();
        return factories.stream()
                .map(f -> f.of(player))
                .reduce(new ShellStateComponent(), ShellStateComponent::combine);
    }

    public DefaultedList<ItemStack> getItems() {
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
        copy.items = this.items.stream().map(ItemStack::copy).collect(Collectors.toCollection(() -> DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)));
        copy.level = this.level;
        copy.levelTotal = this.levelTotal;
        copy.levelProgress = this.levelProgress;
        copy.exhaustion = this.exhaustion;
        copy.saturation = this.saturation;
        copy.food = this.food;
        copy.health = this.health;
        return copy;
    }

    public void applyToPlayer(ServerPlayerEntity player, RegistryWrapper.WrapperLookup registryLookup) {
        // Apply component data to player
    }

    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Objects.requireNonNull(nbt, "nbt");
        Objects.requireNonNull(registryLookup, "registryLookup");

        NbtList list = new NbtList();
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                NbtCompound entry = new NbtCompound();
                entry.putInt("slot", i);
                entry.put("stack", stack.encode(registryLookup));
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

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Objects.requireNonNull(nbt, "nbt");
        Objects.requireNonNull(registryLookup, "registryLookup");

        this.items.clear();
        this.items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

        if (nbt.contains("items", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("items", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompound(i);
                int slot = entry.getInt("slot");
                if (slot >= 0 && slot < this.items.size() && entry.contains("stack", NbtElement.COMPOUND_TYPE)) {
                    this.items.set(slot, ItemStack.fromNbtOrEmpty(registryLookup, entry.getCompound("stack")));
                }
            }
        }

        this.level = nbt.getInt("level");
        this.levelTotal = nbt.getInt("levelTotal");
        this.levelProgress = nbt.getInt("levelProgress");

        this.exhaustion = nbt.getFloat("exhaustion");
        this.saturation = nbt.getFloat("saturation");
        this.food = nbt.getInt("food");
        this.health = nbt.getFloat("health");
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