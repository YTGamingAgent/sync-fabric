package net.stacking.sync_mod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The treadmill always starts on top of the block you click, no matter which face you click.
 * From there it runs one more block away from you (see TreadmillBlock#getPlacementState).
 */
public class TreadmillItem extends BlockItem {
    public TreadmillItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        // Clicking something replaceable (tall grass, snow layer, ...) places it right there, like vanilla.
        if (context.replacingClickedOnBlock()) {
            return context;
        }

        BlockPos clickedPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        return BlockPlaceContext.at(context, clickedPos, Direction.UP);
    }
}
