package com.devaxo.templeoftheaxolotl.maze;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent once-per-world Origin construction journal. */
public final class OriginTempleState extends SavedData {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "fractured_origin"));
    private boolean started, complete;
    private long seed;
    private int placed, lootPlaced;
    private java.util.UUID triggeringPlayer;
    private int lockedWrathTier = 1;
    private int lockedWrathDefeats;
    private boolean originResolved, originForgiven, rescueStarted;
    private net.minecraft.core.BlockPos rewardChest;
    public static OriginTempleState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(OriginTempleState::new, OriginTempleState::load), "tota_origin_generation");
    }
    private static OriginTempleState load(CompoundTag tag, HolderLookup.Provider lookup) {
        OriginTempleState state = new OriginTempleState();
        state.started = tag.getBoolean("Started"); state.complete = tag.getBoolean("Complete");
        state.seed = tag.getLong("Seed"); state.placed = tag.getInt("Placed");
        state.lootPlaced = tag.getInt("LootPlaced");
        if (tag.hasUUID("TriggeringPlayer")) state.triggeringPlayer = tag.getUUID("TriggeringPlayer");
        state.lockedWrathTier = Math.max(1, Math.min(3, tag.getInt("LockedWrathTier")));
        state.lockedWrathDefeats = tag.getInt("LockedWrathDefeats");
        state.originResolved = tag.getBoolean("OriginResolved");
        state.originForgiven = tag.getBoolean("OriginForgiven");
        state.rescueStarted = tag.getBoolean("RescueStarted");
        if (tag.contains("RewardChest")) state.rewardChest = net.minecraft.nbt.NbtUtils.readBlockPos(tag, "RewardChest").orElse(null);
        return state;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putBoolean("Started", started); tag.putBoolean("Complete", complete);
        tag.putLong("Seed", seed); tag.putInt("Placed", placed);
        tag.putInt("LootPlaced", lootPlaced);
        if (triggeringPlayer != null) tag.putUUID("TriggeringPlayer", triggeringPlayer);
        tag.putInt("LockedWrathTier", lockedWrathTier);
        tag.putInt("LockedWrathDefeats", lockedWrathDefeats);
        tag.putBoolean("OriginResolved", originResolved);
        tag.putBoolean("OriginForgiven", originForgiven);
        tag.putBoolean("RescueStarted", rescueStarted);
        if (rewardChest != null) tag.put("RewardChest", net.minecraft.nbt.NbtUtils.writeBlockPos(rewardChest));
        return tag;
    }
    public boolean originResolved() { return originResolved; }
    public boolean originForgiven() { return originForgiven; }
    public boolean rescueStarted() { return rescueStarted; }
    public net.minecraft.core.BlockPos rewardChest() { return rewardChest; }
    public void resolve(boolean forgiven, net.minecraft.core.BlockPos chest) {
        originResolved = true; originForgiven = forgiven; rewardChest = chest; setDirty();
    }
    public void beginRescue() { rescueStarted = true; setDirty(); }
    public boolean started() { return started; }
    public boolean complete() { return complete; }
    public long seed() { return seed; }
    public int placed() { return placed; }
    public int lootPlaced() { return lootPlaced; }
    public java.util.UUID triggeringPlayer() { return triggeringPlayer; }
    public int lockedWrathTier() { return lockedWrathTier; }
    public int lockedWrathDefeats() { return lockedWrathDefeats; }
    public void lockTrigger(net.minecraft.server.level.ServerPlayer player) {
        if (triggeringPlayer != null) return;
        triggeringPlayer = player.getUUID();
        lockedWrathTier = com.devaxo.templeoftheaxolotl.traps.TempleProgress.originWrathTier(player);
        lockedWrathDefeats = com.devaxo.templeoftheaxolotl.traps.TempleProgress.decoyWrathDefeats(player);
        setDirty();
    }
    public void start(long value) { started = true; seed = value; setDirty(); }
    public void progress(int value) { placed = value; setDirty(); }
    public void lootProgress(int value) { lootPlaced = value; setDirty(); }
    public void finish() { complete = true; setDirty(); }
    /** Operator-only reset; never invoked by normal dungeon completion. */
    public void resetForDevelopment() { started = false; complete = false; seed = 0L; placed = 0; lootPlaced = 0; triggeringPlayer = null; lockedWrathTier = 1; lockedWrathDefeats = 0; originResolved = false; originForgiven = false; rescueStarted = false; rewardChest = null; setDirty(); }
}
