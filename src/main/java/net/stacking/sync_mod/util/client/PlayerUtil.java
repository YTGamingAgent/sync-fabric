package net.stacking.sync_mod.util.client;

import com.google.common.collect.Queues;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.util.WorldUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@Environment(EnvType.CLIENT)
public final class PlayerUtil {
    private static final ResourceLocation ANY_WORLD = Sync.locate("any_world");
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final ConcurrentMap<ResourceLocation, ConcurrentLinkedQueue<PlayerUpdate>> UPDATES = new ConcurrentHashMap<>();

    public static void recordPlayerUpdate(PlayerUpdate playerUpdate) {
        recordPlayerUpdate(null, playerUpdate);
    }

    public static void recordPlayerUpdate(ResourceLocation worldId, PlayerUpdate playerUpdate) {
        worldId = worldId == null ? ANY_WORLD : worldId;

        if (CLIENT.player != null && existsInTargetWorld(CLIENT.player, worldId)) {
            playerUpdate.onLoad(CLIENT.player, ((net.minecraft.client.multiplayer.ClientLevel)WorldUtil.level(CLIENT.player)), CLIENT);
        } else {
            UPDATES.computeIfAbsent(worldId, id -> Queues.newConcurrentLinkedQueue()).add(playerUpdate);
        }
    }

    private static boolean existsInTargetWorld(Entity entity, ResourceLocation worldId) {
        return worldId == ANY_WORLD || WorldUtil.isOf(worldId, WorldUtil.level(entity));
    }

    private static void executeUpdates(LocalPlayer player, ClientLevel world, ConcurrentLinkedQueue<PlayerUpdate> queue) {
        if (queue == null) {
            return;
        }

        while (!queue.isEmpty()) {
            queue.poll().onLoad(player, world, CLIENT);
        }
    }

    static {
        ClientEntityEvents.ENTITY_LOAD.register((Entity entity, ClientLevel world) -> {
            if (entity == CLIENT.player) {
                executeUpdates(CLIENT.player, world, UPDATES.get(WorldUtil.getId(world)));
                executeUpdates(CLIENT.player, world, UPDATES.get(ANY_WORLD));
            }
        });
    }

    @FunctionalInterface
    public interface PlayerUpdate {
        void onLoad(LocalPlayer player, ClientLevel world, Minecraft client);
    }
}