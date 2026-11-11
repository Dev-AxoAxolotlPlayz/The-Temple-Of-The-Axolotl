package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Lonely Ghostfish seek nearby groups, following an existing fish without overcrowding. */
public class GhostfishSchoolGoal extends Goal {
    private final GhostfishEntity fish;
    private GhostfishEntity leader;
    private int nextSearch;
    public GhostfishSchoolGoal(GhostfishEntity fish) {
        this.fish = fish;
        setFlags(EnumSet.of(Flag.MOVE));
    }
    @Override public boolean canUse() {
        if (!fish.isInWater() || fish.getY() > 100 || --nextSearch > 0) return false;
        nextSearch = 100 + fish.getRandom().nextInt(40);
        List<GhostfishEntity> nearby = fish.level().getEntitiesOfClass(GhostfishEntity.class,
                new AABB(fish.blockPosition()).inflate(32), f -> f != fish && f.isAlive() && f.getY() <= 100);
        if (nearby.isEmpty()) return false;
        // A fish with nearby company is not lonely; only seek schools when separated.
        if (nearby.stream().anyMatch(f -> fish.distanceToSqr(f) < 64)) return false;
        nearby.removeIf(f -> fish.level().getEntitiesOfClass(GhostfishEntity.class,
                f.getBoundingBox().inflate(9), other -> other.isAlive()).size() >= 4);
        leader = nearby.stream().min(Comparator.comparingDouble(fish::distanceToSqr)).orElse(null);
        return leader != null;
    }
    @Override public boolean canContinueToUse() {
        return leader != null && leader.isAlive() && fish.isInWater() && fish.getY() <= 100
                && leader.getY() <= 100 && fish.distanceToSqr(leader) > 25
                && fish.distanceToSqr(leader) < 1600;
    }
    @Override public void tick() {
        if (leader != null) fish.getMoveControl().setWantedPosition(
                leader.getX(), leader.getY(), leader.getZ(), 1.0);
    }
    @Override public void stop() { leader = null; nextSearch = Math.max(nextSearch, 80); }
}
