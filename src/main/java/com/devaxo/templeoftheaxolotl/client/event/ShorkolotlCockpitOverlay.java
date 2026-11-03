package com.devaxo.templeoftheaxolotl.client.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Lightweight translucent light-blue cockpit glass for the local pilot only. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class ShorkolotlCockpitOverlay {
    private ShorkolotlCockpitOverlay() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof Shorkolotl100Entity)
                || !mc.options.getCameraType().isFirstPerson()) return;
        var gui = event.getGuiGraphics();
        int w = gui.guiWidth(), h = gui.guiHeight();
        gui.fill(0, 0, w, h, 0x174EB9DB); // subtle translucent blue glass
        int rim = Math.max(5, Math.min(w, h) / 65);
        gui.fill(0, 0, w, rim, 0x8064BED4);
        gui.fill(0, h - rim, w, h, 0x8064BED4);
        gui.fill(0, 0, rim, h, 0x8064BED4);
        gui.fill(w - rim, 0, w, h, 0x8064BED4);
    }
}
