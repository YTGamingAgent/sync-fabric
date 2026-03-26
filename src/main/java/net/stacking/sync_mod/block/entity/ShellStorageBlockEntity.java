package net.stacking.sync_mod.block.entity;

import net.minecraft.nbt.NbtCompound;
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
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
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
    private boolean isPrimaryStorage = false;

    public ShellStorageBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SHELL_STORAGE, pos, state);
        this.entityState = EntityState.NONE;
        this.connectorAnimator = new BooleanAnimator(false);
    }

    // ── GeoBlockEntity ────────────────────────────────────────────────────────

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

    // ── Indicator color (red/green/blue) ──────────────────────────────────────

    public DyeColor getIndicatorColor() {
        // Check power first — always show RED when unpowered, even if primary
        boolean isPowered = this.world != null && ShellStorageBlock.isPowered(this.world.getBlockState(this.pos));

        if (!isPowered) {
            return DyeColor.RED;  // Unpowered = always red (even if primary)
        }

        // Powered: check if primary
        if (this.isPrimaryStorage) {
            return DyeColor.BLUE;  // Powered + primary = blue
        }

        // Powered but not primary = green or custom dye color
        return this.color == null ? DyeColor.LIME : this.color;
    }

    public boolean isPrimaryStorage() { return this.isPrimaryStorage; }

    private void setPrimaryStorage(boolean primary) {
        if (this.isPrimaryStorage == primary) return;
        this.isPrimaryStorage = primary;
        sync(); // ← call sync instead of just markDirty()
    }

    /**
     * Marks dirty for saving AND fires an update packet to watching clients.
     */
    protected void sync() {
        markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(),
                    Block.NOTIFY_LISTENERS);
        }
    }

    @Environment(EnvType.CLIENT)
    public float getConnectorProgress(float tickDelta) {
        return this.getBottomPart()
                .map(x -> ((ShellStorageBlockEntity) x).connectorAnimator.getProgress(tickDelta))
                .orElse(0f);
    }

    // ── Client sync ───────────────────────────────────────────────────────
    // Send the primary storage flag to all watching clients immediately.

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    @Override
    public void onServerTick(World world, BlockPos pos, BlockState state) {
        super.onServerTick(world, pos, state);

        SyncConfig config = Sync.getConfig();
        boolean infinitePower = config.shellStorageConsumption() == 0;
        boolean isReceivingRedstonePower = !infinitePower
                && config.shellStorageAcceptsRedstone()
                && ShellStorageBlock.isEnabled(state);
        boolean hasEnergy = infinitePower || this.storedEnergy > 0;
        boolean isPowered = infinitePower || isReceivingRedstonePower || hasEnergy;
        boolean shouldBeOpen = isPowered;

        ShellStorageBlock.setPowered(state, world, pos, isPowered);
        ShellStorageBlock.setOpen(state, world, pos, shouldBeOpen);

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

        if (this.entityState == EntityState.NONE) {
            boolean isInside = BlockPosUtil.isEntityInside(entity, this.pos);
            if (!isInside) {
                double dx  = entity.getX() - (this.pos.getX() + 0.5);
                double dz  = entity.getZ() - (this.pos.getZ() + 0.5);
                double dot = dx * state.get(ShellStorageBlock.FACING).getOffsetX()
                        + dz * state.get(ShellStorageBlock.FACING).getOffsetZ();
                if (dot <= 0) return;
            }
            PlayerSyncEvents.ShellSelectionFailureReason failureReason = !isInside && client.player == entity
                    ? PlayerSyncEvents.ALLOW_SHELL_SELECTION.invoker().allowShellSelection(player, this)
                    : null;
            this.entityState = isInside || failureReason != null
                    ? EntityState.CHILLING : EntityState.ENTERING;
            if (failureReason != null) player.sendMessage(failureReason.toText(), true);

        } else if (this.entityState != EntityState.CHILLING && client.currentScreen == null) {
            BlockPosUtil.moveEntity(entity, this.pos, state.get(ShellStorageBlock.FACING),
                    this.entityState == EntityState.ENTERING);
        }

        if (this.entityState == EntityState.ENTERING && client.player == entity
                && client.currentScreen == null
                && BlockPosUtil.isEntityInside(entity, this.pos)) {
            client.setScreen(new ShellSelectorGUI(
                    () -> this.entityState = EntityState.CHILLING,
                    () -> this.entityState = EntityState.LEAVING
            ));
        }
    }

    // ── Right-click interaction ───────────────────────────────────────────────

    @Override
    public ActionResult onUse(World world, BlockPos pos, PlayerEntity player, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();

        // Bed right-click: make this storage primary ────────────────────────────
        if (item.getName().getString().toLowerCase().contains("bed")) {
            // Check if powered
            BlockState state = world.getBlockState(pos);
            if (!ShellStorageBlock.isPowered(state)) {
                player.sendMessage(Text.literal("§cShell Storage must be powered to set as primary!"), true);
                return ActionResult.FAIL;
            }

            // If already primary, do nothing
            if (this.isPrimaryStorage) {
                player.sendMessage(Text.literal("§aThis is already the primary Shell Storage."), true);
                return ActionResult.SUCCESS;
            }

            // Set this as primary and unset all others nearby
            setPrimaryStorage(true);
            unsetAllOtherPrimaryStorages(world, pos);
            player.sendMessage(Text.literal("§bThis Shell Storage is now the primary storage!"), true);
            return ActionResult.SUCCESS;
        }

        // Dye right-click: recolor indicator ───────────────────────────────────
        if (item instanceof DyeItem dye) {
            if (stack.getCount() > 0) {
                stack.decrement(1);
                this.color = dye.getColor();
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    /**
     * Finds all other ShellStorageBlockEntities within a reasonable range
     * (e.g., 100 blocks) and unsets their isPrimaryStorage flag.
     */
    private void unsetAllOtherPrimaryStorages(World world, BlockPos myPos) {
        int range = 100;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = myPos.getX() - range; x <= myPos.getX() + range; x++) {
            for (int y = myPos.getY() - range; y <= myPos.getY() + range; y++) {
                for (int z = myPos.getZ() - range; z <= myPos.getZ() + range; z++) {
                    mutable.set(x, y, z);
                    if (mutable.equals(myPos)) continue; // Skip self

                    var blockEntity = world.getBlockEntity(mutable);
                    if (blockEntity instanceof ShellStorageBlockEntity other) {
                        other.setPrimaryStorage(false);
                    }
                }
            }
        }
    }

    // ── NBT persistence ──────────────────────────────────────────────────────

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.isPrimaryStorage = nbt.getBoolean("IsPrimary");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("IsPrimary", this.isPrimaryStorage);
    }


    // ── EnergyStorage ─────────────────────────────────────────────────────────

    @Override public boolean supportsInsertion() {
        return Sync.getConfig().shellStorageConsumption() != 0;
    }

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