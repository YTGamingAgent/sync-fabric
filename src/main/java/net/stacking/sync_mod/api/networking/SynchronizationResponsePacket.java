package net.stacking.sync_mod.api.networking;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.shell.ClientShell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class SynchronizationResponsePacket implements ClientPlayerPacket {
    private ResourceLocation startWorld;
    private BlockPos startPos;
    private Direction startFacing;
    private ResourceLocation targetWorld;
    private BlockPos targetPos;
    private Direction targetFacing;
    private ShellState storedState;

    public SynchronizationResponsePacket() {
        this(null, BlockPos.ZERO, Direction.NORTH, null, BlockPos.ZERO, Direction.NORTH, null);
    }

    public SynchronizationResponsePacket(
            ResourceLocation startWorld,
            BlockPos startPos,
            Direction startFacing,
            ResourceLocation targetWorld,
            BlockPos targetPos,
            Direction targetFacing,
            @Nullable ShellState storedState
    ) {
        this.startWorld = startWorld;
        this.startPos = startPos;
        this.startFacing = startFacing;
        this.targetWorld = targetWorld;
        this.targetPos = targetPos;
        this.targetFacing = targetFacing;
        this.storedState = storedState;
    }

    @Override
    public ResourceLocation getId() {
        return Sync.locate("packet.shell.synchronization.response");
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.startWorld);
        buffer.writeBlockPos(this.startPos);
        buffer.writeVarInt(this.startFacing.get3DDataValue());
        buffer.writeResourceLocation(this.targetWorld);
        buffer.writeBlockPos(this.targetPos);
        buffer.writeVarInt(this.targetFacing.get3DDataValue());

        if (this.storedState == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            buffer.writeNbt(this.storedState.writeNbt(new CompoundTag()));
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer) {
        this.startWorld = buffer.readResourceLocation();
        this.startPos = buffer.readBlockPos();
        this.startFacing = Direction.from3DDataValue(buffer.readVarInt());
        this.targetWorld = buffer.readResourceLocation();
        this.targetPos = buffer.readBlockPos();
        this.targetFacing = Direction.from3DDataValue(buffer.readVarInt());
        this.storedState = buffer.readBoolean() ? ShellState.fromNbt(buffer.readNbt()) : null;
    }

    @Override
    public ResourceLocation getTargetWorldId() {
        return this.targetWorld;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void execute(Minecraft client, LocalPlayer player, ClientPacketListener handler) {
        ((ClientShell)player).endSync(this.startWorld, this.startPos, this.startFacing, this.targetWorld, this.targetPos, this.targetFacing, this.storedState);
    }
}