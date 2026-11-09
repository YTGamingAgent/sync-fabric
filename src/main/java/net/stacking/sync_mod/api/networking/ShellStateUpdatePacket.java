package net.stacking.sync_mod.api.networking;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.shell.Shell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.api.shell.ShellStateUpdateType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import java.util.UUID;

public class ShellStateUpdatePacket implements ClientPlayerPacket {
    private ShellStateUpdateType type;
    private ShellState shellState;
    private UUID uuid;
    private float progress;
    private DyeColor color;
    private BlockPos pos;

    // ADDED: Default no-args constructor
    public ShellStateUpdatePacket() {
        this(ShellStateUpdateType.NONE, null);
    }

    public ShellStateUpdatePacket(ShellStateUpdateType type, ShellState shellState) {
        this.type = type;
        this.shellState = shellState;
    }

    @Override
    public ResourceLocation getId() {
        return Sync.locate("packet.shell.state.update");
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        if (this.shellState == null && this.type != ShellStateUpdateType.NONE) {
            throw new IllegalStateException();
        }

        buffer.writeEnum(type);
        switch (type) {
            case ADD:
                buffer.writeNbt(this.shellState.writeNbt(new CompoundTag()));
                break;

            case REMOVE:
                buffer.writeUUID(this.shellState.getUuid());
                break;

            case UPDATE:
                buffer.writeUUID(this.shellState.getUuid());
                buffer.writeVarInt((int) (this.shellState.getProgress() * 100));
                buffer.writeVarInt(this.shellState.getColor() == null ? Byte.MAX_VALUE : this.shellState.getColor().getId());
                buffer.writeBlockPos(this.shellState.getPos());
                break;

            default:
                break;
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer) {
        this.type = buffer.readEnum(ShellStateUpdateType.class);
        switch (this.type) {
            case ADD:
                this.shellState = ShellState.fromNbt(buffer.readNbt());
                break;

            case REMOVE:
                this.uuid = buffer.readUUID();
                break;

            case UPDATE:
                this.uuid = buffer.readUUID();
                this.progress = Mth.clamp(buffer.readVarInt() / 100F, 0F, 1F);
                int colorId = buffer.readVarInt();
                this.color = colorId < 0 || colorId > 15 ? null : DyeColor.byId(colorId);
                this.pos = buffer.readBlockPos();
                break;

            default:
                break;
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void execute(Minecraft client, LocalPlayer player, ClientPacketListener handler) {
        Shell shell = (Shell) player;
        if (shell == null) {
            return;
        }

        ShellState state;
        switch (this.type) {
            case ADD:
                shell.add(this.shellState);
                break;

            case REMOVE:
                state = shell.getShellStateByUuid(this.uuid);
                if (state != null) {
                    shell.remove(state);
                }
                break;

            case UPDATE:
                state = shell.getShellStateByUuid(this.uuid);
                if (state != null) {
                    state.setProgress(this.progress);
                    state.setColor(this.color);
                    state.setPos(this.pos);
                }
                break;

            default:
                break;
        }
    }
}