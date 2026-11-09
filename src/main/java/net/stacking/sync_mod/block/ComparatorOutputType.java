package net.stacking.sync_mod.block;

import net.minecraft.util.StringRepresentable;

public enum ComparatorOutputType implements StringRepresentable {

    PROGRESS("progress"),
    INVENTORY("inventory");

    private final String name;

    ComparatorOutputType(String name) {
        this.name = name;
    }

    /** Used by Minecraft's EnumProperty serialization (state NBT, commands, etc.) */
    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}