package net.stacking.sync_mod.api.networking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
*///?}

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A packet sent from the server to a client.
 */
public interface ClientPlayerPacket extends PlayerPacket {
    Map<ResourceLocation, ClientPlayerPacket> REGISTRY = new HashMap<>();

    @Environment(EnvType.CLIENT)
    void execute(Minecraft client, LocalPlayer player, ClientPacketListener handler);

    default ResourceLocation getTargetWorldId() {
        return null;
    }

    default void send(ServerPlayer player) {
        //? if >=1.20.5 {
        ServerPlayNetworking.send(player, new RawPayload(Types.get(this.getId()), this.toBytes()));
        //?} else {
        /*ServerPlayNetworking.send(player, this.getId(), new FriendlyByteBuf(Unpooled.wrappedBuffer(this.toBytes())));
        *///?}
    }

    default void send(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            this.send(player);
        }
    }

    default void sendToAll(Collection<ServerPlayer> players) {
        this.send(players);
    }

    /**
     * Registers the packet type. Must run on both the client and the server (common initializer).
     */
    static <T extends ClientPlayerPacket> void register(Class<T> packetClass) {
        T instance = PlayerPacket.instantiate(packetClass);
        REGISTRY.put(instance.getId(), instance);
        //? if >=1.20.5 {
        CustomPacketPayload.Type<RawPayload> type = Types.create(instance.getId());
        PayloadTypeRegistry./*? if >=26.1 {*//*clientboundPlay()*//*?} else {*/playS2C()/*?}*/.register(type, RawPayload.codec(type));
        //?}
    }

    /**
     * Registers the client-side handler for the packet (client initializer only).
     */
    @Environment(EnvType.CLIENT)
    static <T extends ClientPlayerPacket> void registerReceiver(Class<T> packetClass) {
        ResourceLocation id = PlayerPacket.instantiate(packetClass).getId();
        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(Types.get(id), (payload, context) -> {
            T packet = PlayerPacket.fromBytes(packetClass, payload.data());
            context.client().execute(() -> {
                if (context.player() != null) {
                    packet.execute(context.client(), context.player(), context.client().getConnection());
                }
            });
        });
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, responseSender) -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            T packet = PlayerPacket.fromBytes(packetClass, data);
            client.execute(() -> {
                if (client.player != null) {
                    packet.execute(client, client.player, handler);
                }
            });
        });
        *///?}
    }

    //? if >=1.20.5 {
    final class Types {
        private static final Map<ResourceLocation, CustomPacketPayload.Type<RawPayload>> TYPES = new HashMap<>();

        static CustomPacketPayload.Type<RawPayload> create(ResourceLocation id) {
            return TYPES.computeIfAbsent(id, CustomPacketPayload.Type::new);
        }

        static CustomPacketPayload.Type<RawPayload> get(ResourceLocation id) {
            CustomPacketPayload.Type<RawPayload> type = TYPES.get(id);
            if (type == null) {
                throw new IllegalStateException("Packet type is not registered: " + id);
            }
            return type;
        }

        private Types() { }
    }
    //?}
}
