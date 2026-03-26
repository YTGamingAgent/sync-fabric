package net.stacking.sync_mod.block.entity;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.event.EntityFitnessEvents;
import net.stacking.sync_mod.block.TreadmillBlock;
import net.stacking.sync_mod.config.SyncConfig;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class TreadmillBlockEntity extends BlockEntity
        implements DoubleBlockEntity, TickableBlockEntity, EnergyStorage, GeoBlockEntity {

    public static final long CAPACITY = 64_000L;

    private static final long   PASSIVE_ANIMAL_ENERGY_PER_TICK = 200L;

    // Within 1 block squared — loose enough to absorb minor physics jitter
    // while still releasing the runner if it genuinely wanders off.
    private static final double MAX_SQUARED_DISTANCE = 1.0;

    // ── Animation
    private static final float TREADMILL_LIMB_SPEED = 0.6f;

    private static final Map<net.minecraft.entity.EntityType<?>, Long> ENERGY_MAP;

    // Server-side only. The actual entity reference is resolved from UUID each tick.
    private Entity runner;

    // Synced to the client via toUpdatePacket / toInitialChunkDataNbt so that
    // onClientTick can look up the entity and drive its limb animation.
    private UUID runnerUuid;

    private int  runningTime;
    private long energy;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TreadmillBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.TREADMILL, pos, state);
    }

    // ── GeoBlockEntity

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ── Client sync
    // Without these two overrides the client-side block entity instance never
    // receives the runner UUID, so onClientTick can never drive the animation.

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    /**
     * Marks dirty for saving AND fires an incremental update packet to every
     * client watching this chunk. Call whenever runnerUuid changes so the
     * client animates immediately rather than waiting for the next chunk reload.
     */
    private void sync() {
        markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(
                    this.pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    // ── TickableBlockEntity
    @Override
    public void onClientTick(net.minecraft.world.World world, BlockPos pos, BlockState state) {
        // this.runner is never set on the client — only the synced UUID is available.
        // Search near the treadmill for the specific entity and animate it.
        if (this.runnerUuid == null) return;

        Direction facing  = state.get(TreadmillBlock.FACING);
        Vec3d     pivot   = computeTreadmillPivot(pos, facing);

        // Search a box that comfortably covers the full 2-block treadmill footprint.
        Box searchBox = new Box(
                pivot.x - 2, pos.getY() - 0.5, pivot.z - 2,
                pivot.x + 2, pos.getY() + 2.5,  pivot.z + 2);

        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class, searchBox,
                e -> !e.isPlayer() && this.runnerUuid.equals(e.getUuid()));

        if (candidates.isEmpty()) return;

        LivingEntity living = candidates.get(0);

        // Drive the walking animation at a calm treadmill pace.
        // LimbAnimator manages the internal swing counter; we just set the speed.
        living.limbAnimator.updateLimbs(TREADMILL_LIMB_SPEED, 1.0f);

        // Lock the pig's facing toward the console on the client too,
        // so the head never snaps or drifts between server corrections.
        float yaw = facing.getOpposite().asRotation();
        living.setYaw(yaw);
        living.setHeadYaw(yaw);
        living.setBodyYaw(yaw);
    }

    @Override
    public void onServerTick(net.minecraft.world.World world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!TreadmillBlock.isBack(state)) return;

        // Resolve runner entity from UUID after chunk reload.
        if (this.runner == null && this.runnerUuid != null) {
            this.runner = serverWorld.getEntity(this.runnerUuid);
        }

        if (this.runner != null) {
            Direction facing = state.get(TreadmillBlock.FACING);
            Vec3d     pivot  = computeTreadmillPivot(pos, facing);

            if (!isValidRunner(this.runner) || !isEntityNear(this.runner, pivot)) {
                releaseRunner();
                return;
            }

            if (!this.runner.isPlayer()) {
                // Face the pig toward the console (opposite of belt run direction).
                float yaw = facing.getOpposite().asRotation();
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
                    if (this.runningTime % 100 == 0) markDirty();
                }
            }
        }

        // Push stored energy to adjacent machines.
        if (this.energy > 0) transferEnergy(world, pos, state);
    }

    private void releaseRunner() {
        if (this.runner != null) {
            EntityFitnessEvents.STOP_RUNNING.invoker().onStopRunning(this.runner, this);
        }
        this.runner      = null;
        this.runnerUuid  = null;
        this.runningTime = 0;
        sync(); // notify clients the runner is gone
    }

    private void transferEnergy(net.minecraft.world.World world,
                                BlockPos backPos, BlockState state) {
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

    /** Called by TreadmillBlock when an entity steps on or is placed on either block. */
    public void onSteppedOn(BlockPos pos, BlockState state, Entity entity) {
        if (this.world == null || this.world.isClient) return;
        if (!TreadmillBlock.isBack(state)) return;
        if (this.runner != null) return;

        Vec3d pivot = computeTreadmillPivot(pos, state.get(TreadmillBlock.FACING));
        if (!isEntityNear(entity, pivot)) return;
        if (!isValidRunner(entity)) return;

        this.runner      = entity;
        this.runnerUuid  = entity.getUuid();
        this.runningTime = 0;
        EntityFitnessEvents.START_RUNNING.invoker().onStartRunning(entity, this);
        sync(); // notify clients a runner has arrived
    }

    public boolean isOverheated() { return false; }

    // ── EnergyStorage ─────────────────────────────────────────────────────────

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) return 0;
        long accepted = Math.min(CAPACITY - this.energy, maxAmount);
        if (accepted > 0) { this.energy += accepted; markDirty(); }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) return 0;
        long extracted = Math.min(this.energy, maxAmount);
        if (extracted > 0) { this.energy -= extracted; markDirty(); }
        return extracted;
    }

    @Override public long getAmount()             { return this.energy; }
    @Override public long getCapacity()            { return CAPACITY;    }
    @Override public boolean supportsInsertion()  { return false;       }
    @Override public boolean supportsExtraction() { return true;        }

    // ── DoubleBlockEntity ──────────────────────────────────────────────────────

    @Override
    public DoubleBlockProperties.Type getBlockType(BlockState state) {
        return state.get(TreadmillBlock.PART) == TreadmillBlock.Part.BACK
                ? DoubleBlockProperties.Type.FIRST
                : DoubleBlockProperties.Type.SECOND;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.runnerUuid  = nbt.containsUuid("Runner") ? nbt.getUuid("Runner") : null;
        this.energy      = nbt.getLong("Energy");
        this.runningTime = nbt.getInt("RunningTime");
        this.runner      = null; // always re-resolve from UUID on server
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        UUID uuid = this.runnerUuid != null ? this.runnerUuid
                : this.runner     != null ? this.runner.getUuid()
                : null;
        if (uuid != null) nbt.putUuid("Runner", uuid);
        nbt.putLong("Energy",      this.energy);
        nbt.putInt ("RunningTime", this.runningTime);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static Long getOutputEnergyQuantityForEntity(Entity entity, EnergyStorage storage) {
        if (entity instanceof PassiveEntity) return PASSIVE_ANIMAL_ENERGY_PER_TICK;
        return EntityFitnessEvents.MODIFY_OUTPUT_ENERGY_QUANTITY.invoker()
                .modifyOutputEnergyQuantity(entity, storage, ENERGY_MAP.get(entity.getType()));
    }

    private static boolean isValidRunner(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;
        return !entity.isSpectator()
                && !entity.isSneaking()
                && !entity.isSwimming()
                && (!(entity instanceof LivingEntity l) || (l.hurtTime <= 0 && !l.isBaby()))
                && (!(entity instanceof TameableEntity t) || !t.isSitting());
    }

    private static boolean isEntityNear(Entity entity, Vec3d pos) {
        return entity.squaredDistanceTo(pos) < MAX_SQUARED_DISTANCE;
    }

    /**
     * The pivot is the BOUNDARY between the BACK and FRONT blocks — the pig's
     * entity-centre sits right at this seam so its body straddles both blocks,
     * with its snout reaching into the BACK block touching the console panel.
     *
     * For a SOUTH-facing treadmill (BACK=placed block, FRONT=one block south):
     *   Boundary Z = backPos.getZ() + 1.0  (south face of BACK = north face of FRONT)
     *   That places pig centre at Z+1, snout reaching to Z+0.5 (mid-console) ✓
     */
    public static Vec3d computeTreadmillPivot(BlockPos pos, Direction facing) {
        double x = switch (facing) {
            case WEST -> pos.getX();          // west face of BACK = east face of FRONT
            case EAST -> pos.getX() + 1.0;    // east face of BACK = west face of FRONT
            default   -> pos.getX() + 0.5;    // centred on X for N/S facings
        };
        double z = switch (facing) {
            case SOUTH -> pos.getZ() + 1.0;   // south face of BACK = north face of FRONT
            case NORTH -> pos.getZ();          // north face of BACK = south face of FRONT
            default    -> pos.getZ() + 0.5;   // centred on Z for E/W facings
        };
        return new Vec3d(x, pos.getY() + 0.175, z);
    }

    static {
        ENERGY_MAP = Sync.getConfig().energyMap().stream()
                .collect(Collectors.toUnmodifiableMap(
                        SyncConfig.EnergyMapEntry::getEntityType,
                        SyncConfig.EnergyMapEntry::outputEnergyQuantity,
                        (a, b) -> a));
        EnergyStorage.SIDED.registerForBlockEntities(
                (be, dir) -> (EnergyStorage) be,
                SyncBlockEntities.TREADMILL);
    }
}