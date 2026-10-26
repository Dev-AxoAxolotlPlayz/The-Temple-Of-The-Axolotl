package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent owner -> living Cluckles ID, shared by all dimensions. */
public final class ClucklesRegistry extends SavedData {
    private final Map<UUID, UUID> active = new HashMap<>();
    private final java.util.Set<UUID> pendingLoss = new java.util.HashSet<>();
    public static ClucklesRegistry get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ClucklesRegistry::new, ClucklesRegistry::load), "tota_cluckles_owners");
    }
    private static ClucklesRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        ClucklesRegistry data = new ClucklesRegistry();
        ListTag list = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (e.hasUUID("Owner") && e.hasUUID("Chicken")) data.active.put(e.getUUID("Owner"), e.getUUID("Chicken"));
        }
        ListTag pending = tag.getList("PendingLoss", Tag.TAG_COMPOUND);
        for (int i = 0; i < pending.size(); i++) { CompoundTag e = pending.getCompound(i); if (e.hasUUID("Owner")) data.pendingLoss.add(e.getUUID("Owner")); }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        active.forEach((owner, chicken) -> {
            CompoundTag e = new CompoundTag(); e.putUUID("Owner", owner); e.putUUID("Chicken", chicken); list.add(e);
        });
        tag.put("Owners", list);
        ListTag pending = new ListTag();
        for (UUID owner : pendingLoss) { CompoundTag e = new CompoundTag(); e.putUUID("Owner", owner); pending.add(e); }
        tag.put("PendingLoss", pending);
        return tag;
    }
    public boolean has(UUID owner) { return active.containsKey(owner); }
    public void register(UUID owner, UUID chicken) { active.put(owner, chicken); setDirty(); }
    public boolean consumeLoss(UUID owner) { if (pendingLoss.remove(owner)) { setDirty(); return true; } return false; }
    public void unregister(UUID owner, UUID chicken) {
        if (chicken.equals(active.get(owner))) { active.remove(owner); pendingLoss.add(owner); setDirty(); }
    }
}
