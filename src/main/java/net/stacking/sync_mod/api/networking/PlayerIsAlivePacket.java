package net.stacking.sync_mod.api.networking;

import net.stacking.sync_mod.Sync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.UUID;

public class PlayerIsAlivePacket implements ClientPlayerPacket {
    private UUID playerUuid;

    // ADDED: Default no-args constructor
    public PlayerIsAlivePacket() {
        this(Util.NIL_UUID);
    }

    public PlayerIsAlivePacket(PlayerEntity player) {
        this(player == null ? null : player.getUuid());
    }

    public PlayerIsAlivePacket(UUID playerUuid) {
        this.playerUuid = playerUuid == null ? Util.NIL_UUID : playerUuid;
    }

    @Override
    public Identifier getId() {
        return Sync.locate("packet.shell.alive");
    }

    @Override
    public void write(PacketByteBuf buffer) {
        buffer.writeUuid(this.playerUuid);
    }

    @Override
    public void read(PacketByteBuf buffer) {
        this.playerUuid = buffer.readUuid();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void execute(MinecraftClient client, ClientPlayerEntity player, ClientPlayNetworkHandler handler) {
        PlayerEntity updatedPlayer = player.clientWorld.getPlayerByUuid(this.playerUuid);
        if (updatedPlayer != null) {
            if (updatedPlayer.getHealth() <= 0) {
                updatedPlayer.setHealth(0.01F);
            }
            updatedPlayer.deathTime = 0;
        }
    }
}