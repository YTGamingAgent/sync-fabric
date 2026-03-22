package net.stacking.sync_mod.compat.iris;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Optional compatibility helpers for Iris.
 *
 * Iris changes / replaces certain render layers. Historically this mod relied on
 * a few Iris-provided layers, but Iris internals change frequently. To avoid
 * hard API coupling, this class uses a "best effort" approach: when Iris is
 * present we attempt to call its public helpers reflectively; otherwise we fall
 * back to vanilla render layers.
 */
@Environment(EnvType.CLIENT)
public final class IrisRenderLayer {

    private IrisRenderLayer() {
    }

    /** Vanilla fallback. */
    private static RenderLayer VOXELS = RenderLayer.getEntitySolid(Identifier.of("sync", "textures/misc/blank.png"));

    public static RenderLayer getVoxels() {
        // Try to pull Iris' layer if available.
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            RenderLayer iris = tryCallIrisLayer("getVoxels");
            if (iris != null) {
                return iris;
            }
        }
        return VOXELS;
    }

    public static RenderLayer getEntityTranslucentPartiallyTextured(Identifier textureId, float cutoutY, boolean affectsOutline) {
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            RenderLayer iris = tryCallIrisLayer("getEntityTranslucentPartiallyTextured", new Class<?>[]{Identifier.class, float.class, boolean.class}, new Object[]{textureId, cutoutY, affectsOutline});
            if (iris != null) {
                return iris;
            }
        }
        // Vanilla fallback – ignore cutoutY/outline.
        return RenderLayer.getEntityTranslucent(textureId);
    }

    public static RenderLayer getPrintingMask(Identifier textureId) {
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            RenderLayer iris = tryCallIrisLayer("getPrintingMask", new Class<?>[]{Identifier.class}, new Object[]{textureId});
            if (iris != null) {
                return iris;
            }
        }
        return RenderLayer.getEntityCutoutNoCull(textureId);
    }

    private static RenderLayer tryCallIrisLayer(String methodName) {
        return tryCallIrisLayer(methodName, new Class<?>[0], new Object[0]);
    }

    private static RenderLayer tryCallIrisLayer(String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            // Newer Iris versions expose helpers in net.irisshaders.iris.api.v0.IrisApi
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = irisApi.getMethod("getInstance").invoke(null);
            try {
                Object layer = api.getClass().getMethod(methodName, paramTypes).invoke(api, args);
                return (layer instanceof RenderLayer rl) ? rl : null;
            } catch (NoSuchMethodException ignored) {
                // Fall through
            }

            // Older Iris integrations might expose a static utility class.
            Class<?> util = Class.forName("net.irisshaders.iris.api.v0.IrisRenderLayers");
            Object layer = util.getMethod(methodName, paramTypes).invoke(null, args);
            return (layer instanceof RenderLayer rl) ? rl : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
