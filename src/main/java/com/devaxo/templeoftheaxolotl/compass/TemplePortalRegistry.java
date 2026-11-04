package com.devaxo.templeoftheaxolotl.compass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;

/** World-wide permanent list of retired portal blocks, saved in overworld data. */
public final class TemplePortalRegistry extends SavedData {
    private static final String FILE = "tota_consumed_portals";
    private final Set<Long> consumed = new HashSet<>();

    public static TemplePortalRegistry get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(TemplePortalRegistry::new, TemplePortalRegistry::load), FILE);
    }

    private static TemplePortalRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        TemplePortalRegistry data = new TemplePortalRegistry();
        ListTag list = tag.getList("Consumed", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) data.consumed.add(list.getCompound(i).getLong("Pos"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (long pos : consumed) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos);
            list.add(entry);
        }
        tag.put("Consumed", list);
        return tag;
    }

    public boolean isConsumed(BlockPos pos) {
        // Structure locator returns the jigsaw start, whereas the saved location
        // is the actual portal block near the center of its 49x49 footprint.
        for (long packed : consumed) {
            BlockPos portal = BlockPos.of(packed);
            if (Math.abs(portal.getX() - pos.getX()) <= 64 &&
                Math.abs(portal.getZ() - pos.getZ()) <= 64) return true;
        }
        return false;
    }
    /** Dev-only: forget a retired portal at this exact block, not its 64-block neighborhood. */
    public boolean unconsumeExact(BlockPos pos) {
        boolean removed = consumed.remove(pos.asLong());
        if (removed) setDirty();
        return removed;
    }
    /** Development-only reset of retirement markers affecting a reused portal. */
    public int clearNearbyForDev(BlockPos pos) {
        int before = consumed.size();
        consumed.removeIf(packed -> {
            BlockPos old = BlockPos.of(packed);
            return Math.abs(old.getX() - pos.getX()) <= 64
                    && Math.abs(old.getZ() - pos.getZ()) <= 64;
        });
        int removed = before - consumed.size();
        if (removed > 0) setDirty();
        return removed;
    }
    public boolean consume(BlockPos pos) {
        boolean added = consumed.add(pos.asLong());
        if (added) setDirty();
        return added;
    }
}
