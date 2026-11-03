package com.devaxo.templeoftheaxolotl.client.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Hide the rider's model in third person and to other clients: morph-style appearance. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class ShorkolotlPilotRenderEvents {
    private ShorkolotlPilotRenderEvents() {}

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof Shorkolotl100Entity) {
            event.setCanceled(true);
        }
    }
}
