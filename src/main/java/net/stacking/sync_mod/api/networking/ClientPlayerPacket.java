package net.stacking.sync_mod.api.networking;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface ClientPlayerPacket extends PlayerPacket {
    Map<Class<? extends ClientPlayerPacket>, ClientPlayerPacket> REGISTRY = new HashMap<>();

    @Environment(EnvType.CLIENT)
    void execute(MinecraftClient client, ClientPlayerEntity player, ClientPlayNetworkHandler handler);

    default Identifier getTargetWorldId() {
        return null;
    }

    default void send(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        this.write(buf);

        // Copy only the readable bytes
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);

        ServerPlayNetworking.send(player, new PayloadWrapper(this.getId(), data));
    }

    default void send(Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            send(player);
        }
    }

    default void sendToAll(Collection<ServerPlayerEntity> players) {
        send(players);
    }

    @SuppressWarnings("unchecked")
    static <T extends ClientPlayerPacket> void register(Class<T> packetClass) {
        try {
            T instance = packetClass.getDeclaredConstructor().newInstance();
            REGISTRY.put(packetClass, instance);

            CustomPayload.Id<PayloadWrapper> payloadId = new CustomPayload.Id<>(instance.getId());

            PayloadTypeRegistry.playS2C().register(
                    payloadId,
                    PacketCodec.of(
                            (wrapper, buf) -> {
                                buf.writeBytes(wrapper.data());
                            },
                            (buf) -> {
                                // Read all remaining bytes
                                byte[] data = new byte[buf.readableBytes()];
                                buf.readBytes(data);
                                return new PayloadWrapper(instance.getId(), data);
                            }
                    )
            );

            ClientPlayNetworking.registerGlobalReceiver(payloadId, (payload, context) -> {
                try {
                    T packet = packetClass.getDeclaredConstructor().newInstance();
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
                    packet.read(buf);

                    context.client().execute(() -> {
                        if (context.player() != null) {
                            packet.execute(context.client(), context.player(), context.client().getNetworkHandler());
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException("Failed to handle packet: " + packetClass.getName(), e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to register packet: " + packetClass.getName(), e);
        }
    }

    record PayloadWrapper(Identifier id, byte[] data) implements CustomPayload {
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return new CustomPayload.Id<>(id);
        }
    }
}