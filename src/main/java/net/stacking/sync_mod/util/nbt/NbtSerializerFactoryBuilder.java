package net.stacking.sync_mod.util.nbt;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Small builder for {@link NbtSerializerFactory}.
 *
 * <p>This is intentionally explicit (getter + setter) rather than reflective. That keeps it
 * stable across Yarn / Mojang mapping changes.</p>
 */
public final class NbtSerializerFactoryBuilder<T> {
    private static final Map<Class<?>, BiFunction<NbtCompound, String, ?>> NBT_GETTERS = new HashMap<>();

    static {
        BiFunction<BiFunction<NbtCompound, String, ?>, BiFunction<NbtCompound, String, ?>, BiFunction<NbtCompound, String, ?>> getOrDefault =
                (getter, defaultValue) -> (nbt, key) -> nbt.contains(key) ? getter.apply(nbt, key) : defaultValue.apply(nbt, key);

        NBT_GETTERS.put(boolean.class, getOrDefault.apply(NbtCompound::getBoolean, (x, key) -> false));
        NBT_GETTERS.put(Boolean.class, getOrDefault.apply(NbtCompound::getBoolean, (x, key) -> false));
        NBT_GETTERS.put(byte.class, getOrDefault.apply(NbtCompound::getByte, (x, key) -> (byte) 0));
        NBT_GETTERS.put(Byte.class, getOrDefault.apply(NbtCompound::getByte, (x, key) -> (byte) 0));
        NBT_GETTERS.put(short.class, getOrDefault.apply(NbtCompound::getShort, (x, key) -> (short) 0));
        NBT_GETTERS.put(Short.class, getOrDefault.apply(NbtCompound::getShort, (x, key) -> (short) 0));
        NBT_GETTERS.put(int.class, getOrDefault.apply(NbtCompound::getInt, (x, key) -> 0));
        NBT_GETTERS.put(Integer.class, getOrDefault.apply(NbtCompound::getInt, (x, key) -> 0));
        NBT_GETTERS.put(long.class, getOrDefault.apply(NbtCompound::getLong, (x, key) -> 0L));
        NBT_GETTERS.put(Long.class, getOrDefault.apply(NbtCompound::getLong, (x, key) -> 0L));
        NBT_GETTERS.put(float.class, getOrDefault.apply(NbtCompound::getFloat, (x, key) -> 0F));
        NBT_GETTERS.put(Float.class, getOrDefault.apply(NbtCompound::getFloat, (x, key) -> 0F));
        NBT_GETTERS.put(double.class, getOrDefault.apply(NbtCompound::getDouble, (x, key) -> 0D));
        NBT_GETTERS.put(Double.class, getOrDefault.apply(NbtCompound::getDouble, (x, key) -> 0D));
        NBT_GETTERS.put(String.class, getOrDefault.apply(NbtCompound::getString, (x, key) -> ""));
        NBT_GETTERS.put(UUID.class, getOrDefault.apply(NbtCompound::getUuid, (x, key) -> new UUID(0, 0)));
        NBT_GETTERS.put(Identifier.class, getOrDefault.apply((x, key) -> Identifier.tryParse(x.getString(key)), (x, key) -> Identifier.of("missingno")));
        NBT_GETTERS.put(DyeColor.class, getOrDefault.apply((nbt, key) -> DyeColor.byId(nbt.getInt(key)), (nbt, key) -> null));
        NBT_GETTERS.put(BlockPos.class, getOrDefault.apply((nbt, key) -> {
            if (nbt.contains(key)) {
                long[] pos = nbt.getLongArray(key);
                if (pos.length == 3) {
                    return new BlockPos((int) pos[0], (int) pos[1], (int) pos[2]);
                }
            }
            return BlockPos.ORIGIN;
        }, (x, key) -> BlockPos.ORIGIN));
    }

    private final List<FieldSerializer<T>> serializers = new ArrayList<>();

    private NbtSerializerFactoryBuilder() {
    }

    public static <T> NbtSerializerFactoryBuilder<T> create() {
        return new NbtSerializerFactoryBuilder<>();
    }

    public <V> NbtSerializerFactoryBuilder<T> add(Class<V> type, String key, Function<T, V> getter, BiConsumer<T, V> setter) {
        BiFunction<NbtCompound, String, ?> nbtGetter = NBT_GETTERS.get(type);
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
    private static <V> void putToNbt(NbtCompound nbt, String key, V value, Class<V> type) {
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
            nbt.putUuid(key, (UUID) value);
        } else if (type == Identifier.class) {
            nbt.putString(key, value.toString());
        } else if (type == DyeColor.class) {
            nbt.putInt(key, ((DyeColor) value).getId());
        } else if (type == BlockPos.class) {
            BlockPos pos = (BlockPos) value;
            nbt.putLongArray(key, new long[]{pos.getX(), pos.getY(), pos.getZ()});
        } else if (value instanceof NbtCompound compound) {
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