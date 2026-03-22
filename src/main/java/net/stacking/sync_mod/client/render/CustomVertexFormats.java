package net.stacking.sync_mod.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

@Environment(EnvType.CLIENT)
public final class CustomVertexFormats {
    public static final VertexFormat POSITION_COLOR_OVERLAY_LIGHT_NORMAL = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
}