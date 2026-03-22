package net.stacking.sync_mod.util.reflect;

import net.stacking.sync_mod.util.function.FunctionUtil;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ClassUtil {
    public static Optional<Method> getMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        return FunctionUtil.tryInvoke(() -> type.getMethod(name, parameterTypes));
    }
}