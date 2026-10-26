package com.devaxo.templeoftheaxolotl.pre07;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Sentinel tridents may attack players, but cannot become free weapons. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class SentinelProjectileEvents {
    private SentinelProjectileEvents() {}
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ThrownTrident trident)) return;
        if (trident.getOwner() instanceof Drowned drowned
                && drowned.getPersistentData().getBoolean("tota_sentinel_reward")) {
            trident.pickup = AbstractArrow.Pickup.DISALLOWED;
        }
    }
}
