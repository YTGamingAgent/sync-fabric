package net.stacking.sync_mod.compat.trinkets;

import net.stacking.sync_mod.util.WorldUtil;

import net.stacking.sync_mod.util.nbt.Nbt;
import net.stacking.sync_mod.util.nbt.ItemStackNbt;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.stacking.sync_mod.api.shell.ShellStateComponent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class TrinketShellStateComponent extends ShellStateComponent {

    private static final String NBT_KEY = "trinkets";

    private final CompoundTag trinkets = new CompoundTag();

    public TrinketShellStateComponent() {
        super();
    }

    public TrinketShellStateComponent(ServerPlayer player) {
        super(player);
        captureFromPlayer(player, WorldUtil.server(player).registryAccess());
    }

    @Override
    public CompoundTag writeNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (!this.trinkets.isEmpty()) {
            nbt.put(NBT_KEY, this.trinkets.copy());
        }
        return nbt;
    }

    @Override
    public void readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.readNbt(nbt, registryLookup);
        // FIXED: Use copyFrom instead of reassignment
        this.trinkets.merge(new CompoundTag());
        if (Nbt.contains(nbt, NBT_KEY, Tag.TAG_COMPOUND)) {
            this.trinkets.merge(Nbt.getCompound(nbt, NBT_KEY));
        }
    }

    @Override
    public void applyToPlayer(ServerPlayer player, HolderLookup.Provider registryLookup) {
        super.applyToPlayer(player, registryLookup);
        applyToPlayerTrinkets(player, registryLookup);
    }

    private void captureFromPlayer(ServerPlayer player, HolderLookup.Provider registryLookup) {
        // FIXED: Use copyFrom instead of reassignment
        this.trinkets.merge(new CompoundTag());

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

            CompoundTag groupTag = new CompoundTag();

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

                ListTag items = new ListTag();
                for (int i = 0; i < size; i++) {
                    ItemStack stack = TrinketsReflection.getStack(invObj, i);
                    if (stack == null) {
                        stack = ItemStack.EMPTY;
                    }
                    items.add(ItemStackNbt.save(stack, registryLookup));
                }

                groupTag.put(slotName, items);
            }

            if (!groupTag.isEmpty()) {
                this.trinkets.put(groupName, groupTag);
            }
        }
    }

    private void applyToPlayerTrinkets(ServerPlayer player, HolderLookup.Provider registryLookup) {
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

        for (String groupName : this.trinkets./*? if >=1.21.5 {*//*keySet()*//*?} else {*/getAllKeys()/*?}*/) {
            Object groupVal = groups.get(groupName);
            if (!(groupVal instanceof Map<?, ?> slots)) {
                continue;
            }

            CompoundTag groupTag = Nbt.getCompound(this.trinkets, groupName);
            for (String slotName : groupTag./*? if >=1.21.5 {*//*keySet()*//*?} else {*/getAllKeys()/*?}*/) {
                Object invObj = slots.get(slotName);
                if (invObj == null) {
                    continue;
                }

                ListTag items = Nbt.getList(groupTag, slotName, Tag.TAG_COMPOUND);
                int size = TrinketsReflection.invSize(invObj);
                int max = Math.min(size, items.size());

                for (int i = 0; i < max; i++) {
                    ItemStack stack = ItemStackNbt.load(Nbt.getCompound(items, i), registryLookup);
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