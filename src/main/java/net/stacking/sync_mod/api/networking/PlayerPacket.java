package net.stacking.sync_mod.api.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/**
 * Base interface for packets in the Sync mod.
 * Packets serialize themselves to raw bytes; the version-specific transport lives in
 * {@link ClientPlayerPacket} and {@link ServerPlayerPacket}.
 */
public interface PlayerPacket {
    ResourceLocation getId();

    void write(FriendlyByteBuf buffer);

    void read(FriendlyByteBuf buffer);

    default byte[] toBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        this.write(buf);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    static <T extends PlayerPacket> T fromBytes(Class<T> packetClass, byte[] data) {
        try {
            T packet = packetClass.getDeclaredConstructor().newInstance();
            packet.read(new FriendlyByteBuf(Unpooled.wrappedBuffer(data)));
            return packet;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to read packet: " + packetClass.getName(), e);
        }
    }

    static <T extends PlayerPacket> T instantiate(Class<T> packetClass) {
        try {
            return packetClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create packet: " + packetClass.getName(), e);
        }
    }

    //? if >=1.20.5 {
    /**
     * The packet's bytes, wrapped as a vanilla custom payload (1.20.5+ networking).
     */
    record RawPayload(CustomPacketPayload.Type<RawPayload> type, byte[] data) implements CustomPacketPayload {
        public static StreamCodec<FriendlyByteBuf, RawPayload> codec(CustomPacketPayload.Type<RawPayload> type) {
            return StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data()),
                buf -> {
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    return new RawPayload(type, data);
                }
            );
        }
    }
    //?}
}
