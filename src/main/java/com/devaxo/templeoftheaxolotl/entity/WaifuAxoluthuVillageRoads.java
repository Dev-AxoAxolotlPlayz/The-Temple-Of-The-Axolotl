package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * Non-destructive road blueprint between two approved settlement points.
 * Sea lantern markers occur every third path block, counted from the start.
 * Terrain, ownership and obstruction checks must occur before construction.
 */
public final class WaifuAxoluthuVillageRoads {
    private WaifuAxoluthuVillageRoads() {}

    public static CompoundTag preview(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dz = Math.abs(from.getZ() - to.getZ());
        if (dx + dz > 256 || dx + dz < 2
                || Math.abs(from.getY() - to.getY()) > 1)
            throw new IllegalArgumentException("Road endpoints too far apart or uneven");
        ListTag blocks = new ListTag();
        int x = from.getX(), z = from.getZ(), step = 0;
        while (x != to.getX() || z != to.getZ()) {
            if (x != to.getX()) x += Integer.signum(to.getX() - x);
            else z += Integer.signum(to.getZ() - z);
            step++;
            CompoundTag cell = new CompoundTag();
            cell.putLong("pos", new BlockPos(x, from.getY(), z).asLong());
            cell.putString("block", step % 3 == 0
                    ? "minecraft:sea_lantern" : "minecraft:cobblestone");
            cell.putInt("index", step);
            blocks.add(cell);
        }
        CompoundTag result = new CompoundTag();
        result.putInt("schema", 27);
        result.putLong("start", from.asLong());
        result.putLong("end", to.asLong());
        result.putBoolean("ownerApprovalRequired", true);
        result.putBoolean("constructionAllowed", false);
        result.putString("status", "PREVIEW_ONLY_PENDING_TERRAIN_SURVEY");
        result.put("blocks", blocks);
        return result;
    }
}
