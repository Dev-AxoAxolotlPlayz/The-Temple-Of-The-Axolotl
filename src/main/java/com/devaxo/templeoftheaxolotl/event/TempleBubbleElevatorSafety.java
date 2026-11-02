package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.maze.TempleDimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/** Temple-only exemption for players physically occupying a bubble elevator. */
public final class TempleBubbleElevatorSafety {
    private TempleBubbleElevatorSafety() {}

    public static boolean inside(Player player) {
        if (!player.level().dimension().equals(TempleDimensionManager.TEMPLE_DIMENSION)) return false;
        // Check both feet and eye positions, since the player may straddle block boundaries.
        BlockPos feet = player.blockPosition();
        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return player.level().getBlockState(feet).is(Blocks.BUBBLE_COLUMN)
                || player.level().getBlockState(eyes).is(Blocks.BUBBLE_COLUMN);
    }
}
