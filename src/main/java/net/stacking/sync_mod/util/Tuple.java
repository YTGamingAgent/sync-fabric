package net.stacking.sync_mod.util;

/**
 * A simple pair of values (Minecraft's own Tuple class was removed in 26.2).
 */
public final class Tuple<A, B> {
    private final A a;
    private final B b;

    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return this.a;
    }

    public B getB() {
        return this.b;
    }
}
