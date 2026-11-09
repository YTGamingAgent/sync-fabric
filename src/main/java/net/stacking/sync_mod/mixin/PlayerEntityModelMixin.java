package net.stacking.sync_mod.mixin;

import net.stacking.sync_mod.client.model.VoxelProvider;
import net.stacking.sync_mod.util.client.render.ModelUtil;
import net.stacking.sync_mod.util.math.Voxel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
@Mixin(PlayerModel.class)
abstract class PlayerEntityModelMixin implements VoxelProvider {
    @Final
    @Shadow
    private boolean slim;

    @Override
    @SuppressWarnings("ConstantConditions")
    public Stream<Voxel> getVoxels() {
        //? if >=1.21.2 {
        /*PlayerModel model = (PlayerModel)(Object)this;
        *///?} else
        PlayerModel<?> model = (PlayerModel<?>)(Object)this;

        float cX = -2;
        float cY = 0;
        float cZ = -2;

        ModelPart head = ModelUtil.copy(model.head);
        head.setPos(cX - 2, cY - 8, cZ - 2);

        ModelPart body = ModelUtil.copy(model.body);
        body.setPos(cX - 2, cY, cZ);

        ModelPart leftArm = ModelUtil.copy(model.leftArm);
        ModelPart rightArm = ModelUtil.copy(model.rightArm);
        if (this.slim) {
            leftArm.setPos(cX + 6, cY + 0.5F, cZ);
            rightArm.setPos(cX - 5, cY + 0.5F, cZ);
        } else {
            leftArm.setPos(cX + 6, cY, cZ);
            rightArm.setPos(cX - 6, cY, cZ);
        }

        ModelPart leftLeg = ModelUtil.copy(model.leftLeg);
        leftLeg.setPos(cX + 1.9F, cY + 12, cZ);

        ModelPart rightLeg = ModelUtil.copy(model.rightLeg);
        rightLeg.setPos(cX - 1.9F, cY + 12, cZ);

        return Stream.of(leftLeg, rightLeg, leftArm, rightArm, body, head).flatMap(ModelUtil::asVoxels);
    }
}
