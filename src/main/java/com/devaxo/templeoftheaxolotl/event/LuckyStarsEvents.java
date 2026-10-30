
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.enchantment.Enchantment;

import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public class LuckyStarsEvents {

    // =========================================
    // LUCKY STARS I - TRIPLE SCAVENGER
    // =========================================

    // Three independent chances:
    // 50% for the first bonus loot table
    // 25% for the second bonus loot table
    // 10% for the third bonus loot table
    //
    // Maximum: 4 total loot-table rolls,
    // including Minecraft's original roll.

    private static final float[] SCAVENGER_CHANCES = {
            0.50F,
            0.25F,
            0.10F
    };

    private static final ResourceKey<Enchantment> LUCKY_STARS =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "lucky_stars"
                    )
            );

    // =========================================
    // ENCHANTMENT DETECTION
    // =========================================

    private static boolean hasLuckyStars(
            ServerPlayer player
    ) {

        ItemStack ring = player.getMainHandItem();

        if (!ring.is(ModItems.RING_OF_THE_ONE.get())) {
            return false;
        }

        Holder<Enchantment> enchantment =
                player.level()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolder(LUCKY_STARS)
                        .orElse(null);

        return enchantment != null
                && ring.getEnchantmentLevel(enchantment) > 0;
    }

    // =========================================
    // MOB DEATH - SCAVENGER ROLLS
    // =========================================

    @SubscribeEvent
    public static void onDrops(
            LivingDropsEvent event
    ) {

        LivingEntity victim = event.getEntity();

        // Server-side only.
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }

        // Only direct player-attributed kills.
        //
        // Regular Ring kills: YES
        // Event Horizon executions: YES
        // Supernova fire deaths: NO

        if (!(event.getSource().getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        // Must be holding the enchanted Ring.
        if (!hasLuckyStars(player)) {
            return;
        }

        // =====================================
        // CREATE NORMAL LOOT CONTEXT
        // =====================================

        LootParams params =
                new LootParams.Builder(level)

                        .withParameter(
                                LootContextParams.THIS_ENTITY,
                                victim
                        )

                        .withParameter(
                                LootContextParams.ORIGIN,
                                victim.position()
                        )

                        .withParameter(
                                LootContextParams.DAMAGE_SOURCE,
                                event.getSource()
                        )

                        .withOptionalParameter(
                                LootContextParams.ATTACKING_ENTITY,
                                player
                        )

                        .withOptionalParameter(
                                LootContextParams.DIRECT_ATTACKING_ENTITY,
                                event.getSource().getDirectEntity()
                        )

                        .withOptionalParameter(
                                LootContextParams.LAST_DAMAGE_PLAYER,
                                player
                        )

                        .withLuck(player.getLuck())

                        .create(LootContextParamSets.ENTITY);

        // =====================================
        // GET VICTIM'S ORIGINAL LOOT TABLE
        // =====================================

        LootTable table =
                level.getServer()
                        .reloadableRegistries()
                        .getLootTable(
                                victim.getLootTable()
                        );

        // =====================================
        // TRIPLE SCAVENGER
        // =====================================

        // Each chance is independent.
        //
        // Successful rolls generate entirely
        // new drops from the same loot table.
        //
        // The original Minecraft loot roll
        // remains untouched.

        int highestSuccessfulRoll = 0;
        for (int roll = 0; roll < SCAVENGER_CHANCES.length; roll++) {
            float chance = SCAVENGER_CHANCES[roll];

            // Failed proc: skip this bonus roll.
            if (level.random.nextFloat() >= chance) {
                continue;
            }

            highestSuccessfulRoll = roll + 1;
            // Successful proc:
            // generate a fresh complete loot roll.

            for (ItemStack stack : table.getRandomItems(params)) {

                if (stack.isEmpty()) {
                    continue;
                }

                ItemEntity bonusDrop =
                        new ItemEntity(
                                level,
                                victim.getX(),
                                victim.getY(),
                                victim.getZ(),
                                stack
                        );

                // Add to the existing drops.
                // Do not remove or replace
                // the original loot.

                event.getDrops().add(bonusDrop);
            }
        }
        // Exactly one harmless starburst, using the highest successful bonus roll.
        if (highestSuccessfulRoll > 0) {
            double x = victim.getX();
            double y = victim.getY() + victim.getBbHeight() * 0.6;
            double z = victim.getZ();
            // Dust colors: lavender, soft white, cyan (RGB floats).
            float r = highestSuccessfulRoll == 1 ? 0.66f : highestSuccessfulRoll == 2 ? 0.95f : 0.28f;
            float g = highestSuccessfulRoll == 1 ? 0.42f : highestSuccessfulRoll == 2 ? 0.93f : 0.89f;
            float b = highestSuccessfulRoll == 1 ? 0.88f : highestSuccessfulRoll == 2 ? 1.00f : 1.00f;
            var dust = new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(r, g, b), 1.15f);
            // Particle positions form a six-point star in the horizontal plane.
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2.0 * i / 12.0;
                double radius = (i % 2 == 0) ? 0.72 : 0.33;
                level.sendParticles(dust, x + Math.cos(angle) * radius,
                        y + Math.sin(angle) * radius, z,
                        2, 0.035, 0.035, 0.035, 0.005);
            }
            level.sendParticles(ParticleTypes.END_ROD, x, y, z,
                    7, 0.25, 0.25, 0.25, 0.025);
            level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_TWINKLE,
                    SoundSource.PLAYERS, 0.55f, 1.3f);
        }
    }
}
