package net.stacking.sync_mod.block;

import net.minecraft.util.StringIdentifiable;

public enum ComparatorOutputType implements StringIdentifiable {

    PROGRESS("progress"),
    INVENTORY("inventory");

    private final String name;

    ComparatorOutputType(String name) {
        this.name = name;
    }

    /** Used by Minecraft's EnumProperty serialization (state NBT, commands, etc.) */
    @Override
    public String asString() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}