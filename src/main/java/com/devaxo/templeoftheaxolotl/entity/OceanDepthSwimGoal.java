package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** Full XYZ swimming with dimension-aware thermocline waypoint limits. */
public class OceanDepthSwimGoal extends Goal {
    private static final int MEGOLOTL_MAX_ABYSS_Y = 120;
    private static final int JELLY_MIN_ABYSS_Y = 111;
    private final PathfinderMob mob;
    private final double speed;
    private final boolean jelly;
    private Vec3 target;
    private int cooldown;

    public OceanDepthSwimGoal(PathfinderMob mob, double speed, boolean jelly) {
        this.mob = mob;
        this.speed = speed;
        this.jelly = jelly;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean abyss() {
        return mob.level().dimension().location().equals(
                ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "ethereal_abyss"));
    }

    @Override public boolean canUse() { return mob.isInWater(); }
    @Override public boolean canContinueToUse() { return mob.isInWater(); }

    @Override public void tick() {
        if (mob.level().isClientSide) return;
        boolean inAbyss = abyss();
        boolean outsideRange = inAbyss && (jelly
                ? mob.getY() < JELLY_MIN_ABYSS_Y
                : mob.getY() > MEGOLOTL_MAX_ABYSS_Y);
        // An old destination must never carry the creature outside its habitat limit.
        if (target != null && inAbyss && (jelly
                ? target.y < JELLY_MIN_ABYSS_Y + 0.5
                : target.y > MEGOLOTL_MAX_ABYSS_Y + 0.5)) target = null;
        if (target == null || mob.position().distanceToSqr(target) < 5.0 || --cooldown <= 0) {
            target = choose(inAbyss, outsideRange);
            cooldown = outsideRange ? 30 : 60 + mob.getRandom().nextInt(80);
        }
        if (target != null) mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
    }

    private Vec3 choose(boolean inAbyss, boolean recovery) {
        int minY = mob.level().getMinBuildHeight() + 2;
        int maxY = mob.level().getMaxBuildHeight() - 3;
        if (inAbyss) {
            if (jelly) minY = Math.max(minY, JELLY_MIN_ABYSS_Y);
            else maxY = Math.min(maxY, MEGOLOTL_MAX_ABYSS_Y);
        }
        if (minY > maxY) return null;
        for (int i = 0; i < 40; i++) {
            int x = mob.blockPosition().getX() + mob.getRandom().nextInt(33) - 16;
            int z = mob.blockPosition().getZ() + mob.getRandom().nextInt(33) - 16;
            int y = mob.blockPosition().getY() + mob.getRandom().nextInt(23) - 11;
            if (inAbyss) {
                if (jelly) {
                    if (recovery) y = 115 + mob.getRandom().nextInt(8);
                } else if (recovery) {
                    y = 85 + mob.getRandom().nextInt(15);
                } else if (mob.getRandom().nextInt(5) != 0) {
                    y = Math.min(y, 100); // Most Megolotl patrols remain below the thermocline.
                }
            }
            y = Math.max(minY, Math.min(maxY, y));
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
