package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Approval-bound Town Center special-feature requirements.
 * No world modification until the actual statue/Watergate block IDs
 * and lifecycle rules are integrated.
 */
public final class WaifuAxoluthuTownCenterFeatures {
    private WaifuAxoluthuTownCenterFeatures() {}
    public static CompoundTag plan(BlockPos origin, int turns) {
        CompoundTag out = new CompoundTag();
        out.putInt("schema",25);
        out.putLong("center",origin.offset(7,1,7).asLong());
        out.putInt("rotation",Math.floorMod(turns,4));
        out.putString("feature","axoluthu_statue_with_watergate_portal");
        out.putBoolean("requiresOwnerApproval",true);
        out.putBoolean("conjured",false);
        out.putBoolean("constructionEnabled",false);
        return out;
    }
}
