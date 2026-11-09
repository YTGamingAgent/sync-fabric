package net.stacking.sync_mod.util.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
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

    private static File getPlayerDataFile(ServerLevel overworld, UUID userId) {
        MinecraftServer server = overworld.getServer();
        Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        return playerDataDir.resolve(userId.toString() + ".dat").toFile();
    }

    public static Optional<CompoundTag> getPlayerNbt(MinecraftServer server, UUID userId) {
        File playerDataFolder = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File file = new File(playerDataFolder, userId.toString() + ".dat");

        if (!file.exists() || !file.isFile()) {
            return Optional.empty();
        }

        try {
            //? if >=1.20.3 {
            CompoundTag nbt = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
            //?} else
            /*CompoundTag nbt = NbtIo.readCompressed(file);*/
            return Optional.of(nbt);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    // ADD: savePlayerNbt method
    public static void savePlayerNbt(MinecraftServer server, UUID userId, CompoundTag nbt) {
        File playerDataFolder = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File file = new File(playerDataFolder, userId.toString() + ".dat");

        try {
            //? if >=1.20.3 {
            NbtIo.writeCompressed(nbt, file.toPath());
            //?} else
            /*NbtIo.writeCompressed(nbt, file);*/
        } catch (IOException e) {
            // Log error
        }
    }

    public static CompoundTag readPlayerNbt(ServerLevel overworld, UUID userId) {
        File file = getPlayerDataFile(overworld, userId);
        if (!file.isFile()) {
            return null;
        }
        return null;
    }

    public static void writePlayerNbt(ServerLevel overworld, UUID userId, CompoundTag nbt) {
        File file = getPlayerDataFile(overworld, userId);
        try {
            //? if >=1.20.3 {
            NbtIo.writeCompressed(nbt, file.toPath());
            //?} else
            /*NbtIo.writeCompressed(nbt, file);*/
        } catch (IOException e) {
            throw new RuntimeException("Failed to write offline player nbt for " + userId, e);
        }
    }

    /**
     * Read-modify-write helper used by server mixins.
     */
    public static void editPlayerNbt(MinecraftServer server, UUID userId, Consumer<CompoundTag> mutator) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }

        CompoundTag nbt = readPlayerNbt(overworld, userId);
        if (nbt == null) {
            return;
        }

        mutator.accept(nbt);
        writePlayerNbt(overworld, userId, nbt);
    }
}
