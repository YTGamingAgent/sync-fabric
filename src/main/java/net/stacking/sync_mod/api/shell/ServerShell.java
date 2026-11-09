package net.stacking.sync_mod.api.shell;

import com.mojang.datafixers.util.Either;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-side version of the {@link Shell}.
 */
public interface ServerShell extends Shell {
    @Override
    default boolean isClient() {
        return false;
    }

    /**
     * Synchronizes the shell with the given state.
     *
     * @param state Target state.
     * @return New state that was generated during the sync process, if it was successful; otherwise, failure reason is returned.
     */
    Either<ShellState, PlayerSyncEvents.SyncFailureReason> sync(ShellState state);

    /**
     * Modifies the internal state of the shell to match the specified one.
     *
     * @param state Target state.
     */
    void apply(ShellState state);

    /**
     * @return Location of the player's primary shell storage (where their original body is parked while they use a shell), if any.
     */
    @Nullable
    GlobalPos getPrimaryShellStorage();

    /**
     * Sets the location of the player's primary shell storage.
     *
     * @param pos Location of the storage, or {@code null} to clear it.
     */
    void setPrimaryShellStorage(@Nullable GlobalPos pos);


    /**
     * Returns a shell with the given uuid.
     *
     * @param server The server.
     * @param uuid The uuid of the target shell.
     * @return Shell with the given uuid.
     */
    static Optional<ServerShell> getByUuid(MinecraftServer server, UUID uuid) {
        return Optional.ofNullable((ServerShell)server.getPlayerList().getPlayer(uuid));
    }

    /**
     * Returns a shell with the given name.
     *
     * @param server The server.
     * @param name The name of the target shell.
     * @return Shell with the given name.
     */
    static Optional<ServerShell> getByName(MinecraftServer server, String name) {
        return Optional.ofNullable((ServerShell)server.getPlayerList().getPlayerByName(name));
    }
}
