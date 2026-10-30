package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.loot.TempleRewardMenuTypes;
import com.devaxo.templeoftheaxolotl.compass.CelestialModulesMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class TempleRewardScreenRegistration {
    private TempleRewardScreenRegistration() {}
    public static void register(RegisterMenuScreensEvent event) {
        event.register(TempleRewardMenuTypes.REWARD_VAULT.get(), TempleRewardScreen::new);
        event.register(com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.MENU.get(),com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingScreen::new);
        event.register(com.devaxo.templeoftheaxolotl.compass.CelestialModulesMenuTypes.MODULES.get(),
                TempleRewardScreenRegistration::createModulesScreen);
        event.register(com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenuTypes.REPOSITORY.get(), CelestialRepositoryScreen::new);
    }

    private static CelestialModulesScreen createModulesScreen(CelestialModulesMenu menu, Inventory inventory, Component title) {
        if (title.getString().equals("Cluckles Controls")) {
            return new ClucklesControlsScreen(menu, inventory, title);
        }
        return new CelestialModulesScreen(menu, inventory, title);
    }
}
