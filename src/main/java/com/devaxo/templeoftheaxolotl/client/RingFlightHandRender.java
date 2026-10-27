package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/** Visual-only suppression: the server keeps the actual Ring in the inventory. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class RingFlightHandRender {
    private RingFlightHandRender() {}

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get())) return;
        String marker = "ring_boomerang:" + mc.player.getUUID();
        AABB search = mc.player.getBoundingBox().inflate(12.0);
        boolean inFlight = !mc.level.getEntitiesOfClass(Display.ItemDisplay.class, search,
                entity -> entity.hasCustomName() && marker.equals(entity.getCustomName().getString())).isEmpty();
        if (inFlight && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            event.setCanceled(true);
        }
    }
}
