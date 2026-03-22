package net.stacking.sync_mod.compat.trinkets;

/**
 * Placeholder kept for backwards compatibility.
 *
 * Earlier versions of Sync had a dedicated trinket component for the ShellEntity.
 * The current implementation stores trinket state inside {@link TrinketShellStateComponent}
 * via reflection and does not require a separate component type.
 */
public final class ShellEntityTrinketComponent {
    private ShellEntityTrinketComponent() {
    }
}
