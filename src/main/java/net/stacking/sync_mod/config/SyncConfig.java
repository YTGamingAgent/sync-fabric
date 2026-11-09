package net.stacking.sync_mod.config;

import net.stacking.sync_mod.api.shell.ShellPriority;
import net.stacking.sync_mod.compat.cloth.SyncClothConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import java.util.List;

public interface SyncConfig {
    default boolean isValuable(ItemStack itemStack) {
        return itemStack.getRarity().ordinal() >= Rarity.RARE.ordinal();
    }

    List<ShellPriorityEntry> DEFAULT_SYNC_PRIORITY = List.of(new ShellPriorityEntry() { });

    static SyncConfig resolve() {
        return FabricLoader.getInstance().isModLoaded("cloth-config") ? SyncClothConfig.getInstance() : new SyncConfig() { };
    }

    default boolean enableInstantShellConstruction() {
        return false;
    }

    default boolean warnPlayerInsteadOfKilling() {
        return false;
    }

    default float fingerstickDamage() {
        return 0.5F;
    }

    default float hardcoreFingerstickDamage() {
        return 2F;
    }

    default long shellStorageCapacity() {
        return 320;
    }

    default long shellStorageConsumption() {
        return 16;
    }

    default boolean shellStorageAcceptsRedstone() {
        return true;
    }

    default int shellStorageMaxUnpoweredLifespan() {
        return 20;
    }

    default List<ShellPriorityEntry> syncPriority() {
        return DEFAULT_SYNC_PRIORITY;
    }

    default String wrench() {
        return "minecraft:stick";
    }

    default boolean updateTranslationsAutomatically() {
        return false;
    }

    default boolean preserveOrigins() {
        return false;
    }

    interface ShellPriorityEntry {
        default ShellPriority priority() {
            return ShellPriority.NATURAL;
        }
    }
}