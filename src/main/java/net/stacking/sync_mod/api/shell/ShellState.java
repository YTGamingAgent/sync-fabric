package net.stacking.sync_mod.api.shell;

import net.stacking.sync_mod.util.nbt.Nbt;
import net.stacking.sync_mod.entity.ShellEntity;
import net.stacking.sync_mod.item.SimpleInventory;
import net.stacking.sync_mod.util.WorldUtil;
import net.stacking.sync_mod.util.math.Radians;
import net.stacking.sync_mod.util.nbt.NbtSerializer;
import net.stacking.sync_mod.util.nbt.NbtSerializerFactory;
import net.stacking.sync_mod.util.nbt.NbtSerializerFactoryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class ShellState {
    public static final float PROGRESS_START = 0F;
    public static final float PROGRESS_DONE = 1F;
    public static final float PROGRESS_PRINTING = 0.75F;
    public static final float PROGRESS_PAINTING = PROGRESS_DONE - PROGRESS_PRINTING;

    private static final NbtSerializerFactory<ShellState> NBT_SERIALIZER_FACTORY;

    private UUID uuid;
    private float progress;
    private DyeColor color;
    private boolean isArtificial;

    private UUID ownerUuid;
    private String ownerName;
    private float health;
    private int gameMode;
    private SimpleInventory inventory;
    private ShellStateComponent component;

    private int foodLevel;
    private float saturationLevel;
    private float exhaustion;

    private int experienceLevel;
    private float experienceProgress;
    private int totalExperience;

    private ResourceLocation world;
    private BlockPos pos;

    private final NbtSerializer<ShellState> serializer;

    public ShellState() {
        this.uuid = UUID.randomUUID();
        this.progress = PROGRESS_START;
        this.inventory = new SimpleInventory();
        this.component = ShellStateComponent.empty();
        this.serializer = NBT_SERIALIZER_FACTORY.build(this);
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public float getProgress() {
        return this.progress;
    }

    public void setProgress(float progress) {
        this.progress = Mth.clamp(progress, PROGRESS_START, PROGRESS_DONE);
    }

    public DyeColor getColor() {
        return this.color;
    }

    public void setColor(DyeColor color) {
        this.color = color;
    }

    public boolean isArtificial() {
        return this.isArtificial;
    }

    public void setArtificial(boolean artificial) {
        isArtificial = artificial;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public float getHealth() {
        return this.health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public GameType getGameMode() {
        return GameType.byId(this.gameMode);
    }

    public void setGameMode(GameType gameMode) {
        this.gameMode = gameMode.getId();
    }

    public SimpleInventory getInventory() {
        return this.inventory;
    }

    public ShellStateComponent getComponent() {
        return this.component;
    }

    public void setComponent(ShellStateComponent component) {
        this.component = component;
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public float getSaturationLevel() {
        return this.saturationLevel;
    }

    public void setSaturationLevel(float saturationLevel) {
        this.saturationLevel = saturationLevel;
    }

    public float getExhaustion() {
        return this.exhaustion;
    }

    public void setExhaustion(float exhaustion) {
        this.exhaustion = exhaustion;
    }

    public int getExperienceLevel() {
        return this.experienceLevel;
    }

    public void setExperienceLevel(int experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public float getExperienceProgress() {
        return this.experienceProgress;
    }

    public void setExperienceProgress(float experienceProgress) {
        this.experienceProgress = experienceProgress;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public void setTotalExperience(int totalExperience) {
        this.totalExperience = totalExperience;
    }

    public ResourceLocation getWorld() {
        return this.world;
    }

    public void setWorld(ResourceLocation world) {
        this.world = world;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    /**
     * Creates empty shell of the specified player.
     *
     * @param player The player.
     * @param pos Position of the shell.
     * @return Empty shell of the specified player.
     */
    public static ShellState empty(ServerPlayer player, BlockPos pos) {
        return empty(player, pos, null);
    }

    /**
     * Creates empty shell of the specified player.
     *
     * @param player The player.
     * @param pos Position of the shell.
     * @param color Color of the shell.
     * @return Empty shell of the specified player.
     */
    public static ShellState empty(ServerPlayer player, BlockPos pos, DyeColor color) {
        return create(player, pos, color, PROGRESS_START, true, false);
    }

    /**
     * Creates shell that is a full copy of the specified player.
     *
     * @param player The player.
     * @param pos Position of the shell.
     * @return Shell that is a full copy of the specified player.
     */
    public static ShellState of(ServerPlayer player, BlockPos pos) {
        return of(player, pos, null);
    }

    /**
     * Creates shell that is a full copy of the specified player.
     *
     * @param player The player.
     * @param pos Position of the shell.
     * @param color Color of the shell.
     * @return Shell that is a full copy of the specified player.
     */
    public static ShellState of(ServerPlayer player, BlockPos pos, DyeColor color) {
        return create(player, pos, color, PROGRESS_DONE, ((Shell)player).isArtificial(), true);
    }

    private static ShellState create(ServerPlayer player, BlockPos pos, DyeColor color, float progress, boolean isArtificial, boolean copyPlayerState) {
        ShellState shell = new ShellState();

        shell.uuid = UUID.randomUUID();
        shell.progress = progress;
        shell.color = color;
        shell.isArtificial = isArtificial;

        shell.ownerUuid = player.getUUID();
        shell.ownerName = player.getName().getString();
        shell.gameMode = player.gameMode.getGameModeForPlayer().getId();
        shell.inventory = new SimpleInventory();

        if (copyPlayerState) {
            shell.health = player.getHealth();
            shell.inventory.clone(player.getInventory());
            shell.component = ShellStateComponent.of(player);

            shell.foodLevel = player.getFoodData().getFoodLevel();
            shell.saturationLevel = player.getFoodData().getSaturationLevel();
            //? if >=1.21.2 {
            /*shell.exhaustion = player.getFoodData().exhaustionLevel;
            *///?} else
            shell.exhaustion = player.getFoodData().getExhaustionLevel();

            shell.experienceLevel = player.experienceLevel;
            shell.experienceProgress = player.experienceProgress;
            shell.totalExperience = player.totalExperience;
        } else {
            shell.health = player.getMaxHealth();
            shell.component = ShellStateComponent.empty();
            shell.foodLevel = 20;
            shell.saturationLevel = 5;
        }

        shell.world = WorldUtil.getId(WorldUtil.level(player));
        shell.pos = pos;

        return shell;
    }

    public void dropInventory(ServerLevel world) {
        this.dropInventory(world, this.pos);
    }

    public void dropInventory(ServerLevel world, BlockPos pos) {
        Stream
                .of(this.inventory.main, this.inventory.armor, this.inventory.offHand, this.component.getItems())
                .flatMap(Collection::stream)
                .forEach(x -> this.dropItemStack(world, pos, x));
    }

    public void dropXp(ServerLevel world) {
        this.dropXp(world, this.pos);
    }

    public void dropXp(ServerLevel world, BlockPos pos) {
        int xp = Math.min(this.experienceLevel * 7, 100) + this.component.getXp();
        Vec3 vecPos = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ExperienceOrb.award(world, vecPos, xp);
    }

    public void drop(ServerLevel world) {
        this.drop(world, this.pos);
    }

    public void drop(ServerLevel world, BlockPos pos) {
        this.dropInventory(world, pos);
        this.dropXp(world, pos);
    }

    private void dropItemStack(Level world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEntity item = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        item.setPickUpDelay(40);

        float h = world.getRandom().nextFloat() * 0.5F;
        float v = world.getRandom().nextFloat() * 2 * Radians.R_PI;
        item.setDeltaMovement(-Mth.sin(v) * h, 0.2, Mth.cos(v) * h);
        world.addFreshEntity(item);
    }

    // Registry-aware NBT methods for 1.21
    public CompoundTag writeNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        this.serializer.writeNbt(nbt);

        if (this.inventory != null) {
            ListTag inventoryList = new ListTag();
            this.inventory.writeNbt(inventoryList, registryLookup);
            nbt.put("inventory", inventoryList);
        }

        if (this.component != null) {
            nbt.put("components", this.component.writeNbt(new CompoundTag(), registryLookup));
        }

        return nbt;
    }

    public void readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        this.serializer.readNbt(nbt);

        if (Nbt.contains(nbt, "inventory", Tag.TAG_LIST)) {
            this.inventory = new SimpleInventory();
            this.inventory.readNbt(Nbt.getList(nbt, "inventory", Tag.TAG_COMPOUND), registryLookup);
        } else if (this.inventory == null) {
            this.inventory = new SimpleInventory();
        }

        if (Nbt.contains(nbt, "components", Tag.TAG_COMPOUND)) {
            this.component = ShellStateComponent.empty();
            this.component.readNbt(Nbt.getCompound(nbt, "components"), registryLookup);
        } else if (this.component == null) {
            this.component = ShellStateComponent.empty();
        }
    }

    public static ShellState fromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        ShellState state = new ShellState();
        state.readNbt(nbt, registryLookup);
        return state;
    }

    // Backwards-compatible non-registry methods
    public CompoundTag writeNbt(CompoundTag nbt) {
        this.serializer.writeNbt(nbt);
        return nbt;
    }

    public void readNbt(CompoundTag nbt) {
        this.serializer.readNbt(nbt);
    }

    public static ShellState fromNbt(CompoundTag nbt) {
        ShellState state = new ShellState();
        state.readNbt(nbt);
        return state;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ShellState state && Objects.equals(this.uuid, state.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uuid);
    }

    @Environment(EnvType.CLIENT)
    private ShellEntity entityInstance;

    @Environment(EnvType.CLIENT)
    public ShellEntity asEntity() {
        if (this.entityInstance == null) {
            this.entityInstance = new ShellEntity(this);
        }
        return this.entityInstance;
    }

    static {
        NBT_SERIALIZER_FACTORY = NbtSerializerFactoryBuilder.<ShellState>create()
                .add(UUID.class, "uuid", s -> s.uuid, (s, v) -> s.uuid = v)
                .add(float.class, "progress", s -> s.progress, (s, v) -> s.progress = v)
                .add(DyeColor.class, "color", s -> s.color, (s, v) -> s.color = v)
                .add(boolean.class, "isArtificial", s -> s.isArtificial, (s, v) -> s.isArtificial = v)
                .add(UUID.class, "ownerUuid", s -> s.ownerUuid, (s, v) -> s.ownerUuid = v)
                .add(String.class, "ownerName", s -> s.ownerName, (s, v) -> s.ownerName = v)
                .add(float.class, "health", s -> s.health, (s, v) -> s.health = v)
                .add(int.class, "gameMode", s -> s.gameMode, (s, v) -> s.gameMode = v)
                .add(int.class, "foodLevel", s -> s.foodLevel, (s, v) -> s.foodLevel = v)
                .add(float.class, "saturationLevel", s -> s.saturationLevel, (s, v) -> s.saturationLevel = v)
                .add(float.class, "exhaustion", s -> s.exhaustion, (s, v) -> s.exhaustion = v)
                .add(int.class, "experienceLevel", s -> s.experienceLevel, (s, v) -> s.experienceLevel = v)
                .add(float.class, "experienceProgress", s -> s.experienceProgress, (s, v) -> s.experienceProgress = v)
                .add(int.class, "totalExperience", s -> s.totalExperience, (s, v) -> s.totalExperience = v)
                .add(ResourceLocation.class, "world", s -> s.world, (s, v) -> s.world = v)
                .add(BlockPos.class, "pos", s -> s.pos, (s, v) -> s.pos = v)
                .build();
    }
}