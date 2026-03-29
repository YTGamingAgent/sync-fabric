package net.stacking.sync_mod.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.api.networking.PlayerIsAlivePacket;
import net.stacking.sync_mod.api.networking.ShellStateUpdatePacket;
import net.stacking.sync_mod.api.networking.ShellUpdatePacket;
import net.stacking.sync_mod.api.shell.*;
import net.stacking.sync_mod.block.entity.ShellStorageBlockEntity;
import net.stacking.sync_mod.entity.KillableEntity;
import net.stacking.sync_mod.util.BlockPosUtil;
import net.stacking.sync_mod.util.WorldUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerEntityMixin extends PlayerEntity implements ServerShell, KillableEntity {
    @Shadow
    private int syncedExperience;

    @Shadow
    private float syncedHealth;

    @Shadow
    private int syncedFoodLevel;

    @Final
    @Shadow
    public MinecraftServer server;

    @Shadow
    public ServerPlayNetworkHandler networkHandler;

    @Unique
    private boolean isArtificial = false;

    @Unique
    private boolean shellDirty = false;

    @Unique
    private boolean undead = false;

    @Unique
    private ConcurrentMap<UUID, ShellState> shellsById = new ConcurrentHashMap<>();

    @Unique
    private Map<UUID, Pair<ShellStateUpdateType, ShellState>> shellStateChanges = new ConcurrentHashMap<>();


    private ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }


    @Override
    public UUID getShellOwnerUuid() {
        return this.getGameProfile().getId();
    }

    @Override
    public boolean isArtificial() {
        return this.isArtificial;
    }

    @Override
    public void changeArtificialStatus(boolean isArtificial) {
        if (this.isArtificial != isArtificial) {
            this.isArtificial = isArtificial;
            this.shellDirty = true;
        }
    }

    @Override
    public Stream<ShellState> getAvailableShellStates() {
        return this.shellsById.values().stream();
    }

    @Override
    public void setAvailableShellStates(Stream<ShellState> states) {
        this.shellsById = states.collect(Collectors.toConcurrentMap(ShellState::getUuid, x -> x));
        this.shellDirty = true;
    }

    @Override
    public ShellState getShellStateByUuid(UUID uuid) {
        return uuid == null ? null : this.shellsById.get(uuid);
    }

    @Override
    public void add(ShellState state) {
        if (!this.canBeApplied(state)) {
            return;
        }

        this.shellsById.put(state.getUuid(), state);
        this.shellStateChanges.put(state.getUuid(), new Pair<>(ShellStateUpdateType.ADD, state));
    }

    @Override
    public void remove(ShellState state) {
        if (state == null) {
            return;
        }

        if (this.shellsById.remove(state.getUuid()) != null) {
            this.shellStateChanges.put(state.getUuid(), new Pair<>(ShellStateUpdateType.REMOVE, state));
        }
    }

    @Override
    public void update(ShellState state) {
        if (state == null) {
            return;
        }

        boolean updated;
        if (this.canBeApplied(state)) {
            updated = this.shellsById.put(state.getUuid(), state) != null;
        } else {
            updated = this.shellsById.computeIfPresent(state.getUuid(), (a, b) -> state) != null;
        }
        this.shellStateChanges.put(state.getUuid(), new Pair<>(updated ? ShellStateUpdateType.UPDATE : ShellStateUpdateType.ADD, state));
    }

    @Inject(method = "playerTick", at = @At("HEAD"))
    private void playerTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        if (this.shellDirty) {
            this.shellDirty = false;
            this.shellStateChanges.clear();
            new ShellUpdatePacket(WorldUtil.getId(this.getWorld()), this.isArtificial, this.shellsById.values()).send(player);
        }

        for (Pair<ShellStateUpdateType, ShellState> upd : this.shellStateChanges.values()) {
            new ShellStateUpdatePacket(upd.getLeft(), upd.getRight()).send(player);
        }
        this.shellStateChanges.clear();
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onDeath(DamageSource source, CallbackInfo ci) {
        if (!this.isArtificial) {
            return;
        }

        ShellState respawnShell = this.shellsById.values().stream()
                .filter(x -> this.canBeApplied(x) && x.getProgress() >= ShellState.PROGRESS_DONE)
                .findAny()
                .orElse(null);

        if (respawnShell == null) {
            return;
        }

        if (this.getWorld().getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES)) {
            this.sendDeathMessageInChat();
        } else {
            this.sendEmptyDeathMessageInChat();
        }

        if (this.getWorld().getGameRules().getBoolean(GameRules.FORGIVE_DEAD_PLAYERS)) {
            this.forgiveMobAnger();
        }

        if (!this.isSpectator()) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            this.drop(serverWorld, source);
        }

        this.undead = true;
        ci.cancel();
    }

    @Override
    public boolean updateKillableEntityPostDeath() {
        this.deathTime = MathHelper.clamp(++this.deathTime, 0, 20);

        if (this.isArtificial && this.shellsById.values().stream()
                .anyMatch(x -> this.canBeApplied(x) && x.getProgress() >= ShellState.PROGRESS_DONE)) {
            return true;
        }

        if (this.undead) {
            this.onDeath(getWorld().getDamageSources().magic());
            this.undead = false;
        }

        if (this.deathTime == 20) {
            this.getWorld().sendEntityStatus(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
        return true;
    }

    @Unique
    private void sendDeathMessageInChat() {
        Text text = this.getDamageTracker().getDeathMessage();
        this.networkHandler.sendPacket(new DeathMessageS2CPacket(this.getId(), text));

        AbstractTeam abstractTeam = this.getScoreboardTeam();
        if (abstractTeam != null && abstractTeam.getDeathMessageVisibilityRule() != AbstractTeam.VisibilityRule.ALWAYS) {
            if (abstractTeam.getDeathMessageVisibilityRule() == AbstractTeam.VisibilityRule.HIDE_FOR_OTHER_TEAMS) {
                this.server.getPlayerManager().sendToTeam(this, text);
            } else if (abstractTeam.getDeathMessageVisibilityRule() == AbstractTeam.VisibilityRule.HIDE_FOR_OWN_TEAM) {
                this.server.getPlayerManager().sendToOtherTeams(this, text);
            }
        } else {
            this.server.getPlayerManager().broadcast(text, false);
        }
    }

    @Unique
    private void sendEmptyDeathMessageInChat() {
        this.networkHandler.sendPacket(new DeathMessageS2CPacket(this.getId(), Text.empty()));
    }

    @Override
    public Either<ShellState, PlayerSyncEvents.SyncFailureReason> sync(ShellState targetState) {
        if (!this.canBeApplied(targetState)) {
            return Either.right(PlayerSyncEvents.SyncFailureReason.INVALID_SHELL);
        }

        if (targetState.getProgress() < ShellState.PROGRESS_DONE) {
            return Either.right(PlayerSyncEvents.SyncFailureReason.INVALID_SHELL);
        }

        PlayerSyncEvents.SyncFailureReason failureReason = PlayerSyncEvents.ALLOW_SYNCING.invoker().allowSync(this, targetState);
        if (failureReason != null) {
            return Either.right(failureReason);
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) (Object) this;
        ServerWorld targetWorld = this.server.getWorld(targetState.getWorld() != null
                ? this.server.getWorldRegistryKeys().stream()
                  .filter(key -> key.getValue().equals(targetState.getWorld()))
                  .findFirst()
                  .orElse(this.getServerWorld().getRegistryKey())
                : this.getServerWorld().getRegistryKey());

        if (targetWorld == null) {
            return Either.right(PlayerSyncEvents.SyncFailureReason.OTHER_PROBLEM);
        }

        ShellState storedState = ShellState.copy(serverPlayer, targetState.getPos());
        storedState.setProgress(ShellState.PROGRESS_START);

        this.stopRiding();
        this.extinguish();
        this.setFrozenTicks(0);
        this.setOnFire(false);
        this.clearStatusEffects();

        new PlayerIsAlivePacket(serverPlayer).send(this.server.getPlayerManager().getPlayerList());
        this.setWorld(targetWorld);
        this.setPos(targetState.getPos().getX() + 0.5, targetState.getPos().getY(), targetState.getPos().getZ() + 0.5);

        // Set yaw to face out through shell storage doors
        BlockEntity storageEntity = targetWorld.getBlockEntity(targetState.getPos());
        if (storageEntity instanceof ShellStorageBlockEntity storage) {
            Direction facing = storage.getCachedState().get(net.minecraft.state.property.Properties.FACING);
            float yaw = facing.asRotation();
            this.setYaw(yaw);
            this.setPitch(0);
        }

        this.isArtificial = targetState.isArtificial();

        if (!targetState.isArtificial()) {
            this.setInvisible(true);
            this.setNoGravity(true);
        }
        storedState.setHealth(0.01f);

        PlayerInventory inventory = this.getInventory();
        int selectedSlot = inventory.selectedSlot;
        targetState.getInventory().copyTo(inventory);
        inventory.selectedSlot = selectedSlot;

        ShellStateComponent targetComponent = targetState.getComponent();
        ShellStateComponent playerComponent = ShellStateComponent.of(serverPlayer);

        serverPlayer.changeGameMode(targetState.getGameMode());
        this.setHealth(targetState.getHealth());
        this.experienceLevel = targetState.getExperienceLevel();
        this.experienceProgress = targetState.getExperienceProgress();
        this.totalExperience = targetState.getTotalExperience();
        this.getHungerManager().setFoodLevel(targetState.getFoodLevel());
        this.getHungerManager().setSaturationLevel(targetState.getSaturationLevel());
        this.getHungerManager().setExhaustion(targetState.getExhaustion());

        this.undead = false;
        this.dead = false;
        this.deathTime = 0;
        this.fallDistance = 0;
        this.syncedExperience = -1;
        this.syncedHealth = -1;
        this.syncedFoodLevel = -1;
        this.shellDirty = true;

        ServerWorld currentWorld = this.getServerWorld();
        ShellStorageBlockEntity primaryStorage = null;

        BlockPos playerPos = serverPlayer.getBlockPos();
        int searchRadius = 128;

        for (int x = playerPos.getX() - searchRadius; x <= playerPos.getX() + searchRadius; x += 16) {
            for (int y = playerPos.getY() - searchRadius; y <= playerPos.getY() + searchRadius; y += 16) {
                for (int z = playerPos.getZ() - searchRadius; z <= playerPos.getZ() + searchRadius; z += 16) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockEntity blockEntity = currentWorld.getBlockEntity(checkPos);
                    if (blockEntity instanceof ShellStorageBlockEntity storage && storage.isPrimaryStorage()) {
                        primaryStorage = storage;
                        break;
                    }
                }
                if (primaryStorage != null) break;
            }
            if (primaryStorage != null) break;
        }

        if (primaryStorage != null) {
            primaryStorage.setShellState(storedState);
            primaryStorage.sync();  // Explicitly sync to persist the stored body to disk
            this.add(storedState);
        }

        ShellStateContainer constructorContainer = ShellStateContainer.find(currentWorld, targetState);
        if (constructorContainer != null) {
            constructorContainer.setShellState(null);
            if (constructorContainer instanceof ShellStorageBlockEntity storage) {
                storage.sync();  // Persist the cleared state
            }
            this.remove(targetState);
        } else {
            // Fallback: remove from shells list directly
            this.remove(targetState);
        }
        return Either.left(storedState);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        NbtList shellList = new NbtList();
        this.shellsById.values().stream()
                .map(x -> x.writeNbt(new NbtCompound()))
                .forEach(shellList::add);

        nbt.putBoolean("IsArtificial", this.isArtificial);
        nbt.put("Shells", shellList);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        this.isArtificial = nbt.getBoolean("IsArtificial");
        this.shellsById = nbt.getList("Shells", NbtElement.COMPOUND_TYPE)
                .stream()
                .map(x -> ShellState.fromNbt((NbtCompound) x))
                .collect(Collectors.toConcurrentMap(ShellState::getUuid, x -> x));

        Collection<Pair<ShellStateUpdateType, ShellState>> updates = ((ShellStateManager) this.server).popPendingUpdates(this.uuid);
        for (Pair<ShellStateUpdateType, ShellState> update : updates) {
            ShellState state = update.getRight();
            switch (update.getLeft()) {
                case ADD, UPDATE -> {
                    if (this.canBeApplied(state)) {
                        this.shellsById.put(state.getUuid(), state);
                    }
                }
                case REMOVE -> this.shellsById.remove(state.getUuid());
            }
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        ServerShell oldShell = (ServerShell) oldPlayer;
        this.shellsById = oldShell.getAvailableShellStates().collect(Collectors.toConcurrentMap(ShellState::getUuid, x -> x));
        this.isArtificial = oldShell.isArtificial();
    }

    @Shadow
    protected abstract void forgiveMobAnger();

    @Shadow
    protected abstract void worldChanged(ServerWorld origin);

    @Shadow
    public abstract ServerWorld getServerWorld();

    @Shadow
    public abstract boolean isInTeleportationState();

    @Shadow
    public abstract void onTeleportationDone();

    @Unique
    private void putPendingUpdate(ShellState state, ShellStateUpdateType type) {
        this.shellStateChanges.put(state.getUuid(), new Pair<>(type, state));
    }

    @Unique
    private Shell getShellById(UUID id) {
        return (Shell)this.shellsById.get(id);
    }

    @Override
    public boolean canBeApplied(ShellState state) {
        return state != null && this.uuid.equals(state.getOwnerUuid());
    }
}