package net.stacking.sync_mod.api.networking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
*///?}

import java.util.HashMap;
import java.util.Map;

/**
 * A packet sent from a client to the server.
 */
public interface ServerPlayerPacket extends PlayerPacket {
    Map<ResourceLocation, ServerPlayerPacket> REGISTRY = new HashMap<>();

    void execute(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler);

    @Environment(EnvType.CLIENT)
    default void send() {
        //? if >=1.20.5 {
        ClientPlayNetworking.send(new RawPayload(Types.get(this.getId()), this.toBytes()));
        //?} else {
        /*ClientPlayNetworking.send(this.getId(), new FriendlyByteBuf(Unpooled.wrappedBuffer(this.toBytes())));
        *///?}
    }

    /**
     * Registers the packet type and its server-side handler (common initializer).
     */
    static <T extends ServerPlayerPacket> void register(Class<T> packetClass) {
        T instance = PlayerPacket.instantiate(packetClass);
        ResourceLocation id = instance.getId();
        REGISTRY.put(id, instance);

        //? if >=1.20.5 {
        CustomPacketPayload.Type<RawPayload> type = Types.create(id);
        PayloadTypeRegistry./*? if >=26.1 {*//*serverboundPlay()*//*?} else {*/playC2S()/*?}*/.register(type, RawPayload.codec(type));
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            T packet = PlayerPacket.fromBytes(packetClass, payload.data());
            context.server().execute(() -> packet.execute(context.server(), context.player(), context.player().connection));
        });
        //?} else {
        /*ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, responseSender) -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            T packet = PlayerPacket.fromBytes(packetClass, data);
            server.execute(() -> packet.execute(server, player, handler));
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
