package net.stacking.sync_mod.mixin;

import net.stacking.sync_mod.util.nbt.Nbt;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.stacking.sync_mod.util.Tuple;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;
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

@Mixin(ServerPlayer.class)
abstract class ServerPlayerEntityMixin extends Player implements ServerShell, KillableEntity {
    @Shadow
    private int lastSentExp;

    @Shadow
    private float lastSentHealth;

    @Shadow
    private int lastSentFood;

    @Final
    @Shadow
    public MinecraftServer server;

    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Unique
    private boolean isArtificial = false;

    @Unique
    private boolean shellDirty = false;

    @Unique
    private boolean undead = false;

    @Unique
    private ConcurrentMap<UUID, ShellState> shellsById = new ConcurrentHashMap<>();

    @Unique
    private Map<UUID, Tuple<ShellStateUpdateType, ShellState>> shellStateChanges = new ConcurrentHashMap<>();

    @Unique
    @Nullable
    private GlobalPos primaryShellStorage;


    private ServerPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile profile) {
        //? if >=1.21.6 {
        /*super(world, profile);
        *///?} else if >=1.19.3 {
        super(world, pos, yaw, profile);
        //?} else {
        /*super(world, pos, yaw, profile, null);
        *///?}
    }


    @Override
    public UUID getShellOwnerUuid() {
        return this.getGameProfile()./*? if >=1.21.9 {*//*id()*//*?} else {*/getId()/*?}*/;
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
    @Nullable
    public GlobalPos getPrimaryShellStorage() {
        return this.primaryShellStorage;
    }

    @Override
    public void setPrimaryShellStorage(@Nullable GlobalPos pos) {
        this.primaryShellStorage = pos;
    }

    @Override
    public Either<ShellState, PlayerSyncEvents.SyncFailureReason> sync(ShellState state) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        BlockPos currentPos = this.blockPosition();
        Level currentWorld = WorldUtil.level(player);

        if (!this.canBeApplied(state) || state.getProgress() < ShellState.PROGRESS_DONE) {
            return Either.right(PlayerSyncEvents.SyncFailureReason.INVALID_SHELL);
        }

        boolean isDead = this.isDeadOrDying();
        ShellStateContainer currentShellContainer = isDead ? null : ShellStateContainer.find(currentWorld, currentPos);
        if (!isDead && (currentShellContainer == null || currentShellContainer.getShellState() != null)) {
            return Either.right(PlayerSyncEvents.SyncFailureReason.INVALID_CURRENT_LOCATION);
        }

        PlayerSyncEvents.ShellSelectionFailureReason selectionFailureReason = PlayerSyncEvents.ALLOW_SHELL_SELECTION.invoker().allowShellSelection(player, currentShellContainer);
        if (selectionFailureReason != null) {
            return Either.right(selectionFailureReason::toText);
        }

        ResourceLocation targetWorldId = state.getWorld();
        ServerLevel targetWorld = WorldUtil.findWorld(this.server.getAllLevels(), targetWorldId).orElse(null);
        if (targetWorld == null) {
            return Either.right(PlayerSyncEvents.SyncFailureReason.INVALID_TARGET_LOCATION);
        }

        BlockPos targetPos = state.getPos();
        ChunkAccess targetChunk = targetWorld.getChunk(targetPos);
        ShellStateContainer targetShellContainer = targetChunk == null ? null : ShellStateContainer.find(targetWorld, state);
        if (targetShellContainer == null) {
            return Either.right(PlayerSyncEvents.SyncFailureReason.INVALID_TARGET_LOCATION);
        }

        state = targetShellContainer.getShellState();
        PlayerSyncEvents.SyncFailureReason finalFailureReason = this.canBeApplied(state) ? PlayerSyncEvents.ALLOW_SYNCING.invoker().allowSync(this, state) : PlayerSyncEvents.SyncFailureReason.INVALID_SHELL;
        if (finalFailureReason != null) {
            return Either.right(finalFailureReason);
        }

        PlayerSyncEvents.START_SYNCING.invoker().onStartSyncing(this, state);

        ShellState storedState = null;
        if (currentShellContainer != null) {
            // The body we're leaving stays in the storage we're standing in. The exception is our
            // original (non-artificial) body: if we have a usable primary storage, it gets parked there.
            ShellStateContainer bodyContainer = currentShellContainer;
            BlockPos bodyPos = currentPos;
            Level bodyWorld = currentWorld;

            ShellStorageBlockEntity primaryStorage = this.isArtificial ? null : ShellStorageBlockEntity.findPrimaryStorage(player);
            if (primaryStorage != null && primaryStorage != targetShellContainer) {
                bodyContainer = primaryStorage;
                bodyPos = primaryStorage.getBlockPos();
                bodyWorld = Objects.requireNonNull(primaryStorage.getLevel());
            }

            storedState = ShellState.of(player, bodyPos, bodyContainer.getColor());
            storedState.setWorld(WorldUtil.getId(bodyWorld));
            bodyContainer.setShellState(storedState);
            if (bodyContainer.isRemotelyAccessible()) {
                this.add(storedState);
            }
        }

        targetShellContainer.setShellState(null);
        this.remove(state);
        this.apply(state);

        PlayerSyncEvents.STOP_SYNCING.invoker().onStopSyncing(player, currentPos, storedState);
        return Either.left(storedState);
    }

    @Override
    public void apply(ShellState state) {
        Objects.requireNonNull(state);

        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        ServerLevel targetWorld = WorldUtil.findWorld(this.server.getAllLevels(), state.getWorld()).orElse(null);
        if (targetWorld == null) {
            return;
        }

        this.stopRiding();
        this.removeEntitiesOnShoulder();
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);
        this.removeAllEffects();

        new PlayerIsAlivePacket(serverPlayer).send(this.server.getPlayerList().getPlayers());
        this.teleportToShell(targetWorld, state.getPos());
        this.isArtificial = state.isArtificial();

        Inventory inventory = this.getInventory();
        //? if >=1.21.5 {
        /*int selectedSlot = inventory.getSelectedSlot();
        *///?} else
        int selectedSlot = inventory.selected;
        state.getInventory().copyTo(inventory);
        //? if >=1.21.5 {
        /*inventory.setSelectedSlot(selectedSlot);
        *///?} else
        inventory.selected = selectedSlot;

        state.getComponent().applyToPlayer(serverPlayer, this.server.registryAccess());

        serverPlayer.setGameMode(state.getGameMode());
        this.setHealth(state.getHealth());
        this.experienceLevel = state.getExperienceLevel();
        this.experienceProgress = state.getExperienceProgress();
        this.totalExperience = state.getTotalExperience();
        this.getFoodData().setFoodLevel(state.getFoodLevel());
        this.getFoodData().setSaturation(state.getSaturationLevel());
        //? if >=1.21.2 {
        /*this.getFoodData().exhaustionLevel = state.getExhaustion();
        *///?} else
        this.getFoodData().setExhaustion(state.getExhaustion());

        this.undead = false;
        this.dead = false;
        this.deathTime = 0;
        this.fallDistance = 0;
        this.lastSentExp = -1;
        this.lastSentHealth = -1;
        this.lastSentFood = -1;
        this.shellDirty = true;
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
        this.shellStateChanges.put(state.getUuid(), new Tuple<>(ShellStateUpdateType.ADD, state));
    }

    @Override
    public void remove(ShellState state) {
        if (state == null) {
            return;
        }

        if (this.shellsById.remove(state.getUuid()) != null) {
            this.shellStateChanges.put(state.getUuid(), new Tuple<>(ShellStateUpdateType.REMOVE, state));
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
        this.shellStateChanges.put(state.getUuid(), new Tuple<>(updated ? ShellStateUpdateType.UPDATE : ShellStateUpdateType.ADD, state));
    }

    @Inject(method = "doTick", at = @At("HEAD"))
    private void playerTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;

        if (this.shellDirty) {
            this.shellDirty = false;
            this.shellStateChanges.clear();
            new ShellUpdatePacket(WorldUtil.getId(WorldUtil.level(this)), this.isArtificial, this.shellsById.values()).send(player);
        }

        for (Tuple<ShellStateUpdateType, ShellState> upd : this.shellStateChanges.values()) {
            new ShellStateUpdatePacket(upd.getA(), upd.getB()).send(player);
        }
        this.shellStateChanges.clear();
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onDeath(DamageSource source, CallbackInfo ci) {
        if (!this.isArtificial) {
            return;
        }

        ShellState respawnShell = this.shellsById.values().stream().filter(x -> this.canBeApplied(x) && x.getProgress() >= ShellState.PROGRESS_DONE).findAny().orElse(null);
        if (respawnShell == null) {
            return;
        }

        if (WorldUtil.gameRules(this).getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
            this.sendDeathMessageInChat();
        } else {
            this.sendEmptyDeathMessageInChat();
        }

        this.removeEntitiesOnShoulder();
        if (WorldUtil.gameRules(this).getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }

        if (!this.isSpectator()) {
            //? if >=1.21 {
            this.dropAllDeathLoot((ServerLevel)WorldUtil.level(this), source);
            //?} else {
            /*this.dropAllDeathLoot(source);
            *///?}
        }

        this.undead = true;
        ci.cancel();
    }

    @Override
    public boolean updateKillableEntityPostDeath() {
        this.deathTime = Mth.clamp(++this.deathTime, 0, 20);
        if (this.isArtificial && this.shellsById.values().stream().anyMatch(x -> this.canBeApplied(x) && x.getProgress() >= ShellState.PROGRESS_DONE)) {
            return true;
        }

        if (this.undead) {
            //? if >=1.19.4 {
            this.die(WorldUtil.level(this).damageSources().magic());
            //?} else
            /*this.die(net.minecraft.world.damagesource.DamageSource.MAGIC);*/
            this.undead = false;
        }

        if (this.deathTime == 20) {
            WorldUtil.level(this).broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
        }
        return true;
    }

    @Unique
    private void sendDeathMessageInChat() {
        Component text = this.getCombatTracker().getDeathMessage();
        this.connection.send(new ClientboundPlayerCombatKillPacket(/*? if >=1.20 {*/this.getId()/*?} else {*//*this.getCombatTracker()*//*?}*/, text), PacketSendListener.exceptionallySend(() -> {
            String truncatedString = text.getString(256);
            Component messageWasTooLong = Component.translatable("death.attack.message_too_long", Component.literal(truncatedString).withStyle(ChatFormatting.YELLOW));
            Component magic = Component.translatable("death.attack.even_more_magic", this.getDisplayName()).withStyle((style) -> style.withHoverEvent(/*? if >=1.21.5 {*//*new HoverEvent.ShowText(messageWasTooLong)*//*?} else {*/new HoverEvent(HoverEvent.Action.SHOW_TEXT, messageWasTooLong)/*?}*/));
            return new ClientboundPlayerCombatKillPacket(/*? if >=1.20 {*/this.getId()/*?} else {*//*this.getCombatTracker()*//*?}*/, magic);
        }));
        Team abstractTeam = this.getTeam();
        if (abstractTeam != null && abstractTeam.getDeathMessageVisibility() != Team.Visibility.ALWAYS) {
            if (abstractTeam.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                this.server.getPlayerList().broadcastSystemToTeam(this, text);
            } else if (abstractTeam.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                this.server.getPlayerList().broadcastSystemToAllExceptTeam(this, text);
            }
        } else {
            this.server.getPlayerList().broadcastSystemMessage(text, false);
        }
    }

    @Unique
    private void sendEmptyDeathMessageInChat() {
        this.connection.send(new ClientboundPlayerCombatKillPacket(/*? if >=1.20 {*/this.getId()/*?} else {*//*this.getCombatTracker()*//*?}*/, Component.empty()));
    }

    /**
     * Moves the player into the shell container at {@code pos}, facing out through its doors.
     * Vanilla's teleport handles both same-world and cross-dimension moves.
     */
    @Unique
    private void teleportToShell(ServerLevel targetWorld, BlockPos pos) {
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        float yaw = BlockPosUtil.getHorizontalFacing(pos, targetWorld).map(d -> d.getOpposite().toYRot()).orElse(0F);
        //? if >=1.21.2 {
        /*serverPlayer.teleportTo(targetWorld, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, java.util.Set.of(), yaw, 0, true);
        *///?} else
        serverPlayer.teleportTo(targetWorld, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
    }

    @Shadow
    protected abstract void tellNeutralMobsThatIDied();


    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCustomDataToNbt(CompoundTag nbt, CallbackInfo ci) {
        ListTag shellList = new ListTag();
        this.shellsById
            .values()
            .stream()
            .map(x -> x.writeNbt(new CompoundTag()))
            .forEach(shellList::add);

        nbt.putBoolean("IsArtificial", this.isArtificial);
        nbt.put("Shells", shellList);
        if (this.primaryShellStorage != null) {
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.primaryShellStorage).result().ifPresent(x -> nbt.put("PrimaryShellStorage", x));
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readCustomDataFromNbt(CompoundTag nbt, CallbackInfo ci) {
        this.isArtificial = Nbt.getBoolean(nbt, "IsArtificial");
        this.shellsById = Nbt.getList(nbt, "Shells", Tag.TAG_COMPOUND)
            .stream()
            .map(x -> ShellState.fromNbt((CompoundTag)x))
            .collect(Collectors.toConcurrentMap(ShellState::getUuid, x -> x));
        this.primaryShellStorage = nbt.contains("PrimaryShellStorage")
            ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, nbt.get("PrimaryShellStorage")).result().orElse(null)
            : null;

        Collection<Tuple<ShellStateUpdateType, ShellState>> updates = ((ShellStateManager)this.server).popPendingUpdates(this.uuid);
        for (Tuple<ShellStateUpdateType, ShellState> update : updates) {
            ShellState state = update.getB();
            switch (update.getA()) {
                case ADD, UPDATE -> {
                    if (this.uuid.equals(state.getOwnerUuid())) {
                        this.shellsById.put(state.getUuid(), state);
                    }
                }
                case REMOVE -> this.shellsById.remove(state.getUuid());
            }
        }

        this.shellStateChanges = new ConcurrentHashMap<>();
        this.shellDirty = true;
    }

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerShell shell = (ServerShell)oldPlayer;
        this.isArtificial = alive && shell.isArtificial();
        this.shellsById = shell.getAvailableShellStates().collect(Collectors.toConcurrentMap(ShellState::getUuid, x -> x));
        this.primaryShellStorage = shell.getPrimaryShellStorage();
        this.shellStateChanges = new ConcurrentHashMap<>();
        this.shellDirty = true;
    }

    @Inject(method = /*? if >=1.20 {*/"setServerLevel"/*?} else {*//*"setLevel"*//*?}*/, at = @At("HEAD"))
    private void setWorld(ServerLevel world, CallbackInfo ci) {
        if (world != WorldUtil.level(this)) {
            this.shellDirty = true;
        }
    }

    @Override
    public boolean canBeApplied(ShellState state) {
        return state != null && this.uuid.equals(state.getOwnerUuid());
    }
}
