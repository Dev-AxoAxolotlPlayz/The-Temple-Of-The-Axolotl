package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Custom combat interpretations for Ring enchantments on a boss, not a player. */
final class SecondFallEnchantments {
    private SecondFallEnchantments() {}

    static void onHit(ItemStack ring, LivingEntity victim, BossWrathEntity attacker) {
        if (ring.isEmpty()) return;
        ItemEnchantments enchants = ring.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchants.entrySet()) {
            var key = entry.getKey().unwrapKey();
            if (key.isEmpty()) continue;
            int level = entry.getIntValue();
            if (level <= 0) continue;
            switch (key.get().location().getPath()) {
                case "fire_aspect", "flame" -> victim.igniteForSeconds(Math.min(20, 2 + 2 * level));
                case "knockback", "punch" -> {
                    double dx = victim.getX() - attacker.getX();
                    double dz = victim.getZ() - attacker.getZ();
                    double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
                    victim.push(dx / len * Math.min(2.5, 0.25 * level), 0.12, dz / len * Math.min(2.5, 0.25 * level));
                    victim.hurtMarked = true;
                }
                case "binding_curse" -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.min(200, 30 * level), Math.min(3, level - 1)));
                case "swift_sneak", "soul_speed", "depth_strider" -> attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, Math.min(4, level - 1)));
                case "respiration", "aqua_affinity", "channeling" -> {
                    if (victim.isInWaterOrRain()) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, Math.min(3, level - 1)));
                }
                case "thorns" -> attacker.heal(Math.min(8, level));
                case "protection", "projectile_protection", "blast_protection", "fire_protection" -> attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, Math.min(3, level / 3)));
                case "unbreaking", "mending" -> attacker.heal(Math.min(5, level * 0.25F));
                case "sharpness", "smite", "bane_of_arthropods", "impaling", "power", "piercing", "density", "breach" -> victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, Math.min(4, level)));
                case "frost_walker" -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, Math.min(5, level)));
                case "feather_falling" -> attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, Math.min(3, level / 2)));
                case "efficiency", "quick_charge", "multishot", "sweeping_edge" -> attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, Math.min(4, level)));
                case "looting", "fortune", "luck_of_the_sea", "lure" -> attacker.heal(Math.min(4F, level * .5F));
                case "wind_burst", "riptide" -> victim.push(0, Math.min(1.2, .2 * level), 0);
                case "event_horizon" -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                case "celestial_convergence" -> victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, Math.min(4, level)));
                case "supernova" -> victim.igniteForSeconds(6);
                case "lucky_stars" -> attacker.heal(3F);
                default -> { /* Other enchantments remain preserved, but have no bespoke boss handler yet. */ }
            }
        }
    }
}
