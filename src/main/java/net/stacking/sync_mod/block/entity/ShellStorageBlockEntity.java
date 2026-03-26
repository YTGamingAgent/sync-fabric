package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.api.shell.ShellStateContainer;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.ShellStorageBlock;
import net.stacking.sync_mod.client.gui.ShellSelectorGUI;
import net.stacking.sync_mod.config.SyncConfig;
import net.stacking.sync_mod.util.BlockPosUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

@SuppressWarnings({"UnstableApiUsage"})
public class ShellStorageBlockEntity extends AbstractShellContainerBlockEntity
        implements EnergyStorage, GeoBlockEntity {

    private static final RawAnimation DOOR_OPEN =
            RawAnimation.begin().thenPlayAndHold("animation.shell_storage.door_open");
    private static final RawAnimation DOOR_CLOSE =
            RawAnimation.begin().thenPlayAndHold("animation.shell_storage.door_close");
    private static final RawAnimation SHELL_INSERT =
            RawAnimation.begin().thenPlayAndHold("animation.shell_storage.shell_insert");
    private static final RawAnimation SHELL_EJECT =
            RawAnimation.begin().thenPlayAndHold("animation.shell_storage.shell_eject");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private EntityState entityState;
    private int ticksWithoutPower;
    private long storedEnergy;
    private final BooleanAnimator connectorAnimator;

    public ShellStorageBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SHELL_STORAGE, pos, state);
        this.entityState = EntityState.NONE;
        this.connectorAnimator = new BooleanAnimator(false);
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "doors", 0, state -> {
            if (this.hasWorld() && AbstractShellContainerBlock.isOpen(this.getCachedState())) {
                return state.setAndContinue(DOOR_OPEN);
            }
            return state.setAndContinue(DOOR_CLOSE);
        }));
        controllers.add(new AnimationController<>(this, "connector", 0, state -> {
            float progress = this.getConnectorProgress(0);
            if (progress > 0.5f) return state.setAndContinue(SHELL_INSERT);
            else if (progress > 0f) return state.setAndContinue(SHELL_EJECT);
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ── Indicator ─────────────────────────────────────────────────────────────

    public DyeColor getIndicatorColor() {
        // Always read the live world state — never the stale getCachedState() snapshot
        if (this.world != null && ShellStorageBlock.isPowered(this.world.getBlockState(this.pos))) {
            return this.color == null ? DyeColor.LIME : this.color;
        }
        return DyeColor.RED;
    }

    @Environment(EnvType.CLIENT)
    public float getConnectorProgress(float tickDelta) {
        return this.getBottomPart()
                .map(x -> ((ShellStorageBlockEntity) x).connectorAnimator.getProgress(tickDelta))
                .orElse(0f);
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    @Override
    public void onServerTick(World world, BlockPos pos, BlockState state) {
        super.onServerTick(world, pos, state);

        SyncConfig config = Sync.getConfig();
        boolean infinitePower = config.shellStorageConsumption() == 0;

        // ── FIX: read the LIVE world state, not the stale 'state' snapshot ──
        // The snapshot may be several ticks behind after neighbourUpdate changes.
        BlockState liveState = world.getBlockState(pos);

        boolean isReceivingRedstonePower = !infinitePower
                && config.shellStorageAcceptsRedstone()
                && ShellStorageBlock.isEnabled(liveState);   // ← live state ✅

        boolean hasEnergy  = infinitePower || this.storedEnergy > 0;
        boolean isPowered  = infinitePower || isReceivingRedstonePower || hasEnergy;

        // Doors open when the storage is empty regardless of power —
        // power is only needed to keep a stored shell alive.
        boolean shouldBeOpen = isPowered;

        // ── FIX: combine POWERED + OPEN into ONE setBlockState call ──────────
        // Two separate calls both start from 'liveState', so the second call
        // would overwrite the first call's changes (POWERED reset to false).
        BlockState newState = liveState
                .with(ShellStorageBlock.POWERED, isPowered)
                .with(AbstractShellContainerBlock.OPEN, shouldBeOpen);
        if (!newState.equals(liveState)) {
            world.setBlockState(pos, newState, net.minecraft.block.Block.NOTIFY_ALL);
        }

        // Shell degradation when unpowered
        if (!infinitePower) {
            if (this.shell != null && !isPowered) {
                ++this.ticksWithoutPower;
                if (this.ticksWithoutPower >= config.shellStorageMaxUnpoweredLifespan()) {
                    this.destroyShell((ServerWorld) world, pos);
                }
            } else {
                this.ticksWithoutPower = 0;
            }
        }

        // Energy drain
        if (!infinitePower && !isReceivingRedstonePower && hasEnergy) {
            this.storedEnergy = (long) MathHelper.clamp(
                    this.storedEnergy - config.shellStorageConsumption(),
                    0, config.shellStorageCapacity());
        }
    }

    // ── Client tick ───────────────────────────────────────────────────────────

    @Override
    public void onClientTick(World world, BlockPos pos, BlockState state) {
        super.onClientTick(world, pos, state);
        this.connectorAnimator.setValue(this.shell != null);
        this.connectorAnimator.step();
        if (this.entityState == EntityState.LEAVING || this.entityState == EntityState.CHILLING) {
            this.entityState = BlockPosUtil.hasPlayerInside(pos, world)
                    ? this.entityState : EntityState.NONE;
        }
    }

    // ── Entity collision (client) ─────────────────────────────────────────────

    @Environment(EnvType.CLIENT)
    public void onEntityCollisionClient(Entity entity, BlockState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(entity instanceof PlayerEntity player)) return;

        Direction facing = state.get(ShellStorageBlock.FACING);

        if (this.entityState == EntityState.NONE) {
            boolean isInside = BlockPosUtil.isEntityInside(entity, this.pos);

            // Only allow entry from the front face ────────────────────────────
            if (!isInside) {
                double dx  = entity.getX() - (this.pos.getX() + 0.5);
                double dz  = entity.getZ() - (this.pos.getZ() + 0.5);
                double dot = dx * facing.getOffsetX() + dz * facing.getOffsetZ();
                // dot <= 0 means player is to the side or behind — reject
                if (dot <= 0) return;
            }

            PlayerSyncEvents.ShellSelectionFailureReason failureReason = !isInside && client.player == entity
                    ? PlayerSyncEvents.ALLOW_SHELL_SELECTION.invoker().allowShellSelection(player, this)
                    : null;
            this.entityState = isInside || failureReason != null
                    ? EntityState.CHILLING : EntityState.ENTERING;
            if (failureReason != null) player.sendMessage(failureReason.toText(), true);

        } else if (this.entityState != EntityState.CHILLING && client.currentScreen == null) {
            BlockPosUtil.moveEntity(entity, this.pos, facing, this.entityState == EntityState.ENTERING);
        }

        if (this.entityState == EntityState.ENTERING && client.player == entity
                && client.currentScreen == null
                && BlockPosUtil.isEntityInside(entity, this.pos)) {
            client.setScreen(new ShellSelectorGUI(
                    () -> this.entityState = EntityState.CHILLING,  // Escape / close = stay inside
                    () -> this.entityState = EntityState.LEAVING    // Leave button = exit
            ));
        }
    }

    // ── Right-click (dye recolor) ─────────────────────────────────────────────

    @Override
    public ActionResult onUse(World world, BlockPos pos, PlayerEntity player, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        if (stack.getCount() > 0 && item instanceof DyeItem dye) {
            stack.decrement(1);
            this.color = dye.getColor();
        }
        return ActionResult.SUCCESS;
    }

    // ── EnergyStorage ─────────────────────────────────────────────────────────

    @Override public boolean supportsInsertion() { return Sync.getConfig().shellStorageConsumption() != 0; }
    @Override public boolean supportsExtraction() { return false; }

    @Override
    public long insert(long amount, TransactionContext context) {
        if (Sync.getConfig().shellStorageConsumption() == 0) return 0;
        ShellStorageBlockEntity bottom = (ShellStorageBlockEntity) this.getBottomPart().orElse(null);
        if (bottom == null) return 0;
        long capacity  = bottom.getCapacity();
        long maxEnergy = (long) MathHelper.clamp(capacity - bottom.storedEnergy, 0, capacity);
        long inserted  = (long) MathHelper.clamp(amount, 0, maxEnergy);
        context.addCloseCallback((ctx, result) -> {
            if (result.wasCommitted()) bottom.storedEnergy += inserted;
        });
        return inserted;
    }

    @Override public long extract(long amount, TransactionContext context) { return 0; }
    @Override public long getAmount() { return 0; }
    @Override public long getCapacity() {
        return Sync.getConfig().shellStorageConsumption() == 0
                ? 0 : Sync.getConfig().shellStorageCapacity();
    }

    private enum EntityState { NONE, ENTERING, CHILLING, LEAVING }

    static {
        ShellStateContainer.LOOKUP.registerForBlockEntity(
                (x, s) -> x.hasWorld() && AbstractShellContainerBlock.isBottom(x.getCachedState())
                        && (s == null || s.equals(x.getShellState())) ? x : null,
                SyncBlockEntities.SHELL_STORAGE);
        EnergyStorage.SIDED.registerForBlockEntities(
                (x, __) -> (EnergyStorage) x, SyncBlockEntities.SHELL_STORAGE);
    }
}