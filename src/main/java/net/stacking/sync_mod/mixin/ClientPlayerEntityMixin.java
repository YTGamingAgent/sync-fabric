package net.stacking.sync_mod.mixin;

import com.mojang.authlib.GameProfile;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.api.networking.SynchronizationRequestPacket;
import net.stacking.sync_mod.api.shell.ClientShell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.client.gui.controller.DeathScreenController;
import net.stacking.sync_mod.client.gui.controller.HudController;
import net.stacking.sync_mod.api.shell.ShellPriority;
import net.stacking.sync_mod.config.SyncConfig;
import net.stacking.sync_mod.entity.KillableEntity;
import net.stacking.sync_mod.entity.LookingEntity;
import net.stacking.sync_mod.util.BlockPosUtil;
import net.stacking.sync_mod.entity.PersistentCameraEntity;
import net.stacking.sync_mod.entity.PersistentCameraEntityGoal;
import net.stacking.sync_mod.util.WorldUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
abstract class ClientPlayerEntityMixin extends AbstractClientPlayer implements ClientShell, KillableEntity, LookingEntity {
    @Final
    @Shadow
    protected Minecraft minecraft;

    @Unique
    private boolean isArtificial = false;

    @Unique
    private ConcurrentMap<UUID, ShellState> shellsById = new ConcurrentHashMap<>();


    private ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile/*? if <1.19.3 {*//*, null*//*?}*/);
    }


    @Override
    public @Nullable PlayerSyncEvents.SyncFailureReason beginSync(ShellState state) {
        ClientLevel world = (ClientLevel)WorldUtil.level(this);
        if (world == null) {
            return PlayerSyncEvents.SyncFailureReason.OTHER_PROBLEM;
        }

        PlayerSyncEvents.SyncFailureReason failureReason =
                this.canBeApplied(state) && state.getProgress() >= ShellState.PROGRESS_DONE
                        ? PlayerSyncEvents.ALLOW_SYNCING.invoker().allowSync(this, state)
                        : PlayerSyncEvents.SyncFailureReason.INVALID_SHELL;

        if (failureReason != null) {
            return failureReason;
        }

        PlayerSyncEvents.START_SYNCING.invoker().onStartSyncing(this, state);

        BlockPos pos = this.blockPosition();
        Direction facing = BlockPosUtil.getHorizontalFacing(pos, world).orElse(this.getDirection().getOpposite());
        SynchronizationRequestPacket request = new SynchronizationRequestPacket(state);
        PersistentCameraEntityGoal cameraGoal = this.isDeadOrDying()
                ? PersistentCameraEntityGoal.limbo(pos, facing, state.getPos(), __ -> request.send())
                : PersistentCameraEntityGoal.stairwayToHeaven(pos, facing, state.getPos(), __ -> request.send());

        HudController.hide();
        if (this.isDeadOrDying()) {
            DeathScreenController.suspend();
        }
        this.minecraft.setScreen(null);
        PersistentCameraEntity.setup(this.minecraft, cameraGoal);
        return null;
    }

    @Override
    public void endSync(ResourceLocation startWorld, BlockPos startPos, Direction startFacing, ResourceLocation targetWorld, BlockPos targetPos, Direction targetFacing, @Nullable ShellState storedState) {
        LocalPlayer player = (LocalPlayer)(Object)this;
        boolean syncFailed = Objects.equals(startPos, targetPos);

        if (!syncFailed) {
            if (this.getHealth() <= 0) {
                this.setHealth(0.01F);
            }
            this.deathTime = 0;
        }

        float yaw = targetFacing.getOpposite().toYRot();
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.yBodyRotO = this.yBodyRot = yaw;
        this.yHeadRotO = this.yHeadRot = yaw;
        player.yBobO = player.yBob = yaw;

        this.setXRot(0);
        this.xRotO = 0;
        player.xBobO = player.xBob = 0;

        Runnable restore = () -> {
            PersistentCameraEntity.unset(this.minecraft);
            HudController.restore();
            DeathScreenController.restore();
            if (!syncFailed) {
                PlayerSyncEvents.STOP_SYNCING.invoker().onStopSyncing(this, startPos, storedState);
            }
        };

        boolean enableCamera = Objects.equals(startWorld, targetWorld);
        if (enableCamera) {
            PersistentCameraEntityGoal cameraGoal = PersistentCameraEntityGoal.highwayToHell(startPos, startFacing, targetPos, targetFacing, __ -> restore.run());
            PersistentCameraEntity.setup(this.minecraft, cameraGoal);
        } else {
            restore.run();
        }
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
        this.isArtificial = isArtificial;
    }

    @Override
    public void setAvailableShellStates(Stream<ShellState> states) {
        this.shellsById = states.collect(Collectors.toConcurrentMap(ShellState::getUuid, x -> x));
    }

    @Override
    public Stream<ShellState> getAvailableShellStates() {
        return this.shellsById.values().stream();
    }

    @Override
    public ShellState getShellStateByUuid(UUID uuid) {
        return uuid == null ? null : this.shellsById.get(uuid);
    }

    @Override
    public void add(ShellState state) {
        if (this.canBeApplied(state)) {
            this.shellsById.put(state.getUuid(), state);
        }
    }

    @Override
    public void remove(ShellState state) {
        if (state != null) {
            this.shellsById.remove(state.getUuid());
        }
    }

    @Override
    public void update(ShellState state) {
        if (this.canBeApplied(state) || state != null && this.shellsById.containsKey(state.getUuid())) {
            this.shellsById.put(state.getUuid(), state);
        }
    }

    @Override
    public boolean changeLookingEntityLookDirection(double cursorDeltaX, double cursorDeltaY) {
        return this.minecraft.getCameraEntity() instanceof PersistentCameraEntity;
    }

    @Override
    public void onKillableEntityDeath() {
        boolean canRespawn = this.shellsById.values().stream()
        .anyMatch(s -> this.canBeApplied(s) && s.getProgress() >= ShellState.PROGRESS_DONE);
        BlockPos pos = this.blockPosition();
        ResourceLocation world = WorldUtil.getId(WorldUtil.level(this));
        Comparator<ShellState> comparator = ShellPriority.asComparator(world, pos, Sync.getConfig().syncPriority().stream().map(SyncConfig.ShellPriorityEntry::priority));
        ShellState respawnShell = canRespawn ? this.shellsById.values().stream().filter(x -> this.canBeApplied(x) && x.getProgress() >= ShellState.PROGRESS_DONE).min(comparator).orElse(null) : null;
        if (respawnShell != null) {
            this.beginSync(respawnShell);
        }
    }

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void updatePostDeath(CallbackInfo ci) {
        if (this.minecraft.screen instanceof DeathScreen) {
            this.deathTime = Mth.clamp(this.deathTime, 0, 19);
        } else {
            this.deathTime = Mth.clamp(++this.deathTime, 0, 20);
            ci.cancel();
        }
    }
}