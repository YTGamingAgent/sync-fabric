package net.stacking.sync_mod.compat.trinkets;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.stacking.sync_mod.api.shell.ShellStateComponent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class TrinketShellStateComponent extends ShellStateComponent {

    private static final String NBT_KEY = "trinkets";

    private final NbtCompound trinkets = new NbtCompound();

    public TrinketShellStateComponent() {
        super();
    }

    public TrinketShellStateComponent(ServerPlayerEntity player) {
        super(player);
        captureFromPlayer(player, player.getRegistryManager());
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (!this.trinkets.isEmpty()) {
            nbt.put(NBT_KEY, this.trinkets.copy());
        }
        return nbt;
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        // FIXED: Use copyFrom instead of reassignment
        this.trinkets.copyFrom(new NbtCompound());
        if (nbt.contains(NBT_KEY, NbtElement.COMPOUND_TYPE)) {
            this.trinkets.copyFrom(nbt.getCompound(NBT_KEY));
        }
    }

    @Override
    public void applyToPlayer(ServerPlayerEntity player, RegistryWrapper.WrapperLookup registryLookup) {
        super.applyToPlayer(player, registryLookup);
        applyToPlayerTrinkets(player, registryLookup);
    }

    private void captureFromPlayer(ServerPlayerEntity player, RegistryWrapper.WrapperLookup registryLookup) {
        // FIXED: Use copyFrom instead of reassignment
        this.trinkets.copyFrom(new NbtCompound());

        Optional<Object> componentOpt = TrinketsReflection.getComponent(player);
        if (componentOpt.isEmpty()) {
            return;
        }

        Object component = componentOpt.get();
        Map<?, ?> groups = TrinketsReflection.getInventory(component);
        if (groups == null) {
            return;
        }

        for (Map.Entry<?, ?> groupEntry : groups.entrySet()) {
            String groupName = String.valueOf(groupEntry.getKey());
            Object groupVal = groupEntry.getValue();
            if (!(groupVal instanceof Map<?, ?> slots)) {
                continue;
            }

            NbtCompound groupTag = new NbtCompound();

            for (Map.Entry<?, ?> slotEntry : slots.entrySet()) {
                String slotName = String.valueOf(slotEntry.getKey());
                Object invObj = slotEntry.getValue();
                if (invObj == null) {
                    continue;
                }

                int size = TrinketsReflection.invSize(invObj);
                if (size <= 0) {
                    continue;
                }

                NbtList items = new NbtList();
                for (int i = 0; i < size; i++) {
                    ItemStack stack = TrinketsReflection.getStack(invObj, i);
                    if (stack == null) {
                        stack = ItemStack.EMPTY;
                    }
                    items.add(stack.encode(registryLookup));
                }

                groupTag.put(slotName, items);
            }

            if (!groupTag.isEmpty()) {
                this.trinkets.put(groupName, groupTag);
            }
        }
    }

    private void applyToPlayerTrinkets(ServerPlayerEntity player, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.trinkets.isEmpty()) {
            return;
        }

        Optional<Object> componentOpt = TrinketsReflection.getComponent(player);
        if (componentOpt.isEmpty()) {
            return;
        }

        Object component = componentOpt.get();
        Map<?, ?> groups = TrinketsReflection.getInventory(component);
        if (groups == null) {
            return;
        }

        for (String groupName : this.trinkets.getKeys()) {
            Object groupVal = groups.get(groupName);
            if (!(groupVal instanceof Map<?, ?> slots)) {
                continue;
            }

            NbtCompound groupTag = this.trinkets.getCompound(groupName);
            for (String slotName : groupTag.getKeys()) {
                Object invObj = slots.get(slotName);
                if (invObj == null) {
                    continue;
                }

                NbtList items = groupTag.getList(slotName, NbtElement.COMPOUND_TYPE);
                int size = TrinketsReflection.invSize(invObj);
                int max = Math.min(size, items.size());

                for (int i = 0; i < max; i++) {
                    ItemStack stack = ItemStack.fromNbtOrEmpty(registryLookup, items.getCompound(i));
                    TrinketsReflection.setStack(invObj, i, stack);
                }
            }
        }
    }

    private static final class TrinketsReflection {

        private static final String TRINKETS_API = "dev.emi.trinkets.api.TrinketsApi";
        private static final String LIVING_ENTITY = "net.minecraft.entity.LivingEntity";

        private static Method getTrinketComponent;
        private static Method getInventory;
        private static Method invSize;
        private static Method invGetStack;
        private static Method invSetStack;

        private TrinketsReflection() {
        }

        static Optional<Object> getComponent(Object livingEntity) {
            try {
                if (getTrinketComponent == null) {
                    Class<?> api = Class.forName(TRINKETS_API);
                    Class<?> living = Class.forName(LIVING_ENTITY);
                    getTrinketComponent = api.getMethod("getTrinketComponent", living);
                }

                Object opt = getTrinketComponent.invoke(null, livingEntity);
                if (opt instanceof Optional<?> o) {
                    return (Optional<Object>) o;
                }

                return Optional.ofNullable(opt);
            } catch (Throwable ignored) {
                return Optional.empty();
            }
        }

        static Map<?, ?> getInventory(Object component) {
            try {
                if (getInventory == null) {
                    getInventory = component.getClass().getMethod("getInventory");
                }
                Object inv = getInventory.invoke(component);
                if (inv instanceof Map<?, ?> m) {
                    return m;
                }
                return null;
            } catch (Throwable ignored) {
                return null;
            }
        }

        static int invSize(Object inv) {
            try {
                if (invSize == null) {
                    invSize = inv.getClass().getMethod("size");
                }
                Object r = invSize.invoke(inv);
                return (r instanceof Integer i) ? i : 0;
            } catch (Throwable ignored) {
                return 0;
            }
        }

        static ItemStack getStack(Object inv, int slot) {
            try {
                if (invGetStack == null) {
                    invGetStack = inv.getClass().getMethod("getStack", int.class);
                }
                Object r = invGetStack.invoke(inv, slot);
                return (r instanceof ItemStack s) ? s : ItemStack.EMPTY;
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

        static void setStack(Object inv, int slot, ItemStack stack) {
            try {
                if (invSetStack == null) {
                    invSetStack = inv.getClass().getMethod("setStack", int.class, ItemStack.class);
                }
                invSetStack.invoke(inv, slot, stack);
            } catch (Throwable ignored) {
                // no-op
            }
        }
    }
}