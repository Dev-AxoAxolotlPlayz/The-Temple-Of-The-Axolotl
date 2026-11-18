package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.UUID;

/** Encounter-only stationary minion; owner UUID survives chunk unload/reload. */
public class AttackTentacleEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID owner;
    private int emergeTicks = 66;
    private int strikeCooldown = 35;
    private int strikeTicks;
    private int blinkCooldown = 80;

    public AttackTentacleEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.ATTACK_DAMAGE, 24).add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }
    public void setEncounterOwner(UUID uuid) { owner = uuid; }
    public UUID getEncounterOwner() { return owner; }
    @Override protected void registerGoals() { }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public void knockback(double strength, double x, double z) { }
    @Override public void travel(Vec3 movement) { setDeltaMovement(Vec3.ZERO); }
    @Override public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) { return false; }
    @Override public boolean hurt(DamageSource source, float damage) {
        Entity direct = source.getDirectEntity();
        // Encounter minions only accept trident damage. Preserve the incoming
        // amount: unlike AxoCthulu's final phase, tentacles have no damage cap.
        if (!(direct instanceof net.minecraft.world.entity.projectile.ThrownTrident thrown)) return false;
        net.minecraft.world.item.ItemStack weapon =
                ((com.devaxo.templeoftheaxolotl.mixin.AxoliteArrowStackAccessor) thrown).tota$pickupStack();
        if (!weapon.is(net.minecraft.world.item.Items.TRIDENT)
                && !com.devaxo.templeoftheaxolotl.item.AxoliteTridentMode.isAxoliteFamily(weapon)) return false;
        return super.hurt(source, damage);
    }
    @Override public void tick() {
        super.tick();
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (owner != null) {
            Entity boss = server.getEntity(owner);
            if (!(boss instanceof AxoCthuluEntity) || !boss.isAlive()) { discard(); return; }
        }
        if (emergeTicks > 0) { emergeTicks--; return; }
        Player nearest = level().getNearestPlayer(this, 64);
        if (nearest == null || nearest.isCreative() || nearest.isSpectator()) return;
        if (--blinkCooldown <= 0 && distanceToSqr(nearest) > 18 * 18) {
            Vec3 target = nearest.position().add((random.nextDouble() - 0.5) * 12, 0,
                    (random.nextDouble() - 0.5) * 12);
            if (level().noCollision(this, getBoundingBox().move(target.subtract(position())))) {
                teleportTo(target.x, target.y, target.z);
            }
            blinkCooldown = 100;
        }
        if (strikeTicks > 0) {
            if (--strikeTicks == 6 && distanceToSqr(nearest) <= 10 * 10) {
                nearest.hurt(damageSources().mobAttack(this), 24.0F);
            }
        } else if (--strikeCooldown <= 0 && distanceToSqr(nearest) <= 10 * 10) {
            strikeTicks = 20;
            strikeCooldown = 55;
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("EldritchOwner", owner);
        tag.putInt("EmergeTicks", emergeTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("EldritchOwner")) owner = tag.getUUID("EldritchOwner");
        if (tag.contains("EmergeTicks")) emergeTicks = tag.getInt("EmergeTicks");
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            if (emergeTicks > 0) state.setAnimation(RawAnimation.begin().thenPlay("animation.attack_tentacle.emerge"));
            else if (strikeTicks > 0) state.setAnimation(RawAnimation.begin().thenPlay("animation.attack_tentacle.strike"));
            else state.setAnimation(RawAnimation.begin().thenLoop("animation.attack_tentacle.idle"));
            return PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
