package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Prevent friendly damage to tameables, with owner-only sneak + direct melee override. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class UniversalCompanionFriendlyFire {
    private UniversalCompanionFriendlyFire() {}

    @SubscribeEvent public static void protect(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal victim) || !victim.isTame()) return;
        Entity credited = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (credited == null) return; // Keep ordinary environmental and hostile damage.

        if (credited instanceof Player player && player.getUUID().equals(victim.getOwnerUUID())) {
            // The override applies ONLY to a direct player melee hit, not arrows or AoE.
            if (direct == player && player.isShiftKeyDown()) return;
            event.setCanceled(true);
            return;
        }
        if (credited instanceof TamableAnimal ally && ally.isTame()
                && ally.getOwnerUUID() != null
                && ally.getOwnerUUID().equals(victim.getOwnerUUID())) {
            event.setCanceled(true);
        }
    }
}
