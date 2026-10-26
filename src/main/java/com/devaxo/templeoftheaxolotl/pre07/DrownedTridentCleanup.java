package com.devaxo.templeoftheaxolotl.pre07;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Remove drowned-fired tridents exactly 20 ticks after they lodge in a block. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class DrownedTridentCleanup {
    private DrownedTridentCleanup() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ThrownTrident trident) || trident.level().isClientSide()) return;
        if (!(trident.getOwner() instanceof Drowned)) return;
        // Only count down while embedded; a projectile in flight remains unaffected.
        if (!trident.isNoPhysics() && trident.getDeltaMovement().lengthSqr() > 0.0001D) {
            return;
        }
        var data = trident.getPersistentData();
        int landedTicks = data.getInt("tota_drowned_landed_ticks") + 1;
        data.putInt("tota_drowned_landed_ticks", landedTicks);
        if (landedTicks >= 20) trident.discard();
    }
}
