package com.devaxo.templeoftheaxolotl.maze;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;
public final class OriginPortalRegistry extends SavedData {
    private final Set<Long> converted = new HashSet<>();
    public static OriginPortalRegistry get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(OriginPortalRegistry::new,
                OriginPortalRegistry::load), "tota_origin_portals");
    }
    private static OriginPortalRegistry load(CompoundTag tag, HolderLookup.Provider lookup) {
        OriginPortalRegistry data = new OriginPortalRegistry();
        for (long value : tag.getLongArray("Converted")) data.converted.add(value);
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putLongArray("Converted", converted.stream().mapToLong(Long::longValue).toArray()); return tag;
    }
    public boolean isOrigin(BlockPos pos) { return converted.contains(pos.asLong()); }
    public void convert(BlockPos pos) { if (converted.add(pos.asLong())) setDirty(); }
}
