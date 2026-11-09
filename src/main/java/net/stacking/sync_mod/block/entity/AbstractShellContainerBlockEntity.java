package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.util.nbt.Nbt;
import net.stacking.sync_mod.util.nbt.ItemStackNbt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.stacking.sync_mod.api.networking.ShellDestroyedPacket;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.api.shell.ShellStateContainer;
import net.stacking.sync_mod.api.shell.ShellStateManager;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.ComparatorOutputType;
import net.stacking.sync_mod.item.SimpleInventory;
import net.stacking.sync_mod.util.ItemUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public abstract class AbstractShellContainerBlockEntity extends BlockEntity implements ShellStateContainer, DoubleBlockEntity, TickableBlockEntity, Container {
    protected final BooleanAnimator doorAnimator;
    protected ShellState shell;
    protected DyeColor color;
    protected int progressComparatorOutput;
    protected int inventoryComparatorOutput;
    private AbstractShellContainerBlockEntity bottomPart;

    private ShellState syncedShell;
    private BlockPos syncedShellPos;
    private DyeColor syncedShellColor;
    private float syncedShellProgress;
    private DyeColor syncedColor;
    private boolean inventoryDirty;
    private boolean visibleInventoryDirty;
    private boolean topPartChecked;

    public AbstractShellContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.doorAnimator = new BooleanAnimator(AbstractShellContainerBlock.isOpen(state));
    }

    @Override
    public void setShellState(ShellState shell) {
        this.shell = shell;
        if (this.level != null && !this.level.isClientSide() && this.worldPosition != null && this.getBlockState() != null) {
            this.checkShellState(this.level, this.worldPosition, this.getBlockState());
        }
    }

    @Override
    public ShellState getShellState() {
        return this.shell;
    }

    @Override
    @Nullable
    public DyeColor getColor() {
        return this.color;
    }

    public int getProgressComparatorOutput() {
        return this.getBottomPart().map(x -> x.progressComparatorOutput).orElse(0);
    }

    public int getInventoryComparatorOutput() {
        return this.getBottomPart().map(x -> x.inventoryComparatorOutput).orElse(0);
    }

    protected ShellStateManager getShellStateManager() {
        return (ShellStateManager)Objects.requireNonNull(this.level).getServer();
    }

    protected Optional<AbstractShellContainerBlockEntity> getBottomPart() {
        if (this.bottomPart == null && this.level != null) {
            this.bottomPart = AbstractShellContainerBlock.isBottom(this.getBlockState())
                    ? this
                    : (this.level.getBlockEntity(this.worldPosition.relative(Direction.DOWN)) instanceof AbstractShellContainerBlockEntity x ? x : null);
        }
        return Optional.ofNullable(this.bottomPart);
    }

    @Override
    public void onServerTick(Level world, BlockPos pos, BlockState state) {
        this.ensureTopPartExists(world, pos);
        this.checkShellState(world, pos, state);
    }

    /**
     * Machines placed with older builds of this port have no block entity in their top half,
     * so the top half never renders. Create it (once) if it's missing.
     */
    private void ensureTopPartExists(Level world, BlockPos pos) {
        if (this.topPartChecked) {
            return;
        }
        this.topPartChecked = true;

        BlockPos topPos = pos.above();
        BlockState topState = world.getBlockState(topPos);
        if (!topState.is(this.getBlockState().getBlock())) {
            return;
        }

        LevelChunk chunk = world.getChunkAt(topPos);
        if (chunk.getBlockEntity(topPos, LevelChunk.EntityCreationType.CHECK) != null) {
            return;
        }

        BlockEntity topPart = chunk.getBlockEntity(topPos, LevelChunk.EntityCreationType.IMMEDIATE);
        if (topPart == null) {
            return;
        }
        if (world.isClientSide()) {
            // Rebuild the chunk section so the new top half gets rendered.
            world.sendBlockUpdated(topPos, topState, topState, Block.UPDATE_IMMEDIATE);
        } else {
            topPart.setChanged();
        }
    }

    private void checkShellState(Level world, BlockPos pos, BlockState state) {
        if (this.shell != null && this.shell.getColor() != this.color) {
            this.shell.setColor(this.color);
        }

        if (this.requiresSync()) {
            this.updateShell(this.shell != this.syncedShell, !this.visibleInventoryDirty);
            this.updateComparatorOutput(world, pos, state);

            this.syncedShellPos = this.shell == null ? null : this.shell.getPos();
            this.syncedShellColor = this.shell == null ? null : this.shell.getColor();
            this.syncedShellProgress = this.shell == null ? -1 : this.shell.getProgress();
            this.syncedShell = this.shell;
            this.syncedColor = this.color;
            this.inventoryDirty = false;
            this.visibleInventoryDirty = false;

            this.sync();
            world.blockEntityChanged(pos);
        }

        if (this.inventoryDirty) {
            this.updateComparatorOutput(world, pos, state);
            this.inventoryDirty = false;
            world.blockEntityChanged(pos);
        }
    }

    private boolean requiresSync() {
        return (
                this.visibleInventoryDirty ||
                        this.syncedShell != this.shell ||
                        this.syncedColor != this.color ||
                        this.shell != null && (
                                !this.shell.getPos().equals(this.syncedShellPos) ||
                                        !Objects.equals(this.shell.getColor(), this.syncedShellColor) ||
                                        this.shell.getProgress() != this.syncedShellProgress
                        )
        );
    }

    private void updateShell(boolean isNew, boolean partialUpdate) {
        ShellStateManager shellManager = this.getShellStateManager();
        if (isNew) {
            if (this.syncedShell != null) {
                shellManager.remove(this.syncedShell);
            }
            // Only add the new shell if it actually exists.
            if (this.shell != null) {
                shellManager.add(this.shell);
            }
        } else if (partialUpdate) {
            if (this.shell != null) {
                shellManager.update(this.shell);
            }
        } else {
            if (this.shell != null) {
                shellManager.add(this.shell);
            }
        }
    }

    private void updateComparatorOutput(Level world, BlockPos pos, BlockState state) {
        int currentProgressOutput = this.shell == null ? 0 : Mth.clamp((int)(this.shell.getProgress() * 15), 1, 15);
        int currentInventoryOutput = this.shell == null ? 0 : AbstractContainerMenu.getRedstoneSignalFromContainer(this.shell.getInventory());
        BlockPos topPartPos = pos.relative(AbstractShellContainerBlock.getDirectionTowardsAnotherPart(state));
        BlockState topPartState = world.getBlockState(topPartPos);

        if (this.progressComparatorOutput != currentProgressOutput) {
            this.progressComparatorOutput = currentProgressOutput;
            if (state.getValue(AbstractShellContainerBlock.OUTPUT) == ComparatorOutputType.PROGRESS) {
                world.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            if (topPartState.hasProperty(AbstractShellContainerBlock.OUTPUT) &&
                    topPartState.getValue(AbstractShellContainerBlock.OUTPUT) == ComparatorOutputType.PROGRESS) {
                world.updateNeighbourForOutputSignal(topPartPos, topPartState.getBlock());
            }
        }

        if (this.inventoryComparatorOutput != currentInventoryOutput) {
            this.inventoryComparatorOutput = currentInventoryOutput;
            if (state.getValue(AbstractShellContainerBlock.OUTPUT) == ComparatorOutputType.INVENTORY) {
                world.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            if (topPartState.hasProperty(AbstractShellContainerBlock.OUTPUT) &&
                    topPartState.getValue(AbstractShellContainerBlock.OUTPUT) == ComparatorOutputType.INVENTORY) {
                world.updateNeighbourForOutputSignal(topPartPos, topPartState.getBlock());
            }
        }
    }

    @Override
    public void onClientTick(Level world, BlockPos pos, BlockState state) {
        this.ensureTopPartExists(world, pos);
        this.doorAnimator.setValue(AbstractShellContainerBlock.isOpen(state));
        this.doorAnimator.step();
    }

    //? if >=1.21.5 {
    /*/^*
     * Called just before the block is removed. Destroys the stored shell (dropping its items).
     * Deliberately doesn't call super, which would also drop this container's contents.
     ^/
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && AbstractShellContainerBlock.isBottom(state)) {
            this.onBreak(this.level, pos);
        }
    }
    *///?}

    public void onBreak(Level world, BlockPos pos) {
        if (this.shell != null && world instanceof ServerLevel serverWorld) {
            this.getShellStateManager().remove(this.shell);
            this.destroyShell(serverWorld, pos);
        }
    }

    protected void destroyShell(ServerLevel world, BlockPos pos) {
        if (this.shell != null) {
            this.shell.drop(world, pos);
            new ShellDestroyedPacket(pos).send(PlayerLookup.around(world, pos, 32));
            this.shell = null;
        }
    }

    public abstract InteractionResult onUse(Level world, BlockPos pos, Player player, InteractionHand hand);

    @Environment(EnvType.CLIENT)
    public float getDoorOpenProgress(float tickDelta) {
        return this.getBottomPart().map(x -> x.doorAnimator.getProgress(tickDelta)).orElse(0f);
    }

    @Override
    public DoubleBlockCombiner.BlockType getBlockType(BlockState state) {
        return AbstractShellContainerBlock.getShellContainerHalf(state);
    }

    protected void sync() {
        if (this.level instanceof ServerLevel serverWorld) {
            serverWorld.getChunkSource().blockChanged(this.worldPosition);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if >=1.21.6 {
    /*@Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return this.saveWithoutMetadata(registryLookup);
    }

    // Data is kept in our own compound tag so the same code works on every version.
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag nbt = new CompoundTag();
        this.writeData(nbt, this.level == null ? null : this.level.registryAccess());
        output.store("sync", CompoundTag.CODEC, nbt);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        this.readData(input.read("sync", CompoundTag.CODEC).orElseGet(CompoundTag::new), input.lookup());
    }
    *///?} else if >=1.20.5 {
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return this.saveWithoutMetadata(registryLookup);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        this.writeData(nbt, registryLookup);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        this.readData(nbt, registryLookup);
    }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        this.writeData(nbt, null);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.readData(nbt, null);
    }
    *///?}

    /**
     * Writes this block entity's data. {@code registryLookup} is null before 1.20.5.
     */
    protected void writeData(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        if (this.shell != null) {
            nbt.put("shell", this.shell.writeNbt(new CompoundTag(), registryLookup));
        }
        nbt.putInt("color", this.color == null ? -1 : this.color.getId());
    }

    protected void readData(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        this.shell = nbt.contains("shell") ? ShellState.fromNbt(Nbt.getCompound(nbt, "shell"), registryLookup) : null;
        int colorId = Nbt.contains(nbt, "color", Tag.TAG_INT) ? Nbt.getInt(nbt, "color") : -1;
        this.color = colorId == -1 ? null : DyeColor.byId(colorId);
    }

    private static int reorderSlotIndex(int slot, SimpleInventory inventory) {
        final int mainSize = inventory.main.size();
        final int armorSize = inventory.armor.size();
        final int offHandSize = inventory.offHand.size();
        return (
                slot >= 0 && slot < armorSize
                        ? (slot + mainSize)
                        : slot >= armorSize && slot < (armorSize + offHandSize)
                        ? (slot + mainSize)
                        : (slot - armorSize - offHandSize)
        );
    }

    private static boolean isVisibleSlot(int slot, SimpleInventory inventory) {
        final int armorSize = inventory.armor.size();
        final int offHandSize = inventory.offHand.size();
        return slot >= 0 && slot <= (armorSize + offHandSize);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        AbstractShellContainerBlockEntity bottom = this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) {
            return false;
        }

        SimpleInventory inventory = bottom.shell.getInventory();
        final int armorSize = inventory.armor.size();
        boolean isArmorSlot = slot >= 0 && slot < armorSize;
        if (isArmorSlot) {
            EquipmentSlot equipmentSlot = ItemUtil.getPreferredEquipmentSlot(stack);
            return ItemUtil.isArmor(stack) && equipmentSlot.getType() == ItemUtil.armorSlotType() && slot == equipmentSlot.getIndex();
        }

        boolean isOffHandSlot = slot >= armorSize && slot < (armorSize + inventory.offHand.size());
        if (isOffHandSlot) {
            return ItemUtil.getPreferredEquipmentSlot(stack) == EquipmentSlot.OFFHAND ||
                    inventory.main.stream().noneMatch(x -> x.isEmpty() ||
                            (x.getCount() + stack.getCount()) <= x.getMaxStackSize() && ItemStackNbt.isSameItemSameData(x, stack));
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.getBottomPart()
                .filter(x -> x.shell != null)
                .map(x -> x.shell.getInventory().getItem(reorderSlotIndex(slot, x.shell.getInventory())))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        AbstractShellContainerBlockEntity bottom = this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null || bottom.shell.getProgress() < ShellState.PROGRESS_DONE) {
            return;
        }

        SimpleInventory inventory = bottom.shell.getInventory();
        inventory.setItem(reorderSlotIndex(slot, inventory), stack);
        bottom.inventoryDirty = true;
        bottom.visibleInventoryDirty |= isVisibleSlot(slot, inventory);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        AbstractShellContainerBlockEntity bottom = this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) {
            return ItemStack.EMPTY;
        }

        SimpleInventory inventory = bottom.shell.getInventory();
        ItemStack removed = inventory.removeItem(reorderSlotIndex(slot, inventory), amount);
        bottom.inventoryDirty = true;
        bottom.visibleInventoryDirty |= !removed.isEmpty() && isVisibleSlot(slot, inventory);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        AbstractShellContainerBlockEntity bottom = this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) {
            return ItemStack.EMPTY;
        }

        SimpleInventory inventory = bottom.shell.getInventory();
        ItemStack removed = inventory.removeItemNoUpdate(reorderSlotIndex(slot, inventory));
        bottom.inventoryDirty = true;
        bottom.visibleInventoryDirty |= !removed.isEmpty() && isVisibleSlot(slot, inventory);
        return removed;
    }

    @Override
    public void clearContent() {
        AbstractShellContainerBlockEntity bottom = this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) {
            return;
        }

        bottom.shell.getInventory().clearContent();
        bottom.inventoryDirty = true;
        bottom.visibleInventoryDirty = true;
    }

    @Override
    public int getContainerSize() {
        return this.getBottomPart()
                .map(x -> x.shell == null || x.shell.getProgress() < ShellState.PROGRESS_DONE ? 0 : x.shell.getInventory().getContainerSize())
                .orElse(0);
    }

    @Override
    public boolean isEmpty() {
        return this.getBottomPart()
                .map(x -> x.shell == null || x.shell.getInventory().isEmpty())
                .orElse(true);
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}