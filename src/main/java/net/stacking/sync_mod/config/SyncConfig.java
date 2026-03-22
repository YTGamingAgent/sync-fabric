package net.stacking.sync_mod.config;

import net.minecraft.item.ItemStack;
import net.stacking.sync_mod.api.shell.ShellPriority;
import net.stacking.sync_mod.compat.cloth.SyncClothConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Rarity;  // FIXED: Changed from net.minecraft.item.Rarity
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;

public interface SyncConfig {
    List<EnergyMapEntry> DEFAULT_ENERGY_MAP = List.of(
            EnergyMapEntry.of(EntityType.CHICKEN, 2),
            EnergyMapEntry.of(EntityType.PIG, 16),
            EnergyMapEntry.of(EntityType.PLAYER, 20),
            EnergyMapEntry.of(EntityType.WOLF, 24),
            EnergyMapEntry.of(EntityType.CREEPER, 80),
            EnergyMapEntry.of(EntityType.ENDERMAN, 160)
    );

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
        return 20F;
    }

    default float hardcoreFingerstickDamage() {
        return 40F;
    }

    default long shellConstructorCapacity() {
        return 256000;
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

    default List<EnergyMapEntry> energyMap() {
        return DEFAULT_ENERGY_MAP;
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

    interface EnergyMapEntry {
        default String entityId() {
            return "minecraft:pig";
        }

        default long outputEnergyQuantity() {
            return 16;
        }

        default EntityType<?> getEntityType() {
            Identifier id = Identifier.tryParse(this.entityId());
            return id == null ? EntityType.PIG : Registries.ENTITY_TYPE.get(id);
        }

        static EnergyMapEntry of(EntityType<?> entityType, long outputEnergyQuantity) {
            return of(Registries.ENTITY_TYPE.getId(entityType).toString(), outputEnergyQuantity);
        }

        static EnergyMapEntry of(String id, long outputEnergyQuantity) {
            return new EnergyMapEntry() {
                @Override
                public String entityId() {
                    return id;
                }

                @Override
                public long outputEnergyQuantity() {
                    return outputEnergyQuantity;
                }
            };
        }
    }

    interface ShellPriorityEntry {
        default ShellPriority priority() {
            return ShellPriority.NATURAL;
        }
    }
}