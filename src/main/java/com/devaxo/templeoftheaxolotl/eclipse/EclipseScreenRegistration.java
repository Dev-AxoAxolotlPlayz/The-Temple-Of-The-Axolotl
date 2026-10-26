package com.devaxo.templeoftheaxolotl.eclipse;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EclipseScreenRegistration {
    private EclipseScreenRegistration() {}
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(EclipseMenus.TRANSMUTATION.get(), EclipseScreen::new);
        event.register(EclipseMenus.INSCRIPTION.get(), EclipseScreen::new);
    }
}
