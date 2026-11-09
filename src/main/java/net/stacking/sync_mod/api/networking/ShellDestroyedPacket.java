package net.stacking.sync_mod.api.networking;

import net.stacking.sync_mod.util.WorldUtil;

import net.stacking.sync_mod.Sync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;

public class ShellDestroyedPacket implements ClientPlayerPacket {
    private BlockPos pos;

    // ADDED: Default no-args constructor
    public ShellDestroyedPacket() {
        this(BlockPos.ZERO);
    }

    public ShellDestroyedPacket(BlockPos pos) {
        this.pos = pos == null ? BlockPos.ZERO : pos;
    }

    @Override
    public ResourceLocation getId() {
        return Sync.locate("packet.shell.destroyed");
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public void read(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void execute(Minecraft client, LocalPlayer player, ClientPacketListener handler) {
        for (int i = 0; i < 3; ++i) {
            ((net.minecraft.client.multiplayer.ClientLevel)WorldUtil.level(player)).addDestroyBlockEffect(this.pos, Blocks.DEEPSLATE.defaultBlockState());
            ((net.minecraft.client.multiplayer.ClientLevel)WorldUtil.level(player)).addDestroyBlockEffect(this.pos.above(), Blocks.DEEPSLATE.defaultBlockState());
        }
        ((net.minecraft.client.multiplayer.ClientLevel)WorldUtil.level(player)).playSound(player, this.pos, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1F, player.getVoicePitch());
    }
}