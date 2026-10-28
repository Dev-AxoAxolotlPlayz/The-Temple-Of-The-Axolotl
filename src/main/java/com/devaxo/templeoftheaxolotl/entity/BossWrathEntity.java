
package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Phase 1: cow/player targeting, headbutt, pounce and terrain-display ground shockwave. */
public class BossWrathEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID headPartUUID;
    private boolean sanctumBound = false;
    private boolean sanctumActivated = false;
    private boolean victoryProcessed = false;
    private BlockPos sanctumOrigin = BlockPos.ZERO;

    /** Coordinates are offsets from the 60x48x60 combined Loot/Sanctum template origin. */
    public void bindToSanctum(BlockPos templateOrigin) {
        this.sanctumBound = true;
        this.sanctumActivated = false;
        this.sanctumOrigin = templateOrigin.immutable();
        this.bossBar.setVisible(false);
    }

    private void checkSanctumActivation(ServerLevel server) {
        if (!sanctumBound || sanctumActivated) return;
        // Authored test coordinates relative to (167,-60,-179):
        // X 183..210, Y -60..-55, Z -164..-136.
        AABB arena = new AABB(sanctumOrigin.getX() + 16, sanctumOrigin.getY(), sanctumOrigin.getZ() + 15,
                sanctumOrigin.getX() + 44, sanctumOrigin.getY() + 6, sanctumOrigin.getZ() + 44);
        for (ServerPlayer player : server.players()) {
            if (player.isSpectator() || !player.isAlive()) continue;
            if (arena.intersects(player.getBoundingBox())) {
                sanctumActivated = true;
                bossBar.setVisible(true);
                break;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SanctumBound", sanctumBound);
        tag.putBoolean("SanctumActivated", sanctumActivated);
        tag.putBoolean("WrathVictoryProcessed", victoryProcessed);
        tag.put("SanctumOrigin", NbtUtils.writeBlockPos(sanctumOrigin));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        sanctumBound = tag.getBoolean("SanctumBound");
        sanctumActivated = tag.getBoolean("SanctumActivated");
        victoryProcessed = tag.getBoolean("WrathVictoryProcessed");
        if (tag.contains("SanctumOrigin")) sanctumOrigin = NbtUtils.readBlockPos(tag, "SanctumOrigin").orElse(BlockPos.ZERO);
        bossBar.setVisible(!sanctumBound || sanctumActivated);
    }

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Fallen Wrath"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle_crown_and_breathing");
    private static final RawAnimation WINGS = RawAnimation.begin().thenLoop("idle_wings");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walking");
    private static final RawAnimation HEADBUTT = RawAnimation.begin().thenPlay("head_butt");
    private static final RawAnimation POUNCE = RawAnimation.begin().thenPlay("giga_pounce");
    private static final RawAnimation ENTER_RAMPAGE = RawAnimation.begin().thenPlay("normal_to_rampage");
    private static final RawAnimation EXIT_RAMPAGE = RawAnimation.begin().thenPlay("rampage_to_normal");
    private static final RawAnimation LASER = RawAnimation.begin().thenPlay("laser_charge_and_fire");

    private static final int HEADBUTT_TICKS = 30;
    private static final int POUNCE_TICKS = 48;
    private static final int SHOCKWAVE_TICKS = 14;
    private static final double SHOCKWAVE_RADIUS = 9.0;
    private static final double SHOCKWAVE_HEIGHT = 1.25;
    private static final int DISPLAY_SEGMENTS = 24;

    private static final int LASER_TOTAL_TICKS = 80; // 4-second GeckoLib animation
    private static final int LASER_DODGE_START = 60; // final 0.5s before firing
    private static final int LASER_FIRE_TICK = 70;
    private static final double LASER_RANGE = 24.0;
    private static final double LASER_RADIUS = 1.0;
    private static final double SONIC_RADIUS = 1.5;
    private static final float SONIC_DAMAGE = 8.0F;
    private int normalHits;
    private int rampageHits;
    private boolean rampage;
    private float laserLockedYaw, laserLockedPitch;
    private int laserTick = -1;
    private Vec3 laserOrigin = Vec3.ZERO;
    private Vec3 laserDirection = new Vec3(0, 0, 1);
    private boolean shockwaveRegisteredHit;
    private int attackTicks;
    private int attackCooldown;
    private int pounceCooldown;
    private static final double POUNCE_MIN_RANGE = 4.0;
    private static final double POUNCE_MAX_RANGE = 8.0;
    private static final int POUNCE_COOLDOWN = 150;
    private static final DustParticleOptions HORN_GLOW = new DustParticleOptions(new Vector3f(0.66F, 0.20F, 0.96F), 1.4F);
    private static final DustParticleOptions BEAM_GLOW = new DustParticleOptions(new Vector3f(0.79F, 0.40F, 1.0F), 1.8F);
    private static final DustParticleOptions BEAM_CORE = new DustParticleOptions(new Vector3f(0.97F, 0.85F, 1.0F), 1.5F);
    private boolean pouncing;
    private boolean pounceImpactDone;
    private int shockwaveTick = -1;
    private Vec3 shockwaveOrigin = Vec3.ZERO;
    private final Set<UUID> waveHit = new HashSet<>();
    private final List<TemporaryFragment> fragments = new ArrayList<>();

    private record TemporaryFragment(Display.BlockDisplay display, int expiresAt) {}

    public BossWrathEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WrathCombatGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Cow.class, true));
    }

    private final class WrathCombatGoal extends Goal {
        WrathCombatGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            LivingEntity target = getTarget();
            return (!sanctumBound || sanctumActivated) && target != null && target.isAlive();
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            if (laserTick < 0) getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackTicks > 0 || laserTick >= 0 || pendingLaser) {
                getNavigation().stop();
                return;
            }
            double distSq = distanceToSqr(target);
            // Pursuit is the default. Pounce is an occasional special attack, never locomotion.
            if (attackCooldown > 0) {
                getNavigation().moveTo(target, 1.1);
                return;
            }
            if (distSq <= 3.4 * 3.4 && hasLineOfSight(target)) {
                startHeadbutt();
            } else if (pounceCooldown == 0 && onGround() && hasLineOfSight(target)
                    && distSq >= POUNCE_MIN_RANGE * POUNCE_MIN_RANGE
                    && distSq <= POUNCE_MAX_RANGE * POUNCE_MAX_RANGE
                    && getRandom().nextInt(5) == 0) {
                startPounce(target);
            } else {
                getNavigation().moveTo(target, 1.1);
            }
        }
    }

    private void startHeadbutt() {
        attackTicks = HEADBUTT_TICKS;
        pouncing = false;
        attackCooldown = rampage ? 8 : 15;
        getNavigation().stop();
        triggerAnim("wrath_attack", "headbutt");
    }

    private void startPounce(LivingEntity target) {
        attackTicks = POUNCE_TICKS;
        pouncing = true;
        pounceImpactDone = false;
        pounceCooldown = POUNCE_COOLDOWN;
        attackCooldown = rampage ? 18 : 35;
        getNavigation().stop();
        triggerAnim("wrath_attack", "pounce");
        // Leap toward the target. The actual shockwave starts on landing, not at launch.
        Vec3 toward = target.position().subtract(position());
        Vec3 horizontal = new Vec3(toward.x, 0, toward.z);
        if (horizontal.lengthSqr() > 0.0001) {
            horizontal = horizontal.normalize().scale(0.55);
            setDeltaMovement(getDeltaMovement().add(horizontal.x, 0.55, horizontal.z));
            hasImpulse = true;
        }
    }

    @Override public void tick() {
        super.tick();
        if (getRemainingFireTicks() > 0) clearFire();
        if (level().isClientSide) return;
        if (level() instanceof ServerLevel server) checkSanctumActivation(server);
        if (sanctumBound && !sanctumActivated) {
            getNavigation().stop();
            Player watcher = level().getNearestPlayer(this, 128.0);
            if (watcher != null && !watcher.isSpectator()) {
                getLookControl().setLookAt(watcher, 30.0F, 30.0F);
            }
        }
        if (laserTick >= 0) {
            getNavigation().stop();
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
            setYRot(laserLockedYaw);
            setYHeadRot(laserLockedYaw);
            yBodyRot = laserLockedYaw;
            setXRot(laserLockedPitch);
        }

        if (headPartUUID != null && level() instanceof ServerLevel server) {
            Entity oldProxy = server.getEntity(headPartUUID);
            if (oldProxy != null) oldProxy.discard();
            headPartUUID = null;
        }
        bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / Math.max(1.0F, getMaxHealth()))));
        bossBar.setColor(rampage || laserTick >= 0 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
        if (attackCooldown > 0) attackCooldown--;
        if (pounceCooldown > 0) pounceCooldown--;
        if (laserTick >= 0) tickLaser();
        if (attackTicks > 0) {
            int elapsed = (pouncing ? POUNCE_TICKS : HEADBUTT_TICKS) - attackTicks;
            // Hit at the end of the 1.5-second headbutt animation.
            if (!pouncing && attackTicks == 1) performHeadbutt();
            attackTicks--;
            if (attackTicks == 0 && pouncing && !pounceImpactDone) {
                pounceImpactDone = true;
                beginShockwave(); // Only the explicit POUNCE state may create a wave.
                pouncing = false;
            }
        }
        if (pendingLaser && attackTicks <= 0 && laserTick < 0) beginLaser();
        if (shockwaveTick >= 0) advanceShockwave();
        Iterator<TemporaryFragment> it = fragments.iterator();
        while (it.hasNext()) {
            TemporaryFragment fragment = it.next();
            if (tickCount >= fragment.expiresAt()) {
                fragment.display().discard();
                it.remove();
            }
        }
    }

    /** Wrath attacks ignore the ordinary post-hit cooldown; custom armor dodge is skipped by its event handler. */
    private boolean wrathDamage(LivingEntity victim, float amount) {
        if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        victim.invulnerableTime = 0;
        boolean landed = victim.hurt(damageSources().mobAttack(this), amount);
        if (landed && victim instanceof ServerPlayer) tryLifesteal();
        return landed;
    }

    private void performHeadbutt() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !hasLineOfSight(target)) return;
        double reach = getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 1.0;
        if (distanceToSqr(target) <= reach * reach) {
            if (rampage) {
                if (wrathDamage(target, 1.25F * Math.max(2.0F, (float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))))
                    registerSuccessfulExchange();
            } else doHurtTarget(target);
        }
    }

    private void beginShockwave() {
        shockwaveOrigin = position();
        shockwaveTick = 0;
        waveHit.clear();
        shockwaveRegisteredHit = false;
        // Direct pounce contact is covered by the expanding shockwave; no double damage.
    }

    private void advanceShockwave() {
        if (!(level() instanceof ServerLevel server)) return;
        double previousRadius = SHOCKWAVE_RADIUS * shockwaveTick / SHOCKWAVE_TICKS;
        double currentRadius = SHOCKWAVE_RADIUS * (shockwaveTick + 1) / SHOCKWAVE_TICKS;
        spawnTerrainFragments(server, currentRadius);
        AABB search = new AABB(shockwaveOrigin.x - currentRadius - 2, shockwaveOrigin.y - 1,
                shockwaveOrigin.z - currentRadius - 2, shockwaveOrigin.x + currentRadius + 2,
                shockwaveOrigin.y + SHOCKWAVE_HEIGHT + 2, shockwaveOrigin.z + currentRadius + 2);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != this && entity.isAlive() && !isAlliedTo(entity))) {
            if (waveHit.contains(victim.getUUID())) continue;
            AABB bb = victim.getBoundingBox();
            if (bb.minY >= shockwaveOrigin.y + SHOCKWAVE_HEIGHT || bb.maxY <= shockwaveOrigin.y) continue;
            // Approximate horizontal box/ring overlap using the closest and farthest corners.
            double nearestX = Math.max(bb.minX, Math.min(shockwaveOrigin.x, bb.maxX));
            double nearestZ = Math.max(bb.minZ, Math.min(shockwaveOrigin.z, bb.maxZ));
            double minDist = Math.hypot(nearestX - shockwaveOrigin.x, nearestZ - shockwaveOrigin.z);
            double farX = Math.max(Math.abs(bb.minX - shockwaveOrigin.x), Math.abs(bb.maxX - shockwaveOrigin.x));
            double farZ = Math.max(Math.abs(bb.minZ - shockwaveOrigin.z), Math.abs(bb.maxZ - shockwaveOrigin.z));
            double maxDist = Math.hypot(farX, farZ);
            if (minDist > currentRadius || maxDist < previousRadius) continue;
            waveHit.add(victim.getUUID());
            if (wrathDamage(victim, rampage ? 30.0F : 24.0F)
                    && !shockwaveRegisteredHit) {
                shockwaveRegisteredHit = true;
                registerSuccessfulExchange(); // one outgoing action, regardless of cows struck
            }
            // No shockwave knockback. Vanilla hurt reactions may still occur.
        }
        shockwaveTick++;
        if (shockwaveTick >= SHOCKWAVE_TICKS) {
            shockwaveTick = -1;
            waveHit.clear();
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
        if (sanctumBound && !sanctumActivated) bossBar.setVisible(false);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    /** All projectile damage is rejected except Create Potato Cannon golden-carrot ammunition. */
    private boolean validGoldenCarrotCannonShot(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct == null) return false;
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType()).toString();
        if (!type.equals("create:potato_projectile")) return false;
        // Create versions may expose the ammunition stack through a public getItem().
        // Fail closed if the API differs, rather than allowing ordinary cannon rounds.
        try {
            Object item = direct.getClass().getMethod("getItem").invoke(direct);
            return item instanceof ItemStack stack && stack.is(Items.GOLDEN_CARROT);
        } catch (ReflectiveOperationException | SecurityException ex) {
            return false;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE) && !validGoldenCarrotCannonShot(source)) return false;
        boolean damaged = super.hurt(source, Math.min(amount, getMaxHealth() * 0.03F));
        if (damaged && !level().isClientSide && laserTick < 0) registerSuccessfulExchange();
        return damaged;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean damaged = super.doHurtTarget(target);
        if (damaged && target instanceof ServerPlayer) tryLifesteal();
        if (damaged && !level().isClientSide && laserTick < 0) registerSuccessfulExchange();
        return damaged;
    }

    private void tryLifesteal() {
        if (level().isClientSide) return;
        float roll = getRandom().nextFloat();
        if (roll < 0.50F) {
            float fraction = 0.04F + getRandom().nextFloat() * 0.01F;
            float before = getHealth();
            heal(getMaxHealth() * fraction);
            System.out.println("[WRATH LIFESTEAL] SUCCESS roll=" + roll + " healed=" + (getHealth() - before) + " fraction=" + fraction + " health=" + getHealth());
        } else {
            System.out.println("[WRATH LIFESTEAL] FAILED roll=" + roll + " threshold=0.50");
        }
    }

    private void registerSuccessfulExchange() {
        if (laserTick >= 0) return;
        if (!rampage) {
            if (++normalHits >= 10) {
                normalHits = 0;
                rampageHits = 0;
                rampage = true;
                triggerAnim("wrath_attack", "enter_rampage");
            }
        } else if (++rampageHits >= 3) {
            rampageHits = 0;
            // Finish the current melee animation before charging the laser.
            pendingLaser = true;
        }
    }

    private boolean pendingLaser;

    private void beginLaser() {
        pendingLaser = false;
        laserTick = 0;
        attackCooldown = 0;
        laserOrigin = laserFocusPoint();
        LivingEntity target = getTarget();
        Vec3 aim = target != null && target.isAlive()
                ? target.getEyePosition().subtract(laserOrigin)
                : getLookAngle();
        laserDirection = aim.lengthSqr() > 0.001 ? aim.normalize() : getLookAngle();
        laserLockedYaw = (float)(Math.toDegrees(Math.atan2(-laserDirection.x, laserDirection.z)));
        laserLockedPitch = (float)(-Math.toDegrees(Math.asin(laserDirection.y)));
        laserLockedYaw = (float) (Math.atan2(-laserDirection.x, laserDirection.z) * (180.0 / Math.PI));
        laserLockedPitch = (float) (-Math.asin(laserDirection.y) * (180.0 / Math.PI));
        setYRot(laserLockedYaw);
        setYHeadRot(laserLockedYaw);
        yBodyRot = laserLockedYaw;
        setXRot(laserLockedPitch);
        getNavigation().stop();
        triggerAnim("wrath_attack", "laser");
    }

    private static final double[][] HORN_TIP_COORDS = {
            {9.395224, 22.495430, -22.575100},
            {6.876249, 25.042689, -26.168933},
            {-9.422551, 22.476383, -22.669389},
            {-6.798066, 25.091798, -26.130519}
    };
    private static final double[] FOCUS_COORDS = {0.000000, 29.268830, -32.128000};

    // Phase 4: model-pivot-based laser anchors. These are derived from the five
    // empty locator bones in boss_fallen_wrath.geo.json, including the horn
    // parents' rest-pose rotations. The entity heading is applied each tick.
    // Animated GeckoLib pose transforms still require client-side bone tracking.
    private Vec3 modelAnchor(double x, double y, double z) {
        Vec3 forward = new Vec3(-Math.sin(Math.toRadians(getYRot())), 0,
                Math.cos(Math.toRadians(getYRot())));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return position().add(right.scale(x / 16.0))
                .add(0, y / 16.0, 0).add(forward.scale(-z / 16.0));
    }

    private Vec3[] laserHornTips() {
        return new Vec3[] {
                modelAnchor(HORN_TIP_COORDS[0][0], HORN_TIP_COORDS[0][1], HORN_TIP_COORDS[0][2]),
                modelAnchor(HORN_TIP_COORDS[1][0], HORN_TIP_COORDS[1][1], HORN_TIP_COORDS[1][2]),
                modelAnchor(HORN_TIP_COORDS[2][0], HORN_TIP_COORDS[2][1], HORN_TIP_COORDS[2][2]),
                modelAnchor(HORN_TIP_COORDS[3][0], HORN_TIP_COORDS[3][1], HORN_TIP_COORDS[3][2])
        };
    }

    private Vec3 laserFocusPoint() {
        return modelAnchor(FOCUS_COORDS[0], FOCUS_COORDS[1], FOCUS_COORDS[2]);
    }

    private void particleLine(ServerLevel server, Vec3 from, Vec3 to,
                              net.minecraft.core.particles.ParticleOptions particle, double spacing) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(1, (int)Math.ceil(delta.length() / spacing));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(delta.scale(i / (double)steps));
            server.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    private void glowSphere(ServerLevel server, Vec3 center, double radius,
                            net.minecraft.core.particles.ParticleOptions particle) {
        for (int i = 0; i < 12; i++) {
            double theta = 2.399963229728653 * i;
            double y = 1.0 - 2.0 * (i + 0.5) / 12.0;
            double r = Math.sqrt(Math.max(0, 1 - y*y));
            Vec3 at = center.add(radius*r*Math.cos(theta), radius*y, radius*r*Math.sin(theta));
            server.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    private void tickLaser() {
        if (!(level() instanceof ServerLevel server)) return;
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        setYRot(laserLockedYaw);
        setYHeadRot(laserLockedYaw);
        setYBodyRot(laserLockedYaw);
        setXRot(laserLockedPitch);
        Vec3 start = getEyePosition();
        Vec3 side = new Vec3(-laserDirection.z, 0, laserDirection.x);
        if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(laserDirection).normalize();
        Vec3 focus = laserFocusPoint();
        Vec3[] horns = laserHornTips();

        // 0-2s: four separate horn-tip spheres charge.
        for (Vec3 horn : horns) glowSphere(server, horn, 0.13 + 0.15 * Math.min(1.0, laserTick / 40.0), HORN_GLOW);

        // 2-3.5s: four thin beams converge into the central focus sphere.
        if (laserTick >= 40 && laserTick < LASER_FIRE_TICK) {
            for (Vec3 horn : horns) particleLine(server, horn, focus, BEAM_GLOW, 0.16);
            double progress = Math.min(1.0, (laserTick - 40) / 30.0);
            glowSphere(server, focus, 0.18 + 0.55 * progress,
                    laserTick >= LASER_DODGE_START ? BEAM_CORE : HORN_GLOW);
        }

        // Pull during 0-3s only. The last 0.5s before firing is a dodge window.
        if (laserTick < LASER_DODGE_START) {
            AABB area = getBoundingBox().inflate(LASER_RANGE + 2);
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != this && e.isAlive() && !isAlliedTo(e))) {
                Vec3 delta = entity.position().subtract(start);
                double along = delta.dot(laserDirection);
                if (along < 0 || along > LASER_RANGE) continue;
                Vec3 closest = start.add(laserDirection.scale(along));
                Vec3 pull = closest.subtract(entity.position());
                double length = pull.length();
                if (length > 0.15 && length < 8.0) {
                    Vec3 nudge = pull.scale(Math.min(0.11, length * 0.08) / length);
                    entity.setDeltaMovement(entity.getDeltaMovement().scale(0.65).add(nudge));
                    entity.hurtMarked = true;
                }
            }
        }

        // 3.5-4s: the thick central beam fires. Damage is applied only once.
        if (laserTick == LASER_DODGE_START) {
            // Remove stored gravity-pull velocity rather than launching the victim clear of the beam.
            AABB captureArea = getBoundingBox().inflate(LASER_RANGE + 2);
            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, captureArea,
                    e -> e != this && e.isAlive() && !isAlliedTo(e))) {
                Vec3 delta = victim.position().subtract(start);
                double along = delta.dot(laserDirection);
                if (along >= 0 && along <= LASER_RANGE &&
                        victim.position().distanceToSqr(start.add(laserDirection.scale(along))) < 9.0) {
                    victim.setDeltaMovement(Vec3.ZERO);
                    victim.hurtMarked = true;
                }
            }
        }
        if (laserTick == LASER_DODGE_START) {
            AABB holdArea = getBoundingBox().inflate(LASER_RANGE + 2);
            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, holdArea,
                    e -> e != this && e.isAlive() && !isAlliedTo(e))) {
                victim.setDeltaMovement(Vec3.ZERO);
                victim.hurtMarked = true;
            }
        }
        if (laserTick >= LASER_FIRE_TICK && laserTick < LASER_TOTAL_TICKS) {
            Vec3 end = focus.add(laserDirection.scale(LASER_RANGE));
            particleLine(server, focus, end, BEAM_CORE, 0.16);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0;
                Vec3 offset = side.scale(Math.cos(angle) * 0.65).add(up.scale(Math.sin(angle) * 0.65));
                particleLine(server, focus.add(offset), end.add(offset), BEAM_GLOW, 0.24);
            }
            // Warden-style sonic rings along the same firing axis, for the entire firing window.
            for (double distance = 1.0; distance <= LASER_RANGE; distance += 2.0) {
                Vec3 ring = focus.add(laserDirection.scale(distance));
                server.sendParticles(ParticleTypes.SONIC_BOOM, ring.x, ring.y, ring.z, 1, 0, 0, 0, 0);
                // Visible circumferential envelope, oriented perpendicular to the locked beam.
                for (int segment = 0; segment < 12; segment++) {
                    double theta = segment * Math.PI / 6.0;
                    Vec3 edge = ring.add(side.scale(Math.cos(theta) * SONIC_RADIUS))
                            .add(up.scale(Math.sin(theta) * SONIC_RADIUS));
                    server.sendParticles(BEAM_GLOW, edge.x, edge.y, edge.z, 1, 0, 0, 0, 0);
                }
            }
            if (laserTick == LASER_FIRE_TICK) {
                AABB area = getBoundingBox().inflate(LASER_RANGE + 5);
                for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, area,
                        e -> e != this && e.isAlive() && !isAlliedTo(e))) {
                    AABB coreBox = entity.getBoundingBox().inflate(LASER_RADIUS);
                    AABB sonicBox = entity.getBoundingBox().inflate(SONIC_RADIUS);
                    boolean coreHit = coreBox.clip(focus, end).isPresent() || coreBox.contains(focus);
                    boolean sonicHit = sonicBox.clip(focus, end).isPresent() || sonicBox.contains(focus);
                    if (sonicHit) {
                        // Combine damage into ONE hit to avoid vanilla invulnerability frames
                        // suppressing the second component. No extra scripted knockback.
                        float combinedDamage = SONIC_DAMAGE + (coreHit ? 14.0F : 0.0F);
                        wrathDamage(entity, combinedDamage);
                    }
                }
            }
        }
        if (++laserTick >= LASER_TOTAL_TICKS) {
            laserTick = -1;
            rampage = false;
            normalHits = 0;
            rampageHits = 0;
            triggerAnim("wrath_attack", "exit_rampage");
            attackCooldown = 20;
        }
    }

    private void spawnTerrainFragments(ServerLevel server, double radius) {
        for (int i = 0; i < DISPLAY_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / DISPLAY_SEGMENTS;
            double x = shockwaveOrigin.x + Math.cos(angle) * radius;
            double z = shockwaveOrigin.z + Math.sin(angle) * radius;
            BlockPos base = BlockPos.containing(x, shockwaveOrigin.y - 0.15, z);
            // Sample only nearby terrain so roofs above Wrath don't become fragments.
            for (int depth = 0; depth < 3 && server.getBlockState(base).isAir(); depth++) base = base.below();
            BlockState terrain = server.getBlockState(base);
            if (terrain.isAir() || terrain.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE)
                terrain = Blocks.STONE.defaultBlockState();
            Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, server);
            CompoundTag data = new CompoundTag();
            data.put("block_state", NbtUtils.writeBlockState(terrain));
            CompoundTag transform = new CompoundTag();
            ListTag scale = new ListTag();
            scale.add(FloatTag.valueOf(0.33F));
            scale.add(FloatTag.valueOf(0.23F));
            scale.add(FloatTag.valueOf(0.33F));
            transform.put("scale", scale);
            ListTag translation = new ListTag();
            translation.add(FloatTag.valueOf(0.0F));
            translation.add(FloatTag.valueOf(0.0F));
            translation.add(FloatTag.valueOf(0.0F));
            transform.put("translation", translation);
            ListTag rotation = new ListTag();
            rotation.add(FloatTag.valueOf(0.0F));
            rotation.add(FloatTag.valueOf(0.0F));
            rotation.add(FloatTag.valueOf(0.0F));
            rotation.add(FloatTag.valueOf(1.0F));
            transform.put("left_rotation", rotation);
            transform.put("right_rotation", rotation.copy());
            data.put("transformation", transform);
            display.load(data);
            display.setPos(x - 0.16, base.getY() + 1.02 + 0.12 * Math.sin(angle * 3), z - 0.16);
            server.addFreshEntity(display);
            fragments.add(new TemporaryFragment(display, tickCount + 4));
        }
    }



    // Phase 5.4: sampled Blockbench keyframes from the shipped animation JSON.
    // Server-side equivalent of the animated root/head X rotations, translation and uniform scale.
    private static final double[][] HB_Y = new double[][] {{0.0, 0}, {0.7083, 0}, {0.75, 2.3}, {1.0417, 2.3}, {1.0833, 0}, {1.2083, 0}};
    private static final double[][] HB_Z = new double[][] {{0.0, 0}, {0.7083, 0}, {0.75, -2.4}, {1.0417, -3.4}, {1.0833, -2}, {1.2083, 0}};
    private static final double[][] HB_PITCH = new double[][] {{0.0, 0}, {0.5, -8}, {1.0, 8}, {1.125, 0}, {1.2083, 0}};
    private static final double[][] HB_SCALE = new double[][] {{0.0, 1}};
    private static final double[][] HB_HEAD_PITCH = new double[][] {{0.0, 0}, {0.5, 15}, {1.4583, 10}, {1.5, 0}};
    private static final double[][] GP_Y = new double[][] {{0.0, 0}, {0.75, 0}, {0.8333, 17.6}, {2.0, 17.6}, {2.3333, 17.6}, {2.375, 0}};
    private static final double[][] GP_Z = new double[][] {{0.0, 0}, {0.75, 0}, {0.8333, -14.5}, {2.0, -14.5}, {2.3333, -14.5}, {2.375, -14.5}};
    private static final double[][] GP_PITCH = new double[][] {{0.0, 0}, {0.5, -8}, {2.0, 14.5}, {2.3333, 14.5}, {2.375, 0}};
    private static final double[][] GP_SCALE = new double[][] {{1.0, 1}, {2.2917, 1.3}, {2.375, 1}};
    private static final double[][] GP_HEAD_PITCH = new double[][] {{0.0, 0}, {0.5, -15}, {2.0, 10}, {2.375, 0}};
    private static final double[][] LZ_Y = new double[][] {{0.0, 1.5}};
    private static final double[][] LZ_Z = new double[][] {{0.0, 0}};
    private static final double[][] LZ_PITCH = new double[][] {{0.0, 0}};
    private static final double[][] LZ_SCALE = new double[][] {{0.0, 1.2}};
    private static final double[][] LZ_HEAD_PITCH = new double[][] {{0.0, 0}, {2.0, 5}};
    private static double sample(double[][] keys, double seconds) {
        if (seconds <= keys[0][0]) return keys[0][1];
        for (int i = 1; i < keys.length; i++) {
            if (seconds <= keys[i][0]) {
                double span = keys[i][0] - keys[i-1][0];
                double alpha = span <= 0 ? 1 : (seconds - keys[i-1][0]) / span;
                return keys[i-1][1] + (keys[i][1] - keys[i-1][1]) * alpha;
            }
        }
        return keys[keys.length-1][1];
    }
    private static Vec3 rotateX(Vec3 v, double degrees) {
        double r = Math.toRadians(degrees), c = Math.cos(r), sn = Math.sin(r);
        return new Vec3(v.x, v.y*c-v.z*sn, v.y*sn+v.z*c);
    }

    // Phase 5.8: one enlarged native body hitbox; no secondary head proxy.
    // Existing head proxies from older saves are cleaned up by remove() below.

    // Phase 6.2: only a genuine Sanctum boss death completes the dungeon.
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            System.out.println("[WRATH DEBUG] die() called! sanctumBound=" + sanctumBound
                    + ", alreadyDying=" + this.isDeadOrDying());
        }
        if (!victoryProcessed && sanctumBound && level() instanceof ServerLevel server) {
            victoryProcessed = true;
            System.out.println("[WRATH DEBUG] Victory triggered!");
            com.devaxo.templeoftheaxolotl.loot.DungeonRewardDevCommands.completeSanctumVictory(server, this);
            // Random defeat title, visible to nearby players; does not delay rewards.
            int line = server.random.nextInt(3);
            String title = switch (line) {
                case 0 -> "You think this is over...";
                case 1 -> "Dont Worry...";
                default -> "Here Ye Lies False Hope...";
            };
            String subtitle = switch (line) {
                case 1 -> "I'll See You Soon...";
                case 2 -> "Under the Seas...";
                default -> "";
            };
            for (ServerPlayer player : server.players()) {
                if (player.isSpectator() || player.distanceToSqr(this) > 96 * 96) continue;
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(title)));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
            }
            float shardRoll = server.random.nextFloat();
            if (shardRoll < 0.40F) {
                System.out.println("[WRATH SHARD] SUCCESS! Roll: " + shardRoll + " | Threshold: 0.40");
                net.minecraft.world.entity.item.ItemEntity shard = this.spawnAtLocation(new ItemStack(
                        com.devaxo.templeoftheaxolotl.item.ModItems.SHARD_OF_DIVINITY.get()));
                System.out.println(shard == null
                        ? "[WRATH SHARD] WARNING: Shard item entity failed to spawn!"
                        : "[WRATH SHARD] Shard item entity created at " + shard.blockPosition());
            } else {
                System.out.println("[WRATH SHARD] FAILED! Roll: " + shardRoll + " | Threshold: 0.40");
                System.out.println("[WRATH SHARD] No shard awarded.");
            }
        }
        super.die(source);
    }

    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && level() instanceof ServerLevel server && headPartUUID != null) {
            Entity part = server.getEntity(headPartUUID);
            if (part != null) part.discard();
        }
        super.remove(reason);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "wrath_idle", 5, state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(this, "wrath_wings", 5, state -> state.setAndContinue(WINGS)));
        controllers.add(new AnimationController<>(this, "wrath_walk", 5, state -> {
            if (state.isMoving()) return state.setAndContinue(WALK);
            return software.bernie.geckolib.animation.PlayState.STOP;
        }));
        controllers.add(new AnimationController<>(this, "wrath_attack", 0,
                state -> software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("headbutt", HEADBUTT)
                .triggerableAnim("pounce", POUNCE)
                .triggerableAnim("enter_rampage", ENTER_RAMPAGE)
                .triggerableAnim("exit_rampage", EXIT_RAMPAGE)
                .triggerableAnim("laser", LASER));
    }

    @Override public boolean canBeLeashed() { return false; }
    @Override protected boolean canRide(Entity vehicle) {
        if (vehicle instanceof net.minecraft.world.entity.vehicle.Boat) return false;
        return super.canRide(vehicle);
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
