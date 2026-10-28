
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AboveAllsGraceEvents {

    private static final ResourceKey<Enchantment> GRACE_KEY =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "above_alls_grace"
                    )
            );

    private static final ResourceLocation SWIM_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(
                    TempleOfTheAxolotl.MODID,
                    "above_alls_grace_swim_bonus"
            );

    // Additional swimming boost beyond the
    // maximum vanilla Depth Strider efficiency.
    private static final double SWIM_BONUS = 0.15D;

    private AboveAllsGraceEvents() {
    }

    private static boolean hasGrace(Player player) {

        ItemStack boots = player.getItemBySlot(
                EquipmentSlot.FEET
        );

        if (!boots.is(ModItems.AXOLOTL_BOOTS.get())) {
            return false;
        }

        var registry = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);

        var grace = registry.getOrThrow(GRACE_KEY);

        return EnchantmentHelper.getEnchantmentsForCrafting(boots).getLevel(grace) > 0;
    }

    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        if (event.getSource().getEntity() instanceof com.devaxo.templeoftheaxolotl.entity.BossWrathEntity) return;
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        if (!hasGrace(player)) {
            return;
        }

        // Complete immunity to cactus contact damage.
        if (event.getSource().is(DamageTypes.CACTUS)) {
            event.setCanceled(true);
            return;
        }

        // Complete immunity to magma block floor damage.
        if (event.getSource().is(DamageTypes.HOT_FLOOR)) {
            event.setCanceled(true);
            return;
        }

        // Complete immunity to sweet berry bush damage.
        if (event.getSource().is(DamageTypes.SWEET_BERRY_BUSH)) {
            event.setCanceled(true);
            return;
        }

        // Reduce fall damage by 10%.
        if (event.getSource().is(DamageTypes.FALL)) {
            event.setAmount(event.getAmount() * 0.90F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        AttributeInstance swimAttribute =
                player.getAttribute(NeoForgeMod.SWIM_SPEED);

        if (swimAttribute == null) {
            return;
        }

        boolean shouldBoost =
                hasGrace(player)
                        && player.isInWater()
                        && player.isSwimming();

        boolean hasBoost =
                swimAttribute.hasModifier(SWIM_BONUS_ID);

        if (shouldBoost && !hasBoost) {

            swimAttribute.addTransientModifier(
                    new AttributeModifier(
                            SWIM_BONUS_ID,
                            SWIM_BONUS,
                            AttributeModifier.Operation
                                    .ADD_MULTIPLIED_TOTAL
                    )
            );

        } else if (!shouldBoost && hasBoost) {

            swimAttribute.removeModifier(SWIM_BONUS_ID);
        }
    }
}
