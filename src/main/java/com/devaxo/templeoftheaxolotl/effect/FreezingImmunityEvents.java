
package com.devaxo.templeoftheaxolotl.effect;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class FreezingImmunityEvents {

    private FreezingImmunityEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        if (player.hasEffect(ModEffects.FREEZING_IMMUNITY)) {
            player.setTicksFrozen(0);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        var entity = event.getEntity();

        if (!entity.hasEffect(ModEffects.FREEZING_IMMUNITY)) {
            return;
        }

        if (event.getSource().is(DamageTypes.FREEZE)) {
            event.setCanceled(true);
        }
    }
}
