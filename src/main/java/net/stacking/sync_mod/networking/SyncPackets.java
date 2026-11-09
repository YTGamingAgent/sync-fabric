package net.stacking.sync_mod.networking;

import net.stacking.sync_mod.api.networking.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public final class SyncPackets {
    /**
     * Registers every packet type. Runs on both the client and the dedicated server,
     * which needs to know server-to-client types in order to send them.
     */
    public static void init() {
        ServerPlayerPacket.register(SynchronizationRequestPacket.class);

        ClientPlayerPacket.register(ShellUpdatePacket.class);
        ClientPlayerPacket.register(ShellStateUpdatePacket.class);
        ClientPlayerPacket.register(SynchronizationResponsePacket.class);
        ClientPlayerPacket.register(PlayerIsAlivePacket.class);
        ClientPlayerPacket.register(ShellDestroyedPacket.class);
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayerPacket.registerReceiver(ShellUpdatePacket.class);
        ClientPlayerPacket.registerReceiver(ShellStateUpdatePacket.class);
        ClientPlayerPacket.registerReceiver(SynchronizationResponsePacket.class);
        ClientPlayerPacket.registerReceiver(PlayerIsAlivePacket.class);
        ClientPlayerPacket.registerReceiver(ShellDestroyedPacket.class);
    }
}
