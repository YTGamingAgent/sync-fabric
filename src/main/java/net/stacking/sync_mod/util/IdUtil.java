package net.stacking.sync_mod.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class IdUtil {
    private static final Pattern FIRST_LETTER = Pattern.compile("^\\w|_\\w");

    public static String prettify(ResourceLocation identifier) {
        Matcher matcher = FIRST_LETTER.matcher(identifier.getPath());
        return matcher.replaceAll(x -> {
            String value = x.group(0);
            return value.length() == 1 ? value.toUpperCase() : value.toUpperCase().replace('_', ' ');
        });
    }

    public static Component prettifyAsText(ResourceLocation identifier) {
        return Component.nullToEmpty(prettify(identifier));
    }

    public static ResourceLocation of(String namespace, String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new ResourceLocation(namespace, path);
        *///?}
    }

    public static ResourceLocation vanilla(String path) {
        //? if >=1.21 {
        return ResourceLocation.withDefaultNamespace(path);
        //?} else {
        /*return new ResourceLocation(path);
        *///?}
    }

    /**
     * Parses "namespace:path" (or just "path" for minecraft:path).
     */
    public static ResourceLocation parse(String id) {
        //? if >=1.21 {
        return ResourceLocation.parse(id);
        //?} else {
        /*return new ResourceLocation(id);
        *///?}
    }
}