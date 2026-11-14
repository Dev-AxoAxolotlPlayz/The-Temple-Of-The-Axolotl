package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Server-authoritative death receipts; persisted in the Overworld SavedData. */
public final class OfflineCompanionDeaths extends SavedData {
    private final Map<UUID, CompoundTag> pending = new LinkedHashMap<>();

    public static OfflineCompanionDeaths get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(OfflineCompanionDeaths::new, OfflineCompanionDeaths::load),
                "tota_offline_companion_deaths_v1");
    }

    private static OfflineCompanionDeaths load(CompoundTag tag, HolderLookup.Provider provider) {
        OfflineCompanionDeaths data = new OfflineCompanionDeaths();
        ListTag list = tag.getList("Deaths", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("UUID") && entry.hasUUID("Owner"))
                data.pending.put(entry.getUUID("UUID"), entry.copy());
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        pending.values().forEach(entry -> list.add(entry.copy()));
        tag.put("Deaths", list);
        return tag;
    }

    public void record(CompoundTag entry) {
        if (!entry.hasUUID("UUID") || !entry.hasUUID("Owner") || pending.containsKey(entry.getUUID("UUID"))) return;
        pending.put(entry.getUUID("UUID"), entry.copy());
        setDirty();
    }

    public boolean isPending(UUID companion) { return pending.containsKey(companion); }

    /** Remove only after the owner's player NBT has received the death snapshot. */
    public void reconcile(net.minecraft.server.level.ServerPlayer owner) {
        List<UUID> completed = new ArrayList<>();
        for (var item : pending.entrySet()) {
            CompoundTag entry = item.getValue();
            if (!owner.getUUID().equals(entry.getUUID("Owner"))) continue;
            AbyssalCompanionRegistry.applyOfflineDeath(owner, entry);
            completed.add(item.getKey());
        }
        if (!completed.isEmpty()) {
            completed.forEach(pending::remove);
            setDirty();
            AbyssalCompanionRegistry.sync(owner);
        }
    }
}
