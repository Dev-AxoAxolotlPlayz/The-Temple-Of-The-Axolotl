package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.nbt.CompoundTag;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A vehicle, not a mob. Hull yaw is the sole heading authority. */
public class Shorkolotl100Entity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> HEADLIGHTS =
            SynchedEntityData.defineId(Shorkolotl100Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> HULL_INTEGRITY =
            SynchedEntityData.defineId(Shorkolotl100Entity.class, EntityDataSerializers.INT);
    private final Map<BlockPos, BlockState> temporaryLightStates = new HashMap<>();
    public boolean headlightsOn() { return entityData.get(HEADLIGHTS); }
    public void toggleHeadlights() { if (!level().isClientSide) {
        entityData.set(HEADLIGHTS, !headlightsOn());
        if (!headlightsOn()) clearTemporaryLights();
    } }
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation FORWARD = RawAnimation.begin().thenLoop("Forward_Propulsion");
    private static final RawAnimation LEFT = RawAnimation.begin().thenPlayAndHold("Turning_Left");
    private static final RawAnimation RIGHT = RawAnimation.begin().thenPlayAndHold("Turning_Right");
    private static final RawAnimation LEFT_RECOVER = RawAnimation.begin().thenPlay("Left_to_stable");
    private static final RawAnimation RIGHT_RECOVER = RawAnimation.begin().thenPlay("Right_to_stable");

    private int previousTurn = 0;
    private int recoveringTurn = 0;
    private int recoveryTicks = 0;
    // Pilot seat inside the hull, rather than above the roof.
    private static final double PILOT_VERTICAL_OFFSET = 1.15D;
    private boolean recollected;
    public int hullIntegrity(){return entityData.get(HULL_INTEGRITY);}
    public void leviathanHullHit(){
        if(level().isClientSide||isRemoved()||recollected)return;
        int remaining = Math.max(0, hullIntegrity() - 50);
        entityData.set(HULL_INTEGRITY, remaining);
        if(remaining==0){
            clearTemporaryLights();
            ejectPassengers();
            recollected=true;
            discard(); // Leviathan consumes the submarine; no item drop.
        }
    }

    public Shorkolotl100Entity(EntityType<? extends Shorkolotl100Entity> type, Level level) {
        super(type, level);
    }


    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEADLIGHTS, false);
        builder.define(HULL_INTEGRITY, 100);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(HEADLIGHTS, tag.getBoolean("ShorkolotlHeadlights"));
        entityData.set(HULL_INTEGRITY, tag.contains("HullIntegrity") ? tag.getInt("HullIntegrity") : 100);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("ShorkolotlHeadlights", headlightsOn());
        tag.putInt("HullIntegrity",hullIntegrity());
        // Never serialize our temporary vanilla light blocks into a world save.
        clearTemporaryLights();
    }

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

    /**
     * Exit on either side, beyond the hull. Use the submarine's current yaw
     * rather than the passenger's camera direction.
     *
     * The candidate must fit the passenger's full collision box. Never move
     * the submarine itself to make room for a dismount.
     */
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yaw = Math.toRadians(getYRot());
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        double clearance = Math.max(getBbWidth() * 0.5, 1.0)
                + 2.0 + passenger.getBbWidth() * 0.5;
        Vec3 base = position().add(0, PILOT_VERTICAL_OFFSET, 0);
        for (int side : new int[]{-1, 1}) {
            Vec3 candidate = base.add(right.scale(side * clearance));
            // A small vertical search handles uneven seafloors without
            // selecting a location above/below the submarine arbitrarily.
            for (double yOffset : new double[]{0, 0.5, 1, -0.5, -1}) {
                Vec3 target = candidate.add(0, yOffset, 0);
                AABB bounds = passenger.getBoundingBox().move(
                        target.x - passenger.getX(),
                        target.y - passenger.getY(),
                        target.z - passenger.getZ());
                if (level().noCollision(passenger, bounds)) return target;
            }
        }
        // Vanilla fallback: never claim the side exit is guaranteed when
        // both sides are obstructed. A follow-up can block unsafe exits.
        return super.getDismountLocationForPassenger(passenger);
    }

    /** An unoccupied submarine can be recovered with one player punch. */
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isRemoved() || recollected) return false;
        if (!(source.getEntity() instanceof Player player)) return false;
        if (!getPassengers().isEmpty()) return false;
        if (!level().isClientSide) {
            recollected = true;
            clearTemporaryLights();
            if (!player.getAbilities().instabuild) {
                ItemEntity drop = new ItemEntity(level(), getX(), getY() + 0.4, getZ(),
                        new ItemStack(ModItems.SHORKOLOTL_100_ITEM.get()));
                level().addFreshEntity(drop);
            }
            discard();
        }
        return true;
    }

    private boolean inThermocline() {
        // Sample the hull and the layer immediately above/below its center.
        // This prevents a brief loss of propulsion at the custom-fluid seam.
        for (int offset = -2; offset <= 2; offset++) {
            var pos = blockPosition().offset(0, offset, 0);
            if (level().getFluidState(pos).is(com.devaxo.templeoftheaxolotl.pre07.Pre07Fluids.THERMOCLINE.get())
                    || level().getFluidState(pos).is(com.devaxo.templeoftheaxolotl.pre07.Pre07Fluids.THERMOCLINE_FLOWING.get()))
                return true;
        }
        return false;
    }

    private boolean pilotAscending;
    private boolean pilotDescending;
    public void setPilotControls(boolean ascending, boolean descending) {
        this.pilotAscending = ascending;
        this.pilotDescending = descending && !ascending;
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 4 == 0) updateHeadlights();
        Player pilot = getControllingPassenger() instanceof Player p ? p : null;
        if (pilot != null) {
            // A/D rotates the hull at any speed. No body/head yaw or idle recentering.
            float steer = pilot.xxa;
            if (Math.abs(steer) > 0.05F) setYRot(getYRot() - steer * 2.5F);
            double heading = Math.toRadians(getYRot());
            // The thermocline is a custom fluid, not vanilla water. Treat it as
            // navigable water for this authorized sub, including ascent.
            boolean inWater = isInWater() || inThermocline();
            double throttle = Math.max(0.0F, pilot.zza);
            double power = inWater ? 0.055D : 0.03D;
            // Forward motion remains horizontal when looking up; looking down
            // dives with W. Space ascends independently and overrides descent.
            // Depth is controlled by Space and V, independently of camera pitch.
            Vec3 forward = new Vec3(
                    -Math.sin(heading),
                    0.0D,
                    Math.cos(heading)).scale(throttle * power);
            double vertical = inWater
                    ? (pilotAscending ? 0.055D : pilotDescending ? -0.055D : 0.0D)
                    : 0.0D;
            Vec3 velocity = getDeltaMovement().multiply(inWater ? 0.94D : 0.70D,
                    inWater ? 0.94D : 0.70D, inWater ? 0.94D : 0.70D)
                    .add(forward).add(0, vertical, 0);
            if (!inWater) velocity = velocity.add(0, -0.08D, 0);
            // Exploration-friendly sustained cap; Axoleviathan burst is 5/4 of this.
            // Cap combined horizontal + vertical motion to avoid diagonal speed boosts.
            if (inWater && velocity.lengthSqr() > 0.65D * 0.65D)
                velocity = velocity.normalize().scale(0.65D);
            setDeltaMovement(velocity);
            move(MoverType.SELF, velocity);
            pilot.setAirSupply(pilot.getMaxAirSupply());
            // Only warn for an actual Axoleviathan target lock, not mere proximity.
            // Action bar updates every 10 ticks while piloting, in any camera mode.
            if (!level().isClientSide && tickCount % 10 == 0
                    && pilot instanceof net.minecraft.server.level.ServerPlayer serverPilot) {
                double nearest = Double.POSITIVE_INFINITY;
                for (AxoleviathanEntity leviathan : level().getEntitiesOfClass(
                        AxoleviathanEntity.class, getBoundingBox().inflate(128))) {
                    if (!leviathan.isHunting(this)) continue;
                    double distance = Math.max(0.0D,
                            Math.sqrt(leviathan.distanceToSqr(this))
                            - Math.max(getBbWidth(), getBbHeight()) * 0.5D
                            - leviathan.getBbWidth() * 0.5D);
                    nearest = Math.min(nearest, distance);
                }
                if (Double.isFinite(nearest)) {
                    serverPilot.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Unidentified Entity Approaching " + (int)Math.ceil(nearest) + "m"), true);
                }
            }
        } else {
            pilotAscending = false;
            pilotDescending = false;
            if (isInWater() || inThermocline()) setDeltaMovement(getDeltaMovement().scale(0.88D));
            else setDeltaMovement(getDeltaMovement().add(0, -0.08D, 0));
            move(MoverType.SELF, getDeltaMovement());
        }
        if (!level().isClientSide && tickCount % 2 == 0) emitThrusterBubbles();
    }

    /** Bubble exhaust at the rear air-jet thrusters, not at the headlamps. */
    private void emitThrusterBubbles() {
        if (!(level() instanceof ServerLevel server) || !isInWater()) return;
        double speed = getDeltaMovement().length();
        if (speed < 0.08D) return;
        double yaw = Math.toRadians(getYRot());
        Vec3 velocity = getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double pitch = Math.atan2(velocity.y, Math.max(0.05D, horizontal));
        pitch = Math.max(-Math.PI / 4, Math.min(Math.PI / 4, pitch));
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch),
                Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        int count = Math.min(7, Math.max(1, (int) Math.ceil(speed * 2.5D)));
        for (int side : new int[]{-1, 1}) {
            Vec3 outlet = position().add(0, 0.95D, 0)
                    .subtract(forward.scale(2.0D)).add(right.scale(side * 0.85D));
            server.sendParticles(ParticleTypes.BUBBLE, outlet.x, outlet.y, outlet.z,
                    count, 0.12D, 0.12D, 0.12D, 0.035D + speed * 0.02D);
        }
    }

    /** Two headlamps, projected forward from the existing model's headlight bones.
     *  Invisible light blocks are temporary, placed only into air/source water,
     *  and restored when the vehicle moves, switches off, or is removed.
     */
    private void updateHeadlights() {
        if (!(level() instanceof ServerLevel server)) return;
        if (!headlightsOn() || !isInWater()) { clearTemporaryLights(); return; }
        // The lamps are mounted at the front, aimed 25 degrees below the hull axis.
        // Their reach is measured from the lamp, never from the entity center.
        double yaw = Math.toRadians(getYRot());
        Vec3 horizontalForward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        double angle = Math.toRadians(25);
        Vec3 beam = horizontalForward.scale(Math.cos(angle)).add(0, -Math.sin(angle), 0);
        Set<BlockPos> next = new HashSet<>();
        for (int side : new int[]{-1, 1}) {
            Vec3 origin = position().add(0, 1.45, 0)
                    .add(horizontalForward.scale(2.0)).add(right.scale(side * 0.98));
            for (int step = 1; step <= 10; step++) {
                double distance = step * 0.5; // Exactly five blocks maximum.
                Vec3 point = origin.add(beam.scale(distance));
                BlockPos pos = BlockPos.containing(point);
                if (!server.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) break;
                BlockState state = server.getBlockState(pos);
                if (!state.isAir() && !state.is(Blocks.WATER) && !state.is(Blocks.LIGHT)) break;
                if (step == 4 || step == 8) {
                    next.add(pos);
                    if (!temporaryLightStates.containsKey(pos) && !state.is(Blocks.LIGHT)) {
                        temporaryLightStates.put(pos, state);
                        BlockState light = Blocks.LIGHT.defaultBlockState()
                                .setValue(LightBlock.LEVEL, step == 4 ? 15 : 13)
                                .setValue(LightBlock.WATERLOGGED, state.is(Blocks.WATER));
                        server.setBlock(pos, light, 3);
                    }
                }
            }
        }
        for (BlockPos old : new HashSet<>(temporaryLightStates.keySet())) {
            if (!next.contains(old)) restoreLight(old);
        }
    }

    private void restoreLight(BlockPos pos) {
        BlockState original = temporaryLightStates.remove(pos);
        if (original != null && level() instanceof ServerLevel server && server.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && server.getBlockState(pos).is(Blocks.LIGHT)) {
            server.setBlock(pos, original, 3);
        }
    }
    private void clearTemporaryLights() {
        for (BlockPos pos : new HashSet<>(temporaryLightStates.keySet())) restoreLight(pos);
    }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide) clearTemporaryLights();
        super.remove(reason);
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
