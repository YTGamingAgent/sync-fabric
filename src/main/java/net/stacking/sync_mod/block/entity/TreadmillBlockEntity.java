package net.stacking.sync_mod.block.entity;

import net.stacking.sync_mod.util.EntityUtil;

import net.stacking.sync_mod.util.nbt.Nbt;
import net.stacking.sync_mod.block.TreadmillBlock;
import net.stacking.sync_mod.api.event.EntityFitnessEvents;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.UUID;

@SuppressWarnings({"UnstableApiUsage"})
public class TreadmillBlockEntity extends BlockEntity implements DoubleBlockEntity, TickableBlockEntity, EnergyStorage {
    /**
     * Energy produced per tick by any valid runner (passive or neutral mob).
     */
    public static final long ENERGY_PER_TICK = 200;

    /**
     * Running time is counted up to this many ticks (15 minutes); it's kept with the treadmill's data.
     */
    private static final int MAX_RUNNING_TIME = 20 * 60 * 15;
    /**
     * Ticks it takes for the runner's legs to go from standing still to a full run (2 seconds).
     */
    private static final int LEG_SPEED_UP_TICKS = 40;
    private static final double MAX_SQUARED_DISTANCE = 0.5;

    private UUID runnerUUID;
    private Integer runnerId;
    private Entity runner;
    private int runningTime;
    private long storedEnergy;
    private long producibleEnergyQuantity;
    private TreadmillBlockEntity cachedBackPart;


