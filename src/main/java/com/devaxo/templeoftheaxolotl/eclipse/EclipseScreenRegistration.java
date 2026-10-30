package com.devaxo.templeoftheaxolotl.eclipse;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class EclipseScreenRegistration {
    private EclipseScreenRegistration() {}
    public static void register(RegisterMenuScreensEvent event) {
        event.register(EclipseMenus.TRANSMUTATION.get(), EclipseScreen::new);
        event.register(EclipseMenus.INSCRIPTION.get(), EclipseScreen::new);
    }
}
