package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.api.shell.ShellStateContainer;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.ShellConstructorBlock;
import net.stacking.sync_mod.config.SyncConfig;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

@SuppressWarnings({"UnstableApiUsage"})
public class ShellConstructorBlockEntity extends AbstractShellContainerBlockEntity
        implements EnergyStorage, GeoBlockEntity {

    private static final RawAnimation DOOR_OPEN =
            RawAnimation.begin().thenPlayAndHold("animation.shell_constructor.door_open");
    private static final RawAnimation DOOR_CLOSE =
            RawAnimation.begin().thenPlayAndHold("animation.shell_constructor.door_close");
    private static final RawAnimation SPRAYERS_ACTIVE =
            RawAnimation.begin().thenPlayAndHold("animation.shell_constructor.sprayers_active");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long storedEnergy = 0;

    public ShellConstructorBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SHELL_CONSTRUCTOR, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "doors", 0, state -> {
            if (this.hasWorld() && AbstractShellContainerBlock.isOpen(this.getCachedState())) {
                return state.setAndContinue(DOOR_OPEN);
            }
            return state.setAndContinue(DOOR_CLOSE);
        }));

        // Sprayers play ONCE from start to finish, not looping
        controllers.add(new AnimationController<>(this, "sprayers", 0, state -> {
            if (this.shell != null && this.shell.getProgress() > 0 && this.shell.getProgress() < ShellState.PROGRESS_DONE) {
                return state.setAndContinue(SPRAYERS_ACTIVE);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public float getSprayerProgress(float partialTick) {
        return this.shell == null ? 0.0f : Math.min(this.shell.getProgress(), 1.0f);
    }

    public int getProgressComparatorOutput() {
        return this.shell == null ? 0 : Math.max(1, (int) (this.shell.getProgress() / (ShellState.PROGRESS_DONE / 15.0f)));
    }

    public int getInventoryComparatorOutput() {
        return 0;
    }

    // ── Right-click to create shell ──────────────────────────────────────────

    @Override
    public ActionResult onUse(World world, BlockPos pos, PlayerEntity player, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;

        PlayerSyncEvents.ShellConstructionFailureReason failureReason = this.beginShellConstruction(player);
        if (failureReason != null) {
            player.sendMessage(failureReason.toText(), true);
            return ActionResult.FAIL;
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    private PlayerSyncEvents.ShellConstructionFailureReason beginShellConstruction(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;
        }

        // Only allow on BOTTOM half
        BlockState state = this.getCachedState();
        if (!AbstractShellContainerBlock.isBottom(state)) {
            return PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;
        }

        // Check if there's already a shell being built
        if (this.shell != null) {
            return PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;
        }

        // Check event
        PlayerSyncEvents.ShellConstructionFailureReason failureReason =
                PlayerSyncEvents.ALLOW_SHELL_CONSTRUCTION.invoker().allowShellConstruction(player, this);
        if (failureReason != null) {
            return failureReason;
        }

        // Apply damage
        SyncConfig config = Sync.getConfig();
        float damage = 0.5f;  // Half a heart
        boolean isCreative = !serverPlayer.interactionManager.getGameMode().isSurvivalLike();
        boolean isLowOnHealth = (player.getHealth() + player.getAbsorptionAmount()) <= damage;
        boolean hasTotemOfUndying = player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

        if (isLowOnHealth && !isCreative && !hasTotemOfUndying && config.warnPlayerInsteadOfKilling()) {
            return PlayerSyncEvents.ShellConstructionFailureReason.NOT_ENOUGH_HEALTH;
        }

        if (!isCreative) {
            player.damage(this.world.getDamageSources().sweetBerryBush(), damage);
        }

        // Create the shell — WHITE/VANILLA appearance
        this.shell = ShellState.empty(serverPlayer, this.pos);

        if (isCreative && config.enableInstantShellConstruction()) {
            this.shell.setProgress(ShellState.PROGRESS_DONE);
        }
        this.markDirty();
        return null;
    }

    // ── Server tick (energy, construction progress, door logic)

    @Override
    public void onServerTick(World world, BlockPos pos, BlockState state) {
        super.onServerTick(world, pos, state);

        SyncConfig config = Sync.getConfig();

        // ── Handle shell energy and progress
        // Construction takes 1 minute (1200 ticks = 60 seconds)
        // Progress: 0 to 100, increment per tick = 100 / 1200 ≈ 0.0833f per tick
        if (this.shell != null && this.shell.getProgress() < ShellState.PROGRESS_DONE) {
            long consumption = 200L;  // Energy per tick
            if (this.storedEnergy >= consumption) {
                this.storedEnergy -= consumption;
                // Advance by ~0.083 per tick = 100 progress over 1200 ticks = 1 minute
                this.shell.setProgress(this.shell.getProgress() + (100.0f / 1200.0f));
            }
        }

        // Clear shell when construction completes (so next one can be made)
        if (this.shell != null && this.shell.getProgress() >= ShellState.PROGRESS_DONE) {
            // Don't clear immediately — let it persist until next right-click
            // But mark that construction is done
        }

        // ── Door logic: ONLY open if shell is 100% AND player has synced ──────
        boolean shellComplete = this.shell != null && this.shell.getProgress() >= ShellState.PROGRESS_DONE;

        // Check if player has synced into the shell by looking at shell's owner
        if (shellComplete && this.shell != null) {
            // Player syncs into shell — this should be detected elsewhere and flag set
            // For now, doors open only after shell is complete
        }

        // Doors open once shell construction is complete
        // Player will take it from storage, so no need to wait for them physically inside
        boolean shouldBeOpen = shellComplete;
        ShellConstructorBlock.setOpen(state, world, pos, shouldBeOpen);
    }

    // ── Energy storage

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) return 0;
        long capacity = 5000L;  // 5000 FE capacity
        long accepted = Math.min(maxAmount, capacity - this.storedEnergy);
        if (accepted > 0) {
            this.storedEnergy += accepted;
            this.markDirty();
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        return this.storedEnergy;
    }

    @Override
    public long getCapacity() {
        return 5000L;
    }

    @Override
    public boolean supportsInsertion() {
        return true;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    static {
        ShellStateContainer.LOOKUP.registerForBlockEntity(
                (x, s) -> x.hasWorld() && AbstractShellContainerBlock.isBottom(x.getCachedState())
                        && (s == null || s.equals(x.getShellState())) ? x : null,
                SyncBlockEntities.SHELL_CONSTRUCTOR);
        EnergyStorage.SIDED.registerForBlockEntities(
                (x, __) -> (EnergyStorage) x, SyncBlockEntities.SHELL_CONSTRUCTOR);
    }
}