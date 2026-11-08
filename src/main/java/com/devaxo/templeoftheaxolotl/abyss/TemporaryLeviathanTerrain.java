package com.devaxo.templeoftheaxolotl.abyss;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

/** Persistent, reversible, server-authoritative temporary terrain displacement. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class TemporaryLeviathanTerrain extends SavedData {
    private static final String FILE = "tota_leviathan_ghost_terrain";
    private static final int MAX_PENDING = 12000;
    private final Map<Long, Entry> pending = new HashMap<>();
    private record Entry(BlockState original, BlockState temporary, long expires) {}

    public static TemporaryLeviathanTerrain get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TemporaryLeviathanTerrain::new, TemporaryLeviathanTerrain::load), FILE);
    }
    private static TemporaryLeviathanTerrain load(CompoundTag tag, HolderLookup.Provider registries) {
        TemporaryLeviathanTerrain result = new TemporaryLeviathanTerrain();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            BlockState original = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), item.getCompound("Original"));
            BlockState temporary = item.getBoolean("Water") ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            result.pending.put(item.getLong("Pos"), new Entry(original, temporary, item.getLong("Expires")));
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        pending.forEach((pos, entry) -> {
            CompoundTag item = new CompoundTag();
            item.putLong("Pos", pos);
            item.put("Original", NbtUtils.writeBlockState(entry.original));
            item.putBoolean("Water", entry.temporary.is(Blocks.WATER));
            item.putLong("Expires", entry.expires);
            list.add(item);
        });
        tag.put("Entries", list);
        return tag;
    }
    /** A 9-block-diameter sphere. A bounded budget prevents large spikes. */
    public void devourSphere(ServerLevel level, BlockPos center, int budget) {
        int changed = 0;
        long expires = level.getGameTime() + 600;
        for (int dx = -4; dx <= 4 && changed < budget; dx++)
            for (int dy = -4; dy <= 4 && changed < budget; dy++)
                for (int dz = -4; dz <= 4 && changed < budget; dz++) {
                    if (dx * dx + dy * dy + dz * dz > 20) continue; // radius ~4.5
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                    long key = pos.asLong();
                    Entry old = pending.get(key);
                    if (old != null) {
                        if (old.expires < expires) { pending.put(key, new Entry(old.original, old.temporary, expires)); setDirty(); }
                        continue;
                    }
                    if (pending.size() >= MAX_PENDING) return;
                    BlockState original = level.getBlockState(pos);
                    if (original.isAir() || original.hasBlockEntity() || original.getDestroySpeed(level, pos) < 0) continue;
                    if (!BuiltInRegistries.BLOCK.getKey(original.getBlock()).getNamespace().equals("minecraft")) continue;
                    if (original.getCollisionShape(level, pos).isEmpty()) continue;
                    // Preserve a flooded tunnel even when the solid block itself had no fluid state.
                    boolean flooded = level.getFluidState(pos).is(FluidTags.WATER);
                    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                        BlockPos adjacent = pos.relative(dir);
                        if (level.getChunkSource().hasChunk(adjacent.getX() >> 4, adjacent.getZ() >> 4)
                                && level.getFluidState(adjacent).is(FluidTags.WATER)) flooded = true;
                    }
                    BlockState temporary = flooded ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    if (level.setBlock(pos, temporary, 3)) {
                        pending.put(key, new Entry(original, temporary, expires));
                        changed++;
                        setDirty();
                    }
                }
    }
    @SubscribeEvent public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) get(level).restoreDue(level);
    }
    private void restoreDue(ServerLevel level) {
        int restored = 0;
        Iterator<Map.Entry<Long, Entry>> it = pending.entrySet().iterator();
        while (it.hasNext() && restored < 96) {
            Map.Entry<Long, Entry> record = it.next();
            Entry entry = record.getValue();
            if (entry.expires > level.getGameTime()) continue;
            BlockPos pos = BlockPos.of(record.getKey());
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            if (!level.getBlockState(pos).equals(entry.temporary)) { it.remove(); setDirty(); continue; }
            if (!level.getEntities((net.minecraft.world.entity.Entity)null, new AABB(pos).inflate(0.02),
                    entity -> entity.isAlive()).isEmpty()) continue;
            if (level.setBlock(pos, entry.original, 3)) { it.remove(); restored++; setDirty(); }
        }
    }
}
