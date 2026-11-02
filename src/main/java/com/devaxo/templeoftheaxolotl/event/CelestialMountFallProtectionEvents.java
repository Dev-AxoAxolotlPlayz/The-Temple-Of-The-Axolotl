package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.AxolotlAbsoluteEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Prevents fall damage from a celestial mount's flight/landing collision. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class CelestialMountFallProtectionEvents {
    private CelestialMountFallProtectionEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide()
                || !event.getSource().is(DamageTypes.FALL)) return;

        if (player.getVehicle() instanceof AxolotlAbsoluteEntity) {
            player.fallDistance = 0.0F;
            event.setCanceled(true);
        }
    }
}
