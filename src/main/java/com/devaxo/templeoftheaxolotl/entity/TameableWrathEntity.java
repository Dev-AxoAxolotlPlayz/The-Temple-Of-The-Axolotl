
        package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import java.util.UUID;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class TameableWrathEntity extends TamableAnimal implements GeoEntity {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    private UUID secondFallConfirmPlayer;
    private long secondFallConfirmUntil;

    // Flight states
    private static final int GROUNDED = 0;
    private static final int TAKING_OFF = 1;
    private static final int FLYING = 2;
    private static final int LANDING = 3;

    private static final EntityDataAccessor<Integer> FLIGHT_STATE =
            SynchedEntityData.defineId(
                    TameableWrathEntity.class,
                    EntityDataSerializers.INT
            );

    private static final int TAKEOFF_TICKS = 40;
    private static final int LANDING_TICKS = 80;

    // Passive animations
    private static final RawAnimation CROWN_ORBIT =
            RawAnimation.begin().thenLoop(
                    "animation.axolotl_absolute.passive_crown_orbit"
            );

    private static final RawAnimation PASSIVE_GILLS =
            RawAnimation.begin().thenLoop(
                    "animation.axolotl_absolute.passive_gills"
            );

    private static final RawAnimation RESTING_WINGS =
            RawAnimation.begin().thenPlayAndHold(
                    "animation.axolotl_absolute.idle_resting_wings"
            );

    private static final RawAnimation WALKING =
            RawAnimation.begin().thenLoop(
                    "animation.axolotl_absolute.walking"
            );

    private static final RawAnimation STANDING_TO_SITTING =
            RawAnimation.begin().thenPlayAndHold(
                    "animation.axolotl_absolute.standing_to_sitting"
            );

    private static final RawAnimation SITTING_TO_STANDING =
            RawAnimation.begin().thenPlay(
                    "animation.axolotl_absolute.sitting_to_standing"
            );

    private static final RawAnimation IDLE_CURIOSITY =
            RawAnimation.begin().thenPlay(
                    "animation.axolotl_absolute.idle_curiosity"
            );

    // Flight animations
    private static final RawAnimation TAKEOFF =
            RawAnimation.begin().thenPlay(
                    "animation.axolotl_absolute.takeoff"
            );

    private static final RawAnimation FLYING_ANIMATION =
            RawAnimation.begin().thenLoop(
                    "animation.axolotl_absolute.flying"
            );

    private static final RawAnimation LANDING_ANIMATION =
            RawAnimation.begin().thenPlay(
                    "animation.axolotl_absolute.landing"
            );

    // Existing sitting state machine
    private boolean wasSitting = false;
    private boolean standingUp = false;
    private boolean standingAnimationStarted = false;

    // Flight control
    private int flightTicks = 0;
    private BlockPos flightDestination = null;
    private int flightCooldown = 0;
    private int airborneTicks = 0;
    private boolean escortFlight = false;
    private int lastAnimatedFlightState = GROUNDED;

    // Client-side visual movement detection, independent of navigation velocity.
    private double lastAnimationX;
    private double lastAnimationZ;
    private boolean animationPositionInitialized = false;
    private int walkingAnimationGraceTicks = 0;
    private boolean wasWalkingAnimation = false;

    private static final double ESCORT_TAKEOFF_DISTANCE_SQR = 100.0;
    private static final double ESCORT_WALK_STOP_DISTANCE_SQR = 6.25;
    private static final double ESCORT_WALK_START_DISTANCE_SQR = 12.25;
    private static final double ESCORT_LAND_DISTANCE_SQR = 16.0;

    public TameableWrathEntity(
            EntityType<? extends TamableAnimal> type,
            Level level
    ) {
        super(type, level);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(builder);
        builder.define(FLIGHT_STATE, GROUNDED);
    }

    private int getFlightState() {
        return this.entityData.get(FLIGHT_STATE);
    }

    private void setFlightState(int state) {
        this.entityData.set(FLIGHT_STATE, state);
        this.flightTicks = 0;
    }

    // Keep the inherited Above All viewpoint geometry.
    // Set Axo's viewpoint near his blue eyes, below the floating crown.
    // Preserve the existing physical dimensions and collision box.
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(0.8F);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false; // Soul Campfire is a recruitment offering, not breeding food.
    }


    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        // Flight takes priority over wandering.
        this.goalSelector.addGoal(1, new RoyalEscortGoal());

        this.goalSelector.addGoal(2, new ObstacleFlightGoal());

        this.goalSelector.addGoal(
                3,
                new RandomStrollGoal(this, 0.8) {

                    @Override
                    public boolean canUse() {
                        return getFlightState() == GROUNDED
                                && !TameableWrathEntity.this.isTame()
                                && !TameableWrathEntity.this.isOrderedToSit()
                                && super.canUse();
                    }

                    @Override
                    public boolean canContinueToUse() {
                        return getFlightState() == GROUNDED
                                && !TameableWrathEntity.this.isTame()
                                && !TameableWrathEntity.this.isOrderedToSit()
                                && super.canContinueToUse();
                    }
                }
        );
    }

    // Prevent player-attributed damage, including projectiles fired by players.
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return true;
        if (source.getDirectEntity() instanceof Projectile) return true;
        if (source.getEntity() instanceof Player) return true;
        return super.isInvulnerableTo(source);
    }

    // Fire Aspect can ignite a mob independently of the rejected damage.
    // Cancel the fire left by a player-attributed hit without changing
    // environmental fire or ordinary non-player damage rules.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) {
            this.clearFire();
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        ItemStack item = player.getItemInHand(hand);

        // Deliberate two-step betrayal. The first offering never consumes the Ring.
        if (this.isTame() && this.isOwnedBy(player) && item.is(ModItems.RING_OF_THE_ONE.get())) {
            if (!level().isClientSide && player instanceof ServerPlayer sp) {
                var adv = sp.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "the_one_who_fell_twice"));
                if (adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone()) {
                    sp.displayClientMessage(Component.literal("Wrath Refuses to go through that again"), true);
                    return InteractionResult.SUCCESS;
                }
            }
            if (!level().isClientSide && player instanceof ServerPlayer serverPlayer && level() instanceof ServerLevel server) {
                long now = server.getGameTime();
                if (!player.getUUID().equals(secondFallConfirmPlayer) || now > secondFallConfirmUntil) {
                    secondFallConfirmPlayer = player.getUUID();
                    secondFallConfirmUntil = now + 300L;
                    setOrderedToSit(true);
                    setInSittingPose(true);
                    getNavigation().stop();
                    triggerAnim("curiosity_controller", "tamed_curiosity");
                    serverPlayer.displayClientMessage(Component.literal("Wrath stares at the Ring of the One... wondering if you mean it..."), false);
                } else {
                    secondFallConfirmPlayer = null;
                    secondFallConfirmUntil = 0;
                    BossWrathEntity fallen = ModEntities.BOSS_WRATH.get().create(server);
                    if (fallen != null) {
                        fallen.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                        fallen.setOriginDifficulty(3, player.getUUID());
                        fallen.setSecondFallRing(item.copyWithCount(1));
                        fallen.setSecondFallBetrayer(player.getUUID());
                        fallen.addTag("second_fall");
                        if (server.addFreshEntity(fallen)) {
                            if (!player.getAbilities().instabuild) item.shrink(1);
                            var advancement = server.getServer().getAdvancements().get(
                                    ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "the_one_who_fell_twice"));
                            if (advancement != null) serverPlayer.getAdvancements().award(advancement, "completed");
                            discard();
                        }
                    }
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Taming
        if (!this.isTame() && item.is(Items.SOUL_CAMPFIRE)) {

            if (!this.level().isClientSide) {

                if (!player.getAbilities().instabuild) {
                    item.shrink(1);
                }

                this.tame(player);

                this.setOrderedToSit(true);
                this.setInSittingPose(true);
                this.getNavigation().stop();

                this.level().broadcastEntityEvent(this, (byte) 7);

                this.triggerAnim(
                        "curiosity_controller",
                        "tamed_curiosity"
                );
            }

            return InteractionResult.sidedSuccess(
                    this.level().isClientSide
            );
        }

        // Only the owner can equip a vanilla netherite sword or axe.
        if (this.isTame() && this.isOwnedBy(player)
                && (item.is(Items.NETHERITE_SWORD) || item.is(Items.NETHERITE_AXE))) {
            if (!this.level().isClientSide) {
                ItemStack previous = this.getItemBySlot(EquipmentSlot.MAINHAND).copy();
                this.setItemSlot(EquipmentSlot.MAINHAND, item.copyWithCount(1));
                if (!player.getAbilities().instabuild) item.shrink(1);
                if (!previous.isEmpty()) player.getInventory().placeItemBackInInventory(previous);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Owner sit / stand
        if (this.isTame()
                && this.isOwnedBy(player)
                && item.isEmpty()) {

            if (!this.level().isClientSide) {

                boolean shouldSit = !this.isOrderedToSit();

                this.setOrderedToSit(shouldSit);
                this.setInSittingPose(shouldSit);

                if (shouldSit) {
                    this.getNavigation().stop();
                }
            }

            return InteractionResult.sidedSuccess(
                    this.level().isClientSide
            );
        }

        return super.mobInteract(player, hand);
    }

    // Never harm the owner, even when other combat goals are added later.
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (target instanceof LivingEntity victim && victim == getOwner()) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.igniteForSeconds(3.0F);
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0));
        }
        return hit;
    }

    @Override
    public TamableAnimal getBreedOffspring(
            ServerLevel level,
            AgeableMob partner
    ) {
        return null;
    }

    // Server-side flight physics
    @Override
    public void tick() {
        // Also enforce for entities loaded from older saves.
        if (!this.isInvulnerable()) this.setInvulnerable(true);
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (flightCooldown > 0) {
            flightCooldown--;
        }

        int phase = getFlightState();

        if (phase == GROUNDED) {
            return;
        }

        // Gravity is disabled during controlled flight; never accumulate fall damage.
        this.fallDistance = 0.0F;

        LivingEntity owner = this.getOwner();
        if (escortFlight && (this.isOrderedToSit()
                || owner == null || !owner.isAlive()
                || owner.level() != this.level())) {
            if (phase == FLYING) {
                beginLanding();
                phase = LANDING;
            }
        }

        flightTicks++;
        airborneTicks++;

        // Takeoff: accelerate upward gradually.
        if (phase == TAKING_OFF) {
            this.setNoGravity(true);
            this.fallDistance = 0.0F;

            double progress = Math.min(
                    1.0,
                    (double) flightTicks / TAKEOFF_TICKS
            );

            double upwardSpeed = 0.03 + progress * 0.10;

            this.setDeltaMovement(
                    0,
                    upwardSpeed,
                    0
            );

            this.hasImpulse = true;

            if (flightTicks >= TAKEOFF_TICKS) {
                setFlightState(FLYING);
            }

            return;
        }

        // Hover toward the target block.
        if (phase == FLYING) {

            this.setNoGravity(true);
            this.fallDistance = 0.0F;

            if (!escortFlight && (flightDestination == null || airborneTicks > 180)) {
                beginLanding();
                return;
            }

            Vec3 target;
            if (escortFlight && owner != null && owner.isAlive()
                    && !this.isOrderedToSit()) {
                // Follow the moving owner rather than a stale destination.
                target = owner.position().add(0, 2.5, 0);

                if (this.distanceToSqr(owner.getX(), this.getY(), owner.getZ()) <= ESCORT_LAND_DISTANCE_SQR
                        && Math.abs(this.getY() - owner.getY()) < 3.5
                        && hasSafeLandingGround()) {
                    beginLanding();
                    return;
                }
            } else {
                if (flightDestination == null) {
                    beginLanding();
                    return;
                }
                target = new Vec3(
                        flightDestination.getX() + 0.5,
                        flightDestination.getY() + 0.15,
                        flightDestination.getZ() + 0.5
                );
            }

            Vec3 difference = target.subtract(this.position());
            turnToward(target);

            double horizontalDistance = Math.sqrt(
                    difference.x * difference.x
                            + difference.z * difference.z
            );

            double speed = escortFlight ? 0.24 : 0.14;

            double velocityX = 0;
            double velocityZ = 0;

            if (horizontalDistance > 0.1) {
                velocityX = difference.x / horizontalDistance * speed;
                velocityZ = difference.z / horizontalDistance * speed;
            }

            double velocityY = Mth.clamp(
                    difference.y * 0.12,
                    escortFlight ? -0.15 : -0.08,
                    escortFlight ? 0.15 : 0.08
            );

            this.setDeltaMovement(
                    velocityX,
                    velocityY,
                    velocityZ
            );

            this.hasImpulse = true;

            if (!escortFlight && horizontalDistance < 0.65
                    && Math.abs(difference.y) < 0.8) {
                beginLanding();
            }

            return;
        }

        // Landing: slowly descend.
        if (phase == LANDING) {

            this.setNoGravity(true);

            this.setDeltaMovement(
                    0,
                    -0.055,
                    0
            );

            this.hasImpulse = true;

            // Never end the landing phase in midair: that causes fall damage.
            if (this.onGround() && flightTicks >= 18) {
                finishLanding();
            } else if (flightTicks >= LANDING_TICKS && !this.onGround()) {
                // No ground found: regain altitude and try another approach.
                setFlightState(FLYING);
            }
        }
    }

    private void turnToward(Vec3 target) {
        double dx = target.x - this.getX();
        double dz = target.z - this.getZ();
        if (dx * dx + dz * dz < 0.01) return;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    private void beginFlight(BlockPos destination) {
        if (getFlightState() != GROUNDED) {
            return;
        }

        this.flightDestination = destination;
        this.escortFlight = false;
        this.airborneTicks = 0;
        this.getNavigation().stop();

        setFlightState(TAKING_OFF);
    }

    private void beginEscortFlight() {
        if (getFlightState() != GROUNDED) return;
        LivingEntity owner = this.getOwner();
        if (owner == null) return;
        this.flightDestination = owner.blockPosition();
        this.escortFlight = true;
        this.airborneTicks = 0;
        this.getNavigation().stop();
        setFlightState(TAKING_OFF);
    }

    private boolean hasSafeLandingGround() {
        // Check for solid ground beneath Axo, without demanding that
        // the owner's current block is clear of the entity's large hitbox.
        BlockPos feet = this.blockPosition();
        for (int down = 0; down <= 3; down++) {
            BlockPos floor = feet.below(down + 1);
            if (this.level().getBlockState(floor).isFaceSturdy(this.level(), floor, Direction.UP)) {
                return true;
            }
        }
        return false;
    }

    private void beginLanding() {
        if (getFlightState() == LANDING) {
            return;
        }

        setFlightState(LANDING);
    }

    private void finishLanding() {
        this.fallDistance = 0.0F;
        this.setNoGravity(false);
        this.setDeltaMovement(Vec3.ZERO);

        this.flightDestination = null;
        this.escortFlight = false;
        this.flightCooldown = 40;

        setFlightState(GROUNDED);
    }

    // Walk close to the owner; fly when the owner gets too far away.
    private class RoyalEscortGoal extends Goal {
        private Vec3 lastPathTarget;
        private int pathRefreshTicks;

        private RoyalEscortGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean canEscort() {
            LivingEntity owner = TameableWrathEntity.this.getOwner();
            return TameableWrathEntity.this.isTame()
                    && !TameableWrathEntity.this.isOrderedToSit()
                    && owner != null && owner.isAlive()
                    && owner.level() == TameableWrathEntity.this.level()
                    && getFlightState() == GROUNDED;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = TameableWrathEntity.this.getOwner();
            return canEscort() && owner != null
                    && TameableWrathEntity.this.distanceToSqr(owner)
                    > ESCORT_WALK_START_DISTANCE_SQR;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = TameableWrathEntity.this.getOwner();
            return canEscort() && owner != null
                    && TameableWrathEntity.this.distanceToSqr(owner)
                    > ESCORT_WALK_STOP_DISTANCE_SQR;
        }

        @Override
        public void start() {
            lastPathTarget = null;
            pathRefreshTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity owner = TameableWrathEntity.this.getOwner();
            if (owner == null) return;

            double distanceSqr = TameableWrathEntity.this.distanceToSqr(owner);
            if (distanceSqr >= ESCORT_TAKEOFF_DISTANCE_SQR && flightCooldown == 0) {
                beginEscortFlight();
                return;
            }

            // Allow the current path to run; frequent replanning causes circles.
            if (pathRefreshTicks > 0) pathRefreshTicks--;
            Vec3 ownerPos = owner.position();
            boolean ownerMoved = lastPathTarget == null
                    || ownerPos.distanceToSqr(lastPathTarget) >= 2.25;
            if (pathRefreshTicks == 0 &&
                    (ownerMoved || TameableWrathEntity.this.getNavigation().isDone())) {
                TameableWrathEntity.this.getNavigation().moveTo(owner, 1.0);
                lastPathTarget = ownerPos;
                pathRefreshTicks = 20;
            }
        }

        @Override
        public void stop() {
            TameableWrathEntity.this.getNavigation().stop();
            lastPathTarget = null;
        }
    }

    // Find a higher block ahead of Axo.
    private class ObstacleFlightGoal extends Goal {

        private ObstacleFlightGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {

            if (TameableWrathEntity.this.isOrderedToSit()) {
                return false;
            }

            // Royal Escort handles the owner's long-distance travel.
            LivingEntity owner = TameableWrathEntity.this.getOwner();
            if (owner != null && !TameableWrathEntity.this.isOrderedToSit()
                    && TameableWrathEntity.this.distanceToSqr(owner)
                    >= ESCORT_TAKEOFF_DISTANCE_SQR) {
                return false;
            }

            if (getFlightState() != GROUNDED) {
                return false;
            }

            if (flightCooldown > 0) {
                return false;
            }

            if (!TameableWrathEntity.this.onGround()) {
                return false;
            }

            // Look where Axo is facing, not at his actual velocity.
            // A blocked mob can have almost zero horizontal velocity.
            double yaw = Math.toRadians(TameableWrathEntity.this.getYRot());
            Vec3 direction = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));

            // Only launch when a solid obstacle is directly in front.
            BlockPos wall = BlockPos.containing(
                    TameableWrathEntity.this.getX() + direction.x * 1.4,
                    TameableWrathEntity.this.getY() + 0.5,
                    TameableWrathEntity.this.getZ() + direction.z * 1.4
            );
            if (TameableWrathEntity.this.level().getBlockState(wall)
                    .getCollisionShape(TameableWrathEntity.this.level(), wall).isEmpty()) {
                return false;
            }

            // Find a 2- or 3-block-high platform in the forward direction.
            // Check the entire entity's collision box at the landing point.
            for (int height = 2; height <= 3; height++) {
                BlockPos target = BlockPos.containing(
                        TameableWrathEntity.this.getX() + direction.x * 2.5,
                        TameableWrathEntity.this.getY() + height,
                        TameableWrathEntity.this.getZ() + direction.z * 2.5
                );
                BlockPos floor = target.below();
                if (!TameableWrathEntity.this.level().getBlockState(floor).isFaceSturdy(TameableWrathEntity.this.level(), floor, Direction.UP)) {
                    continue;
                }

                Vec3 landingCenter = new Vec3(
                        target.getX() + 0.5,
                        target.getY(),
                        target.getZ() + 0.5
                );
                Vec3 offset = landingCenter.subtract(
                        TameableWrathEntity.this.position()
                );
                if (!TameableWrathEntity.this.level().noCollision(
                        TameableWrathEntity.this,
                        TameableWrathEntity.this.getBoundingBox().move(offset)
                )) {
                    continue;
                }

                flightDestination = target;
                return true;
            }

            return false;
        }

        @Override
        public void start() {
            if (flightDestination != null) {
                beginFlight(flightDestination);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return getFlightState() != GROUNDED;
        }
    }

    // Body animation state machine
    private PlayState bodyAnimation(
            AnimationState<TameableWrathEntity> state
    ) {
        AnimationController<TameableWrathEntity> controller =
                state.getController();

        int flightState = getFlightState();

        if (flightState != lastAnimatedFlightState) {
            lastAnimatedFlightState = flightState;
            walkingAnimationGraceTicks = 0;
            wasWalkingAnimation = false;
            animationPositionInitialized = false;
            controller.forceAnimationReset();
        }

        // Flight animations take priority.
        if (flightState != GROUNDED) {

            if (flightState == TAKING_OFF) {
                controller.setAnimationSpeed(2.0);
                return state.setAndContinue(TAKEOFF);
            }

            if (flightState == FLYING) {
                controller.setAnimationSpeed(1.0);
                return state.setAndContinue(FLYING_ANIMATION);
            }

            if (flightState == LANDING) {
                controller.setAnimationSpeed(1.0);
                return state.setAndContinue(LANDING_ANIMATION);
            }
        }

        // Existing sitting / standing behavior
        boolean sitting = this.isInSittingPose();

        if (sitting != wasSitting) {

            wasSitting = sitting;
            controller.forceAnimationReset();

            if (sitting) {
                standingUp = false;
                standingAnimationStarted = false;
            } else {
                standingUp = true;
                standingAnimationStarted = false;
            }
        }

        if (sitting) {
            controller.setAnimationSpeed(3.0);
            return state.setAndContinue(STANDING_TO_SITTING);
        }

        if (standingUp) {

            controller.setAnimationSpeed(1.5);

            if (!standingAnimationStarted) {
                standingAnimationStarted = true;
                return state.setAndContinue(SITTING_TO_STANDING);
            }

            if (controller.getAnimationState()
                    != AnimationController.State.STOPPED) {
                return PlayState.CONTINUE;
            }

            standingUp = false;
            standingAnimationStarted = false;
            controller.forceAnimationReset();
        }

        controller.setAnimationSpeed(1.0);

        // Measure real position changes rather than relying solely on the
        // client's motion vector (which may be zero for pathfinding mobs).
        double currentX = this.getX();
        double currentZ = this.getZ();
        boolean changedPosition = false;
        if (animationPositionInitialized) {
            double dx = currentX - lastAnimationX;
            double dz = currentZ - lastAnimationZ;
            changedPosition = dx * dx + dz * dz > 0.000001D;
        }
        lastAnimationX = currentX;
        lastAnimationZ = currentZ;
        animationPositionInitialized = true;

        Vec3 motion = this.getDeltaMovement();
        boolean moving = changedPosition || state.isMoving()
                || motion.x * motion.x + motion.z * motion.z > 0.0001D;

        // Briefly keep walking between network position updates.
        if (moving) {
            walkingAnimationGraceTicks = 6;
        } else if (walkingAnimationGraceTicks > 0) {
            walkingAnimationGraceTicks--;
        }

        boolean shouldWalk = walkingAnimationGraceTicks > 0;
        if (shouldWalk != wasWalkingAnimation) {
            wasWalkingAnimation = shouldWalk;
            controller.forceAnimationReset();
        }

        if (shouldWalk) {
            controller.setAnimationSpeed(2.0);
            return state.setAndContinue(WALKING);
        }

        return state.setAndContinue(RESTING_WINGS);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {

        controllers.add(
                new AnimationController<>(
                        this,
                        "crown_controller",
                        0,
                        state -> state.setAndContinue(CROWN_ORBIT)
                )
        );

        controllers.add(
                new AnimationController<>(
                        this,
                        "gills_controller",
                        0,
                        state -> state.setAndContinue(PASSIVE_GILLS)
                )
        );

        controllers.add(
                new AnimationController<>(
                        this,
                        "body_controller",
                        2,
                        this::bodyAnimation
                )
        );

        controllers.add(
                new AnimationController<>(
                        this,
                        "curiosity_controller",
                        0,
                        state -> {
                            state.getController().setAnimationSpeed(1.5);
                            return PlayState.STOP;
                        }
                ).triggerableAnim(
                        "tamed_curiosity",
                        IDLE_CURIOSITY
                )
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
