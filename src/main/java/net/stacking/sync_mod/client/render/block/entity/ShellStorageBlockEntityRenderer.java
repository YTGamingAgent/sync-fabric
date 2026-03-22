package net.stacking.sync_mod.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.block.AbstractShellContainerBlock;
import net.stacking.sync_mod.block.SyncBlocks;
import net.stacking.sync_mod.block.entity.ShellStorageBlockEntity;
import net.stacking.sync_mod.entity.ShellEntity;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@Environment(EnvType.CLIENT)
public class ShellStorageBlockEntityRenderer
        extends GeoBlockRenderer<ShellStorageBlockEntity> {

    private static final BlockState DEFAULT_STATE = SyncBlocks.SHELL_STORAGE
            .getDefaultState()
            .with(AbstractShellContainerBlock.HALF,   DoubleBlockHalf.LOWER)
            .with(AbstractShellContainerBlock.FACING, Direction.SOUTH)
            .with(AbstractShellContainerBlock.OPEN,   false);

    public ShellStorageBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(new DefaultedBlockGeoModel<>(Sync.locate("shell_storage")));
    }

    @Override
    public void render(ShellStorageBlockEntity blockEntity, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        BlockState state = blockEntity.hasWorld()
                ? blockEntity.getCachedState()
                : DEFAULT_STATE;

        if (!AbstractShellContainerBlock.isBottom(state)) return;

        Direction facing = state.get(AbstractShellContainerBlock.FACING);

        // Match ShellConstructor's rotation scheme exactly
        float yRot = switch (facing) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };

        matrices.push();

        // SAME ORDER AS SHELL CONSTRUCTOR - this works
        matrices.translate(0.5, 0.0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRot));
        matrices.translate(-0.5, 0.0, -0.5);

        super.render(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay);

        matrices.pop();

        ShellState shellState = blockEntity.getShellState();
        if (shellState != null) {
            renderStoredShell(shellState, blockEntity, tickDelta, matrices, vertexConsumers, light);
        }
    }

    private void renderStoredShell(ShellState shellState,
                                   ShellStorageBlockEntity blockEntity,
                                   float tickDelta,
                                   MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers,
                                   int light) {
        BlockState blockState = blockEntity.hasWorld()
                ? blockEntity.getCachedState()
                : DEFAULT_STATE;

        float yaw = blockState.get(AbstractShellContainerBlock.FACING)
                .getOpposite().asRotation();

        ShellEntity shellEntity = shellState.asEntity();
        shellEntity.isActive      = shellState.getProgress() >= ShellState.PROGRESS_DONE;
        shellEntity.pitchProgress = shellEntity.isActive
                ? blockEntity.getConnectorProgress(tickDelta)
                : 0;

        EntityRenderer<? super ShellEntity> renderer =
                MinecraftClient.getInstance()
                        .getEntityRenderDispatcher()
                        .getRenderer(shellEntity);

        renderer.render(shellEntity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
