package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.loot.TempleRewardMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Custom, self-drawn 54-slot reward vault. No vanilla chest inventory UI. */
public final class TempleRewardScreen extends AbstractContainerScreen<TempleRewardMenu> {
    private static final net.minecraft.resources.ResourceLocation ABYSS = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "textures/gui/ethereal_abyss.png");

    public TempleRewardScreen(TempleRewardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 148;
        this.inventoryLabelY = 9999; // This viewer intentionally has no player inventory.
        this.titleLabelX = 9;
        this.titleLabelY = 8;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(ABYSS, leftPos, topPos, 0, 0, imageWidth, imageHeight, 326, 189);

        int left = leftPos, top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0x6020152E);
        graphics.fill(left + 3, top + 3, left + imageWidth - 3, top + imageHeight - 3, 0x503B2852);
        for (int i = 0; i < 54; i++) {
            int x = left + 7 + (i % 9) * 18;
            int y = top + 23 + (i / 9) * 18;
            graphics.fill(x, y, x + 18, y + 18, 0xFF8A73A4);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF241A36);
        }
        graphics.drawString(font, "VIEW ONLY", left + 111, top + 8, 0xFFE7D1FF, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
