package net.stacking.sync_mod.entity;

public interface KillableEntity {
    default void onKillableEntityDeath() { }

    default boolean updateKillableEntityPostDeath() {
        return false;
    }
}
