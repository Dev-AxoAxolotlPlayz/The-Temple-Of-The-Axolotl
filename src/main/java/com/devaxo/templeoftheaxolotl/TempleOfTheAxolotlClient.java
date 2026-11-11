
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
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(
                com.devaxo.templeoftheaxolotl.kiln.KilnRegistries.MENU.get(),
                com.devaxo.templeoftheaxolotl.kiln.AnnealingKilnScreen::new
        );
    }

    // Register Axo's GeckoLib renderer!
    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(com.devaxo.templeoftheaxolotl.lore.LoreBlockEntities.LORE.get(), com.devaxo.templeoftheaxolotl.client.renderer.LoreBlockRenderer::new);
        event.registerBlockEntityRenderer(com.devaxo.templeoftheaxolotl.abyss.WatergateEntities.WATERGATE.get(), com.devaxo.templeoftheaxolotl.client.renderer.WatergateRenderer::new);
        event.registerBlockEntityRenderer(com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.ENTITY.get(), com.devaxo.templeoftheaxolotl.client.renderer.CosmicBrewingRenderer::new);
        event.registerBlockEntityRenderer(com.devaxo.templeoftheaxolotl.eclipse.EclipseBlockEntities.ALTAR.get(), com.devaxo.templeoftheaxolotl.client.renderer.CelestialAnvilRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSSAL_AXOLOTL.get(),
                com.devaxo.templeoftheaxolotl.client.renderer.AbyssalAxolotlRenderer::new);
        event.registerEntityRenderer(ModEntities.AXOBLOOP.get(), com.devaxo.templeoftheaxolotl.client.renderer.AxobloopRenderer::new);
        event.registerEntityRenderer(ModEntities.MEGOLOTL.get(), com.devaxo.templeoftheaxolotl.client.renderer.MegolotlRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSSAL_JELLYFISH.get(), com.devaxo.templeoftheaxolotl.client.renderer.AbyssalJellyfishRenderer::new);
        event.registerEntityRenderer(ModEntities.GHOSTFISH.get(), com.devaxo.templeoftheaxolotl.client.renderer.GhostfishRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSSAL_ANGLER.get(), com.devaxo.templeoftheaxolotl.client.renderer.AbyssalAnglerRenderer::new);
        event.registerEntityRenderer(ModEntities.AXOKRAKEN.get(), com.devaxo.templeoftheaxolotl.client.renderer.AxokrakenRenderer::new);
        event.registerEntityRenderer(ModEntities.MEROLOTL.get(), com.devaxo.templeoftheaxolotl.client.renderer.MerolotlRenderer::new);
        event.registerEntityRenderer(ModEntities.AXOLEVIATHAN.get(),com.devaxo.templeoftheaxolotl.client.renderer.AxoleviathanRenderer::new);
        event.registerEntityRenderer(ModEntities.SHORKOLOTL_100.get(),
                com.devaxo.templeoftheaxolotl.client.renderer.Shorkolotl100Renderer::new);
        event.registerEntityRenderer(ModEntities.WAIFU_AXOLUTHU.get(),
                com.devaxo.templeoftheaxolotl.client.renderer.WaifuAxoluthuRenderer::new);
        event.registerEntityRenderer(ModEntities.CLUCKLES.get(),
                com.devaxo.templeoftheaxolotl.client.renderer.ClucklesRenderer::new);
        event.registerEntityRenderer(ModEntities.WRATH_HEAD_HITBOX.get(),
                com.devaxo.templeoftheaxolotl.client.renderer.WrathHeadHitboxRenderer::new);
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
