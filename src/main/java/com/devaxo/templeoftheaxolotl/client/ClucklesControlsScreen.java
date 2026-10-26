package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.compass.CelestialModulesMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dedicated Cluckles management view; reuses the existing server-authoritative module menu. */
public final class ClucklesControlsScreen extends CelestialModulesScreen {
    public ClucklesControlsScreen(CelestialModulesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
    @Override protected void init() {
        super.init();
        addControl("Inbound", 20, 92, 30, 67);
        addControl("Outbound", 21, 165, 30, 67);
        addControl("Radius", 22, 92, 55, 67);
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int mx, int my) {
        super.renderBg(g, tick, mx, my);
        g.fill(leftPos + 81, topPos + 3, leftPos + 249, topPos + 93, 0xF00B1528);
        g.drawString(font, "CLUCKLES LOGISTICS", leftPos + 90, topPos + 9, 0xFFB6F6FF, false);
        g.drawString(font, "IN " + (menu.inbound() ? "ON" : "OFF") + "  OUT " + (menu.outbound() ? "ON" : "OFF") + "  RADIUS " + menu.radius(), leftPos + 90, topPos + 79, 0xFFE9E5FF, false);
        g.drawString(font, menu.logisticsEnabled() ? "Module: Ready" : "Requires Logistics Maestro", leftPos + 90, topPos + 20, 0xFFE9E5FF, false);
    }
}
