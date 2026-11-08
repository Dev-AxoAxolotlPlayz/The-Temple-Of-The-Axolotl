package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.abyss.AbyssTrialEvents;
import com.devaxo.templeoftheaxolotl.abyss.TemporaryLeviathanTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Comparator;
import java.util.List;

/** Patch 5: persistent abyssal titan, 3D swimming and knockback protection. */
public class AxoleviathanEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle_float");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("normal_swimming");
    private static final RawAnimation HUNT = RawAnimation.begin().thenLoop("hunt_swimming");
    private static final RawAnimation NOM = RawAnimation.begin().thenPlay("nom");

    private static final int AIRBORNE_GRACE_TICKS = 40;
    // Idle patrol ceiling: valid underwater waypoints at Y <= 55.
    private static final int DEEP_PATROL_MAX_Y = 55;
    private static final int TERRITORY_RADIUS = 6 * 16;
    private static final int TERRITORY_SPACING = TERRITORY_RADIUS * 2;
    private static final int FLOOR_CLEARANCE = 5;
    private static final int PATROL_SEARCH_RADIUS = 96;
    private static final double CRUISE_SPEED = 0.36;
    private static final double HUNT_SPEED = 0.65;
    private static final double BURST_SPEED = 0.8125; // 5/4 of Shorkolotl 0.65 blocks/tick
    private static final double WATER_ACCELERATION = 0.04;
    private static final double MAX_PITCH = 55.0;

    private static final ResourceKey<DamageType> CAVE_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "axoleviathan_cave"));
    private static final ResourceKey<DamageType> NOM_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "axoleviathan_nom"));

    private DamageSource randomNomDamage() {
        ResourceKey<DamageType> key = getRandom().nextBoolean() ? CAVE_DAMAGE : NOM_DAMAGE;
        return new DamageSource(level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), this);
    }

    /** Server-side target lock used by the submarine proximity warning. */
    public boolean isHunting(Entity entity) {
        return prey == entity && entity != null && entity.isAlive();
    }
    private Entity prey;
    private Vec3 wanderDestination;
    private Vec3 returnDestination;
    // Anchored territory; movement during combat never relocates hunting grounds.
    private BlockPos territoryCenter;
    private int territoryCheckCooldown;
    private int wanderTicks;
    private int attackCooldown;
    // Successful submarine bites are separated by a full attack cooldown;
    // a single collision/tick must never consume both hull stages.
    private java.util.UUID lastBittenSub;
    private long lastSubBiteTick = Long.MIN_VALUE / 2;
    private int ambushPhase; // 0 approach, 1 retreat, 2 flank, 3 re-approach
    private java.util.UUID ambushTarget;
    private Vec3 ambushPoint;
    private Vec3 firstBiteDirection;
    private int ambushTicks;
    private int blockBudgetCooldown;
    private int ticksOutOfWater;
    private float swimPitch;

    public AxoleviathanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Velocity is steered explicitly; do not use a land movement controller.
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1024)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 20)
                .add(Attributes.FOLLOW_RANGE, 96)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        // Player/submarine targeting and wandering are driven by server-side
        // steering below, not ground-oriented vanilla movement goals.
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    // Persist through normal distance-based despawn checks. Chunk unloading still works.
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // Prevent conventional combat knockback and external push impulses.
    // Deliberate AI swimming uses setDeltaMovement() and is unaffected.
    @Override
    public void knockback(double strength, double x, double z) {
        // Intentionally immune.
    }

    @Override
    public void push(double x, double y, double z) {
        // Intentionally immune to external push impulses.
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (isInWater()) {
            // The AI sets real velocity in tick(); avoid Minecraft's normal
            // movement-speed multiplication and aquatic buoyancy.
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.985));
        } else {
            super.travel(input); // air gravity activates after the grace period
        }
    }

    @Override
    public void tick() {
        boolean wet = isInWater();
        if (wet) {
            ticksOutOfWater = 0;
            setNoGravity(true);
        } else {
            ticksOutOfWater = Math.min(AIRBORNE_GRACE_TICKS, ticksOutOfWater + 1);
            setNoGravity(ticksOutOfWater < AIRBORNE_GRACE_TICKS);
            getNavigation().stop();
            if (isNoGravity()) {
                Vec3 v = getDeltaMovement();
                setDeltaMovement(v.x * 0.985, v.y * 0.975, v.z * 0.985);
            }
        }
        super.tick();
        if (level().isClientSide) return;

        if (attackCooldown > 0) attackCooldown--;
        if (tickCount % 10 == 0) acquireTarget();
        if (prey != null && (!prey.isAlive() || distanceToSqr(prey) > 128 * 128)) prey = null;
        if (prey == null || (ambushTarget != null && !ambushTarget.equals(prey.getUUID()))) {
            ambushPhase = 0;
            ambushPoint = null;
            ambushTarget = null;
        }
        if (ambushPhase != 0 && ++ambushTicks > 500) {
            ambushPhase = 0; ambushPoint = null;
        }

        if (wet) {
            Vec3 destination = null;
            double maxSpeed = CRUISE_SPEED;
            if (prey != null) {
                destination = prey.position().add(0, prey.getBbHeight() * 0.5, 0);
                if (ambushPhase == 1) {
                    // Retreat at least 30 blocks from the moving prey before flanking.
                    if (distanceToSqr(prey) >= 32 * 32) {
                        ambushPhase = 2;
                        ambushPoint = null;
                    } else {
                        Vec3 away = position().subtract(prey.position());
                        if (away.lengthSqr() < 1) away = new Vec3(1, 0, 0);
                        destination = prey.position().add(away.normalize().scale(38));
                    }
                }
                if (ambushPhase == 2) {
                    if (ambushPoint == null) {
                        Vec3 old = firstBiteDirection == null ? new Vec3(1, 0, 0) : firstBiteDirection;
                        Vec3 side = new Vec3(-old.z, 0, old.x);
                        if (side.lengthSqr() < 0.01) side = new Vec3(1, 0, 0);
                        if (random.nextBoolean()) side = side.scale(-1);
                        ambushPoint = prey.position().add(side.normalize().scale(34));
                    }
                    destination = ambushPoint;
                    if (position().distanceToSqr(ambushPoint) < 64) {
                        ambushPhase = 3;
                        ambushPoint = null;
                    }
                }
                double distance = Math.sqrt(distanceToSqr(prey));
                maxSpeed = distance > 24 ? BURST_SPEED : HUNT_SPEED;
                if (prey instanceof Shorkolotl100Entity) maxSpeed = BURST_SPEED;
                wanderDestination = null;
                returnDestination = null;
                wanderTicks = 0;
            } else {
                if (territoryCenter == null) territoryCenter = blockPosition();
                if (--territoryCheckCooldown <= 0) {
                    territoryCheckCooldown = 80 + random.nextInt(40);
                    // If another territory overlaps, migrate the anchor toward an
                    // unclaimed seabed instead of continuously re-rolling targets.
                    if (territoryOverlaps(territoryCenter)) {
                        BlockPos replacement = findPatrolPoint(true);
                        if (replacement != null) {
                            territoryCenter = replacement;
                            wanderDestination = Vec3.atCenterOf(replacement);
                            wanderTicks = 120;
                        }
                    }
                }
                // If a chase ends above the idle-patrol ceiling, return downward first.
                // Do not select another shallow idle destination on the way down.
                if (getY() > DEEP_PATROL_MAX_Y) {
                    wanderDestination = null;
                    wanderTicks = 0;
                    // Retain the selected return waypoint instead of re-rolling
                    // every tick, which causes direction jitter.
                    if (returnDestination == null ||
                            position().distanceToSqr(returnDestination) < 9 ||
                            !isValidWaterWaypoint(returnDestination)) {
                        returnDestination = findDeepReturnDestination();
                    }
                    destination = returnDestination;
                } else {
                    returnDestination = null;
                    if (wanderDestination == null || wanderTicks-- <= 0
                            || position().distanceToSqr(wanderDestination) < 9
                            || wanderDestination.y > DEEP_PATROL_MAX_Y + 0.5) {
                        chooseWanderDestination();
                    }
                    destination = wanderDestination;
                }
            }
            steerThroughWater(destination, maxSpeed);

            if (prey != null && ambushPhase != 1 && ambushPhase != 2
                    && distanceToSqr(prey) < 36 && attackCooldown == 0) {
                attackCooldown = 40;
                triggerAnim("attack", "nom");
                if (prey instanceof Shorkolotl100Entity sub) {
                    if (!sub.getUUID().equals(lastBittenSub) || tickCount - lastSubBiteTick >= 40) {
                        sub.leviathanHullHit();
                        lastBittenSub = sub.getUUID();
                        lastSubBiteTick = tickCount;
                        if (ambushPhase == 0) {
                            ambushTarget = sub.getUUID();
                            firstBiteDirection = position().subtract(sub.position()).normalize();
                            ambushPhase = 1;
                            ambushTicks = 0;
                        } else if (ambushPhase == 3) {
                            ambushPhase = 0;
                            ambushTarget = null;
                        }
                    }
                }
                else if (prey instanceof LivingEntity living)
                    living.hurt(randomNomDamage(), 20);
            }
        }
        // The pitch tracks physical motion, not the target's altitude.
        updateSwimmingPitch();
        // Terrain displacement disabled: Axoleviathans must never modify world blocks.
        // Existing temporary terrain entries are still restored by TemporaryLeviathanTerrain. 
    }

    /** A six-chunk radius per animal: anchors must be 192 blocks apart. */
    private boolean territoryOverlaps(BlockPos center) {
        if (!(level() instanceof ServerLevel server)) return false;
        for (var entity : server.getAllEntities()) {
            if (!(entity instanceof AxoleviathanEntity other) || other == this || !other.isAlive()) continue;
            BlockPos otherCenter = other.territoryCenter == null ? other.blockPosition() : other.territoryCenter;
            double dx = center.getX() - otherCenter.getX();
            double dz = center.getZ() - otherCenter.getZ();
            if (dx * dx + dz * dz < (double) TERRITORY_SPACING * TERRITORY_SPACING) return true;
        }
        // Persisted centers also protect unloaded creatures' established grounds.
        return com.devaxo.templeoftheaxolotl.abyss.AxoleviathanSpawnEvents
                .isReserved(server, getUUID(), center, TERRITORY_SPACING);
    }

    /** Prefer a clear, submerged point immediately above the ocean floor. */
    private boolean isOpenWater(BlockPos pos) {
        if (!(level() instanceof ServerLevel server)) return false;
        if (pos.getY() < server.getMinBuildHeight() + 2 || pos.getY() >= server.getMaxBuildHeight() - 3) return false;
        if (!server.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        for (int y = 0; y <= 4; y++) {
            BlockPos sample = pos.above(y);
            if (!server.getFluidState(sample).is(FluidTags.WATER)
                    || !server.getBlockState(sample).getCollisionShape(server, sample).isEmpty()) return false;
        }
        return true;
    }

    private BlockPos floorPatrolAt(int x, int z) {
        if (!(level() instanceof ServerLevel server)
                || !server.getChunkSource().hasChunk(x >> 4, z >> 4)) return null;
        // Scan from the bottom up; the first clear water column is the deepest.
        for (int y = server.getMinBuildHeight() + 2; y <= DEEP_PATROL_MAX_Y; y++) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (!isOpenWater(candidate)) continue;
            // Floor or nearby terrain should be below, not a mid-ocean waypoint.
            boolean floorNearby = false;
            for (int d = 1; d <= FLOOR_CLEARANCE; d++) {
                BlockPos below = candidate.below(d);
                if (!server.getFluidState(below).is(FluidTags.WATER)) {
                    floorNearby = true;
                    break;
                }
            }
            if (floorNearby) return candidate;
        }
        return null;
    }

    /** Do not choose a point within any other Leviathan's six-chunk grounds. */
    private BlockPos findPatrolPoint(boolean relocate) {
        if (!(level() instanceof ServerLevel server)) return null;
        BlockPos origin = relocate ? blockPosition() :
                (territoryCenter == null ? blockPosition() : territoryCenter);
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 90; i++) {
            int radius = relocate ? 192 + random.nextInt(160) : random.nextInt(PATROL_SEARCH_RADIUS + 1);
            double angle = random.nextDouble() * Math.PI * 2;
            int x = origin.getX() + (int)(Math.cos(angle) * radius);
            int z = origin.getZ() + (int)(Math.sin(angle) * radius);
            BlockPos candidate = floorPatrolAt(x, z);
            if (candidate == null || territoryOverlaps(candidate)) continue;
            // Prefer lower seabed and points nearer the established anchor.
            double score = -candidate.getY() * 4.0 - Math.sqrt(candidate.distSqr(origin)) * 0.05;
            if (score > bestScore) { best = candidate; bestScore = score; }
        }
        return best;
    }

    private void chooseWanderDestination() {
        wanderDestination = null;
        wanderTicks = 60;
        BlockPos candidate = findPatrolPoint(false);
        if (candidate == null) candidate = findLowestAvailableWater();
        if (candidate != null && !territoryOverlaps(candidate)) {
            wanderDestination = Vec3.atCenterOf(candidate);
            wanderTicks = 120 + random.nextInt(120);
        }
    }

    /** Lowest loaded open water when no suitable Y<=55 seabed waypoint exists. */
    private BlockPos findLowestAvailableWater() {
        if (!(level() instanceof ServerLevel server)) return null;
        BlockPos origin = blockPosition();
        BlockPos best = null;
        for (int i = 0; i < 30; i++) {
            int x = origin.getX() + random.nextInt(65) - 32;
            int z = origin.getZ() + random.nextInt(65) - 32;
            if (!server.getChunkSource().hasChunk(x >> 4, z >> 4)) continue;
            for (int y = server.getMinBuildHeight() + 2; y <= Math.min(origin.getY(), server.getMaxBuildHeight() - 5); y++) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (isOpenWater(candidate) && !territoryOverlaps(candidate)) {
                    if (best == null || candidate.getY() < best.getY()) best = candidate;
                    break;
                }
            }
        }
        return best;
    }

    private boolean isValidWaterWaypoint(Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        // The fallback may be above Y=55 if no deeper loaded water exists.
        return isOpenWater(pos) && !territoryOverlaps(pos);
    }

    private Vec3 findDeepReturnDestination() {
        BlockPos seabed = findPatrolPoint(false);
        if (seabed == null) seabed = findLowestAvailableWater();
        return seabed == null ? null : Vec3.atCenterOf(seabed);
    }

    private void steerThroughWater(Vec3 destination, double speedLimit) {
        Vec3 velocity = getDeltaMovement();
        if (destination == null) {
            setDeltaMovement(velocity.scale(0.96));
            return;
        }
        Vec3 displacement = destination.subtract(position());
        double distance = displacement.length();
        if (distance < 0.001) return;

        // Full 3D acceleration, including vertical velocity. Slow near target
        // instead of crossing it at 50 blocks/second.
        double desiredSpeed = Math.min(speedLimit, Math.max(0.12, distance * 0.14));
        Vec3 desired = displacement.scale(desiredSpeed / distance);
        Vec3 delta = desired.subtract(velocity);
        if (delta.lengthSqr() > WATER_ACCELERATION * WATER_ACCELERATION)
            delta = delta.normalize().scale(WATER_ACCELERATION);
        Vec3 next = velocity.add(delta);
        if (next.lengthSqr() > speedLimit * speedLimit)
            next = next.normalize().scale(speedLimit);
        setDeltaMovement(next);

        // Face the actual swimming direction. The renderer handles body pitch.
        if (next.horizontalDistanceSqr() > 0.0001) {
            float yaw = (float) (Mth.atan2(next.z, next.x) * (180.0 / Math.PI)) - 90.0F;
            setYRot(Mth.rotLerp(0.12F, getYRot(), yaw));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    private void updateSwimmingPitch() {
        Vec3 v = getDeltaMovement();
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        float target = v.lengthSqr() < 0.0004 ? 0.0F :
                (float) Mth.clamp(-Math.toDegrees(Math.atan2(v.y, horizontal)), -MAX_PITCH, MAX_PITCH);
        swimPitch += (target - swimPitch) * 0.10F;
        // Synced vanilla entity pitch; renderer reads interpolated xRot.
        setXRot(swimPitch);
    }

    private void acquireTarget() {
        if (!(level() instanceof ServerLevel server) || level().dimension() != AbyssTrialEvents.ABYSS) return;
        List<Shorkolotl100Entity> subs = server.getEntitiesOfClass(
                Shorkolotl100Entity.class, getBoundingBox().inflate(20),
                sub -> sub.isAlive() && !sub.getPassengers().isEmpty());
        if (!subs.isEmpty()) {
            subs.sort(Comparator.comparingDouble(this::distanceToSqr));
            prey = subs.get(0);
            return;
        }
        if (prey instanceof Shorkolotl100Entity && prey.isAlive() && distanceToSqr(prey) < 128 * 128) return;
        List<Player> players = server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(80),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                        && (p.isInWater() || p.getVehicle() instanceof Boat));
        players.sort(Comparator.comparingDouble(this::distanceToSqr));
        prey = players.isEmpty() ? null : players.get(0);
    }

    /** Clear a nine-block-diameter water passage ahead of the head.
     * All original states are persisted for restoration after 600 ticks. */
    private void clearObstacles() {
        if (!(level() instanceof ServerLevel server) || !isInWater()) return;
        Vec3 velocity = getDeltaMovement();
        Vec3 direction = velocity.lengthSqr() > 0.001 ? velocity.normalize() : getLookAngle();
        TemporaryLeviathanTerrain terrain = TemporaryLeviathanTerrain.get(server);
        // Multiple samples along the approach reduce visible clipping at high speed.
        for (double lead : new double[]{2.0, 4.0, 6.0}) {
            BlockPos center = BlockPos.containing(position().add(direction.scale(lead)));
            terrain.devourSphere(server, center, 30);
        }
    }

    public boolean hunting() { return prey != null; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(hunting() ? HUNT
                        : getDeltaMovement().lengthSqr() > 0.002 ? SWIM : IDLE)));
        controllers.add(new AnimationController<>(this, "attack", 0,
                state -> PlayState.STOP).triggerableAnim("nom", NOM));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        attackCooldown = tag.getInt("AttackCooldown");
        ambushPhase = tag.getInt("AmbushPhase");
        ambushTicks = tag.getInt("AmbushTicks");
        if (tag.hasUUID("AmbushTarget")) ambushTarget = tag.getUUID("AmbushTarget");
        if (tag.contains("BiteDirX")) firstBiteDirection = new Vec3(
                tag.getDouble("BiteDirX"), 0, tag.getDouble("BiteDirZ"));
        ticksOutOfWater = Math.max(0, Math.min(AIRBORNE_GRACE_TICKS, tag.getInt("TicksOutOfWater")));
        if (tag.contains("TerritoryX") && tag.contains("TerritoryY") && tag.contains("TerritoryZ"))
            territoryCenter = new BlockPos(tag.getInt("TerritoryX"), tag.getInt("TerritoryY"), tag.getInt("TerritoryZ"));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putInt("AmbushPhase", ambushPhase);
        tag.putInt("AmbushTicks", ambushTicks);
        if (ambushTarget != null) tag.putUUID("AmbushTarget", ambushTarget);
        if (firstBiteDirection != null) {
            tag.putDouble("BiteDirX", firstBiteDirection.x);
            tag.putDouble("BiteDirZ", firstBiteDirection.z);
        }
        tag.putInt("TicksOutOfWater", ticksOutOfWater);
        if (territoryCenter != null) {
            tag.putInt("TerritoryX", territoryCenter.getX());
            tag.putInt("TerritoryY", territoryCenter.getY());
            tag.putInt("TerritoryZ", territoryCenter.getZ());
        }
    }
}
