package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Abyss swimmer with model-fitted entity dimensions. */
public class AxobloopEntity extends PathfinderMob implements GeoEntity {
    // Abyss inhabitants persist across player-distance despawn checks.
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public AxobloopEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new net.minecraft.world.entity.ai.control.MoveControl(this) {
            @Override public void tick() {
                if (!AxobloopEntity.this.isInWater()) { super.tick(); return; }
                if (operation != Operation.MOVE_TO) { AxobloopEntity.this.setSpeed(0); return; }
                Vec3 delta = new Vec3(wantedX - getX(), wantedY - getY(), wantedZ - getZ());
                if (delta.lengthSqr() < 0.16) { AxobloopEntity.this.setSpeed(0); return; }
                Vec3 dir = delta.normalize();
                setDeltaMovement(getDeltaMovement().lerp(dir.scale(Math.max(0.04, speedModifier * 0.17)), 0.13));
                setYRot((float)(net.minecraft.util.Mth.atan2(dir.z, dir.x) * 180 / Math.PI) - 90);
                yBodyRot = getYRot();
                float wantedPitch = (float)(-net.minecraft.util.Mth.atan2(dir.y,
                        Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * 180 / Math.PI);
                setXRot(net.minecraft.util.Mth.rotLerp(0.12F, getXRot(),
                        net.minecraft.util.Mth.clamp(wantedPitch, -50F, 50F)));
                setSpeed(0);
            }
        };
    }
    @Override protected PathNavigation createNavigation(Level level) { return new WaterBoundPathNavigation(this, level); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(2, new AbyssalFreeSwimGoal(this, 0.8));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
    @Override public void travel(Vec3 input) {
        if (isInWater()) {
            moveRelative(0.02F, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.90));
        } else super.travel(input);
    }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override protected int increaseAirSupply(int air) { return getMaxAirSupply(); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            boolean swimming = getDeltaMovement().lengthSqr() > 0.0025;
            state.setAnimation(RawAnimation.begin().thenLoop("animation.axobloop." + (swimming ? "swim" : "idle")));
            return PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
