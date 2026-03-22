package net.stacking.sync_mod.api.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Base interface for packets in the Sync mod.
 * Provides common functionality for all packet types.
 */
public interface PlayerPacket {
    Identifier getId();

    void write(PacketByteBuf buffer);

    void read(PacketByteBuf buffer);
}