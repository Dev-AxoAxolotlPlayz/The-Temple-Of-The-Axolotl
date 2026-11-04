package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.network.InventoryStorageOpenPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
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
    private static CelestialQuickButton repositoryButton;
    private static CelestialQuickButton lootbagButton;
    private static CelestialQuickButton modulesButton;

    private InventoryStorageButtons() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory)) return;
        activeScreen = inventory;
        repositoryButton = shortcut("Repository", 0);
        lootbagButton = shortcut("Lootbag", 1);
        modulesButton = shortcut("HUD", 2);
        event.addListener(repositoryButton);
        event.addListener(lootbagButton);
        event.addListener(modulesButton);
        update(inventory);
    }

    private static CelestialQuickButton shortcut(String name, int destination) {
        return new CelestialQuickButton(destination, name, b ->
            PacketDistributor.sendToServer(new InventoryStorageOpenPayload(destination)));
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
        CelestialQuickButton[] buttons = {repositoryButton, lootbagButton, modulesButton};
        // In the horizontal recipe-book layout, shift neighboring buttons to
        // accommodate an expanded label. Never allow buttons to overlap.
        int cursorX = x;
        var mc = net.minecraft.client.Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos() * inventory.width / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * inventory.height / mc.getWindow().getScreenHeight();
        int hoveredIndex = -1;
        if (recipeOpen && my >= y && my < y + 18) {
            for (int i = 0; i < buttons.length; i++) {
                if (mx >= x + i * 20 && mx < x + i * 20 + 18) { hoveredIndex = i; break; }
            }
        }
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].anchor(recipeOpen ? cursorX : x, y + (recipeOpen ? 0 : i * 20), !recipeOpen);
            buttons[i].visible = x >= 0 && y >= 0;
            buttons[i].active = buttons[i].visible;
            if (recipeOpen) cursorX += (i == hoveredIndex ? buttons[i].expandedWidth() : 18) + 2;
        }
    }
}
