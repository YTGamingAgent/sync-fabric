package net.stacking.sync_mod.compat.trinkets;

import net.fabricmc.loader.api.FabricLoader;
import net.stacking.sync_mod.api.shell.ShellStateComponentFactoryRegistry;

/**
 * Optional Trinkets compatibility.
 *
 * We intentionally avoid a hard compile-time dependency on a specific Trinkets API
 * version by using reflection inside {@link TrinketShellStateComponent}. This
 * keeps the mod building across the moving trinkets/accessories ecosystem.
 */
public final class TrinketsCompat {

    private TrinketsCompat() {
    }

    public static void init() {
        // Common mod ids in the ecosystem:
        // - trinkets (dev.emi.trinkets)
        // - accessories (newer replacement)
        // - trinkets-compat-layer (sometimes used on modrinth)
        if (!(FabricLoader.getInstance().isModLoaded("trinkets")
                || FabricLoader.getInstance().isModLoaded("accessories")
                || FabricLoader.getInstance().isModLoaded("trinkets_compat_layer"))) {
            return;
        }

        ShellStateComponentFactoryRegistry.getInstance().register(
                TrinketShellStateComponent::new,
                TrinketShellStateComponent::new
        );
    }
}
