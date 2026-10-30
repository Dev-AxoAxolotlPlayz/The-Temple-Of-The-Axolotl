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
        imageWidth = 270;
        imageHeight = 188;
        titleLabelX = 300; // Suppress vanilla title; custom title is rendered in the control area.
        titleLabelY = 8;
        inventoryLabelX = 101;
        inventoryLabelY = 93;
    }
    @Override protected void init() {
        super.init();
        if (this instanceof ClucklesControlsScreen) return;
        // 18px icon controls: Previous, Play/Pause, Next, Shuffle, Repeat and Magnet.
        addIcon("|<", 15, 108, 25, "Previous disc");
        addIcon(menu.paused() || menu.playing() < 0 ? ">" : "||", 10, 128, 25, "Play / Pause");
        addIcon(">|", 13, 148, 25, "Next disc");
        addIcon("S", 11, 176, 25, "Shuffle on / off");
        addIcon("R", 12, 196, 25, "Repeat: off / all / one");
        addIcon("", 14, 228, 25, "Magnet on / off");
        addIcon("⛏", 16, 228, 48, "Mining mode: on / off");
        addIcon("", 17, 228, 71, "Helmet Night Vision: on / off");
        addIcon("", 18, 207, 71, "Helmet Conduit Power: on / off");
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
        g.fill(leftPos + 37, topPos + 3, leftPos + 91, topPos + 184, 0xB0253A55);
        for (int i = 0; i < 9; i++) {
            int y = topPos + 16 + i * 18;
            g.fill(leftPos + 44, y, leftPos + 62, y + 18, 0xFF7796B5);
            g.fill(leftPos + 45, y + 1, leftPos + 61, y + 17, 0xFF14243A);
        }
        for (int i = 0; i < 9; i++) {
            int sy = topPos + 16 + i * 18;
            g.fill(leftPos + 63, sy, leftPos + 81, sy + 18, 0xFF7796B5);
            g.fill(leftPos + 64, sy + 1, leftPos + 80, sy + 17, 0xFF14243A);
        }
        if (menu.jukeboxInstalled()) {
            g.fill(leftPos + 10, topPos + 3, leftPos + 37, topPos + 184, 0xB0253A55);
            for (int i = 0; i < 9; i++) {
                int y = topPos + 16 + i * 18;
                g.fill(leftPos + 18, y, leftPos + 36, y + 18, 0xFFD4B776);
                g.fill(leftPos + 19, y + 1, leftPos + 35, y + 17, 0xFF14243A);
            }
        }
        g.fill(leftPos + 99, topPos + 3, leftPos + 267, topPos + 93, 0xD00B1528);
        // Draw controls as icon-only, with illuminated state indicated by a colored outline.
        if (menu.jukeboxInstalled()) {
            g.drawString(font, "JUKEBOX", leftPos + 108, topPos + 8, 0xFFB6F6FF, false);
            String current = menu.playing() < 0 ? "None" : "Slot " + (menu.playing() + 1);
            String next = menu.nextUp() == -2 ? "Random" : menu.nextUp() < 0 ? "None" : "Slot " + (menu.nextUp() + 1);
            g.drawString(font, "Now: " + current + (menu.paused() ? " (paused)" : ""), leftPos + 108, topPos + 54, 0xFFE9E5FF, false);
            g.drawString(font, "Next: " + next, leftPos + 108, topPos + 69, 0xFFE9E5FF, false);
            if (menu.shuffle()) g.renderOutline(leftPos + 175, topPos + 24, 20, 20, 0xFF68F4CA);
            if (menu.repeat() != 0) g.renderOutline(leftPos + 195, topPos + 24, 20, 20, 0xFF68F4CA);
            if (menu.repeat() == 2) g.drawString(font, "1", leftPos + 207, topPos + 34, 0xFFFFFF8C, true);
        } else {
            g.drawString(font, "CELESTIAL MODULES", leftPos + 108, topPos + 8, 0xFFB6F6FF, false);
        }
        // Custom pixel-art icons, avoiding missing Unicode glyphs in Minecraft's font.
        RingAttackInput.setMiningMode(menu.miningMode());
        if (menu.miningMode()) g.renderOutline(leftPos + 227, topPos + 47, 20, 20, 0xFF68F4CA);
        if (menu.helmetConduitEnabled()) g.renderOutline(leftPos + 206, topPos + 70, 20, 20, 0xFF68F4CA);
        if (menu.helmetNightVisionEnabled()) g.renderOutline(leftPos + 227, topPos + 70, 20, 20, 0xFF68F4CA);
        if (menu.magnetMode() != 2) g.renderOutline(leftPos + 227, topPos + 24, 20, 20, 0xFF68F4CA);
        for (int i = 0; i < 36; i++) {
            int x = leftPos + 100 + (i % 9) * 18;
            int y = topPos + (i < 27 ? 103 + (i / 9) * 18 : 161);
            g.fill(x, y, x + 18, y + 18, 0xFF71849B);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF182538);
        }
    }
    private static void drawMagnet(GuiGraphics g, int x, int y) {
        int c = 0xFFE2B8FF;
        g.fill(x+5,y+4,x+8,y+13,c); g.fill(x+11,y+4,x+14,y+13,c);
        g.fill(x+7,y+11,x+12,y+14,c);
        g.fill(x+5,y+4,x+8,y+7,0xFFFF7474); g.fill(x+11,y+4,x+14,y+7,0xFF8CE9FF);
    }
    private static void drawMoon(GuiGraphics g, int x, int y) {
        // A 12x12 pixel crescent, cut out with the button's own face shade.
        int moon = 0xFFE6E8FF;
        int face = 0xFF686868;
        for (int dy = 3; dy <= 14; dy++) for (int dx = 3; dx <= 14; dx++) {
            int xx = dx - 8, yy = dy - 8;
            if (xx * xx + yy * yy <= 33 && (dx - 11) * (dx - 11) + (dy - 5) * (dy - 5) > 25)
                g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, moon);
        }
    }
    private static void drawFish(GuiGraphics g, int x, int y) {
        int c=0xFF8CEFFF;
        g.fill(x+5,y+7,x+13,y+12,c);
        g.fill(x+3,y+5,x+6,y+14,c);
        g.fill(x+11,y+8,x+15,y+11,0xFFBFFAFF);
        g.fill(x+10,y+8,x+12,y+10,0xFF14243A);
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
        // Draw icon silhouettes AFTER buttons so the button faces cannot cover them.
        drawMagnet(g, leftPos + 228, topPos + 25);
        drawMoon(g, leftPos + 228, topPos + 71);
        drawFish(g, leftPos + 207, topPos + 71);
        renderTooltip(g, mx, my);
    }
}
