package net.stacking.sync_mod.util.nbt;

import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.NbtCompound;

import java.util.function.BiConsumer;

public class NbtSerializerFactory<T> {
    private final Iterable<NbtReader<T>> readers;
    private final Iterable<NbtWriter<T>> writers;

    public NbtSerializerFactory(Iterable<NbtReader<T>> readers, Iterable<NbtWriter<T>> writers) {
        this.readers = ImmutableList.copyOf(readers);
        this.writers = ImmutableList.copyOf(writers);
    }

    public <T> NbtSerializer<T> build(T target) {
        return new NbtSerializer<>(target,
                (Iterable)this.readers,  // Cast to Iterable without type parameter
                (Iterable)this.writers);
    }

    @FunctionalInterface
    public interface NbtWriter<T> extends BiConsumer<T, NbtCompound> {
    }

    @FunctionalInterface
    public interface NbtReader<T> extends BiConsumer<T, NbtCompound> {
    }
}