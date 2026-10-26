package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.network.InventoryStorageOpenPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Inventory shortcuts, aligned with the armor slots and hidden while the recipe book is open. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class InventoryStorageButtons {
    private static InventoryScreen activeScreen;
    private static Button repositoryButton;
    private static Button lootbagButton;
    private static Button modulesButton;

    private InventoryStorageButtons() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory)) return;
        activeScreen = inventory;
        repositoryButton = shortcut("R", "Celestial Repository", 0);
        lootbagButton = shortcut("L", "Dungeon Lootbag", 1);
        modulesButton = shortcut("H", "Celestial HUD Modules", 2);
        event.addListener(repositoryButton);
        event.addListener(lootbagButton);
        event.addListener(modulesButton);
        update(inventory);
    }

    private static Button shortcut(String letter, String tooltip, int destination) {
        return Button.builder(Component.literal(letter), b ->
                PacketDistributor.sendToServer(new InventoryStorageOpenPayload(destination)))
                .bounds(0, 0, 18, 18)
                .tooltip(Tooltip.create(Component.literal(tooltip)))
                .build();
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof InventoryScreen inventory && inventory == activeScreen) {
            update(inventory);
        }
    }

    private static void update(InventoryScreen inventory) {
        if (repositoryButton == null || lootbagButton == null || modulesButton == null) return;
        boolean recipeOpen = inventory.getRecipeBookComponent().isVisible();
        // Recipe-book expansion shifts the inventory; pin shortcuts to its top edge.
        // Otherwise preserve the field-tested vertical column beside armor slots.
        int x = recipeOpen ? inventory.getGuiLeft() + 8 : inventory.getGuiLeft() - 20;
        int y = recipeOpen ? inventory.getGuiTop() - 20 : inventory.getGuiTop() + 8;
        Button[] buttons = {repositoryButton, lootbagButton, modulesButton};
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setX(x + (recipeOpen ? i * 20 : 0));
            buttons[i].setY(y + (recipeOpen ? 0 : i * 20));
            buttons[i].visible = x >= 0 && y >= 0;
            buttons[i].active = buttons[i].visible;
        }
    }
}