    public TreadmillBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.TREADMILL, pos, state);
    }


    private void setRunner(Entity entity) {
        if (this.runner == entity) {
            return;
        }

        if (this.runner != null) {
            EntityFitnessEvents.STOP_RUNNING.invoker().onStopRunning(this.runner, this);
        }

        if (entity == null) {
            this.runningTime = 0;
            this.producibleEnergyQuantity = 0;
        }
        this.runner = entity;

        if (this.runner != null) {
            EntityFitnessEvents.START_RUNNING.invoker().onStartRunning(this.runner, this);
        }

        if (this.level != null && !this.level.isClientSide()) {
            this.setChanged();
            this.sync();
        }
    }

    /**
     * @return {@code true} if a valid mob is currently running on this treadmill (either part can be asked).
     */
    public boolean isRunning() {
        TreadmillBlockEntity back = this.getBackPart();
        return back != null && back.runner != null;
    }

    @Override
    public void onClientTick(Level world, BlockPos pos, BlockState state) {
        if (this.runnerId != null) {
            this.setRunner(this.runnerId == -1 ? null : world.getEntity(this.runnerId));
            this.runnerId = null;
        }

        if (this.runner == null) {
            return;
        }

        if (this.runner instanceof LivingEntity livingEntity) {
            //? if >=1.19.4 {
            livingEntity.walkAnimation.update(Math.min(1.0f, (float)this.runningTime / LEG_SPEED_UP_TICKS), 1.0f/*? if >=1.21.2 {*//*, 1.0f*//*?}*/);
            //?} else
            /*livingEntity.animationSpeed = Math.min(1.0f, (float)this.runningTime / LEG_SPEED_UP_TICKS);*/
        }
        this.runningTime = Math.min(++this.runningTime, MAX_RUNNING_TIME);
    }

    @Override
    public void onServerTick(Level world, BlockPos pos, BlockState state) {
        if (this.runnerUUID != null && world instanceof ServerLevel serverWorld) {
            this.setRunner(serverWorld.getEntity(this.runnerUUID));
            this.runnerUUID = null;
        }

        if (this.runner == null) {
            return;
        }

        Direction face = state.getValue(TreadmillBlock.FACING);
        Vec3 anchor = computeTreadmillPivot(pos, face);
        if (!isValidEntity(this.runner) || !isEntityNear(this.runner, anchor)) {
            this.setRunner(null);
            return;
        }

        float yaw = face.toYRot();
        EntityUtil.absMoveTo(this.runner, anchor.x, anchor.y, anchor.z, yaw, 0);
        this.runner.setYHeadRot(yaw);
        this.runner.setYBodyRot(yaw);
        this.runner.setYRot(yaw);
        this.runner.yRotO = yaw;

        if (this.runner instanceof Mob mobEntity) {
            mobEntity.setNoActionTime(0);
        }
        this.storedEnergy = this.producibleEnergyQuantity;
        this.transferEnergy(world, pos);
        if (this.runningTime < MAX_RUNNING_TIME) {
            ++this.runningTime;
            if (this.runningTime % 1000 == 0) {
                this.setChanged();
                this.sync();
            }
        }
    }

    public void onSteppedOn(BlockPos pos, BlockState state, Entity entity) {
        if (this.runner != null || !isEntityNear(entity, computeTreadmillPivot(pos, state.getValue(TreadmillBlock.FACING)))) {
            return;
        }

        Long energy = isValidEntity(entity) ? getOutputEnergyQuantityForEntity(entity, this) : null;
        if (energy != null) {
            this.setRunner(entity);
            this.producibleEnergyQuantity = energy;
        }
    }

    @Override
    public long getAmount() {
        TreadmillBlockEntity back = this.getBackPart();
        return back == null ? 0 : back.storedEnergy;
    }

    @Override
    public long getCapacity() {
        TreadmillBlockEntity back = this.getBackPart();
        if (back == null || back.runner == null) {
            return 0;
        }
        return back.producibleEnergyQuantity;
    }

    @Override
    public boolean supportsInsertion() {
        return false;
    }

    @Override
    public boolean supportsExtraction() {
        return true;
    }

    @Override
    public long insert(long maxAmount, TransactionContext context) {
        return 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext context) {
        TreadmillBlockEntity back = this.getBackPart();
        if (back == null) {
            return 0;
        }

        long extracted = Math.min(back.storedEnergy, maxAmount);
        context.addCloseCallback((ctx, result) -> {
            if (result.wasCommitted()) {
                back.storedEnergy -= extracted;
            }
        });
        return extracted;
    }

    private void transferEnergy(Level world, BlockPos pos) {
        TreadmillBlockEntity back = this.getBackPart();
        if (back == null) {
            return;
        }

        for (int i = 0; i < 2; ++i) {
            for (Direction direction : Direction.values()) {
                EnergyStorage target = EnergyStorage.SIDED.find(world, pos.relative(direction), direction.getOpposite());
                if (target != null && target.supportsInsertion()) {
                    EnergyStorageUtil.move(back, target, Long.MAX_VALUE, null);
                    if (back.storedEnergy <= 0) {
                        return;
                    }
                }
            }
            pos = pos.relative(this.getBlockState().getValue(TreadmillBlock.FACING));
        }
    }

    @Override
    public DoubleBlockCombiner.BlockType getBlockType(BlockState state) {
        return TreadmillBlock.getTreadmillPart(state);
    }

    private TreadmillBlockEntity getBackPart() {
        if (this.cachedBackPart != null || this.level == null) {
            return this.cachedBackPart;
        }

        if (TreadmillBlock.isBack(this.getBlockState())) {
            this.cachedBackPart = this;
        } else {
            BlockPos backPartPos = this.worldPosition.relative(this.getBlockState().getValue(TreadmillBlock.FACING).getOpposite());
            this.cachedBackPart = this.level.getBlockEntity(backPartPos, SyncBlockEntities.TREADMILL).orElse(null);
        }
        return this.cachedBackPart;
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

    private void readData(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        this.runnerUUID = Nbt.getUuid(nbt, "runner");
        this.runnerId = Nbt.contains(nbt, "runnerId", Tag.TAG_INT) ? Nbt.getInt(nbt, "runnerId") : -1;
        this.producibleEnergyQuantity = Nbt.getLong(nbt, "energy");
        this.runningTime = Nbt.getInt(nbt, "time");
    }

    private void writeData(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        UUID runnerUuid = this.runnerUUID == null ? this.runner == null ? null : this.runner.getUUID() : this.runnerUUID;
        if (runnerUuid != null) {
            Nbt.putUuid(nbt, "runner", runnerUuid);
        }
        if (this.runner != null) {
            nbt.putInt("runnerId", this.runner.getId());
        }
        nbt.putLong("energy", this.producibleEnergyQuantity);
        nbt.putInt("time", this.runningTime);
    }

    private static Long getOutputEnergyQuantityForEntity(Entity entity, EnergyStorage energyStorage) {
        return EntityFitnessEvents.MODIFY_OUTPUT_ENERGY_QUANTITY.invoker().modifyOutputEnergyQuantity(entity, energyStorage, ENERGY_PER_TICK);
    }

    /**
     * A valid runner is a passive or neutral mob (anything that isn't a monster, plus monsters that are
     * only hostile when provoked, such as endermen). Spiders don't count.
     */
    private static boolean isValidRunnerType(Entity entity) {
        if (!(entity instanceof Mob) || entity instanceof Spider) {
            return false;
        }
        return !(entity instanceof Enemy) || entity instanceof NeutralMob;
    }

    private static boolean isValidEntity(Entity entity) {
        if (entity == null || !entity.isAlive() || !isValidRunnerType(entity)) {
            return false;
        }

        return (
            !entity.isSpectator() && !entity.isShiftKeyDown() && !entity.isSwimming() &&
            (!(entity instanceof LivingEntity livingEntity) || livingEntity.hurtTime <= 0 && !livingEntity.isBaby()) &&
            (!(entity instanceof Mob mobEntity) || !mobEntity.isLeashed()) &&
            (!(entity instanceof TamableAnimal tameableEntity) || !tameableEntity.isOrderedToSit())
        );
    }

    private static boolean isEntityNear(Entity entity, Vec3 pos) {
        return entity.distanceToSqr(pos) < MAX_SQUARED_DISTANCE;
    }

    /**
     * The point where a runner stands: the seam between the back part (at {@code pos}) and the front part.
     */
    public static Vec3 computeTreadmillPivot(BlockPos pos, Direction face) {
        double x = switch (face) {
            case WEST -> pos.getX();
            case EAST -> pos.getX() + 1;
            default -> pos.getX() + 0.5D;
        };
        double y = pos.getY() + 0.175;
        double z = switch (face) {
            case SOUTH -> pos.getZ() + 1;
            case NORTH -> pos.getZ();
            default -> pos.getZ() + 0.5D;
        };
        return new Vec3(x, y, z);
    }

    static {
        EnergyStorage.SIDED.registerForBlockEntities((x, __) -> (EnergyStorage)x, SyncBlockEntities.TREADMILL);
    }
}
