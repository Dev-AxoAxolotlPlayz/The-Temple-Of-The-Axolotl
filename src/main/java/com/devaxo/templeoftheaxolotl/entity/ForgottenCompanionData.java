package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Persistent, cross-dimensional records of unregistered unnamed companions lost in service. */
public final class ForgottenCompanionData extends SavedData {
    private final Map<UUID, CompoundTag> records = new LinkedHashMap<>();

    public static ForgottenCompanionData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ForgottenCompanionData::new, ForgottenCompanionData::load),
                "tota_forgotten_companions_v1");
    }

    private static ForgottenCompanionData load(CompoundTag tag, HolderLookup.Provider provider) {
        ForgottenCompanionData data = new ForgottenCompanionData();
        ListTag list = tag.getList("Ghosts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag record = list.getCompound(i);
            if (record.hasUUID("Original") && record.hasUUID("Owner"))
                data.records.put(record.getUUID("Original"), record.copy());
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        records.values().forEach(record -> list.add(record.copy()));
        tag.put("Ghosts", list);
        return tag;
    }

    public void remember(UUID original, UUID owner, String type) {
        if (records.containsKey(original)) return;
        CompoundTag record = new CompoundTag();
        record.putUUID("Original", original);
        record.putUUID("Owner", owner);
        record.putString("Type", type);
        record.putString("Stage", "waiting");
        records.put(original, record);
        setDirty();
    }

    public Collection<CompoundTag> forOwner(UUID owner) {
        List<CompoundTag> result = new ArrayList<>();
        for (CompoundTag record : records.values())
            if (owner.equals(record.getUUID("Owner")) && !"resolved".equals(record.getString("Stage")))
                result.add(record);
        return result;
    }

    public CompoundTag byOriginal(UUID original) { return records.get(original); }

    public void update(CompoundTag record) {
        if (record.hasUUID("Original")) {
            records.put(record.getUUID("Original"), record);
            setDirty();
        }
    }
}
