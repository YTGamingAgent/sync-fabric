package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.util.ChatUtil;

import net.stacking.sync_mod.util.WorldUtil;
import net.stacking.sync_mod.util.BlockPosUtil;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.api.shell.ShellStateContainer;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.ShellConstructorBlock;
import net.stacking.sync_mod.config.SyncConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.stacking.sync_mod.Sync;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ShellConstructorBlockEntity extends AbstractShellContainerBlockEntity {
    /**
     * Build time in seconds, indexed by the number of running treadmills next to the constructor.
     * Index 0 is used when the constructor is powered by redstone alone.
     */
    private static final int[] BUILD_SECONDS_BY_TREADMILL_COUNT = { 180, 180, 120, 60, 30 };
    private static final int SECONDS_SAVED_PER_REDSTONE_SOURCE = 5;
    private static final int MIN_BUILD_SECONDS = 1;

    public ShellConstructorBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SHELL_CONSTRUCTOR, pos, state);
    }

    @Override
    public void onServerTick(Level world, BlockPos pos, BlockState state) {
        boolean hasPlayerInside = BlockPosUtil.hasPlayerInside(pos, world);

        // Construction keeps going no matter who is standing next to (or touching) the constructor.
        if (this.shell != null && this.shell.getProgress() < ShellState.PROGRESS_DONE) {
            int buildTicks = getBuildTicks(world, pos);
            if (buildTicks > 0) {
                this.shell.setProgress(this.shell.getProgress() + 1F / buildTicks);
            }
        }

        super.onServerTick(world, pos, state);

        // Doors open once the shell is finished, and stay open while a player is inside.
        boolean isShellReady = this.shell != null && this.shell.getProgress() >= ShellState.PROGRESS_DONE;
        ShellConstructorBlock.setOpen(state, world, pos, isShellReady || (ShellConstructorBlock.isOpen(state) && hasPlayerInside));
    }

    /**
     * Works out how long a shell takes to build from what's touching the constructor
     * (4 sides × lower and upper half = 8 spots):
     * <ul>
     *     <li>running treadmills: 1 → 3:00, 2 → 2:00, 3 → 1:00, 4 → 0:30;</li>
     *     <li>every redstone block or lit redstone torch takes 5 seconds off (redstone alone starts from 3:00).</li>
     * </ul>
     *
     * @return Build time in ticks, or {@code 0} if the constructor has no power.
     */
    private static int getBuildTicks(Level world, BlockPos bottomPos) {
        Set<BlockPos> runningTreadmills = new HashSet<>();
        int redstoneSources = 0;

        for (BlockPos partPos : new BlockPos[] { bottomPos, bottomPos.above() }) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = partPos.relative(direction);
                if (isRedstoneSource(world.getBlockState(neighborPos))) {
                    ++redstoneSources;
                } else if (world.getBlockEntity(neighborPos) instanceof TreadmillBlockEntity treadmill && treadmill.isRunning()) {
                    runningTreadmills.add(treadmill.getBlockPos());
                }
            }
        }

        if (runningTreadmills.isEmpty() && redstoneSources == 0) {
            return 0;
        }

        int treadmillCount = Math.min(runningTreadmills.size(), BUILD_SECONDS_BY_TREADMILL_COUNT.length - 1);
        int seconds = BUILD_SECONDS_BY_TREADMILL_COUNT[treadmillCount] - SECONDS_SAVED_PER_REDSTONE_SOURCE * redstoneSources;
        return Math.max(seconds, MIN_BUILD_SECONDS) * 20;
    }

    /**
     * Redstone blocks and lit redstone torches (standing or on a wall) count as power.
     */
    private static boolean isRedstoneSource(BlockState state) {
        if (state.is(Blocks.REDSTONE_BLOCK)) {
            return true;
        }
        return state.getBlock() instanceof RedstoneTorchBlock && state.getValue(RedstoneTorchBlock.LIT);
    }

    @Override
    public InteractionResult onUse(Level world, BlockPos pos, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && this.shell != null) {
            return this.cancelShellConstruction(world, pos, player);
        }

        PlayerSyncEvents.ShellConstructionFailureReason failureReason = this.beginShellConstruction(player);
        if (failureReason == null) {
            return InteractionResult.SUCCESS;
        } else {
            ChatUtil.message(player, failureReason.toText(), true);
            return InteractionResult.CONSUME;
        }
    }

    /**
     * Sneak + right-click (empty hand): the shell's owner scraps the shell in this constructor.
     */
    private InteractionResult cancelShellConstruction(Level world, BlockPos pos, Player player) {
        if (!player.getUUID().equals(this.shell.getOwnerUuid()) && !player.isCreative()) {
            ChatUtil.message(player, PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED.toText(), true);
            return InteractionResult.CONSUME;
        }

        if (world instanceof ServerLevel serverWorld) {
            this.getShellStateManager().remove(this.shell);
            this.destroyShell(serverWorld, pos);
            ChatUtil.message(player, Component.translatable("message.sync.shell_constructor.cancelled"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private PlayerSyncEvents.ShellConstructionFailureReason beginShellConstruction(Player player) {
        PlayerSyncEvents.ShellConstructionFailureReason failureReason = this.shell == null
                ? PlayerSyncEvents.ALLOW_SHELL_CONSTRUCTION.invoker().allowShellConstruction(player, this)
                : PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;

        if (failureReason != null) {
            return failureReason;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SyncConfig config = Sync.getConfig();

            float damage = WorldUtil.server(serverPlayer).isHardcore() ? config.hardcoreFingerstickDamage() : config.fingerstickDamage();

            boolean isCreative = !serverPlayer.gameMode.getGameModeForPlayer().isSurvival();
            boolean isLowOnHealth = (player.getHealth() + player.getAbsorptionAmount()) <= damage;
            boolean hasTotemOfUndying = player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
            if (isLowOnHealth && !isCreative && !hasTotemOfUndying && config.warnPlayerInsteadOfKilling()) {
                return PlayerSyncEvents.ShellConstructionFailureReason.NOT_ENOUGH_HEALTH;
            }

            //? if >=1.19.4 {
            player.hurt(WorldUtil.level(player).damageSources().sweetBerryBush(), damage);
            //?} else
            /*player.hurt(net.minecraft.world.damagesource.DamageSource.SWEET_BERRY_BUSH, damage);*/
            this.shell = ShellState.empty(serverPlayer, this.worldPosition);
            if (isCreative && config.enableInstantShellConstruction()) {
                this.shell.setProgress(ShellState.PROGRESS_DONE);
            }
        }
        return null;
    }

    static {
        ShellStateContainer.LOOKUP.registerForBlockEntity((x, s) -> x.hasLevel() && AbstractShellContainerBlock.isBottom(x.getBlockState()) && (s == null || s.equals(x.getShellState())) ? x : null, SyncBlockEntities.SHELL_CONSTRUCTOR);
    }
}
