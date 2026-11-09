package net.stacking.sync_mod.util;

import net.minecraft.world.entity.Entity;

/**
 * Entity helpers whose vanilla names changed between versions.
 */
public final class EntityUtil {
    /**
     * Moves an entity to a position and rotation immediately (no interpolation).
     */
    public static void moveTo(Entity entity, double x, double y, double z, float yaw, float pitch) {
        //? if >=1.21.5 {
        /*entity.snapTo(x, y, z, yaw, pitch);
        *///?} else
        entity.moveTo(x, y, z, yaw, pitch);
    }

    /**
     * Like {@link #moveTo}, but without updating the previous position (used for per-tick pinning).
     */
    public static void absMoveTo(Entity entity, double x, double y, double z, float yaw, float pitch) {
        //? if >=1.21.5 {
        /*entity.absSnapTo(x, y, z, yaw, pitch);
        *///?} else
        entity.absMoveTo(x, y, z, yaw, pitch);
    }

    private EntityUtil() { }
}
