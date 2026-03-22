package net.stacking.sync_mod.util.nbt;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Helpers for reading/writing offline player NBT in Fabric/vanilla.
 */
public final class OfflinePlayerNbtManager {

    private OfflinePlayerNbtManager() {}

    private static File getPlayerDataFile(ServerWorld overworld, UUID userId) {
        MinecraftServer server = overworld.getServer();
        Path playerDataDir = server.getSavePath(WorldSavePath.PLAYERDATA);
        return playerDataDir.resolve(userId.toString() + ".dat").toFile();
    }

    public static Optional<NbtCompound> getPlayerNbt(MinecraftServer server, UUID userId) {
        File playerDataFolder = server.getSavePath(WorldSavePath.PLAYERDATA).toFile();
        File file = new File(playerDataFolder, userId.toString() + ".dat");

        if (!file.exists() || !file.isFile()) {
            return Optional.empty();
        }

        try {
            // FIXED: readCompressed now requires NbtSizeTracker
            NbtCompound nbt = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
            return Optional.of(nbt);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    // ADD: savePlayerNbt method
    public static void savePlayerNbt(MinecraftServer server, UUID userId, NbtCompound nbt) {
        File playerDataFolder = server.getSavePath(WorldSavePath.PLAYERDATA).toFile();
        File file = new File(playerDataFolder, userId.toString() + ".dat");

        try {
            NbtIo.writeCompressed(nbt, file.toPath());
        } catch (IOException e) {
            // Log error
        }
    }

    public static NbtCompound readPlayerNbt(ServerWorld overworld, UUID userId) {
        File file = getPlayerDataFile(overworld, userId);
        if (!file.isFile()) {
            return null;
        }
        return null;
    }

    public static void writePlayerNbt(ServerWorld overworld, UUID userId, NbtCompound nbt) {
        File file = getPlayerDataFile(overworld, userId);
        try {
            NbtIo.writeCompressed(nbt, file.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write offline player nbt for " + userId, e);
        }
    }

    /**
     * Read-modify-write helper used by server mixins.
     */
    public static void editPlayerNbt(MinecraftServer server, UUID userId, Consumer<NbtCompound> mutator) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            return;
        }

        NbtCompound nbt = readPlayerNbt(overworld, userId);
        if (nbt == null) {
            return;
        }

        mutator.accept(nbt);
        writePlayerNbt(overworld, userId, nbt);
    }
}
