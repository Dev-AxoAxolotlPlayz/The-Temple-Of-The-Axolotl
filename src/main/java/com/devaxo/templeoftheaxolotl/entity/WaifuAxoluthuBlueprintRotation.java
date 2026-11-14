package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Rotates an existing approved-format block manifest without regenerating its design. */
public final class WaifuAxoluthuBlueprintRotation {
    private WaifuAxoluthuBlueprintRotation() {}

    public static CompoundTag rotateClockwise(CompoundTag source, BlockPos pivot) {
        CompoundTag result = source.copy();
        if (!source.contains("blocks", 9)) throw new IllegalArgumentException("Missing blueprint blocks");
        ListTag rotated = new ListTag();
        ListTag blocks = source.getList("blocks", 10);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i).copy();
            if (!entry.contains("pos", 4)) throw new IllegalArgumentException("Missing block position");
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            int dx = pos.getX() - pivot.getX();
            int dz = pos.getZ() - pivot.getZ();
            entry.putLong("pos", pivot.offset(-dz, pos.getY() - pivot.getY(), dx).asLong());
            if (entry.contains("facing", 8)) {
                Direction facing = Direction.byName(entry.getString("facing"));
                if (facing == null) throw new IllegalArgumentException("Unknown facing");
                entry.putString("facing", facing.getClockWise().getName());
            }
            if (entry.contains("axis", 8)) {
                String axis = entry.getString("axis");
                if ("x".equals(axis)) entry.putString("axis", "z");
                else if ("z".equals(axis)) entry.putString("axis", "x");
            }
            rotated.add(entry);
        }
        result.put("blocks", rotated);
        return result;
    }
}
