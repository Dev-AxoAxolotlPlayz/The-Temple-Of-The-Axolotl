
package com.devaxo.templeoftheaxolotl.shipyard;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ShipyardClient {

 private ShipyardClient() {}

 public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
  event.registerBlockEntityRenderer(
          ShipyardRegistries.CRANE.get(),
          ShipyardCraneRenderer::new
  );
 }

 public static void screens(RegisterMenuScreensEvent event) {
  event.register(
          ShipyardRegistries.CRANE_MENU.get(),
          ShipyardScreen::new
  );
 }
}
