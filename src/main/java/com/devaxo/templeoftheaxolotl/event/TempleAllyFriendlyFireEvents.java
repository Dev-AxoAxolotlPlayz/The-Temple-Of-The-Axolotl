package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.maze.TempleDimensionManager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** A single Temple is active at once: tagged participants cannot harm one another. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class TempleAllyFriendlyFireEvents {
    private TempleAllyFriendlyFireEvents() {}

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)
                || !victim.getTags().contains(TempleDimensionManager.IN_TEMPLE_TAG)) return;
        // DamageSource#getEntity resolves the credited attacker for arrows and
        // other ordinary player-owned projectiles as well as direct melee.
        if (event.getSource().getEntity() instanceof Player attacker
                && attacker != victim
                && attacker.getTags().contains(TempleDimensionManager.IN_TEMPLE_TAG)) {
            event.setCanceled(true);
        }
    }
}
