package net.stacking.sync_mod.util.function;

@FunctionalInterface
public interface ThrowableSupplier<T> {
    T get() throws Throwable;
}
