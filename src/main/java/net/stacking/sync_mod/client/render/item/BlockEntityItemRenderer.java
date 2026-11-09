package net.stacking.sync_mod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.stacking.sync_mod.Sync;
import net.stacking.sync_mod.util.GameRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a block's block entity as its item (1.21.4+ "special" item model). Used by the shell
 * containers and the treadmill, whose renderers draw the whole machine when the block entity
 * isn't in a world. See assets/sync/items/*.json.
 */
@Environment(EnvType.CLIENT)
public final class BlockEntityItemRenderer implements SpecialModelRenderer<Void> {
    private final BlockEntity blockEntity;

    private BlockEntityItemRenderer(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public static void register() {
        SpecialModelRenderers.ID_MAPPER.put(Sync.locate("block_entity"), Unbaked.CODEC);
    }

    //? if >=1.21.9 {
    /*@Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void submit(@Nullable Void argument, /^? if <26.1 {^/ItemDisplayContext displayContext, /^?}^/PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(this.blockEntity) instanceof net.stacking.sync_mod.client.render.block.entity.DoubleBlockEntityRenderer renderer) {
            net.stacking.sync_mod.client.render.block.entity.DoubleBlockEntityRenderer.State state = renderer.createRenderState();
            state.blockEntity = this.blockEntity;
            //? if <26.1
            state.blockState = this.blockEntity.getBlockState();
            state.lightCoords = light;
            renderer.submit(state, matrices, collector, null);
        }
    }
    *///?} else {
    @Override
    public void render(@Nullable Void argument, ItemDisplayContext displayContext, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, boolean hasFoil) {
        BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(this.blockEntity);
        if (renderer != null) {
            renderer.render(this.blockEntity, 0, matrices, vertexConsumers, light, overlay/*? if >=1.21.5 {*//*, net.minecraft.world.phys.Vec3.ZERO*//*?}*/);
        }
    }
    //?}

    //? if >=1.21.6 {
    /*@Override
    //? if >=1.21.11 {
    /^public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> extents) {
        java.util.Set<org.joml.Vector3f> points = new java.util.HashSet<>();
        this.getExtents(points);
        points.forEach(extents);
    }

    private void getExtents(java.util.Set<org.joml.Vector3f> extents) {
    ^///?} else
    public void getExtents(java.util.Set<org.joml.Vector3f> extents) {
        // The machines are at most 1 x 2 x 1 blocks.
        for (int x = 0; x <= 1; ++x) {
            for (int y = 0; y <= 2; y += 2) {
                for (int z = 0; z <= 1; ++z) {
                    extents.add(new org.joml.Vector3f(x, y, z));
                }
            }
        }
    }
    *///?}

    @Override
    public Void extractArgument(ItemStack stack) {
        return null;
    }

    public record Unbaked(ResourceLocation block) implements SpecialModelRenderer.Unbaked/*? if >=26.1 {*//*<Void>*//*?}*/ {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("block").forGetter(Unbaked::block)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        @Nullable
        //? if >=26.1 {
        /*public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
        *///?} else if >=1.21.9 {
        /*public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
        *///?} else
        public SpecialModelRenderer<?> bake(EntityModelSet entityModels) {
            Block block = GameRegistries.get(GameRegistries.BLOCK, this.block);
            if (!(block instanceof EntityBlock entityBlock)) {
                return null;
            }
            BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            return blockEntity == null ? null : new BlockEntityItemRenderer(blockEntity);
        }
    }
}
