
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID
)
public class RingCombatEvents {

    private static final float LIFESTEAL = 0.01F;
    private static final float EXECUTION_THRESHOLD = 0.15F;
    private static final float EXECUTION_CHANCE = 0.1F;

    private static final ResourceKey<Enchantment> EVENT_HORIZON =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "event_horizon"
                    )
            );

    private static final Set<UUID> PENDING_EXECUTIONS =
            new HashSet<>();

    private static Player getRingAttacker(
            LivingDamageEvent event
    ) {
        if (!(event instanceof LivingDamageEvent.Pre)
                && !(event instanceof LivingDamageEvent.Post)) {
            return null;
        }

        if (!(event.getEntity().level()
                .getServer() != null)) {
            return null;
        }

        if (!(getAttacker(event) instanceof Player player)) {
            return null;
        }

        if (player.getMainHandItem().is(
                ModItems.RING_OF_THE_ONE.get()
        )) {
            return player;
        }

        return null;
    }

    private static net.minecraft.world.entity.Entity getAttacker(
            LivingDamageEvent event
    ) {
        if (event instanceof LivingDamageEvent.Pre pre) {
            return pre.getSource().getDirectEntity();
        }

        if (event instanceof LivingDamageEvent.Post post) {
            return post.getSource().getDirectEntity();
        }

        return null;
    }

    private static boolean hasEventHorizon(
            Player player
    ) {
        ItemStack ring = player.getMainHandItem();

        Holder<Enchantment> enchantment =
                player.level()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(EVENT_HORIZON);

        return EnchantmentHelper.getEnchantmentsForCrafting(ring).getLevel(enchantment) > 0;
    }

    @SubscribeEvent
    public static void onDamagePre(
            LivingDamageEvent.Pre event
    ) {
        Player player = getRingAttacker(event);

        if (player == null) {
            return;
        }

        LivingEntity target = event.getEntity();

        if (target.getType() == EntityType.WITHER
                || target.getType() == EntityType.ENDER_DRAGON) {
            return;
        }

        if (!hasEventHorizon(player)) {
            return;
        }

        if (target.getHealth() <= 0
                || target.getMaxHealth() <= 0) {
            return;
        }

        // Strictly below 10%, measured before this hit.
        if (target.getHealth() >=
                target.getMaxHealth() * EXECUTION_THRESHOLD) {
            return;
        }

        if (event.getNewDamage() <= 0) {
            return;
        }

        // 2% execution chance.
        if (player.getRandom().nextFloat()
                >= EXECUTION_CHANCE) {
            return;
        }

        // This event occurs after damage reductions.
        // Deal enough final damage to defeat the target.
        event.setNewDamage(
                Math.max(
                        event.getNewDamage(),
                        target.getHealth()
                                + target.getAbsorptionAmount()
                )
        );

        PENDING_EXECUTIONS.add(target.getUUID());
    }

    @SubscribeEvent
    public static void onDamagePost(
            LivingDamageEvent.Post event
    ) {
        LivingEntity target = event.getEntity();

        boolean execution =
                PENDING_EXECUTIONS.remove(target.getUUID());

        Player player = getRingAttacker(event);

        if (player == null) {
            return;
        }

        // Final health damage, capped by target's max health.
        float damage = Math.max(
                0.0F,
                event.getNewDamage()
        );

        if (damage <= 0) {
            return;
        }

        // Innate Yang lifesteal.
        player.heal(damage * LIFESTEAL);

        // Awakened Yin singularity.
        if (execution && !target.isAlive()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
