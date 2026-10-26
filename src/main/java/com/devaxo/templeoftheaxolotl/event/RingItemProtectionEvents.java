
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID,
        bus = EventBusSubscriber.Bus.GAME
)
public class RingItemProtectionEvents {

    // =========================================
    // CELESTIAL ITEM PROTECTION
    // Ring of the One + Axolotl Absolute
    // =========================================

    private static boolean isProtectedItem(ItemEntity item) {

        return item.getItem().is(
                ModItems.RING_OF_THE_ONE.get()
        ) || item.getItem().is(
                ModItems.AXOLOTL_ABSOLUTE.get()
        ) || item.getItem().is(ModItems.AXOLOTL_CHESTPLATE.get())
          || item.getItem().is(ModItems.AXOLOTL_LEGGINGS.get())
          || item.getItem().is(ModItems.AXOLOTL_BOOTS.get());
    }

    // =========================================
    // DAMAGE IMMUNITY
    // =========================================

    @SubscribeEvent
    public static void onInvulnerabilityCheck(
            EntityInvulnerabilityCheckEvent event
    ) {

        if (!(event.getEntity() instanceof ItemEntity item)) {
            return;
        }

        if (!isProtectedItem(item)) {
            return;
        }

        // Immune to explosions, cactus, and void damage.
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                || event.getSource().is(DamageTypes.CACTUS)
                || event.getSource().is(DamageTypeTags.IS_FIRE)
                || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {

            event.setInvulnerable(true);
        }
    }

    // =========================================
    // ETERNAL EXISTENCE + VOID REJECTION
    // =========================================

    @SubscribeEvent
    public static void onEntityTick(
            EntityTickEvent.Post event
    ) {

        if (!(event.getEntity() instanceof ItemEntity item)) {
            return;
        }

        if (item.level().isClientSide()) {
            return;
        }

        if (!isProtectedItem(item)) {
            return;
        }

        // =====================================
        // EXTENDED DROPPED-ITEM LIFETIME
        // =====================================

        // Effectively prevents normal despawning.
        item.lifespan = Integer.MAX_VALUE;

        // =====================================
        // VOID REJECTION
        // =====================================

        double minY = item.level().getMinBuildHeight();

        // Start rising before entering the void.
        if (item.getY() < minY + 4) {

            item.setNoGravity(true);

            item.setDeltaMovement(
                    0.0,
                    0.35,
                    0.0
            );

            item.hasImpulse = true;

        } else if (item.isNoGravity()) {

            // Restore normal gravity.
            item.setNoGravity(false);
        }
    }
}
