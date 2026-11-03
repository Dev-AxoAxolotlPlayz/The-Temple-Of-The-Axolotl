package com.devaxo.templeoftheaxolotl.shipyard;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;

public class ShipyardScreen extends AbstractContainerScreen<ShipyardMenu> {
    private Button craftButton;
    public ShipyardScreen(ShipyardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 240;
        imageHeight = 278;
        inventoryLabelY = 183;
        titleLabelX = 12;
        titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        craftButton = addRenderableWidget(Button.builder(Component.literal("Craft"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 184, topPos + 87, 49, 20).build());
        craftButton.visible = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (craftButton != null) {
            craftButton.visible = menu.canCraft();
            craftButton.active = craftButton.visible;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF12363B);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFF20565B);

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                slot(g, x + 12 + col * 18, y + 17 + row * 18);
            }
        }

        // No output slot: a valid recipe enables the Craft button instead.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(g, x + 12 + col * 18, y + 196 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(g, x + 12 + col * 18, y + 254);
        }
    }

    private void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0A2428);
        g.fill(x, y, x + 16, y + 16, 0xFF43767A);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }
}
