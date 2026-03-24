package net.stacking.sync_mod.block.entity;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.event.EntityFitnessEvents;
import net.stacking.sync_mod.block.TreadmillBlock;
import net.stacking.sync_mod.config.SyncConfig;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class TreadmillBlockEntity extends BlockEntity
        implements DoubleBlockEntity, TickableBlockEntity, EnergyStorage, GeoBlockEntity {

    public static final long CAPACITY = 64_000L;
    // Keep squaredDistance tight — entity must be physically standing on the belt.
    private static final double MAX_SQUARED_DISTANCE = 0.5;
    private static final Map<net.minecraft.entity.EntityType<?>, Long> ENERGY_MAP;

    private UUID runnerUuid;
    private Entity runner;
    private int runningTime;
    private long energy;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TreadmillBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.TREADMILL, pos, state);
    }

    // ---- GeoBlockEntity --------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations for the treadmill currently.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---- TickableBlockEntity ---------------------------------------------------

    @Override
    public void onClientTick(net.minecraft.world.World world, BlockPos pos, BlockState state) {
        // Animate limbs client-side if a runner is known.
        if (this.runner instanceof LivingEntity living) {
            living.limbAnimator.updateLimbs(1.0f, 1.0f);
        }
    }

    @Override
    public void onServerTick(net.minecraft.world.World world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        // Only the BACK block entity manages energy and the runner.
        if (!TreadmillBlock.isBack(state)) return;

        // Resolve runner from UUID after load.
        if (this.runner == null && this.runnerUuid != null) {
            this.runner = serverWorld.getEntity(this.runnerUuid);
        }

        if (this.runner != null) {
            Direction facing = state.get(TreadmillBlock.FACING);
            Vec3d pivot = computeTreadmillPivot(pos, facing);

            if (!isValidRunner(this.runner) || !isEntityNear(this.runner, pivot)) {
                // Runner left or became invalid — reset.
                EntityFitnessEvents.STOP_RUNNING.invoker().onStopRunning(this.runner, this);
                this.runner = null;
                this.runnerUuid = null;
                this.runningTime = 0;
                this.markDirty();
            } else {
                // Keep non-player runners on the belt.
                if (!this.runner.isPlayer()) {
                    float yaw = facing.asRotation();
                    this.runner.updatePositionAndAngles(pivot.x, pivot.y, pivot.z, yaw, 0);
                    this.runner.setHeadYaw(yaw);
                    this.runner.setBodyYaw(yaw);
                }

                if (this.runner instanceof LivingEntity living) {
                    living.setDespawnCounter(0);
                }

                // Generate energy.
                Long perTick = getOutputEnergyQuantityForEntity(this.runner, this);
                if (perTick != null && perTick > 0) {
                    long space = CAPACITY - this.energy;
                    if (space > 0) {
                        this.energy += Math.min(space, perTick);
                        this.runningTime++;
                        if (this.runningTime % 100 == 0) this.markDirty();
                    }
                }
            }
        }

        // Push stored energy to adjacent machines each tick.
        if (this.energy > 0) {
            this.transferEnergy(world, pos, state);
        }
    }

    /**
     * Actively push energy into any adjacent EnergyStorage that accepts insertion.
     * Checks all 6 faces of both the BACK block and the FRONT block so the
     * constructor can be placed on any side of the treadmill.
     */
    private void transferEnergy(net.minecraft.world.World world, BlockPos backPos, BlockState state) {
        BlockPos frontPos = backPos.offset(state.get(TreadmillBlock.FACING));
        for (BlockPos origin : new BlockPos[]{backPos, frontPos}) {
            for (Direction dir : Direction.values()) {
                if (this.energy <= 0) return;
                EnergyStorage target = EnergyStorage.SIDED.find(
                        world, origin.offset(dir), dir.getOpposite());
                if (target != null && target.supportsInsertion()) {
                    EnergyStorageUtil.move(this, target, Long.MAX_VALUE, null);
                }
            }
        }
    }

    /** Called by TreadmillBlock when an entity walks onto either block. */
    public void onSteppedOn(BlockPos pos, BlockState state, Entity entity) {
        if (this.world == null || this.world.isClient) return;
        if (!TreadmillBlock.isBack(state)) return;
        if (this.runner != null) return;   // already occupied

        Vec3d pivot = computeTreadmillPivot(pos, state.get(TreadmillBlock.FACING));
        if (!isEntityNear(entity, pivot)) return;
        if (!isValidRunner(entity)) return;

        this.runner = entity;
        this.runnerUuid = entity.getUuid();
        this.runningTime = 0;
        EntityFitnessEvents.START_RUNNING.invoker().onStartRunning(entity, this);
        this.markDirty();
    }

    public boolean isOverheated() {
        return false;
    }

    // ---- EnergyStorage ---------------------------------------------------------

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) return 0;
        long space = CAPACITY - this.energy;
        long accepted = Math.min(space, maxAmount);
        if (accepted > 0) {
            this.energy += accepted;
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) return 0;
        long extracted = Math.min(this.energy, maxAmount);
        if (extracted > 0) {
            this.energy -= extracted;
        }
        return extracted;
    }

    @Override public long getAmount()             { return this.energy; }
    @Override public long getCapacity()           { return CAPACITY; }
    @Override public boolean supportsInsertion()  { return false; }  // generator, not battery
    @Override public boolean supportsExtraction() { return true; }

    // ---- DoubleBlockEntity -----------------------------------------------------

    @Override
    public DoubleBlockProperties.Type getBlockType(BlockState state) {
        return state.get(TreadmillBlock.PART) == TreadmillBlock.Part.BACK
                ? DoubleBlockProperties.Type.FIRST
                : DoubleBlockProperties.Type.SECOND;
    }

    // ---- NBT -------------------------------------------------------------------

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.runnerUuid = nbt.containsUuid("Runner") ? nbt.getUuid("Runner") : null;
        this.energy     = nbt.getLong("Energy");
        this.runningTime = nbt.getInt("RunningTime");
        this.runner = null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        UUID uuid = this.runnerUuid != null ? this.runnerUuid
                  : this.runner    != null ? this.runner.getUuid()
                  : null;
        if (uuid != null) nbt.putUuid("Runner", uuid);
        nbt.putLong("Energy", this.energy);
        nbt.putInt("RunningTime", this.runningTime);
    }

    // ---- Helpers ---------------------------------------------------------------

    private static Long getOutputEnergyQuantityForEntity(Entity entity, EnergyStorage storage) {
        return EntityFitnessEvents.MODIFY_OUTPUT_ENERGY_QUANTITY.invoker()
                .modifyOutputEnergyQuantity(entity, storage, ENERGY_MAP.get(entity.getType()));
    }

    private static boolean isValidRunner(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;
        return !entity.isSpectator() && !entity.isSneaking() && !entity.isSwimming()
                && (!(entity instanceof LivingEntity l) || (l.hurtTime <= 0 && !l.isBaby()))
                && (!(entity instanceof TameableEntity t) || !t.isSitting());
    }

    private static boolean isEntityNear(Entity entity, Vec3d pos) {
        return entity.squaredDistanceTo(pos) < MAX_SQUARED_DISTANCE;
    }

    private static Vec3d computeTreadmillPivot(BlockPos pos, Direction face) {
        double x = switch (face) {
            case WEST  -> pos.getX();
            case EAST  -> pos.getX() + 1;
            default    -> pos.getX() + 0.5;
        };
        double y = pos.getY() + 0.175;
        double z = switch (face) {
            case SOUTH -> pos.getZ() + 1;
            case NORTH -> pos.getZ();
            default    -> pos.getZ() + 0.5;
        };
        return new Vec3d(x, y, z);
    }

    static {
        ENERGY_MAP = Sync.getConfig().energyMap().stream()
                .collect(Collectors.toUnmodifiableMap(
                        SyncConfig.EnergyMapEntry::getEntityType,
                        SyncConfig.EnergyMapEntry::outputEnergyQuantity,
                        (a, b) -> a));
        EnergyStorage.SIDED.registerForBlockEntities((be, dir) -> (EnergyStorage) be,
                SyncBlockEntities.TREADMILL);
    }
}
