package net.stacking.sync_mod.client.model;

import net.stacking.sync_mod.util.math.Voxel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public interface VoxelProvider {
    default boolean isUpsideDown() {
        return true;
    }

    Stream<Voxel> getVoxels();
}