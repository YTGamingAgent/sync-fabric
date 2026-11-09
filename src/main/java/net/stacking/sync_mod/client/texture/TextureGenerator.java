package net.stacking.sync_mod.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

/**
 * Produces a set of images that are registered as dynamic textures (see {@link GeneratedTextureManager}).
 */
@Environment(EnvType.CLIENT)
@FunctionalInterface
public interface TextureGenerator {
    List<NativeImage> generateImages();
}
