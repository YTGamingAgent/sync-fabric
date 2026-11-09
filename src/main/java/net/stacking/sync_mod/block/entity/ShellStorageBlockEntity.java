package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.util.ChatUtil;

import net.stacking.sync_mod.util.WorldUtil;

import net.stacking.sync_mod.util.nbt.Nbt;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.event.PlayerSyncEvents;
import net.stacking.sync_mod.api.shell.ServerShell;
import net.stacking.sync_mod.api.shell.ShellStateContainer;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.ShellStorageBlock;
import net.stacking.sync_mod.client.gui.ShellSelectorGUI;
import net.stacking.sync_mod.config.SyncConfig;
import net.stacking.sync_mod.util.BlockPosUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"UnstableApiUsage"})
public class ShellStorageBlockEntity extends AbstractShellContainerBlockEntity implements EnergyStorage {
    private static final int PRIMARY_OWNER_CHECK_INTERVAL = 20;

    private EntityState entityState;
    private int ticksWithoutPower;
    private long storedEnergy;
    private final BooleanAnimator connectorAnimator;

    /**
     * The player whose primary storage this is. While a player is in a shell,
     * their original body is parked here (see {@link #findPrimaryStorage}).
     */
    @Nullable
    private UUID primaryOwner;

    public ShellStorageBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SHELL_STORAGE, pos, state);
        this.entityState = EntityState.NONE;
        this.connectorAnimator = new BooleanAnimator(false);
    }

    public DyeColor getIndicatorColor() {
        if (this.level != null && ShellStorageBlock.isPowered(this.getBlockState())) {
            if (this.primaryOwner != null) {
                return DyeColor.BLUE;
            }
            return this.color == null ? DyeColor.LIME : this.color;
        }

        return DyeColor.RED;
    }

    @Nullable
    public UUID getPrimaryOwner() {
        return this.primaryOwner;
    }

    @Environment(EnvType.CLIENT)
    public float getConnectorProgress(float tickDelta) {
        return this.getBottomPart().map(x -> ((ShellStorageBlockEntity)x).connectorAnimator.getProgress(tickDelta)).orElse(0f);
    }

    @Override
    public void onServerTick(Level world, BlockPos pos, BlockState state) {
        super.onServerTick(world, pos, state);

        SyncConfig config = Sync.getConfig();
        boolean infinitePower = config.shellStorageConsumption() == 0;
        boolean isReceivingRedstonePower = !infinitePower
                && config.shellStorageAcceptsRedstone()
                && ShellStorageBlock.isEnabled(state);
        boolean hasEnergy = infinitePower || this.storedEnergy > 0;
        boolean isPowered = infinitePower || isReceivingRedstonePower || hasEnergy;
        boolean shouldBeOpen = isPowered && this.getBottomPart().map(x -> x.shell == null).orElse(true);

        ShellStorageBlock.setPowered(state, world, pos, isPowered);
        ShellStorageBlock.setOpen(world.getBlockState(pos), world, pos, shouldBeOpen);

        if (!infinitePower) {
            if (this.shell != null && !isPowered) {
                ++this.ticksWithoutPower;
                if (this.ticksWithoutPower >= config.shellStorageMaxUnpoweredLifespan()) {
                    this.destroyShell((ServerLevel)world, pos);
                }
            } else {
                this.ticksWithoutPower = 0;
            }
        }

        if (!infinitePower && !isReceivingRedstonePower && hasEnergy) {
            this.storedEnergy = (long)Mth.clamp(this.storedEnergy - config.shellStorageConsumption(), 0, config.shellStorageCapacity());
        }

        if (this.primaryOwner != null && world.getGameTime() % PRIMARY_OWNER_CHECK_INTERVAL == 0) {
            this.validatePrimaryOwner((ServerLevel)world);
        }
    }

    @Override
    public void onClientTick(Level world, BlockPos pos, BlockState state) {
        super.onClientTick(world, pos, state);
        this.connectorAnimator.setValue(this.shell != null);
        this.connectorAnimator.step();
        if (this.entityState == EntityState.LEAVING || this.entityState == EntityState.CHILLING) {
            this.entityState = BlockPosUtil.hasPlayerInside(pos, world) ? this.entityState : EntityState.NONE;
        }
    }

    @Environment(EnvType.CLIENT)
    public void onEntityCollisionClient(Entity entity, BlockState state) {
        Minecraft client = Minecraft.getInstance();
        if (!(entity instanceof Player player)) {
            return;
        }

        if (this.entityState == EntityState.NONE) {
            boolean isInside = BlockPosUtil.isEntityInside(entity, this.worldPosition);
            PlayerSyncEvents.ShellSelectionFailureReason failureReason = !isInside && client.player == entity ? PlayerSyncEvents.ALLOW_SHELL_SELECTION.invoker().allowShellSelection(player, this) : null;
            this.entityState = isInside || failureReason != null ? EntityState.CHILLING : EntityState.ENTERING;
            if (failureReason != null) {
                ChatUtil.message(player, failureReason.toText(), true);
            }
        } else if (this.entityState != EntityState.CHILLING && client.screen == null) {
            BlockPosUtil.moveEntity(entity, this.worldPosition, state.getValue(ShellStorageBlock.FACING), this.entityState == EntityState.ENTERING);
        }

        if (this.entityState == EntityState.ENTERING && client.player == entity && client.screen == null && BlockPosUtil.isEntityInside(entity, this.worldPosition)) {
            client.setScreen(new ShellSelectorGUI(() -> this.entityState = EntityState.LEAVING, () -> this.entityState = EntityState.CHILLING));
        }
    }

    @Override
    public InteractionResult onUse(Level world, BlockPos pos, Player player, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemTags.BEDS) && player instanceof ServerPlayer serverPlayer) {
            this.claimAsPrimaryStorage(serverPlayer);
        } else if (stack.getItem() instanceof DyeItem dye && !stack.isEmpty()) {
            //? if >=26.1 {
            /*DyeColor dyeColor = stack.get(net.minecraft.core.component.DataComponents.DYE);
            *///?} else
            DyeColor dyeColor = dye.getDyeColor();
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (dyeColor != null) {
                this.color = dyeColor;
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Makes this storage the player's primary storage (right-click with a bed).
     * A player has at most one primary storage; claiming a new one releases the old one.
     */
    private void claimAsPrimaryStorage(ServerPlayer player) {
        if (!(this.level instanceof ServerLevel serverWorld)) {
            return;
        }
        if (!ShellStorageBlock.isPowered(this.getBlockState())) {
            ChatUtil.message(player, Component.translatable("message.sync.shell_storage.primary.unpowered").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (player.getUUID().equals(this.primaryOwner)) {
            ChatUtil.message(player, Component.translatable("message.sync.shell_storage.primary.already").withStyle(ChatFormatting.GREEN), true);
            return;
        }
        if (this.primaryOwner != null) {
            ChatUtil.message(player, Component.translatable("message.sync.shell_storage.primary.taken").withStyle(ChatFormatting.RED), true);
            return;
        }

        ServerShell shell = (ServerShell)player;
        ShellStorageBlockEntity previous = getPrimaryStorageAt(player, shell.getPrimaryShellStorage());
        if (previous != null && previous != this) {
            previous.setPrimaryOwner(null);
        }

        shell.setPrimaryShellStorage(GlobalPos.of(serverWorld.dimension(), this.worldPosition));
        this.setPrimaryOwner(player.getUUID());
        ChatUtil.message(player, Component.translatable("message.sync.shell_storage.primary.set").withStyle(ChatFormatting.AQUA), true);
    }

    private void setPrimaryOwner(@Nullable UUID owner) {
        if (Objects.equals(this.primaryOwner, owner)) {
            return;
        }
        this.primaryOwner = owner;
        this.setChanged();
        this.sync();
    }

    /**
     * Drops the primary flag if the owner has since picked another primary storage
     * (e.g. while this one was in an unloaded chunk).
     */
    private void validatePrimaryOwner(ServerLevel world) {
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(this.primaryOwner);
        if (owner != null && !GlobalPos.of(world.dimension(), this.worldPosition).equals(((ServerShell)owner).getPrimaryShellStorage())) {
            this.setPrimaryOwner(null);
        }
    }

    @Override
    public void onBreak(Level world, BlockPos pos) {
        if (this.primaryOwner != null && world instanceof ServerLevel serverWorld) {
            ServerPlayer owner = serverWorld.getServer().getPlayerList().getPlayer(this.primaryOwner);
            if (owner != null && GlobalPos.of(serverWorld.dimension(), pos).equals(((ServerShell)owner).getPrimaryShellStorage())) {
                ((ServerShell)owner).setPrimaryShellStorage(null);
            }
        }
        super.onBreak(world, pos);
    }

    /**
     * Finds the player's primary storage if it can take their body right now:
     * the chunk is loaded, it still belongs to them, it's powered, and it's empty.
     */
    @Nullable
    public static ShellStorageBlockEntity findPrimaryStorage(ServerPlayer player) {
        ShellStorageBlockEntity storage = getPrimaryStorageAt(player, ((ServerShell)player).getPrimaryShellStorage());
        if (storage == null || storage.shell != null || !ShellStorageBlock.isPowered(storage.getBlockState())) {
            return null;
        }
        return storage;
    }

    @Nullable
    private static ShellStorageBlockEntity getPrimaryStorageAt(ServerPlayer player, @Nullable GlobalPos location) {
        if (location == null) {
            return null;
        }
        ServerLevel world = WorldUtil.server(player).getLevel(location.dimension());
        if (world == null || !world.hasChunkAt(location.pos())) {
            return null;
        }
        return world.getBlockEntity(location.pos()) instanceof ShellStorageBlockEntity storage
                && storage.hasLevel()
                && AbstractShellContainerBlock.isBottom(storage.getBlockState())
                && player.getUUID().equals(storage.primaryOwner)
                ? storage
                : null;
    }

    @Override
    protected void writeData(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.writeData(nbt, registryLookup);
        if (this.primaryOwner != null) {
            Nbt.putUuid(nbt, "primaryOwner", this.primaryOwner);
        }
    }

    @Override
    protected void readData(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.readData(nbt, registryLookup);
        this.primaryOwner = Nbt.getUuid(nbt, "primaryOwner");
    }

    @Override
    public boolean supportsInsertion() {
        return Sync.getConfig().shellStorageConsumption() != 0;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long insert(long amount, TransactionContext context) {
        if (Sync.getConfig().shellStorageConsumption() == 0) {
            return 0;
        }

        ShellStorageBlockEntity bottom = (ShellStorageBlockEntity)this.getBottomPart().orElse(null);
        if (bottom == null) {
            return 0;
        }

        long capacity = bottom.getCapacity();
        long maxEnergy = (long)Mth.clamp(capacity - bottom.storedEnergy, 0, capacity);
        long inserted = (long)Mth.clamp(amount, 0, maxEnergy);
        context.addCloseCallback((ctx, result) -> {
            if (result.wasCommitted()) {
                bottom.storedEnergy += inserted;
            }
        });
        return inserted;
    }

    @Override
    public long extract(long amount, TransactionContext context) {
        return 0;
    }

    @Override
    public long getAmount() {
        return 0;
    }

    @Override
    public long getCapacity() {
        return Sync.getConfig().shellStorageConsumption() == 0 ? 0 : Sync.getConfig().shellStorageCapacity();
    }

    private enum EntityState {
        NONE,
        ENTERING,
        CHILLING,
        LEAVING
    }

    static {
        ShellStateContainer.LOOKUP.registerForBlockEntity((x, s) -> x.hasLevel() && AbstractShellContainerBlock.isBottom(x.getBlockState()) && (s == null || s.equals(x.getShellState())) ? x : null, SyncBlockEntities.SHELL_STORAGE);
        EnergyStorage.SIDED.registerForBlockEntities((x, __) -> (EnergyStorage)x, SyncBlockEntities.SHELL_STORAGE);
    }
}
