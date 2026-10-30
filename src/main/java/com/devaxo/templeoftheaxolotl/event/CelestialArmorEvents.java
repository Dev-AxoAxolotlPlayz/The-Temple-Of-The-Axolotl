
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.CelestialArmorItem;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.network.CelestialDoubleJumpPayload;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class CelestialArmorEvents {

    private static final int REFRESH_INTERVAL = 40;
    private static final int EFFECT_DURATION = 400;

    private static final String CONDUIT_DISABLED_KEY = "tota_helmet_conduit_disabled";
    private static final String NIGHT_VISION_DISABLED_KEY = "tota_helmet_night_vision_disabled";

    /** Default enabled, stored per player across logins. */
    public static boolean helmetNightVisionEnabled(Player player) {
        return !player.getPersistentData().getBoolean(NIGHT_VISION_DISABLED_KEY);
    }

    public static void toggleHelmetNightVision(Player player) {
        boolean enable = !helmetNightVisionEnabled(player);
        player.getPersistentData().putBoolean(NIGHT_VISION_DISABLED_KEY, !enable);
        if (!enable && hasHelmet(player)) {
            // Clear the helmet's already-applied long-duration effect immediately.
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }


    public static boolean helmetConduitEnabled(Player player) {
        return !player.getPersistentData().getBoolean(CONDUIT_DISABLED_KEY);
    }
    public static void toggleHelmetConduit(Player player) {
        boolean enabled = !helmetConduitEnabled(player);
        player.getPersistentData().putBoolean(CONDUIT_DISABLED_KEY, !enabled);
        if (!enabled && hasHelmet(player)) player.removeEffect(MobEffects.CONDUIT_POWER);
    }

    private static final ResourceLocation SWIM_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(
                    TempleOfTheAxolotl.MODID,
                    "celestial_leggings_swim_speed"
            );

    private CelestialArmorEvents() {}

    private static boolean hasHelmet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD)
                .is(ModItems.AXOLOTL_ABSOLUTE.get());
    }

    private static boolean hasArmor(
            Player player,
            EquipmentSlot slot,
            ArmorItem.Type type
    ) {
        if (!(player.getItemBySlot(slot).getItem()
                instanceof CelestialArmorItem armor)) {
            return false;
        }

        return armor.getType() == type;
    }

    private static void applyEffect(
            Player player,
            Holder<MobEffect> effect,
            int amplifier
    ) {
        player.addEffect(new MobEffectInstance(
                effect,
                EFFECT_DURATION,
                amplifier,
                true,
                false,
                true
        ));
    }

    // =========================================
    // EQUIPMENT PASSIVES
    // =========================================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        boolean helmet = hasHelmet(player);

        boolean chestplate = hasArmor(
                player,
                EquipmentSlot.CHEST,
                ArmorItem.Type.CHESTPLATE
        );

        boolean leggings = hasArmor(
                player,
                EquipmentSlot.LEGS,
                ArmorItem.Type.LEGGINGS
        );

        // -------------------------------------
        // LEGGINGS: MINING FATIGUE IMMUNITY
        // -------------------------------------

        if (leggings && player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }

        // -------------------------------------
        // LEGGINGS: SWIMMING SPEED
        // -------------------------------------

        AttributeInstance swimAttribute =
                player.getAttribute(NeoForgeMod.SWIM_SPEED);

        if (swimAttribute != null) {

            boolean shouldBoost =
                    leggings
                            && player.isInWater()
                            && player.isSwimming();

            boolean hasBoost =
                    swimAttribute.hasModifier(SWIM_SPEED_ID);

            if (shouldBoost && !hasBoost) {

                swimAttribute.addTransientModifier(
                        new AttributeModifier(
                                SWIM_SPEED_ID,
                                0.10,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        )
                );

            } else if (!shouldBoost && hasBoost) {

                swimAttribute.removeModifier(SWIM_SPEED_ID);
            }
        }

        // -------------------------------------
        // POTION EFFECTS: EVERY 2 SECONDS
        // -------------------------------------

        if (player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }

        // HELMET
        if (helmet) {

            applyEffect(player, MobEffects.REGENERATION, 0);
            applyEffect(player, MobEffects.DAMAGE_BOOST, 0);
            if (helmetConduitEnabled(player)) applyEffect(player, MobEffects.CONDUIT_POWER, 1);
            if (helmetNightVisionEnabled(player)) {
                applyEffect(player, MobEffects.NIGHT_VISION, 0);
            }
        }

        // CHESTPLATE
        if (chestplate) {

            applyEffect(player, MobEffects.DIG_SPEED, 0);
        }
    }

    // =========================================
    // LEGGINGS: UNDERWATER MINING SPEED
    // =========================================

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {

        Player player = event.getEntity();

        if (!hasArmor(
                player,
                EquipmentSlot.LEGS,
                ArmorItem.Type.LEGGINGS
        )) {
            return;
        }

        if (!player.isInWater()) {
            return;
        }

        event.setNewSpeed(event.getNewSpeed() * 1.15F);
    }

    // =========================================
    // CHESTPLATE: SONIC BOOM RESISTANCE
    // BOOTS: TEMPORARY FALL DAMAGE IMMUNITY
    // =========================================

    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {

        if (event.getSource().getEntity() instanceof com.devaxo.templeoftheaxolotl.entity.BossWrathEntity) return;
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // -------------------------------------
        // BOOTS: DOUBLE JUMP FALL PROTECTION
        // -------------------------------------

        if (event.getSource().is(DamageTypes.FALL)) {

            long protectionUntil =
                    player.getPersistentData().getLong(
                            CelestialDoubleJumpPayload.FALL_PROTECTION_KEY
                    );

            if (protectionUntil > player.level().getGameTime()) {

                event.setAmount(0.0F);
                return;
            }
        }

        // -------------------------------------
        // CHESTPLATE: SONIC BOOM RESISTANCE
        // -------------------------------------

        if (!hasArmor(
                player,
                EquipmentSlot.CHEST,
                ArmorItem.Type.CHESTPLATE
        )) {
            return;
        }

        if (event.getSource().is(DamageTypes.SONIC_BOOM)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }
}
