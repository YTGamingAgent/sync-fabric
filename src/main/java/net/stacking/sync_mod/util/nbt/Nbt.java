package net.stacking.sync_mod.util.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.5
/*import net.minecraft.core.UUIDUtil;*/

import java.util.UUID;

/**
 * Reads NBT the same way on every Minecraft version. Since 1.21.5 the vanilla getters return
 * Optionals; these helpers return the value or a default (0, "", empty tag/list, null UUID),
 * just like the older getters did.
 */
public final class Nbt {
    public static boolean contains(CompoundTag nbt, String key, int type) {
        //? if >=1.21.5 {
        /*Tag tag = nbt.get(key);
        return tag != null && tag.getId() == type;
        *///?} else
        return nbt.contains(key, type);
    }

    public static boolean getBoolean(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getBooleanOr(key, false);
        *///?} else
        return nbt.getBoolean(key);
    }

    public static byte getByte(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getByteOr(key, (byte)0);
        *///?} else
        return nbt.getByte(key);
    }

    public static short getShort(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getShortOr(key, (short)0);
        *///?} else
        return nbt.getShort(key);
    }

    public static int getInt(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getIntOr(key, 0);
        *///?} else
        return nbt.getInt(key);
    }

    public static long getLong(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getLongOr(key, 0L);
        *///?} else
        return nbt.getLong(key);
    }

    public static float getFloat(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getFloatOr(key, 0F);
        *///?} else
        return nbt.getFloat(key);
    }

    public static double getDouble(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getDoubleOr(key, 0D);
        *///?} else
        return nbt.getDouble(key);
    }

    public static String getString(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getStringOr(key, "");
        *///?} else
        return nbt.getString(key);
    }

    public static long[] getLongArray(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getLongArray(key).orElse(new long[0]);
        *///?} else
        return nbt.getLongArray(key);
    }

    public static CompoundTag getCompound(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.getCompoundOrEmpty(key);
        *///?} else
        return nbt.getCompound(key);
    }

    public static CompoundTag getCompound(ListTag list, int index) {
        //? if >=1.21.5 {
        /*return list.getCompoundOrEmpty(index);
        *///?} else
        return list.getCompound(index);
    }

    /**
     * @param type Element type (only used before 1.21.5, when lists were typed on read).
     */
    public static ListTag getList(CompoundTag nbt, String key, int type) {
        //? if >=1.21.5 {
        /*return nbt.getListOrEmpty(key);
        *///?} else
        return nbt.getList(key, type);
    }

    public static boolean hasUuid(CompoundTag nbt, String key) {
        return getUuid(nbt, key) != null;
    }

    @Nullable
    public static UUID getUuid(CompoundTag nbt, String key) {
        //? if >=1.21.5 {
        /*return nbt.read(key, UUIDUtil.CODEC).orElse(null);
        *///?} else
        return nbt.hasUUID(key) ? nbt.getUUID(key) : null;
    }

    public static void putUuid(CompoundTag nbt, String key, UUID uuid) {
        //? if >=1.21.5 {
        /*nbt.store(key, UUIDUtil.CODEC, uuid);
        *///?} else
        nbt.putUUID(key, uuid);
    }

    private Nbt() { }
}
