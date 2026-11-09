package net.stacking.sync_mod.util.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//? if >=1.19.3 {
import com.mojang.math.Axis;
//?} else
/*import com.mojang.math.Vector3f;*/

/**
 * Rotations for a PoseStack. Minecraft switched from its own math classes to JOML in 1.19.3.
 */
@Environment(EnvType.CLIENT)
public final class PoseUtil {
    public static void rotateXDegrees(PoseStack matrices, float degrees) {
        //? if >=1.19.3 {
        matrices.mulPose(Axis.XP.rotationDegrees(degrees));
        //?} else
        /*matrices.mulPose(Vector3f.XP.rotationDegrees(degrees));*/
    }

    public static void rotateYDegrees(PoseStack matrices, float degrees) {
        //? if >=1.19.3 {
        matrices.mulPose(Axis.YP.rotationDegrees(degrees));
        //?} else
        /*matrices.mulPose(Vector3f.YP.rotationDegrees(degrees));*/
    }

    public static void rotateZ(PoseStack matrices, float radians) {
        //? if >=1.19.3 {
        matrices.mulPose(Axis.ZP.rotation(radians));
        //?} else
        /*matrices.mulPose(Vector3f.ZP.rotation(radians));*/
    }

    private PoseUtil() { }
}
