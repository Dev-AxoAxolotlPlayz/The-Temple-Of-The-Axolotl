package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A vehicle, not a mob. Hull yaw is the sole heading authority. */
public class Shorkolotl100Entity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation FORWARD = RawAnimation.begin().thenLoop("Forward_Propulsion");
    private static final RawAnimation LEFT = RawAnimation.begin().thenPlayAndHold("Turning_Left");
    private static final RawAnimation RIGHT = RawAnimation.begin().thenPlayAndHold("Turning_Right");
    private static final RawAnimation LEFT_RECOVER = RawAnimation.begin().thenPlay("Left_to_stable");
    private static final RawAnimation RIGHT_RECOVER = RawAnimation.begin().thenPlay("Right_to_stable");

    private int previousTurn = 0;
    private int recoveringTurn = 0;
    private int recoveryTicks = 0;
    private static final double PILOT_VERTICAL_OFFSET = 2.65D;

    public Shorkolotl100Entity(EntityType<? extends Shorkolotl100Entity> type, Level level) {
        super(type, level);
    }


    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_DROWNING) || super.isInvulnerableTo(source);
    }

    // Plain Entity instances are not targetable for right-click unless pickable.
    @Override public boolean isPickable() {
        return !isRemoved();
    }

    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide && getPassengers().isEmpty()) player.startRiding(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (hasPassenger(passenger))
            moveFunction.accept(passenger, getX(), getY() + PILOT_VERTICAL_OFFSET, getZ());
    }

    @Override public void tick() {
        super.tick();
        Player pilot = getControllingPassenger() instanceof Player p ? p : null;
        if (pilot != null) {
            // A/D rotates the hull at any speed. No body/head yaw or idle recentering.
            float steer = pilot.xxa;
            if (Math.abs(steer) > 0.05F) setYRot(getYRot() - steer * 2.5F);
            double heading = Math.toRadians(getYRot());
            boolean inWater = isInWater();
            double throttle = Math.max(0.0F, pilot.zza);
            double power = inWater ? 0.12D : 0.03D;
            // Mouse pitch steers the thrust vector vertically: look up to climb,
            // look down to dive. W supplies thrust; S and Space do not dive/climb.
            double pitch = Math.toRadians(pilot.getXRot());
            Vec3 forward = new Vec3(
                    -Math.sin(heading) * Math.cos(pitch),
                    inWater ? -Math.sin(pitch) : 0.0D,
                    Math.cos(heading) * Math.cos(pitch)).scale(throttle * power);
            double vertical = 0.0D;
            Vec3 velocity = getDeltaMovement().multiply(inWater ? 0.84D : 0.70D,
                    inWater ? 0.65D : 0.70D, inWater ? 0.84D : 0.70D)
                    .add(forward).add(0, vertical, 0);
            if (!inWater) velocity = velocity.add(0, -0.08D, 0);
            setDeltaMovement(velocity);
            move(MoverType.SELF, velocity);
            pilot.setAirSupply(pilot.getMaxAirSupply());
        } else {
            if (isInWater()) setDeltaMovement(getDeltaMovement().scale(0.88D));
            else setDeltaMovement(getDeltaMovement().add(0, -0.08D, 0));
            move(MoverType.SELF, getDeltaMovement());
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "propulsion", 5, state -> {
            if (getControllingPassenger() instanceof Player pilot && pilot.zza > 0.05F)
                return state.setAndContinue(FORWARD);
            return PlayState.STOP;
        }));
        // Restore visual banking without changing the vehicle's actual yaw.
        controllers.add(new AnimationController<>(this, "steering", 3, state -> {
            int turn = 0;
            if (getControllingPassenger() instanceof Player pilot) {
                if (pilot.xxa > 0.1F) turn = 1;
                else if (pilot.xxa < -0.1F) turn = -1;
            }
            if (turn != 0) {
                previousTurn = turn;
                recoveringTurn = 0;
                recoveryTicks = 0;
                return state.setAndContinue(turn > 0 ? LEFT : RIGHT);
            }
            if (previousTurn != 0) {
                recoveringTurn = previousTurn;
                previousTurn = 0;
                recoveryTicks = 10;
                state.getController().forceAnimationReset();
            }
            if (recoveryTicks-- > 0)
                return state.setAndContinue(recoveringTurn > 0 ? LEFT_RECOVER : RIGHT_RECOVER);
            return PlayState.STOP;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
}
