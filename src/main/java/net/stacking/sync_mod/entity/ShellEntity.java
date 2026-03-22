package net.stacking.sync_mod.entity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import net.stacking.sync_mod.api.shell.ShellState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Identifier;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
public class ShellEntity extends OtherClientPlayerEntity {
    private static final Cache<UUID, PlayerListEntry> PLAYER_ENTRY_CACHE;

    public boolean isActive;
    public float pitchProgress;
    private final ShellState state;
    private Runnable onInitialized;
    private final PlayerListEntry playerEntry;

    public ShellEntity(ShellState state) {
        this(MinecraftClient.getInstance().world, state);
    }

    public ShellEntity(ClientWorld world, ShellState state) {
        super(world, getPlayerEntry(state).getProfile());
        this.isActive = false;
        this.pitchProgress = 0;
        this.state = state;
        state.getInventory().copyTo(this.getInventory());
        this.playerEntry = getPlayerEntry(state);
        this.refreshPositionAndAngles(state.getPos(), 0, 0);
        this.prevCapeX = this.capeX = getX() + 0.5;
        this.prevCapeY = this.capeY = getY();
        this.prevCapeZ = this.capeZ = getZ() + 0.5;

        if (this.onInitialized != null) {
            this.onInitialized.run();
            this.onInitialized = null;
        }
    }

    public void onInitialized(Runnable runnable) {
        if (this.state == null) {
            this.onInitialized = runnable;
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public ShellState getState() {
        return this.state;
    }

    public String getModel() {
        // In 1.21, getSkinTextures().model() returns a Model object
        // Model.getName() returns "default" or "slim"
        return this.playerEntry.getSkinTextures().model().getName();
    }

    public Identifier getSkinTexture() {
        return this.playerEntry.getSkinTextures().texture();
    }

    @Override
    protected void dropInventory() {
    }

    @Override
    public boolean isCreative() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }

    @Override
    public boolean isPartVisible(PlayerModelPart modelPart) {
        boolean isWideModel = "default".equals(this.getModel());

        return isWideModel
                || modelPart == PlayerModelPart.LEFT_SLEEVE
                || modelPart == PlayerModelPart.RIGHT_SLEEVE
                || modelPart == PlayerModelPart.LEFT_PANTS_LEG
                || modelPart == PlayerModelPart.RIGHT_PANTS_LEG
                || modelPart == PlayerModelPart.JACKET
                || modelPart == PlayerModelPart.HAT;
    }

    @Override
    protected PlayerListEntry getPlayerListEntry() {
        return this.playerEntry;
    }

    private static PlayerListEntry getPlayerEntry(ShellState state) {
        PlayerListEntry entry = PLAYER_ENTRY_CACHE.getIfPresent(state.getOwnerUuid());
        if (entry == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            if (networkHandler != null) {
                entry = networkHandler.getPlayerListEntry(state.getOwnerUuid());
                if (entry == null) {
                    entry = networkHandler.getPlayerListEntry(state.getOwnerName());
                }
            }

            if (entry == null) {
                entry = new PlayerListEntry(new GameProfile(state.getOwnerUuid(), state.getOwnerName()), false);
            }

            PLAYER_ENTRY_CACHE.put(state.getOwnerUuid(), entry);
        }
        return entry;
    }

    static {
        PLAYER_ENTRY_CACHE = CacheBuilder.newBuilder()
                .initialCapacity(20)
                .maximumSize(40)
                .expireAfterAccess(20, TimeUnit.MINUTES)
                .build();
    }
}