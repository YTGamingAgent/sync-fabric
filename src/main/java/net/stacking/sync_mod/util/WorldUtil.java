package net.stacking.sync_mod.util;

import java.util.Optional;
import java.util.stream.StreamSupport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public final class WorldUtil {
    /**
     * The level (world) an entity is in. Entity#level() was called getLevel() before 1.20.
     */
    public static Level level(Entity entity) {
        //? if >=1.20 {
        return entity.level();
        //?} else
        /*return entity.getLevel();*/
    }

    /**
     * Game rules for the (server-side) level an entity is in.
     */
    public static GameRules gameRules(Entity entity) {
        //? if >=1.21.2 {
        /*return ((ServerLevel)level(entity)).getGameRules();
        *///?} else
        return level(entity).getGameRules();
    }

    public static MinecraftServer server(Entity entity) {
        return level(entity).getServer();
    }

    public static ResourceLocation getId(Level world) {
        return world.dimension().location();
    }

    public static boolean isOf(ResourceLocation id, Level world) {
        return world.dimension().location().equals(id);
    }

    public static <T extends Level> Optional<T> findWorld(Iterable<T> world, ResourceLocation id) {
        return StreamSupport.stream(world.spliterator(), false).filter(x -> isOf(id, x)).findAny();
    }
}