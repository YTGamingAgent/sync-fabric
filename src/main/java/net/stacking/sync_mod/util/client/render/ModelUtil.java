package net.stacking.sync_mod.util.client.render;

import net.stacking.sync_mod.util.math.Voxel;
import net.stacking.sync_mod.util.math.VoxelIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
//? if <1.21.2
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Environment(EnvType.CLIENT)
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ModelUtil {
    /**
     * Renders a model part tinted with a packed ARGB color.
     * (Before 1.21 the game took the tint as four floats instead.)
     */
    public static void renderPart(ModelPart part, PoseStack matrices, VertexConsumer vertices, int light, int overlay, int argb) {
        //? if >=1.21 {
        part.render(matrices, vertices, light, overlay, argb);
        //?} else {
        /*part.render(matrices, vertices, light, overlay, red(argb), green(argb), blue(argb), alpha(argb));
        *///?}
    }

    /**
     * Renders a whole model tinted with a packed ARGB color.
     */
    public static void renderModel(Model model, PoseStack matrices, VertexConsumer vertices, int light, int overlay, int argb) {
        //? if >=1.21 {
        model.renderToBuffer(matrices, vertices, light, overlay, argb);
        //?} else {
        /*model.renderToBuffer(matrices, vertices, light, overlay, red(argb), green(argb), blue(argb), alpha(argb));
        *///?}
    }

    /**
     * The top-level parts of a model (their children are reachable through them).
     */
    public static Stream<ModelPart> rootParts(Model model) {
        //? if >=1.21.2 {
        /*return Stream.of(model.root());
        *///?} else {
        if (model instanceof AgeableListModel<?> listModel) {
            return Stream.concat(StreamSupport.stream(listModel.headParts().spliterator(), false), StreamSupport.stream(listModel.bodyParts().spliterator(), false));
        }
        return Stream.empty();
        //?}
    }

    /**
     * Puts a model back into its default (standing, fully visible) pose.
     */
    public static void resetPose(Model model) {
        //? if >=1.21.2 {
        /*model.resetPose();
        *///?} else {
        rootParts(model).forEach(x -> x.setRotation(0, 0, 0));
        if (model instanceof AgeableListModel<?> listModel) {
            listModel.young = false;
        }
        //?}
        if (model instanceof PlayerModel playerModel) {
            //? if >=26.1 {
            /*playerModel.root().getAllParts().forEach(part -> part.visible = true);
            *///?} else
            playerModel.setAllVisible(true);
        }
    }

    public static int argb(float red, float green, float blue, float alpha) {
        return ((int)(alpha * 255) & 0xFF) << 24 | ((int)(red * 255) & 0xFF) << 16 | ((int)(green * 255) & 0xFF) << 8 | ((int)(blue * 255) & 0xFF);
    }

    private static float alpha(int argb) { return (argb >>> 24) / 255F; }
    private static float red(int argb) { return (argb >> 16 & 0xFF) / 255F; }
    private static float green(int argb) { return (argb >> 8 & 0xFF) / 255F; }
    private static float blue(int argb) { return (argb & 0xFF) / 255F; }

    public static ModelPart copy(ModelPart original) {
        ModelPart copy = new ModelPart(original.cubes, original.children);
        copy.loadPose(original.storePose());
        return copy;
    }

    public static Stream<Voxel> asVoxels(ModelPart part) {
        return asVoxels(0, 0, 0, part);
    }

    public static Stream<Voxel> asVoxels(float x, float y, float z, ModelPart part) {
        final float pivotX = x + part.x;
        final float pivotY = y + part.y;
        final float pivotZ = z + part.z;
        return Stream.concat(
            part.cubes.stream().flatMap(cuboid -> asVoxels(pivotX, pivotY, pivotZ, cuboid)),
            part.children.values().stream().flatMap(p -> asVoxels(pivotX, pivotY, pivotZ, p))
        );
    }

    public static Stream<Voxel> asVoxels(float x, float y, float z, ModelPart.Cube cuboid) {
        int sizeX = (int)(cuboid.maxX - cuboid.minX);
        int sizeY = (int)(cuboid.maxY - cuboid.minY);
        int sizeZ = (int)(cuboid.maxZ - cuboid.minZ);
        Iterator<Voxel> iterator = new VoxelIterator(x, y, z, sizeX, sizeY, sizeZ);
        Spliterator<Voxel> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        return StreamSupport.stream(spliterator, false);
    }
}