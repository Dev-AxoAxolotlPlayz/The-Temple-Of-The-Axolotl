package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** Three-dimensional patrol with an enforced deep-water waypoint ceiling. */
public class AbyssalFreeSwimGoal extends Goal {
    private static final int MAX_PATROL_Y = 100;
    private final PathfinderMob mob;
    private final double speed;
    private int cooldown;
    private Vec3 target;

    public AbyssalFreeSwimGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() { return mob.isInWater(); }
    @Override public boolean canContinueToUse() { return mob.isInWater(); }

    @Override public void tick() {
        if (mob.level().isClientSide) return;
        boolean aboveCeiling = mob.getY() > MAX_PATROL_Y;
        // Never retain an invalid patrol waypoint, even after a chase or external displacement.
        if (target != null && target.y > MAX_PATROL_Y + 0.5) target = null;
        if (target == null || mob.position().distanceToSqr(target) < 5.0 || --cooldown <= 0) {
            target = choose(aboveCeiling);
            cooldown = aboveCeiling ? 30 : 60 + mob.getRandom().nextInt(80);
        }
        if (target != null) mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
    }

    private Vec3 choose(boolean recovery) {
        int minY = mob.level().getMinBuildHeight() + 2;
        int maxY = Math.min(MAX_PATROL_Y, mob.level().getMaxBuildHeight() - 3);
        if (minY > maxY) return null;
        for (int i = 0; i < 40; i++) {
            int x = mob.blockPosition().getX() + mob.getRandom().nextInt(25) - 12;
            int z = mob.blockPosition().getZ() + mob.getRandom().nextInt(25) - 12;
            int y = recovery
                    ? Math.min(maxY, 92 + mob.getRandom().nextInt(9))
                    : Math.min(maxY, mob.blockPosition().getY() + mob.getRandom().nextInt(19) - 9);
            y = Math.max(minY, y);
            BlockPos p = new BlockPos(x, y, z);
            if (!mob.level().getChunkSource().hasChunk(p.getX() >> 4, p.getZ() >> 4)) continue;
            if (mob.level().getFluidState(p).is(FluidTags.WATER)
                    && mob.level().getBlockState(p).getCollisionShape(mob.level(), p).isEmpty()) {
                return Vec3.atCenterOf(p);
            }
        }
        return null;
    }

    @Override public void stop() { target = null; cooldown = 0; }
}
