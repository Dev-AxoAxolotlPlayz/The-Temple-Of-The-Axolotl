
package com.devaxo.templeoftheaxolotl;

import com.devaxo.templeoftheaxolotl.client.renderer.AxolotlAbsoluteRenderer;
import com.devaxo.templeoftheaxolotl.entity.ModEntities;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class only loads on the client.
@Mod(value = TempleOfTheAxolotl.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID,
        value = Dist.CLIENT
)
public class TempleOfTheAxolotlClient {

    public TempleOfTheAxolotlClient(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                ConfigurationScreen::new
        );
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        TempleOfTheAxolotl.LOGGER.info("HELLO FROM CLIENT SETUP");
        TempleOfTheAxolotl.LOGGER.info(
                "MINECRAFT NAME >> {}",
                Minecraft.getInstance().getUser().getName()
        );
    }

    // Register Axo's GeckoLib renderer!
    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.ENTITY.get(), com.devaxo.templeoftheaxolotl.client.renderer.CosmicBrewingRenderer::new);
        event.registerBlockEntityRenderer(com.devaxo.templeoftheaxolotl.eclipse.EclipseBlockEntities.ALTAR.get(), com.devaxo.templeoftheaxolotl.client.renderer.CelestialAnvilRenderer::new);
        event.registerEntityRenderer(ModEntities.CLUCKLES.get(),
                com.devaxo.templeoftheaxolotl.client.renderer.ClucklesRenderer::new);
        event.registerEntityRenderer(ModEntities.BOSS_WRATH.get(),
                com.devaxo.templeoftheaxolotl.client.renderer.BossWrathRenderer::new);
        event.registerEntityRenderer(ModEntities.TAMEABLE_WRATH.get(),
                com.devaxo.templeoftheaxolotl.client.renderer.TameableWrathRenderer::new);
        event.registerEntityRenderer(
                ModEntities.AXOLOTL_ABSOLUTE.get(),
                AxolotlAbsoluteRenderer::new
        );
    }
}
