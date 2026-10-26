package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Eight rows of seven item variants, with authoritative bulk quantities. */
public final class CelestialRepositoryScreen extends AbstractContainerScreen<CelestialRepositoryMenu> {
    private static final ResourceLocation ABYSS = ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "textures/gui/ethereal_abyss.png");
    public CelestialRepositoryScreen(CelestialRepositoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 326;
        imageHeight = 189;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelX = 155;
        inventoryLabelY = 93;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Page"), button -> {
                if (minecraft != null && minecraft.gameMode != null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            }).bounds(leftPos + 260, topPos + 6, 58, 16).build());
    }
    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int mouseX, int mouseY) {
        g.blit(ABYSS, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        // Opaque label plates preserve readability against luminous ribbons.
        g.fill(leftPos + 5, topPos + 5, leftPos + 151, topPos + 21, 0xE0071024);
        g.fill(leftPos + 152, topPos + 88, leftPos + 250, topPos + 102, 0xE0071024);
        g.fill(leftPos + 5, topPos + 169, leftPos + 148, topPos + 187, 0xE0071024);
        for (int i = 0; i < 56; i++) {
            int x = leftPos + 7 + (i % 7) * 18;
            int y = topPos + 25 + (i / 7) * 18;
            g.fill(x, y, x + 18, y + 18, 0xFF7286B0);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xDB0B1129);
        }
        if (menu.hasCrafting()) {
            g.drawString(font, "CRAFTING", leftPos + 158, topPos + 14, 0xFFE7EAFF, false);
            for (int i = 0; i < 9; i++) slotFrame(g, 158 + (i % 3) * 18, 26 + (i / 3) * 18);
            slotFrame(g, 228, 44);
            g.drawString(font, ">", leftPos + 216, topPos + 49, 0xFFB5EAFB, false);
        }
        if (menu.hasAnvil()) {
            g.drawString(font, "ANVIL", leftPos + 272, topPos + 14, 0xFFE7EAFF, false);
            slotFrame(g, 276, 28);
            slotFrame(g, 276, 64);
            g.drawString(font, "v", leftPos + 281, topPos + 50, 0xFFB5EAFB, false);
        }
        for (int i = 0; i < 36; i++) {
            int x = leftPos + 154 + (i % 9) * 18;
            int y = topPos + (i < 27 ? 103 + (i / 9) * 18 : 161);
            g.fill(x, y, x + 18, y + 18, 0xFF7286B0);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xDB0B1129);
        }
        if (menu.hasSecondPage())
            g.drawString(font, "PAGE " + (menu.activePage() + 1) + "/2", leftPos + 215, topPos + 7, 0xFFB5EAFB, false);
        g.drawString(font, "L: TAKE  R: STORE", leftPos + 8, topPos + 173, 0xFFB5EAFB, false);
    }
    private void slotFrame(GuiGraphics g, int x, int y) {
        g.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF7286B0);
        g.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xDB0B1129);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFE9E5FF, true);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE9E5FF, true);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g, mx, my, partialTick);
        super.render(g, mx, my, partialTick);
        for (int i = 0; i < 56; i++) {
            int count = menu.quantity(i);
            if (count <= 0) continue;
            int x = leftPos + 8 + (i % 7) * 18;
            int y = topPos + 26 + (i / 7) * 18;
            String label = count >= 1000 ? String.format("%.1fk", count / 1000.0) : Integer.toString(count);
            g.pose().pushPose();
            g.pose().translate(x + 17, y + 13, 250);
            g.pose().scale(0.57f, 0.57f, 1);
            g.drawString(font, label, -font.width(label), 0, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
        renderTooltip(g, mx, my);
    }
}
