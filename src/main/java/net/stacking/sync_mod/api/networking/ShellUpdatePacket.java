package net.stacking.sync_mod.api.networking;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.shell.Shell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
import java.util.List;

public class ShellUpdatePacket implements ClientPlayerPacket {
    private ResourceLocation worldId;
    private boolean isArtificial;
    private Collection<ShellState> states;

    // ADDED: Default no-args constructor
    public ShellUpdatePacket() {
        this(null, false, List.of());
    }

    public ShellUpdatePacket(ResourceLocation worldId, boolean isArtificial, Collection<ShellState> states) {
        this.worldId = worldId;
        this.isArtificial = isArtificial;
        this.states = states == null ? List.of() : states;
    }

    @Override
    public ResourceLocation getId() {
        return Sync.locate("packet.shell.update");
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.worldId);
        buffer.writeBoolean(this.isArtificial);
        buffer.writeVarInt(this.states.size());
        this.states.forEach(x -> buffer.writeNbt(x.writeNbt(new CompoundTag())));
    }

    @Override
    public void read(FriendlyByteBuf buffer) {
        this.worldId = buffer.readResourceLocation();
        this.isArtificial = buffer.readBoolean();
        this.states = buffer.readList(subBuffer -> ShellState.fromNbt(subBuffer.readNbt()));
    }

    @Override
    public ResourceLocation getTargetWorldId() {
        return this.worldId;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void execute(Minecraft client, LocalPlayer player, ClientPacketListener handler) {
        Shell shell = (Shell)player;
        shell.changeArtificialStatus(this.isArtificial);
        shell.setAvailableShellStates(this.states.stream());
    }
}