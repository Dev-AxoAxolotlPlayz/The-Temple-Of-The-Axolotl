package com.devaxo.templeoftheaxolotl.abyss;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.AxoleviathanEntity;
import com.devaxo.templeoftheaxolotl.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Abyss-wide population ledger. Never replaces unloaded registered creatures. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AxoleviathanSpawnEvents {
    private static int ticks;
    private static final int MAX = 5, MIN_DISTANCE = 250;
    private static final int SPAWN_Y = 35;

    public static final class Population extends SavedData {
        private final Map<UUID, BlockPos> known = new LinkedHashMap<>();
        public static Population get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Population::new, Population::load), "tota_axoleviathan_population_v1");
        }
        private static Population load(CompoundTag tag, HolderLookup.Provider provider) {
            Population result = new Population();
            ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag entry = entries.getCompound(i);
                if (entry.hasUUID("Id")) result.known.put(entry.getUUID("Id"),
                    new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
            }
            return result;
        }
        @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            ListTag entries = new ListTag();
            for (var record : known.entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Id", record.getKey());
                entry.putInt("X", record.getValue().getX());
                entry.putInt("Y", record.getValue().getY());
                entry.putInt("Z", record.getValue().getZ());
                entries.add(entry);
            }
            tag.put("Entries", entries);
            return tag;
        }
        void adopt(AxoleviathanEntity entity) {
            if (!known.containsKey(entity.getUUID())) {
                known.put(entity.getUUID(), entity.blockPosition()); setDirty();
            }
        }
    }

    /** Reserve old and unloaded population anchors during territory selection. */
    public static boolean isReserved(ServerLevel level, UUID self, BlockPos point, int distance) {
        Population population = Population.get(level);
        for (var entry : population.known.entrySet()) {
            if (entry.getKey().equals(self)) continue;
            BlockPos old = entry.getValue();
            double dx = point.getX() - old.getX(), dz = point.getZ() - old.getZ();
            if (dx * dx + dz * dz < (double) distance * distance) return true;
        }
        return false;
    }

    @SubscribeEvent public static void onTick(ServerTickEvent.Post event) {
        ServerLevel abyss = event.getServer().getLevel(AbyssTrialEvents.ABYSS);
        if (abyss == null) return;
        Population population = Population.get(abyss);
        // Adopt any preexisting loaded danger noodles BEFORE making new ones.
        for (var entity : abyss.getAllEntities())
            if (entity instanceof AxoleviathanEntity leviathan && leviathan.isAlive())
                population.adopt(leviathan);
        if (++ticks % 1200 != 0 || population.known.size() >= MAX) return;
        if (abyss.players().isEmpty()) return;
        // Spread new individuals far from players and the arrival island. If no suitable
        // deep-water position is already loaded, postpone rather than forcing a bad spawn.
        for (int attempt = 0; attempt < 100; attempt++) {
            ServerPlayer player = abyss.players().get(abyss.random.nextInt(abyss.players().size()));
            double angle = abyss.random.nextDouble() * Math.PI * 2;
            int radius = 180 + abyss.random.nextInt(220);
            int x = player.blockPosition().getX() + (int)(Math.cos(angle) * radius);
            int z = player.blockPosition().getZ() + (int)(Math.sin(angle) * radius);
            if (Math.abs(x) < 150 && Math.abs(z) < 150) continue;
            BlockPos candidate = new BlockPos(x, SPAWN_Y, z);
            if (!abyss.getChunkSource().hasChunk(x >> 4, z >> 4)) continue;
            boolean separated = true;
            for (BlockPos old : population.known.values()) {
                if (Math.pow(old.getX() - x, 2) + Math.pow(old.getZ() - z, 2) < (long)MIN_DISTANCE * MIN_DISTANCE) {
                    separated = false; break;
                }
            }
            if (!separated) continue;
            for (var entity : abyss.getAllEntities()) {
                if (entity instanceof AxoleviathanEntity other && other.isAlive()) {
                    double dx = other.getX() - x, dz = other.getZ() - z;
                    if (dx * dx + dz * dz < (double) MIN_DISTANCE * MIN_DISTANCE) {
                        separated = false; break;
                    }
                }
            }
            if (!separated) continue;
            // Large creature needs a full water corridor, not a shallow shelf.
            boolean clear = true;
            for (int dy = -6; dy <= 12 && clear; dy += 3) {
                for (int dx = -6; dx <= 6 && clear; dx += 6) {
                    for (int dz = -6; dz <= 6; dz += 6) {
                        BlockPos sample = candidate.offset(dx, dy, dz);
                        if (!abyss.getChunkSource().hasChunk(sample.getX() >> 4, sample.getZ() >> 4)
                                || !abyss.getFluidState(sample).is(Fluids.WATER)
                                || !abyss.getBlockState(sample).getCollisionShape(abyss, sample).isEmpty()) {
                            clear = false; break;
                        }
                    }
                }
            }
            if (!clear) continue;
            AxoleviathanEntity mob = ModEntities.AXOLEVIATHAN.get().create(abyss);
            if (mob == null) return;
            mob.moveTo(x + .5, SPAWN_Y + .5, z + .5, abyss.random.nextFloat() * 360, 0);
            if (abyss.addFreshEntity(mob)) population.adopt(mob);
            return;
        }
    }
}
