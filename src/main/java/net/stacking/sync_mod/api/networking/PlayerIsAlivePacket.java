package net.stacking.sync_mod.api.networking;

import net.stacking.sync_mod.util.WorldUtil;

import net.stacking.sync_mod.Sync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

public class PlayerIsAlivePacket implements ClientPlayerPacket {
    private UUID playerUuid;

    // ADDED: Default no-args constructor
    public PlayerIsAlivePacket() {
        this(Util.NIL_UUID);
    }

    public PlayerIsAlivePacket(Player player) {
        this(player == null ? null : player.getUUID());
    }

    public PlayerIsAlivePacket(UUID playerUuid) {
        this.playerUuid = playerUuid == null ? Util.NIL_UUID : playerUuid;
    }

    @Override
    public ResourceLocation getId() {
        return Sync.locate("packet.shell.alive");
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.playerUuid);
    }

    @Override
    public void read(FriendlyByteBuf buffer) {
        this.playerUuid = buffer.readUUID();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void execute(Minecraft client, LocalPlayer player, ClientPacketListener handler) {
        Player updatedPlayer = ((net.minecraft.client.multiplayer.ClientLevel)WorldUtil.level(player)).getPlayerByUUID(this.playerUuid);
        if (updatedPlayer != null) {
            if (updatedPlayer.getHealth() <= 0) {
                updatedPlayer.setHealth(0.01F);
            }
            updatedPlayer.deathTime = 0;
        }
    }
}