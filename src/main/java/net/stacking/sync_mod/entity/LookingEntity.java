package net.stacking.sync_mod.entity;

public interface LookingEntity {
    default boolean changeLookingEntityLookDirection(double cursorDeltaX, double cursorDeltaY) {
        return false;
    }
}
