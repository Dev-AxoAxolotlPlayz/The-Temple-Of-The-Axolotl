package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;

/** Construction-only reach; does not affect combat or ordinary interactions. */
public final class WaifuAxoluthuBuildReach {
    private WaifuAxoluthuBuildReach() {}
    public static boolean inRange(WaifuAxoluthuEntity axo, BlockPos target) {
        double dx = target.getX() + .5 - axo.getX();
        double dz = target.getZ() + .5 - axo.getZ();
        double dy = target.getY() + .5 - axo.getEyeY();
        return dx * dx + dz * dz <= 9.0 && Math.abs(dy) <= 8.0;
    }
}
