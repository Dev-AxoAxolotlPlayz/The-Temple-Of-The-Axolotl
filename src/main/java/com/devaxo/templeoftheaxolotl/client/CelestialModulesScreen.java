package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.compass.CelestialModulesMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact vertical module rack with the player inventory beside it. */
public class CelestialModulesScreen extends AbstractContainerScreen<CelestialModulesMenu> {
    private static final net.minecraft.resources.ResourceLocation ABYSS = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "textures/gui/ethereal_abyss.png");

    public CelestialModulesScreen(CelestialModulesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 252;
        imageHeight = 188;
        titleLabelX = 300; // Suppress vanilla title; custom title is rendered in the control area.
        titleLabelY = 8;
        inventoryLabelX = 83;
        inventoryLabelY = 93;
    }
    @Override protected void init() {
        super.init();
        if (this instanceof ClucklesControlsScreen) return;
        // 18px icon controls: Previous, Play/Pause, Next, Shuffle, Repeat and Magnet.
        addIcon("|<", 15, 90, 25, "Previous disc");
        addIcon(menu.paused() || menu.playing() < 0 ? ">" : "||", 10, 110, 25, "Play / Pause");
        addIcon(">|", 13, 130, 25, "Next disc");
        addIcon("S", 11, 158, 25, "Shuffle on / off");
        addIcon("R", 12, 178, 25, "Repeat: off / all / one");
        addIcon("M", 14, 210, 25, "Magnet on / off");
    }
    protected void addIcon(String glyph, int id, int x, int y, String tooltip) {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(glyph), b -> clickControl(id))
                .bounds(leftPos + x, topPos + y, 18, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(tooltip))).build());
    }
    protected void addControl(String name, int id, int x, int y, int width) {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(name), b -> clickControl(id))
                .bounds(leftPos + x, topPos + y, width, 17).build());
    }
    protected void clickControl(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int mouseX, int mouseY) {
        g.blit(ABYSS, leftPos, topPos, 0, 0, imageWidth, imageHeight, 326, 189);

        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0x700C1725);
        g.fill(leftPos + 37, topPos + 3, leftPos + 72, topPos + 184, 0xB0253A55);
        for (int i = 0; i < 9; i++) {
            int y = topPos + 16 + i * 18;
            g.fill(leftPos + 44, y, leftPos + 62, y + 18, 0xFF7796B5);
            g.fill(leftPos + 45, y + 1, leftPos + 61, y + 17, 0xFF14243A);
        }
        if (menu.jukeboxInstalled()) {
            g.fill(leftPos + 10, topPos + 3, leftPos + 37, topPos + 184, 0xB0253A55);
            for (int i = 0; i < 9; i++) {
                int y = topPos + 16 + i * 18;
                g.fill(leftPos + 18, y, leftPos + 36, y + 18, 0xFFD4B776);
                g.fill(leftPos + 19, y + 1, leftPos + 35, y + 17, 0xFF14243A);
            }
        }
        g.fill(leftPos + 81, topPos + 3, leftPos + 249, topPos + 93, 0xD00B1528);
        // Draw controls as icon-only, with illuminated state indicated by a colored outline.
        if (menu.jukeboxInstalled()) {
            g.drawString(font, "JUKEBOX", leftPos + 90, topPos + 8, 0xFFB6F6FF, false);
            String current = menu.playing() < 0 ? "None" : "Slot " + (menu.playing() + 1);
            String next = menu.nextUp() == -2 ? "Random" : menu.nextUp() < 0 ? "None" : "Slot " + (menu.nextUp() + 1);
            g.drawString(font, "Now: " + current + (menu.paused() ? " (paused)" : ""), leftPos + 90, topPos + 54, 0xFFE9E5FF, false);
            g.drawString(font, "Next: " + next, leftPos + 90, topPos + 69, 0xFFE9E5FF, false);
            if (menu.shuffle()) g.renderOutline(leftPos + 157, topPos + 24, 20, 20, 0xFF68F4CA);
            if (menu.repeat() != 0) g.renderOutline(leftPos + 177, topPos + 24, 20, 20, 0xFF68F4CA);
            if (menu.repeat() == 2) g.drawString(font, "1", leftPos + 189, topPos + 34, 0xFFFFFF8C, true);
        } else {
            g.drawString(font, "CELESTIAL MODULES", leftPos + 90, topPos + 8, 0xFFB6F6FF, false);
        }
        if (menu.magnetMode() != 2) g.renderOutline(leftPos + 209, topPos + 24, 20, 20, 0xFF68F4CA);
        for (int i = 0; i < 36; i++) {
            int x = leftPos + 82 + (i % 9) * 18;
            int y = topPos + (i < 27 ? 103 + (i / 9) * 18 : 161);
            g.fill(x, y, x + 18, y + 18, 0xFF71849B);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF182538);
        }
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 1 && menu.jukeboxInstalled()) {
            for (int i = 0; i < 9; i++) {
                int x = leftPos + 18, y = topPos + 16 + i * 18;
                if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                    if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // The custom panel renders its own heading. Never draw the inherited
        // menu title (which previously appeared outside the panel).
        g.drawString(font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF777777, false);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        renderBackground(g, mx, my, tick);
        super.render(g, mx, my, tick);
        renderTooltip(g, mx, my);
    }
}
