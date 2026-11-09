package net.stacking.sync_mod.api.networking;

import net.stacking.sync_mod.util.ChatUtil;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.util.BlockPosUtil;
import net.stacking.sync_mod.api.shell.ServerShell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.util.WorldUtil;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import java.util.Objects;
import java.util.UUID;

public class SynchronizationRequestPacket implements ServerPlayerPacket {
    private UUID shellUuid;

    public SynchronizationRequestPacket() {
        this((UUID) null);
    }

    public SynchronizationRequestPacket(ShellState shell) {
        this.shellUuid = shell == null ? null : shell.getUuid();
    }

    public SynchronizationRequestPacket(UUID shellUuid) {
        this.shellUuid = shellUuid;
    }

    @Override
    public ResourceLocation getId() {
        return Sync.locate("packet.shell.synchronization.request");
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        if (this.shellUuid == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            buffer.writeUUID(this.shellUuid);
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer) {
        this.shellUuid = buffer.readBoolean() ? buffer.readUUID() : null;
    }

    @Override
    public void execute(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler) {
        ServerShell shell = (ServerShell)player;
        ShellState state = shell.getShellStateByUuid(this.shellUuid);

        BlockPos currentPos = player.blockPosition();
        Level currentWorld = WorldUtil.level(player);
        ResourceLocation currentWorldId = WorldUtil.getId(currentWorld);
        Direction currentFacing = BlockPosUtil.getHorizontalFacing(currentPos, currentWorld).orElse(player.getDirection().getOpposite());

        shell.sync(state).ifLeft(storedState -> {
            Objects.requireNonNull(state);
            ResourceLocation targetWorldId = state.getWorld();
            BlockPos targetPos = state.getPos();
            Direction targetFacing = player.getDirection().getOpposite();
            new SynchronizationResponsePacket(currentWorldId, currentPos, currentFacing, targetWorldId, targetPos, targetFacing, storedState).send(player);
        }).ifRight(failureReason -> {
            ChatUtil.message(player, failureReason.toText(), false);
            new SynchronizationResponsePacket(currentWorldId, currentPos, currentFacing, currentWorldId, currentPos, currentFacing, null).send(player);
        });
    }
}