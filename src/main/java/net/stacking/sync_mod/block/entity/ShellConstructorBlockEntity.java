package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.util.BlockPosUtil;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.api.shell.ShellStateContainer;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.ShellConstructorBlock;
import net.stacking.sync_mod.config.SyncConfig;
import net.stacking.sync_mod.entity.damage.FingerstickDamageSource;
import net.stacking.sync_mod.Sync;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
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

    // ---- GeoBlockEntity --------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Door open/close
        controllers.add(new AnimationController<>(this, "doors", 0, state -> {
            if (this.hasWorld() && AbstractShellContainerBlock.isOpen(this.getCachedState())) {
                return state.setAndContinue(DOOR_OPEN);
            }
            return state.setAndContinue(DOOR_CLOSE);
        }));

        // Sprayer arms — active while building
        controllers.add(new AnimationController<>(this, "sprayers", 0, state -> {
            if (this.getSprayerProgress(0) > 0) {
                return state.setAndContinue(SPRAYERS_ACTIVE);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---- Original logic (unchanged) --------------------------------------------

    @Override
    public void onServerTick(World world, BlockPos pos, BlockState state) {
        super.onServerTick(world, pos, state);

        // ── Clear completed shell ────────────────────────────────────────
        // When construction is done, remove the shell so a new one can start
        if (this.shell != null && this.shell.getProgress() >= ShellState.PROGRESS_DONE) {
            this.shell = null;
            this.markDirty();
        }

        if (ShellConstructorBlock.isOpen(state)) {
            ShellConstructorBlock.setOpen(state, world, pos, BlockPosUtil.hasPlayerInside(pos, world));
        }
    }

    @Override
    public ActionResult onUse(World world, BlockPos pos, PlayerEntity player, Hand hand) {
        PlayerSyncEvents.ShellConstructionFailureReason failureReason = this.beginShellConstruction(player);
        if (failureReason == null) {
            return ActionResult.SUCCESS;
        } else {
            player.sendMessage(failureReason.toText(), true);
            return ActionResult.CONSUME;
        }
    }

    @Nullable
    private PlayerSyncEvents.ShellConstructionFailureReason beginShellConstruction(PlayerEntity player) {
        PlayerSyncEvents.ShellConstructionFailureReason failureReason = this.shell == null
                ? PlayerSyncEvents.ALLOW_SHELL_CONSTRUCTION.invoker().allowShellConstruction(player, this)
                : PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;

        if (failureReason != null) {
            return failureReason;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            SyncConfig config = Sync.getConfig();

            float damage = serverPlayer.server.isHardcore() ? config.hardcoreFingerstickDamage() : config.fingerstickDamage();

            boolean isCreative = !serverPlayer.interactionManager.getGameMode().isSurvivalLike();
            boolean isLowOnHealth = (player.getHealth() + player.getAbsorptionAmount()) <= damage;
            boolean hasTotemOfUndying = player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
            if (isLowOnHealth && !isCreative && !hasTotemOfUndying && config.warnPlayerInsteadOfKilling()) {
                return PlayerSyncEvents.ShellConstructionFailureReason.NOT_ENOUGH_HEALTH;
            }

            player.damage(world.getDamageSources().sweetBerryBush(), damage);
            this.shell = ShellState.empty(serverPlayer, pos);
            if (isCreative && config.enableInstantShellConstruction()) {
                this.shell.setProgress(ShellState.PROGRESS_DONE);
            }
        }
        return null;
    }

    @Override
    public long getAmount() {
        ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) return 0;
        long cap = Sync.getConfig().shellConstructorCapacity();
        return (long) (bottom.shell.getProgress() * cap);
    }

    @Override
    public long getCapacity() {
        ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        return bottom != null && bottom.shell != null ? Sync.getConfig().shellConstructorCapacity() : 0;
    }

    @Override
    public boolean supportsInsertion() {
        return true;
    }

    @Override
    public long insert(long amount, TransactionContext context) {
        ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) return 0;
        if (BlockPosUtil.hasPlayerInside(bottom.getPos(), bottom.getWorld())) return 0;
        if (bottom.shell.getProgress() >= ShellState.PROGRESS_DONE) return 0;

        long capacity = Sync.getConfig().shellConstructorCapacity();
        long missingFE = (long) Math.ceil((ShellState.PROGRESS_DONE - bottom.shell.getProgress()) * capacity);
        long accepted = Math.min(amount, missingFE);
        if (accepted <= 0) return 0;

        context.addCloseCallback((txn, result) -> {
            if (result.wasCommitted()) {
                bottom.shell.setProgress(bottom.shell.getProgress() + (float) accepted / capacity);
            }
        });
        return accepted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext context) {
        return 0;
    }

    static {
        ShellStateContainer.LOOKUP.registerForBlockEntity(
                (x, s) -> x.hasWorld() && AbstractShellContainerBlock.isBottom(x.getCachedState())
                        && (s == null || s.equals(x.getShellState())) ? x : null,
                SyncBlockEntities.SHELL_CONSTRUCTOR);
        EnergyStorage.SIDED.registerForBlockEntities((x, __) -> (EnergyStorage) x, SyncBlockEntities.SHELL_CONSTRUCTOR);
    }
}
