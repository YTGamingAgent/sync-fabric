package net.stacking.sync_mod.client.render;

import net.stacking.sync_mod.Sync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.resource.ResourceManager;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class CustomGameRenderer extends GameRenderer {
    // Fallback: Using vanilla shaders instead of Satin
    private static float cutoutY = 0.0f;
    private static Matrix4f modelMatrix = new Matrix4f();

    private CustomGameRenderer(MinecraftClient client, HeldItemRenderer heldItemRenderer, ResourceManager resourceManager, BufferBuilderStorage buffers) {
        super(client, heldItemRenderer, resourceManager, buffers);
    }

    public static void initRenderTypeEntityTranslucentPartiallyTexturedShader(float cutoutYValue, Matrix4f modelMatrixValue) {
        cutoutY = cutoutYValue;
        modelMatrix = new Matrix4f(modelMatrixValue);
    }

    /**
     * Returns a shader program for entity translucent rendering.
     * In 1.21+ without Satin, we return null to let the RenderLayer handle shader selection.
     */
    public static ShaderProgram getRenderTypeEntityTranslucentPartiallyTexturedShader() {
        // Return null - the RenderLayer will use its own shader program
        return null;
    }

    /**
     * Returns a shader program for voxel rendering.
     * In 1.21+ without Satin, we return null to let the RenderLayer handle shader selection.
     */
    public static ShaderProgram getRenderTypeVoxelShader() {
        // Return null - the RenderLayer will use its own shader program
        return null;
    }

    public static void initClient() {
        // Initialize if needed
    }
}