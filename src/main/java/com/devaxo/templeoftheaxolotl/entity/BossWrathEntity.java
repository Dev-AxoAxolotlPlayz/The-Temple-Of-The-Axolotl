
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
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
    private ItemStack secondFallRing = ItemStack.EMPTY;
    private int secondFallCorruptionCursor = 0;
    private BlockPos secondFallCorruptionOrigin = BlockPos.ZERO;
    public void setSecondFallRing(ItemStack ring) {
        secondFallRing = ring.copyWithCount(1);
        if (!secondFallRing.isEmpty() && secondFallCorruptionCursor == 0) secondFallCorruptionOrigin = blockPosition();
        setItemSlot(EquipmentSlot.MAINHAND, secondFallRing.copy());
        updateWrathBossBarName();
    }
    public boolean isSecondFall() { return !secondFallRing.isEmpty(); }

    private boolean sanctumBound = false;
    private int originDifficultyTier = 0;
    private UUID originTriggeringPlayer;
    private UUID secondFallBetrayer;
    private final java.util.Map<java.util.UUID, Integer> secondFallContactHits = new java.util.HashMap<>();
    private final java.util.Map<UUID, Integer> secondFallContactTicks = new java.util.HashMap<>();
    private boolean secondFallSkyCaptured = false;
    private long secondFallPreviousDayTime = 0L;
    private long secondFallSkyStartGameTime = 0L;
    private static final long SECOND_FALL_NEW_MOON_MIDNIGHT = 4L * 24000L + 18000L;

    public void setSecondFallBetrayer(UUID playerId) { secondFallBetrayer = playerId; }
    public void setOriginDifficulty(int tier, UUID playerId) {
        originDifficultyTier = Math.max(1, Math.min(3, tier));
        originTriggeringPlayer = playerId;
        updateWrathBossBarName();
    }
    public int getOriginDifficultyTier() { return originDifficultyTier; }
    public boolean isOriginWrath() { return originDifficultyTier > 0 && getTags().contains("is bound to Origin Temple"); }
    public BlockPos getSanctumTemplateOrigin() { return sanctumOrigin; }
    public boolean isOriginResolved() { return victoryProcessed; }
    public void resolveOriginByForgiveness() { victoryProcessed = true; }

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
        if (!secondFallRing.isEmpty()) tag.put("SecondFallRing", secondFallRing.save(registryAccess()));
        tag.putInt("SecondFallCorruptionCursor", secondFallCorruptionCursor);
        tag.put("SecondFallCorruptionOrigin", NbtUtils.writeBlockPos(secondFallCorruptionOrigin));
        tag.putBoolean("SanctumBound", sanctumBound);
        tag.putInt("OriginDifficultyTier", originDifficultyTier);
        if (originTriggeringPlayer != null) tag.putUUID("OriginTriggeringPlayer", originTriggeringPlayer);
        if (secondFallBetrayer != null) tag.putUUID("SecondFallBetrayer", secondFallBetrayer);
        tag.putBoolean("SecondFallSkyCaptured", secondFallSkyCaptured);
        tag.putLong("SecondFallPreviousDayTime", secondFallPreviousDayTime);
        tag.putLong("SecondFallSkyStartGameTime", secondFallSkyStartGameTime);
        tag.putBoolean("SanctumActivated", sanctumActivated);
        tag.putBoolean("WrathVictoryProcessed", victoryProcessed);
        tag.put("SanctumOrigin", NbtUtils.writeBlockPos(sanctumOrigin));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SecondFallRing")) setSecondFallRing(ItemStack.parseOptional(registryAccess(), tag.getCompound("SecondFallRing")));
        secondFallCorruptionCursor = tag.getInt("SecondFallCorruptionCursor");
        if (tag.contains("SecondFallCorruptionOrigin")) secondFallCorruptionOrigin = NbtUtils.readBlockPos(tag, "SecondFallCorruptionOrigin").orElse(blockPosition());
        sanctumBound = tag.getBoolean("SanctumBound");
        originDifficultyTier = tag.getInt("OriginDifficultyTier");
        originTriggeringPlayer = tag.hasUUID("OriginTriggeringPlayer") ? tag.getUUID("OriginTriggeringPlayer") : null;
        secondFallBetrayer = tag.hasUUID("SecondFallBetrayer") ? tag.getUUID("SecondFallBetrayer") : null;
        secondFallSkyCaptured = tag.getBoolean("SecondFallSkyCaptured");
        secondFallPreviousDayTime = tag.getLong("SecondFallPreviousDayTime");
        secondFallSkyStartGameTime = tag.getLong("SecondFallSkyStartGameTime");
        sanctumActivated = tag.getBoolean("SanctumActivated");
        victoryProcessed = tag.getBoolean("WrathVictoryProcessed");
        if (tag.contains("SanctumOrigin")) sanctumOrigin = NbtUtils.readBlockPos(tag, "SanctumOrigin").orElse(BlockPos.ZERO);
        updateWrathBossBarName();
        bossBar.setVisible(!sanctumBound || sanctumActivated);
    }

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Fallen Wrath"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private void updateWrathBossBarName() {
        if (isSecondFall()) { bossBar.setName(Component.literal("The One Who Fell Twice")); return; }
        if (originDifficultyTier >= 1 && originDifficultyTier <= 3) {
            String difficulty = switch (originDifficultyTier) {
                case 1 -> "Easy";
                case 2 -> "Medium";
                default -> "Hard";
            };
            bossBar.setName(Component.literal("Origin Wrath Difficulty: " + difficulty));
        } else {
            bossBar.setName(Component.literal("Fallen Wrath"));
        }
    }
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
    private Vec3 laserLockedTarget = Vec3.ZERO;
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
            if (laserTick < 0) {
                getLookControl().setLookAt(target, 180.0F, 90.0F);
                if (isSecondFall()) faceBetrayer(target);
            }
            if (attackTicks > 0 || laserTick >= 0 || pendingLaser) {
                getNavigation().stop();
                return;
            }
            double distSq = distanceToSqr(target);
            // Pursuit is the default. Pounce is an occasional special attack, never locomotion.
            if (attackCooldown > 0) {
                getNavigation().moveTo(target, isSecondFall() ? 1.35 : 1.1);
                return;
            }
            if (distSq <= 3.4 * 3.4 && hasLineOfSight(target)) {
                startHeadbutt();
            } else if ((!isSecondFall() || getRandom().nextInt(4) == 0) && pounceCooldown == 0 && onGround() && hasLineOfSight(target)
                    && distSq >= POUNCE_MIN_RANGE * POUNCE_MIN_RANGE
                    && distSq <= POUNCE_MAX_RANGE * POUNCE_MAX_RANGE
                    && getRandom().nextInt(5) == 0) {
                startPounce(target);
            } else {
                getNavigation().moveTo(target, isSecondFall() ? 1.35 : 1.1);
            }
        }
    }

    private void faceBetrayer(LivingEntity target) {
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        if (dx * dx + dz * dz < 0.001) return;
        float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        setYRot(yaw); setYHeadRot(yaw); yBodyRot = yaw;
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
        if (level() instanceof ServerLevel server) {
            checkSanctumActivation(server);
            if (isSecondFall() && isAlive()) {
                if (!secondFallSkyCaptured) {
                    secondFallPreviousDayTime = server.getDayTime();
                    secondFallSkyStartGameTime = server.getGameTime();
                    secondFallSkyCaptured = true;
                }
                // Overworld-wide midnight with a new moon, no sunlight.
                if (server.dimension() == Level.OVERWORLD
                        && server.getDayTime() != SECOND_FALL_NEW_MOON_MIDNIGHT)
                    server.setDayTime(SECOND_FALL_NEW_MOON_MIDNIGHT);
            }
            if (isSecondFall() && secondFallBetrayer != null) {
                ServerPlayer betrayer = server.getServer().getPlayerList().getPlayer(secondFallBetrayer);
                if (betrayer != null && betrayer.level() == level() && betrayer.isAlive() && !betrayer.isSpectator() && !betrayer.isCreative()) {
                    if (getTarget() != betrayer) setTarget(betrayer);
                    if (laserTick < 0) faceBetrayer(betrayer);
                } else if (getTarget() != null) setTarget(null);
            }
            if (isSecondFall() && secondFallCorruptionCursor < 18000 && tickCount % 2 == 0)
                secondFallCorruptionCursor = SecondFallCorruption.advance(server, secondFallCorruptionOrigin, secondFallCorruptionCursor);
        }
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
            // 0.83s: the forward lunge is already underway (animation impact window).
            if (!pouncing && elapsed == 17) performHeadbutt();
            attackTicks--;
            if (attackTicks == 0 && pouncing && !pounceImpactDone) {
                pounceImpactDone = true;
                beginShockwave(); // Only the explicit POUNCE state may create a wave.
                pouncing = false;
            }
        }
        // Ring blades are continuously active: any overlap with Wrath's full body hitbox
        // causes contact damage, regardless of pounce, laser, or gravity state.
        if (isSecondFall() && isAlive() && level() instanceof ServerLevel server) {
            AABB blades = getBoundingBox();
            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, blades,
                    e -> e != this && e.isAlive() && e.getBoundingBox().intersects(blades)
                            && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
                int last = secondFallContactTicks.getOrDefault(victim.getUUID(), -1000);
                // Three successive intervals of 5, 5, 6 ticks average 16/3 ticks.
                // Relative to the original 4-tick interval this is exactly 25% fewer hits.
                int interval = (secondFallContactHits.getOrDefault(victim.getUUID(), 0) % 3 == 2) ? 6 : 5;
                if (tickCount - last < interval) continue;
                secondFallContactHits.merge(victim.getUUID(), 1, Integer::sum);
                secondFallContactTicks.put(victim.getUUID(), tickCount);
                if (wrathDamage(victim, Math.max(2.0F,
                        (float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.20F))) {
                    SecondFallEnchantments.onHit(secondFallRing, victim, this);
                }
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

    // Supplement the boss's custom rampage damage. Normal melee also goes through vanilla's
    // equipped-weapon enchantment processing, with the original Ring preserved intact.
    private float secondFallAttackBonus() {
        if (secondFallRing.isEmpty()) return 0F;
        float bonus = 0F;
        ItemEnchantments enchantments = secondFallRing.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            var key = entry.getKey().unwrapKey();
            if (key.isEmpty()) continue;
            String path = key.get().location().getPath();
            if (path.equals("sharpness") || path.equals("smite") || path.equals("bane_of_arthropods"))
                bonus += entry.getIntValue() * 0.5F;
        }
        return bonus;
    }

    private void performHeadbutt() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !hasLineOfSight(target)) return;
        double reach = getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 1.0;
        if (distanceToSqr(target) <= reach * reach) {
            if (rampage) {
                if (wrathDamage(target, 1.25F * Math.max(2.0F, (float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) + secondFallAttackBonus())) {
                    SecondFallEnchantments.onHit(secondFallRing, target, this);
                    registerSuccessfulExchange();
                }
            } else {
                if (doHurtTarget(target) && isSecondFall()) SecondFallEnchantments.onHit(secondFallRing, target, this);
            }
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
            if (!isSecondFall() || getRandom().nextInt(4) == 0) pendingLaser = true;
        }
    }

    private boolean pendingLaser;

    private void beginLaser() {
        pendingLaser = false;
        laserTick = 0;
        attackCooldown = 0;
        laserOrigin = animatedLaserLocator("Laser Focus Point");
        LivingEntity target = getTarget();
        Vec3 aim = target != null && target.isAlive()
                ? target.getEyePosition().subtract(laserOrigin)
                : getLookAngle();
        laserDirection = aim.lengthSqr() > 0.001 ? aim.normalize() : getLookAngle();
        laserLockedTarget = laserOrigin.add(aim.lengthSqr() > 0.001 ? aim : laserDirection.scale(LASER_RANGE));
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
    // Dedicated Gravity Focus Point bone (child of Center5) in the updated model.
    private static final double[] GRAVITY_FOCUS_COORDS = {0.000000, 37.268830, -32.128000};

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

    // Unified server-side laser pose. The laser animation already includes the
    // persistent rampage root scale and upward translation; do not double-apply.
    // All six locators share the same
    // animation, scale, pivot hierarchy, and entity-space conversion.
    private record BonePose(Vec3 pivot, Vec3 restRotation, String parent) {}
    private static BonePose laserBone(String name) {
        return switch (name) {
            case "Axolotl" -> new BonePose(new Vec3(-0.07000000, 14.70944000, -5.86910000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Axolotl Wrath");
            case "Axolotl Wrath" -> new BonePose(new Vec3(0.00000000, 12.00000000, 0.00000000), new Vec3(0.00000000, 0.00000000, 0.00000000), null);
            case "Center" -> new BonePose(new Vec3(11.80000000, 20.81926000, -30.87254000), new Vec3(73.84438000, 10.99104000, 88.47347000), "Left  Horn Pair");
            case "Center2" -> new BonePose(new Vec3(-13.60000000, 20.81926000, -29.47254000), new Vec3(29.72106000, -39.21097000, -77.70990000), "Right Horn Pair");
            case "Center3" -> new BonePose(new Vec3(-11.80000000, 20.81926000, -30.87254000), new Vec3(73.84438000, -10.99104000, -88.47347000), "Right Horn Pair");
            case "Center4" -> new BonePose(new Vec3(13.60000000, 20.81926000, -29.47254000), new Vec3(29.72106000, 39.21097000, 77.70990000), "Left  Horn Pair");
            case "Center5" -> new BonePose(new Vec3(0.00000000, 25.76883000, -32.12800000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Crown");
            case "Crown" -> new BonePose(new Vec3(0.00000000, 25.47483000, -32.14633000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Crown Tilt");
            case "Crown Tilt" -> new BonePose(new Vec3(0.00000000, 25.47483000, -32.14633000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Head");
            case "Full Body" -> new BonePose(new Vec3(-0.35000000, 7.87014000, -5.12254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Axolotl");
            case "Gravity Focus Point" -> new BonePose(new Vec3(0.00000000, 37.26883000, -32.12800000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Center5");
            case "Head" -> new BonePose(new Vec3(-0.40000000, 15.97014000, -10.47254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Full Body");
            case "Horns" -> new BonePose(new Vec3(-0.90000000, 20.81926000, -30.17254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Head");
            case "Laser Focus Point" -> new BonePose(new Vec3(0.00000000, 29.26883000, -32.12800000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Center5");
            case "Left  Horn Pair" -> new BonePose(new Vec3(11.80000000, 20.81926000, -30.87254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Horns");
            case "Left Horn Tip A" -> new BonePose(new Vec3(11.80000000, 29.61926000, -30.87254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Center");
            case "Left Horn Tip B" -> new BonePose(new Vec3(13.60000000, 29.41926000, -29.47254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Center4");
            case "Right Horn Pair" -> new BonePose(new Vec3(-13.60000000, 20.81926000, -29.47254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Horns");
            case "Right Horn Tip A" -> new BonePose(new Vec3(-11.80000000, 29.51926000, -30.87254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Center3");
            case "Right Horn Tip B" -> new BonePose(new Vec3(-13.60000000, 29.51926000, -29.47254000), new Vec3(0.00000000, 0.00000000, 0.00000000), "Center2");
            default -> throw new IllegalArgumentException("Unknown laser bone: " + name);
        };
    }

    private static double crownSpin(double t) {
        if (t <= 2) { double u = Math.max(0, t / 2); return -360 * (1 - Math.pow(1 - u, 3)); }
        double u = Math.min(1, (t - 2) / 2);
        return -360 - 1080 * (1 - Math.cos(u * Math.PI / 2));
    }
    private static Vec3 sampleVector(double t, double[][] keys, String easing) {
        if (t <= keys[0][0]) return new Vec3(keys[0][1], keys[0][2], keys[0][3]);
        for (int i = 1; i < keys.length; i++) {
            if (t <= keys[i][0]) {
                double u = (t - keys[i-1][0]) / (keys[i][0] - keys[i-1][0]);
                if ("easeOutCubic".equals(easing)) u = 1 - Math.pow(1-u,3);
                else if ("easeInSine".equals(easing)) u = 1 - Math.cos(u*Math.PI/2);
                return new Vec3(keys[i-1][1] + u*(keys[i][1]-keys[i-1][1]),
                        keys[i-1][2] + u*(keys[i][2]-keys[i-1][2]),
                        keys[i-1][3] + u*(keys[i][3]-keys[i-1][3]));
            }
        }
        double[] k = keys[keys.length-1]; return new Vec3(k[1],k[2],k[3]);
    }
    private static Vec3 laserBoneRotation(String name, double seconds) {
        return switch (name) {
            case "Center2" -> new Vec3(8.90670000, 23.43250000, -64.21390000);
            case "Center4" -> new Vec3(10.88920000, -29.72970000, 62.12950000);
            case "Crown" -> new Vec3(0, crownSpin(seconds), 0);
            case "Crown Tilt" -> new Vec3(-90.00000000, 0.00000000, 180.00000000);
            case "Head" -> sampleVector(seconds, new double[][]{new double[]{0.0, 0, 0, 0}, new double[]{2.0, 5, 0, 0}}, "linear");
            case "Left  Horn Pair" -> new Vec3(22.19310000, 51.57940000, 62.64600000);
            case "Right Horn Pair" -> new Vec3(14.51250000, -56.64090000, -58.04430000);
            default -> Vec3.ZERO;
        };
    }
    private static Vec3 laserBoneTranslation(String name, double seconds) {
        return switch (name) {
            case "Axolotl Wrath" -> new Vec3(0.00000000, 1.50000000, 0.00000000);
            case "Crown" -> sampleVector(seconds, new double[][]{new double[]{0.0, 0, 0, 0}, new double[]{2.0, 0, 0, -2.1}}, "linear");
            case "Crown Tilt" -> new Vec3(0.00000000, -2.80000000, -11.00000000);
            case "Left  Horn Pair" -> new Vec3(-3.70000000, 0.00000000, 0.30000000);
            case "Right Horn Pair" -> new Vec3(3.90000000, -2.30000000, -1.20000000);
            default -> Vec3.ZERO;
        };
    }
    private static Vec3 laserBoneScale(String name, double seconds) {
        return switch (name) {
            case "Axolotl Wrath" -> new Vec3(1.20000000, 1.20000000, 1.20000000);
            case "Horns" -> new Vec3(1.10000000, 1.10000000, 1.10000000);
            default -> new Vec3(1,1,1);
        };
    }
    private static Vec3 rotateEuler(Vec3 point, Vec3 pivot, Vec3 degrees) {
        point = rotateAroundX(point, pivot, degrees.x);
        point = rotateAroundY(point, pivot, degrees.y);
        return rotateAroundZ(point, pivot, degrees.z);
    }
    private Vec3 animatedLaserLocator(String name) {
        double seconds = Math.max(0, laserTick) / 20.0;
        Vec3 point = laserBone(name).pivot();
        String bone = name;
        while (bone != null) {
            BonePose pose = laserBone(bone);
            Vec3 pivot = pose.pivot();
            Vec3 scale = laserBoneScale(bone, seconds);
            Vec3 relative = point.subtract(pivot);
            point = pivot.add(relative.x * scale.x, relative.y * scale.y, relative.z * scale.z);
            point = rotateEuler(point, pivot, pose.restRotation().add(laserBoneRotation(bone, seconds)));
            point = point.add(laserBoneTranslation(bone, seconds));
            bone = pose.parent();
        }
        return modelAnchor(point.x, point.y, point.z);
    }
    private Vec3[] animatedLaserHorns() {
        return new Vec3[]{animatedLaserLocator("Left Horn Tip A"),
                animatedLaserLocator("Left Horn Tip B"),
                animatedLaserLocator("Right Horn Tip A"),
                animatedLaserLocator("Right Horn Tip B")};
    }

    private static Vec3 rotateAroundX(Vec3 point, Vec3 pivot, double degrees) {
        return pivot.add(rotateX(point.subtract(pivot), degrees));
    }

    private static Vec3 rotateAroundY(Vec3 point, Vec3 pivot, double degrees) {
        double r = Math.toRadians(degrees), c = Math.cos(r), sn = Math.sin(r);
        Vec3 v = point.subtract(pivot);
        return pivot.add(v.x * c + v.z * sn, v.y, -v.x * sn + v.z * c);
    }

    private static Vec3 rotateAroundZ(Vec3 point, Vec3 pivot, double degrees) {
        double r = Math.toRadians(degrees), c = Math.cos(r), sn = Math.sin(r);
        Vec3 v = point.subtract(pivot);
        return pivot.add(v.x * c - v.y * sn, v.x * sn + v.y * c, v.z);
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
        Vec3 focus = animatedLaserLocator("Laser Focus Point");
        // The aim point is locked at charge start; the beam is recomputed from
        // the fully transformed crown focus as the animation progresses.
        Vec3 updatedAim = laserLockedTarget.subtract(focus);
        if (updatedAim.lengthSqr() > 0.0001) laserDirection = updatedAim.normalize();
        Vec3 side = new Vec3(-laserDirection.z, 0, laserDirection.x);
        if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(laserDirection).normalize();
        Vec3[] horns = animatedLaserHorns();

        // 0-2s: four separate horn-tip spheres charge.
        for (Vec3 horn : horns) glowSphere(server, horn, 0.13 + 0.15 * Math.min(1.0, laserTick / 40.0), HORN_GLOW);

        // 2-3.5s: four thin beams converge into the central focus sphere.
        if (laserTick >= 40 && laserTick < LASER_FIRE_TICK) {
            for (Vec3 horn : horns) particleLine(server, horn, focus, BEAM_GLOW, 0.16);
            double progress = Math.min(1.0, (laserTick - 40) / 30.0);
            glowSphere(server, focus, 0.18 + 0.55 * progress,
                    laserTick >= LASER_DODGE_START ? BEAM_CORE : HORN_GLOW);
        }

        // Hold captured victims on the locked beam until the actual firing tick.
        // The old dodge-window release let gravity drop victims out of the beam.
        if (laserTick < LASER_FIRE_TICK) {
            AABB captureArea = getBoundingBox().inflate(LASER_RANGE + 2);
            // Pull to the dedicated Gravity Focus Point bone, not the laser focal point.
            // Uses the same laser/rampage animated hierarchy as the laser focal point.
            Vec3 gravityAnchor = animatedLaserLocator("Gravity Focus Point");
            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, captureArea,
                    e -> e != this && e.isAlive() && !isAlliedTo(e))) {
                // Anchor all three axes, rather than projecting onto the beam.
                Vec3 victimCenter = victim.getBoundingBox().getCenter();
                Vec3 pull = gravityAnchor.subtract(victimCenter);
                double distance = pull.length();
                if (distance >= 12.0) continue;
                // Counter gravity every tick, including the final charge-up ticks.
                // Keep horizontal centering without teleporting or moving the beam.
                Vec3 correction = distance > 0.05
                        ? pull.scale(Math.min(0.22, distance * 0.20) / distance)
                        : Vec3.ZERO;
                victim.setDeltaMovement(correction);
                victim.hurtMarked = true;
                victim.fallDistance = 0;
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
            if (isOriginWrath()) {
                com.devaxo.templeoftheaxolotl.maze.OriginSanctumRescue.resolve(server, this, false);
            }
            if (!this.getTags().contains("is bound to Origin Temple")) {
                if (source.getEntity() instanceof ServerPlayer killer)
                    com.devaxo.templeoftheaxolotl.traps.TempleProgress.recordDecoyWrathDefeat(killer);
                com.devaxo.templeoftheaxolotl.loot.DungeonRewardDevCommands.completeSanctumVictory(server, this);
            }
            if (!this.getTags().contains("is bound to Origin Temple")) {
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
        if (level() instanceof ServerLevel server) restoreSecondFallSky(server);
        super.die(source);
    }

    private void restoreSecondFallSky(ServerLevel server) {
        if (!secondFallSkyCaptured || server.dimension() != Level.OVERWORLD) return;
        long elapsed = Math.max(0L, server.getGameTime() - secondFallSkyStartGameTime);
        server.setDayTime(secondFallPreviousDayTime + elapsed);
        secondFallSkyCaptured = false;
    }

    @Override public void remove(RemovalReason reason) {
        if (reason != RemovalReason.UNLOADED_TO_CHUNK && level() instanceof ServerLevel server)
            restoreSecondFallSky(server);
        if (!level().isClientSide && level() instanceof ServerLevel server && headPartUUID != null) {
            Entity part = server.getEntity(headPartUUID);
            if (part != null) part.discard();
        }
        super.remove(reason);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Do not run the idle crown spin/scale on top of the laser crown pose.
        // The laser animation explicitly contains the persistent rampage root
        // scale (1.2) and upward shift (+1.5 Blockbench units).
        controllers.add(new AnimationController<>(this, "wrath_idle", 0, state -> {
            if (laserTick >= 0) return software.bernie.geckolib.animation.PlayState.STOP;
            return state.setAndContinue(IDLE);
        }));
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
