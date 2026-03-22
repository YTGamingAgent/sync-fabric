package net.stacking.sync_mod.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;

@Environment(EnvType.CLIENT)
public final class CustomRenderLayer extends RenderLayer {
    public static final RenderLayer SHELL;
    private static final RenderLayer VOXELS;
    private static final BiFunction<Identifier, Boolean, RenderLayer> ENTITY_TRANSLUCENT_PARTIALLY_TEXTURED;

    private CustomRenderLayer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static RenderLayer getVoxels() {
        return VOXELS;
    }

    public static RenderLayer getEntityTranslucentPartiallyTextured(Identifier textureId, float cutoutY) {
        return getEntityTranslucentPartiallyTextured(textureId, cutoutY, true);
    }

    public static RenderLayer getEntityTranslucentPartiallyTextured(Identifier textureId, float cutoutY, boolean affectsOutline) {
        CustomGameRenderer.initRenderTypeEntityTranslucentPartiallyTexturedShader(cutoutY, MatrixStackStorage.getModelMatrixStack().peek().getPositionMatrix());
        return ENTITY_TRANSLUCENT_PARTIALLY_TEXTURED.apply(textureId, affectsOutline);
    }

    static {
        // SHELL render layer
        SHELL = RenderLayer.getEntityTranslucent(Identifier.ofVanilla("textures/entity/player/wide/steve.png"));

        // VOXELS render layer - using vanilla solid rendering
        VOXELS = of("voxels",
                CustomVertexFormats.POSITION_COLOR_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256,
                false,
                false,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.SOLID_PROGRAM)
                        .transparency(NO_TRANSPARENCY)
                        .cull(DISABLE_CULLING)
                        .lightmap(ENABLE_LIGHTMAP)
                        .overlay(ENABLE_OVERLAY_COLOR)
                        .build(true));

        // Entity translucent partially textured - fallback to vanilla
        ENTITY_TRANSLUCENT_PARTIALLY_TEXTURED = Util.memoize((id, outline) ->
                RenderLayer.getEntityTranslucent(id, outline));
    }
}