package net.stacking.sync_mod.api.networking;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public interface ServerPlayerPacket extends PlayerPacket {
    Map<Class<? extends ServerPlayerPacket>, ServerPlayerPacket> REGISTRY = new HashMap<>();

    void execute(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler);

    default void send() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            this.write(buf);

            // Copy only the readable bytes
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);

            ClientPlayNetworking.send(new PayloadWrapper(this.getId(), data));
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends ServerPlayerPacket> void register(Class<T> packetClass) {
        try {
            T instance = packetClass.getDeclaredConstructor().newInstance();
            REGISTRY.put(packetClass, instance);

            CustomPayload.Id<PayloadWrapper> payloadId = new CustomPayload.Id<>(instance.getId());

            PayloadTypeRegistry.playC2S().register(
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

            ServerPlayNetworking.registerGlobalReceiver(payloadId, (payload, context) -> {
                try {
                    T packet = packetClass.getDeclaredConstructor().newInstance();
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
                    packet.read(buf);

                    context.server().execute(() -> {
                        packet.execute(context.server(), context.player(), context.player().networkHandler);
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