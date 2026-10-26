
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class CelestialInnateEvents {

    private static final String AEGIS_READY_TICK =
            "temple_of_the_axolotl_aegis_ready_tick";

    private static final String REDIRECTED_PROJECTILE =
            "temple_of_the_axolotl_eternal_guard_redirected";

    private static final int AEGIS_COOLDOWN = 2400;

    private CelestialInnateEvents() {
    }

    private static boolean hasChestplate(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST)
                .is(ModItems.AXOLOTL_CHESTPLATE.get());
    }

    private static boolean hasLeggings(Player player) {
        return player.getItemBySlot(EquipmentSlot.LEGS)
                .is(ModItems.AXOLOTL_LEGGINGS.get());
    }

    // =========================================================
    // CELESTIAL LEGGINGS: MELEE EVASION
    // 50% chance to cancel a melee hit.
    // =========================================================

    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        if (!hasLeggings(player)) {
            return;
        }

        DamageSource source = event.getSource();

        // A direct hit from a living attacker counts as melee.
        // Projectiles, explosions, and environmental damage do not.
        boolean isMelee =
                source.getDirectEntity() instanceof LivingEntity
                        && source.getDirectEntity() == source.getEntity();

        if (!isMelee) {
            return;
        }

        if (player.getRandom().nextFloat() < 0.5F) {
            event.setCanceled(true);

            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(
                        ParticleTypes.CLOUD,
                        player.getX(),
                        player.getY() + 1.0,
                        player.getZ(),
                        12,
                        0.35,
                        0.55,
                        0.35,
                        0.05
                );
            }
        }
    }

    // =========================================================
    // CELESTIAL LEGGINGS: MAGIC RESISTANCE
    // CELESTIAL CHESTPLATE: AEGIS
    //
    // Both are processed in the final damage stage so Aegis
    // checks the damage that would actually reach the player.
    // =========================================================

    @SubscribeEvent
    public static void onDamagePre(
            LivingDamageEvent.Pre event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        float damage = event.getNewDamage();
        DamageSource source = event.getSource();

        // ----- LEGGINGS: 50% MAGIC DAMAGE REDUCTION -----

        if (hasLeggings(player) && isMagicalDamage(source)) {
            damage *= 0.5F;
            event.setNewDamage(damage);
        }

        // ----- CHESTPLATE: CELESTIAL AEGIS -----

        if (!hasChestplate(player)) {
            return;
        }

        if (damage <= 0.0F) {
            return;
        }

        // Only activate if this hit would be lethal.
        if (damage < player.getHealth()) {
            return;
        }

        long currentTick = player.level().getGameTime();

        long readyTick = player.getPersistentData()
                .getLong(AEGIS_READY_TICK);

        if (currentTick < readyTick) {
            return;
        }

        // Prevent the lethal damage.
        event.setNewDamage(0.0F);

        // Start the 120-second cooldown.
        player.getPersistentData().putLong(
                AEGIS_READY_TICK,
                currentTick + AEGIS_COOLDOWN
        );

        // Vanilla Totem effects, each increased by one level.
        // Regeneration II -> III
        // Fire Resistance I -> II
        // Absorption II -> III
        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                900,
                2
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                800,
                1
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION,
                100,
                2
        ));

        // Visible cooldown timer: 120 seconds.
        player.addEffect(new MobEffectInstance(
                MobEffects.GLOWING,
                AEGIS_COOLDOWN,
                0
        ));

        ServerLevel level = player.serverLevel();

        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                80,
                0.6,
                0.9,
                0.6,
                0.25
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static boolean isMagicalDamage(DamageSource source) {
        return source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.WITHER_SKULL);
    }

    // =========================================================
    // CELESTIAL CHESTPLATE: ETERNAL GUARD
    //
    // Redirect an incoming projectile in the direction
    // the player is looking.
    // =========================================================

    @SubscribeEvent
    public static void onProjectileImpact(
            ProjectileImpactEvent event
    ) {
        Projectile projectile = event.getProjectile();
        // Tridents are the one projectile Eternal Guard cannot redirect.
        if (projectile instanceof ThrownTrident) return;

        if (projectile.level().isClientSide()) {
            return;
        }

        if (!(event.getRayTraceResult()
                instanceof EntityHitResult hit)) {
            return;
        }

        if (!(hit.getEntity() instanceof Player player)) {
            return;
        }

        if (!hasChestplate(player)) {
            return;
        }

        // Prevent a redirected projectile from repeatedly
        // triggering Eternal Guard.
        if (projectile.getPersistentData()
                .getBoolean(REDIRECTED_PROJECTILE)) {
            return;
        }

        event.setCanceled(true);

        projectile.getPersistentData().putBoolean(
                REDIRECTED_PROJECTILE,
                true
        );

        Vec3 direction = player.getLookAngle().normalize();

        double originalSpeed =
                projectile.getDeltaMovement().length();

        double redirectSpeed =
                Math.max(originalSpeed, 1.5D);

        // Move the projectile ahead of the player so it
        // does not immediately collide with them again.
        Vec3 start = player.getEyePosition()
                .add(direction.scale(1.5D));

        projectile.setPos(
                start.x,
                start.y,
                start.z
        );

        projectile.setDeltaMovement(
                direction.scale(redirectSpeed)
        );

        projectile.setOwner(player);
        projectile.hasImpulse = true;

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    start.x,
                    start.y,
                    start.z,
                    16,
                    0.25,
                    0.25,
                    0.25,
                    0.04
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.35F
            );
        }
    }
}
