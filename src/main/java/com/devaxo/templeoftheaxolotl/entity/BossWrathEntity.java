package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Visual-registration stage only: no boss attacks or encounter progression yet. */
public class BossWrathEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle_crown_and_breathing");
    private static final RawAnimation WINGS = RawAnimation.begin().thenLoop("idle_wings");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walking");

    public BossWrathEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "wrath_idle", 5, state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(this, "wrath_wings", 5, state -> state.setAndContinue(WINGS)));
        controllers.add(new AnimationController<>(this, "wrath_walk", 5, state -> {
            if (state.isMoving()) return state.setAndContinue(WALK);
            return software.bernie.geckolib.animation.PlayState.STOP;
        }));
    }

    // Boss Wrath is never a leashable creature or a boat passenger.
    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {
        if (vehicle instanceof net.minecraft.world.entity.vehicle.Boat) {
            return false;
        }
        return super.canRide(vehicle);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
