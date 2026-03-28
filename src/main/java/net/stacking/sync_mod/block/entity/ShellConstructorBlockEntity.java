package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.api.shell.ShellStateContainer;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.ShellConstructorBlock;
import net.stacking.sync_mod.config.SyncConfig;
import net.stacking.sync_mod.util.BlockPosUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
            RawAnimation.begin().thenLoop("animation.shell_constructor.sprayers_active");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ShellConstructorBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SHELL_CONSTRUCTOR, pos, state);
    }

    // ── GeoBlockEntity ───────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "doors", 0, state -> {
            if (this.hasWorld() && AbstractShellContainerBlock.isOpen(this.getCachedState())) {
                return state.setAndContinue(DOOR_OPEN);
            }
            return state.setAndContinue(DOOR_CLOSE);
        }));

        controllers.add(new AnimationController<>(this, "sprayers", 0, state -> {
            if (this.shell != null
                    && this.shell.getProgress() > ShellState.PROGRESS_START
                    && this.shell.getProgress() < ShellState.PROGRESS_DONE) {
                return state.setAndContinue(SPRAYERS_ACTIVE);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ── Right-click: initiate shell construction, or sneak to cancel ─────────

    @Override
    public ActionResult onUse(World world, BlockPos pos, PlayerEntity player, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;

        // ── Sneak + right-click: cancel and clear any in-progress or completed shell ─
        // Useful when the shell is stuck (e.g. no energy, or you want a fresh build).
        if (player.isSneaking() && this.shell != null) {
            this.getShellStateManager().remove(this.shell);
            this.shell = null;
            this.markDirty();
            player.sendMessage(Text.translatable("message.sync.shell_constructor.cancelled"), true);
            return ActionResult.SUCCESS;
        }

        PlayerSyncEvents.ShellConstructionFailureReason reason = this.beginShellConstruction(player);
        if (reason != null) {
            player.sendMessage(reason.toText(), true);
            return ActionResult.FAIL;
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    private PlayerSyncEvents.ShellConstructionFailureReason beginShellConstruction(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;
        }
        if (!AbstractShellContainerBlock.isBottom(this.getCachedState())) {
            return PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;
        }
        if (this.shell != null) {
            return PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;
        }

        PlayerSyncEvents.ShellConstructionFailureReason eventReason =
                PlayerSyncEvents.ALLOW_SHELL_CONSTRUCTION.invoker().allowShellConstruction(player, this);
        if (eventReason != null) return eventReason;

        SyncConfig config = Sync.getConfig();
        float damage = 0.5f;
        boolean isCreative = !serverPlayer.interactionManager.getGameMode().isSurvivalLike();
        boolean isLowOnHealth = (player.getHealth() + player.getAbsorptionAmount()) <= damage;
        boolean hasTotem = player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
                || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

        if (isLowOnHealth && !isCreative && !hasTotem && config.warnPlayerInsteadOfKilling()) {
            return PlayerSyncEvents.ShellConstructionFailureReason.NOT_ENOUGH_HEALTH;
        }
        if (!isCreative) {
            player.damage(this.world.getDamageSources().sweetBerryBush(), damage);
        }

        this.shell = ShellState.empty(serverPlayer, this.pos);
        System.out.println("Shell created: " + (this.shell != null) + " at " + this.pos);
        if (isCreative && config.enableInstantShellConstruction()) {
            this.shell.setProgress(ShellState.PROGRESS_DONE);
        }
        this.markDirty();
        return null;
    }

    // ── Server tick: door logic ──────────────────────────────────────────────

    @Override
    public void onServerTick(World world, BlockPos pos, BlockState state) {
        super.onServerTick(world, pos, state);

        boolean shellComplete = this.shell != null
                && this.shell.getProgress() >= ShellState.PROGRESS_DONE;
        boolean playerInside  = BlockPosUtil.hasPlayerInside(pos, world);

        // Doors open when the shell is done (showing it's ready for sync).
        // Once sync() clears this.shell via setShellState(null), shellComplete
        // becomes false. The door stays open while the player is still physically
        // inside (second condition), then closes when they walk out.
        boolean shouldBeOpen = shellComplete
                || (AbstractShellContainerBlock.isOpen(state) && playerInside);

        ShellConstructorBlock.setOpen(state, world, pos, shouldBeOpen);
    }

    // ── EnergyStorage — progress IS energy ──────────────────────────────────
    //
    // No separate energy buffer. The treadmill pushes FE in via insert();
    // that call converts FE directly to construction progress.
    // With the default shellConstructorCapacity of 256,000 FE and treadmill
    // output of 200 FE/tick, construction takes ~1,280 ticks ≈ 64 seconds.

    @Override
    public long insert(long amount, TransactionContext context) {
        ShellConstructorBlockEntity bottom =
                (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) return 0;
        if (BlockPosUtil.hasPlayerInside(bottom.getPos(), bottom.getWorld())) return 0;
        if (bottom.shell.getProgress() >= ShellState.PROGRESS_DONE) return 0;

        long capacity = Sync.getConfig().shellConstructorCapacity();
        long missingFE = (long) Math.ceil(
                (ShellState.PROGRESS_DONE - bottom.shell.getProgress()) * capacity);
        long accepted = Math.min(amount, missingFE);
        if (accepted <= 0) return 0;

        context.addCloseCallback((txn, result) -> {
            if (result.wasCommitted()) {
                bottom.shell.setProgress(
                        bottom.shell.getProgress() + (float) accepted / capacity);
            }
        });
        return accepted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext context) {
        return 0;
    }

    @Override
    public long getAmount() {
        ShellConstructorBlockEntity bottom =
                (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) return 0;
        long cap = Sync.getConfig().shellConstructorCapacity();
        return (long) (bottom.shell.getProgress() * cap);
    }

    @Override
    public long getCapacity() {
        ShellConstructorBlockEntity bottom =
                (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        return bottom != null && bottom.shell != null
                ? Sync.getConfig().shellConstructorCapacity() : 0;
    }

    @Override public boolean supportsInsertion()  { return true;  }
    @Override public boolean supportsExtraction() { return false; }

    static {
        ShellStateContainer.LOOKUP.registerForBlockEntity(
                (x, s) -> x.hasWorld()
                        && AbstractShellContainerBlock.isBottom(x.getCachedState())
                        && (s == null || s.equals(x.getShellState())) ? x : null,
                SyncBlockEntities.SHELL_CONSTRUCTOR);
        EnergyStorage.SIDED.registerForBlockEntities(
                (x, __) -> (EnergyStorage) x, SyncBlockEntities.SHELL_CONSTRUCTOR);
    }
}