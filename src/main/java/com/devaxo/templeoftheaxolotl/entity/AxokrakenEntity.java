package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.abyss.AbyssTrialEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Persistent Merolotl neighborhood guardian; attacks players on sight, never residents. */
public class AxokrakenEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos home;
    // Lowest permitted guardian position: safely above the thermocline top (~Y=110).
    private static final double MIN_GUARDIAN_Y = 114.0D;
    private int patrolRadius = 38;
    private int patrolDelay;
    private int snagTicks;
    private int attackCooldown;
    public AxokrakenEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); this.moveControl = new AquaticMoveControl(this); setPersistenceRequired(); }
    public void setNeighborhoodHome(BlockPos pos) { setNeighborhoodHome(pos, 38); }
    public void setNeighborhoodHome(BlockPos pos, int radius) {
        home = pos.immutable(); patrolRadius = Math.max(12, radius); setPersistenceRequired();
    }

    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation(this, level);
    }
    @Override protected int increaseAirSupply(int air) { return getMaxAirSupply(); }
    @Override public void travel(Vec3 input) {
        if (isInWater()) {
            moveRelative(0.02F, input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.87D));
        } else super.travel(input);
    }
    private static final class AquaticMoveControl extends net.minecraft.world.entity.ai.control.MoveControl {
        private final PathfinderMob swimmer;
        AquaticMoveControl(PathfinderMob swimmer) { super(swimmer); this.swimmer = swimmer; }
        @Override public void tick() {
            if (!swimmer.isInWater()) { super.tick(); return; }
            if (operation != Operation.MOVE_TO) { swimmer.setSpeed(0); return; }
            Vec3 delta = new Vec3(wantedX - swimmer.getX(), wantedY - swimmer.getY(), wantedZ - swimmer.getZ());
            if (delta.lengthSqr() < 0.09D) { swimmer.setSpeed(0); return; }
            Vec3 direction = delta.normalize();
            if (direction.y > 0 && swimmer.level().getFluidState(swimmer.blockPosition().above()).isEmpty()) {
                direction = new Vec3(direction.x, 0, direction.z).normalize();
            }
            // Never steer into the thermocline, including while pursuing a player.
            if (swimmer.getY() <= MIN_GUARDIAN_Y + 1.0D && direction.y < 0.0D) {
                direction = new Vec3(direction.x, 0.0D, direction.z);
                if (direction.lengthSqr() > 1.0E-6D) direction = direction.normalize();
            }
            Vec3 desired = direction.scale(Math.max(0.08D, speedModifier * 0.23D));
            if (swimmer.getY() < MIN_GUARDIAN_Y + 1.0D)
                desired = new Vec3(desired.x, Math.max(0.10D, desired.y), desired.z);
            swimmer.setDeltaMovement(swimmer.getDeltaMovement().lerp(desired, 0.18D));
            swimmer.setYRot((float)(net.minecraft.util.Mth.atan2(direction.z, direction.x) * (180D / Math.PI)) - 90F);
            swimmer.yBodyRot = swimmer.getYRot();
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float pitch = (float)(-net.minecraft.util.Mth.atan2(delta.y, horizontal) * (180D / Math.PI));
            swimmer.setXRot(net.minecraft.util.Mth.rotLerp(0.22F, swimmer.getXRot(), pitch));
            swimmer.setSpeed(0);
        }
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(2, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { return home != null && getTarget() == null && !getNavigation().isInProgress(); }
            @Override public void start() { choosePatrolPoint(); }
        });
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
            entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()
                && AxokrakenEntity.this.distanceToSqr(player) <= 100.0D));
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !level().dimension().equals(AbyssTrialEvents.ABYSS)) return;
        if (attackCooldown > 0) attackCooldown--;
        if (patrolDelay > 0) patrolDelay--;
        if (snagTicks > 0) snagTicks--;
        // Hard safety floor, including after knockback, currents, or saved-world reload.
        // Cancel downward velocity and steer back upward instead of teleporting.
        if (getY() < MIN_GUARDIAN_Y) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, Math.max(0.16D, motion.y), motion.z);
            getNavigation().stop();
        }
        // A guardian stays around its village unless actively chasing a nearby player.
        if (home != null && getTarget() == null && horizontalHomeDistanceSqr() > patrolRadius * patrolRadius)
            getNavigation().moveTo(home.getX() + .5, home.getY(), home.getZ() + .5, 1.1);
        if (!(getTarget() instanceof Player target) || !target.isAlive()) return;
        double dist = distanceToSqr(target);
        if (dist > 24 * 24 && home != null && horizontalHomeDistanceSqr() > (patrolRadius + 16) * (patrolRadius + 16)) {
            setTarget(null); return;
        }
        if (dist < 18 * 18 && attackCooldown == 0 && hasLineOfSight(target)) {
            snagTicks = 28;
            attackCooldown = 90;
        }
        // Snag halfway through the animation, then draw the target toward the beak.
        if (snagTicks >= 9 && snagTicks <= 17 && dist < 18 * 18 && hasLineOfSight(target)) {
            Vec3 pull = position().add(0, getBbHeight() * .45, 0).subtract(target.position());
            if (pull.lengthSqr() > 2.25) target.setDeltaMovement(target.getDeltaMovement().scale(.5).add(pull.normalize().scale(.42)));
            target.hurtMarked = true;
        }
        if (snagTicks == 9 && dist < 4 * 4) target.hurt(damageSources().mobAttack(this), 8);
    }
    private double horizontalHomeDistanceSqr() {
        if (home == null) return 0;
        double dx = getX() - home.getX() - .5, dz = getZ() - home.getZ() - .5;
        return dx * dx + dz * dz;
    }
    private void choosePatrolPoint() {
        if (home == null || patrolDelay > 0 || getTarget() != null) return;
        patrolDelay = 35 + random.nextInt(45);
        // Home is above the thermocline. Stay in open water above its upper surface.
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = home.getX() + random.nextInt(patrolRadius * 2 + 1) - patrolRadius;
            int z = home.getZ() + random.nextInt(patrolRadius * 2 + 1) - patrolRadius;
            int y = home.getY() + random.nextInt(13) - 6;
            if (y < (int)MIN_GUARDIAN_Y) y = (int)MIN_GUARDIAN_Y;
            BlockPos destination = new BlockPos(x, y, z);
            if (level().getFluidState(destination).is(net.minecraft.tags.FluidTags.WATER)
                    && level().getFluidState(destination.above()).is(net.minecraft.tags.FluidTags.WATER)) {
                getNavigation().moveTo(x + .5, y + .5, z + .5, .9);
                return;
            }
        }
    }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        // The feeder attack owns damage timing; avoid a second unsynchronized melee hit.
        return false;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) { tag.putInt("GuardianHomeX", home.getX()); tag.putInt("GuardianHomeY", home.getY()); tag.putInt("GuardianHomeZ", home.getZ()); }
        tag.putInt("GuardianPatrolRadius", patrolRadius);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("GuardianHomeX")) home = new BlockPos(tag.getInt("GuardianHomeX"), tag.getInt("GuardianHomeY"), tag.getInt("GuardianHomeZ"));
        if (tag.contains("GuardianPatrolRadius")) patrolRadius = tag.getInt("GuardianPatrolRadius");
        setPersistenceRequired();
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            if (snagTicks > 0) state.setAnimation(RawAnimation.begin().thenPlay("animation.axokraken.feeder_snag"));
            else state.setAnimation(RawAnimation.begin().thenLoop(getDeltaMovement().lengthSqr() > .0025 ? "animation.axokraken.swim" : "animation.axokraken.idle"));
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "gills", 0, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop("animation.axokraken.gill_flutter")); return PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
