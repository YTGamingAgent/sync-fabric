package net.stacking.sync_mod.util.nbt;

import net.stacking.sync_mod.util.IdUtil;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * Small builder for {@link NbtSerializerFactory}.
 *
 * <p>This is intentionally explicit (getter + setter) rather than reflective. That keeps it
 * stable across Yarn / Mojang mapping changes.</p>
 */
public final class NbtSerializerFactoryBuilder<T> {
    private static final Map<Class<?>, BiFunction<CompoundTag, String, ?>> NBT_GETTERS = new HashMap<>();

    static {
        BiFunction<BiFunction<CompoundTag, String, ?>, BiFunction<CompoundTag, String, ?>, BiFunction<CompoundTag, String, ?>> getOrDefault =
                (getter, defaultValue) -> (nbt, key) -> nbt.contains(key) ? getter.apply(nbt, key) : defaultValue.apply(nbt, key);

        NBT_GETTERS.put(boolean.class, getOrDefault.apply(Nbt::getBoolean, (x, key) -> false));
        NBT_GETTERS.put(Boolean.class, getOrDefault.apply(Nbt::getBoolean, (x, key) -> false));
        NBT_GETTERS.put(byte.class, getOrDefault.apply(Nbt::getByte, (x, key) -> (byte) 0));
        NBT_GETTERS.put(Byte.class, getOrDefault.apply(Nbt::getByte, (x, key) -> (byte) 0));
        NBT_GETTERS.put(short.class, getOrDefault.apply(Nbt::getShort, (x, key) -> (short) 0));
        NBT_GETTERS.put(Short.class, getOrDefault.apply(Nbt::getShort, (x, key) -> (short) 0));
        NBT_GETTERS.put(int.class, getOrDefault.apply(Nbt::getInt, (x, key) -> 0));
        NBT_GETTERS.put(Integer.class, getOrDefault.apply(Nbt::getInt, (x, key) -> 0));
        NBT_GETTERS.put(long.class, getOrDefault.apply(Nbt::getLong, (x, key) -> 0L));
        NBT_GETTERS.put(Long.class, getOrDefault.apply(Nbt::getLong, (x, key) -> 0L));
        NBT_GETTERS.put(float.class, getOrDefault.apply(Nbt::getFloat, (x, key) -> 0F));
        NBT_GETTERS.put(Float.class, getOrDefault.apply(Nbt::getFloat, (x, key) -> 0F));
        NBT_GETTERS.put(double.class, getOrDefault.apply(Nbt::getDouble, (x, key) -> 0D));
        NBT_GETTERS.put(Double.class, getOrDefault.apply(Nbt::getDouble, (x, key) -> 0D));
        NBT_GETTERS.put(String.class, getOrDefault.apply(Nbt::getString, (x, key) -> ""));
        NBT_GETTERS.put(UUID.class, getOrDefault.apply(Nbt::getUuid, (x, key) -> new UUID(0, 0)));
        NBT_GETTERS.put(ResourceLocation.class, getOrDefault.apply((x, key) -> ResourceLocation.tryParse(Nbt.getString(x, key)), (x, key) -> IdUtil.vanilla("missingno")));
        NBT_GETTERS.put(DyeColor.class, getOrDefault.apply((nbt, key) -> DyeColor.byId(Nbt.getInt(nbt, key)), (nbt, key) -> null));
        NBT_GETTERS.put(BlockPos.class, getOrDefault.apply((nbt, key) -> {
            if (nbt.contains(key)) {
                long[] pos = Nbt.getLongArray(nbt, key);
                if (pos.length == 3) {
                    return new BlockPos((int) pos[0], (int) pos[1], (int) pos[2]);
                }
            }
            return BlockPos.ZERO;
        }, (x, key) -> BlockPos.ZERO));
    }

    private final List<FieldSerializer<T>> serializers = new ArrayList<>();

    private NbtSerializerFactoryBuilder() {
    }

    public static <T> NbtSerializerFactoryBuilder<T> create() {
        return new NbtSerializerFactoryBuilder<>();
    }

    public <V> NbtSerializerFactoryBuilder<T> add(Class<V> type, String key, Function<T, V> getter, BiConsumer<T, V> setter) {
        BiFunction<CompoundTag, String, ?> nbtGetter = NBT_GETTERS.get(type);
        if (nbtGetter == null) {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }

        this.serializers.add(new FieldSerializer<>(
                key,
                (target, nbt) -> {
                    V value = getter.apply(target);
                    if (value != null) {
                        putToNbt(nbt, key, value, type);
                    }
                },
                (target, nbt) -> {
                    if (nbt.contains(key)) {
                        @SuppressWarnings("unchecked")
                        V value = (V) nbtGetter.apply(nbt, key);
                        setter.accept(target, value);
                    }
                }
        ));

        return this;
    }

    public NbtSerializerFactory<T> build() {
        List<NbtSerializerFactory.NbtReader<T>> readers = new ArrayList<>(this.serializers.size());
        List<NbtSerializerFactory.NbtWriter<T>> writers = new ArrayList<>(this.serializers.size());

        for (FieldSerializer<T> serializer : this.serializers) {
            writers.add(serializer.writer);
            readers.add(serializer.reader);
        }

        return new NbtSerializerFactory<>(readers, writers);
    }

    @SuppressWarnings("unchecked")
    private static <V> void putToNbt(CompoundTag nbt, String key, V value, Class<V> type) {
        if (type == boolean.class || type == Boolean.class) {
            nbt.putBoolean(key, (Boolean) value);
        } else if (type == byte.class || type == Byte.class) {
            nbt.putByte(key, (Byte) value);
        } else if (type == short.class || type == Short.class) {
            nbt.putShort(key, (Short) value);
        } else if (type == int.class || type == Integer.class) {
            nbt.putInt(key, (Integer) value);
        } else if (type == long.class || type == Long.class) {
            nbt.putLong(key, (Long) value);
        } else if (type == float.class || type == Float.class) {
            nbt.putFloat(key, (Float) value);
        } else if (type == double.class || type == Double.class) {
            nbt.putDouble(key, (Double) value);
        } else if (type == String.class) {
            nbt.putString(key, (String) value);
        } else if (type == UUID.class) {
            Nbt.putUuid(nbt, key, (UUID) value);
        } else if (type == ResourceLocation.class) {
            nbt.putString(key, value.toString());
        } else if (type == DyeColor.class) {
            nbt.putInt(key, ((DyeColor) value).getId());
        } else if (type == BlockPos.class) {
            BlockPos pos = (BlockPos) value;
            nbt.putLongArray(key, new long[]{pos.getX(), pos.getY(), pos.getZ()});
        } else if (value instanceof CompoundTag compound) {
            nbt.put(key, compound);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }

    private record FieldSerializer<T>(
            String key,
            NbtSerializerFactory.NbtWriter<T> writer,
            NbtSerializerFactory.NbtReader<T> reader
    ) {
    }
}