package com.devaxo.templeoftheaxolotl.eclipse;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Celestial Eclipse layout. Inscription execution/networking is reserved for the next stage. */
public final class EclipseScreen extends AbstractContainerScreen<EclipseMenu> {
    private static final ResourceLocation ABYSS = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "textures/gui/ethereal_abyss.png");
    // Client-side ghost previews only: never inserted into the actual menu inventory.
    private static final ItemStack[] DYE_GHOSTS = java.util.Arrays.stream(DyeColor.values())
            .map(color -> new ItemStack(DyeItem.byColor(color)))
            .toArray(ItemStack[]::new);
    private static final ItemStack[] STYLE_GHOSTS = {
            new ItemStack(Items.GLOW_INK_SAC),
            new ItemStack(Items.BLUE_ORCHID),
            new ItemStack(Items.STRING),
            new ItemStack(Items.SKELETON_SKULL),
            new ItemStack(Items.WITHER_SKELETON_SKULL),
            new ItemStack(Items.ZOMBIE_HEAD),
            new ItemStack(Items.CREEPER_HEAD),
            new ItemStack(Items.PLAYER_HEAD),
            new ItemStack(Items.DRAGON_HEAD),
            new ItemStack(Items.PIGLIN_HEAD),
            new ItemStack(Items.IRON_SWORD)
    };
    private static final long GHOST_INTERVAL_MS = 1000L;
    private EditBox nameBox;
    private boolean uniformFont;

    public EclipseScreen(EclipseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 254;
        imageHeight = 233;
        titleLabelX = 10;
        titleLabelY = 8;
        inventoryLabelX = 47;
        inventoryLabelY = 140;
    }
    private void send(int id) {
        if (Minecraft.getInstance().gameMode != null)
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Transmutation"), b -> send(0))
                .bounds(leftPos + 10, topPos - 21, 113, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Inscription"), b -> send(1))
                .bounds(leftPos + 131, topPos - 21, 113, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+100 XP"), b -> send(2))
                .bounds(leftPos - 62, topPos + 77, 61, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-100 XP"), b -> send(3))
                .bounds(leftPos + imageWidth + 1, topPos + 77, 61, 20).build());
        if (!menu.isInscription()) {
            addRenderableWidget(Button.builder(Component.literal("TRANSMUTE"), b -> send(4))
                    .bounds(leftPos - 65, topPos + 171, 104, 20).build());
        } else {
            nameBox = new EditBox(font, leftPos + 43, topPos + 27, 168, 18, Component.literal("Inscribed name"));
            nameBox.setMaxLength(50);
            nameBox.setHint(Component.literal("Enter item name..."));
            addRenderableWidget(nameBox);
            addRenderableWidget(Button.builder(Component.literal("Font: Default"), b -> {
                uniformFont = !uniformFont;
                b.setMessage(Component.literal(uniformFont ? "Font: Uniform" : "Font: Default"));
            }).bounds(leftPos + 4, topPos + 47, 72, 17).build());
            addRenderableWidget(Button.builder(Component.literal("INSCRIBE"), b -> {
                if (nameBox != null) PacketDistributor.sendToServer(
                        new EclipseInscriptionPayload(nameBox.getValue(), uniformFont));
            }).bounds(leftPos - 65, topPos + 171, 104, 20).build());
        }
    }
    private void slotFrame(GuiGraphics g, int x, int y, int border) {
        int a = leftPos + x, b = topPos + y;
        g.fill(a - 2, b - 2, a + 18, b + 18, border);
        g.fill(a, b, a + 16, b + 16, 0xFF241C37);
    }
    private void ghost(GuiGraphics g, int slot, ItemStack icon) {
        if (!menu.slots.get(slot).hasItem()) {
            var s = menu.slots.get(slot);
            // Vanilla item sprites used as empty-slot guides, not actual inventory items.
            g.renderFakeItem(icon, leftPos + s.x, topPos + s.y);
            g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0x99241C37);
        }
    }
    private ItemStack cyclingGhost(ItemStack[] choices) {
        // World time pauses in single-player menus; a client clock keeps previews cycling.
        int index = (int) ((net.minecraft.Util.getMillis() / GHOST_INTERVAL_MS) % choices.length);
        return choices[index];
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF17112A);
        // Existing Ethereal Abyss artwork retained as a decorative panel.
        g.blit(ABYSS, leftPos + 2, topPos + 2, 0, 0, 250, 132, 326, 189);
        g.fill(leftPos + 2, topPos + 2, leftPos + 252, topPos + 137, 0xAA100B20);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, 0xFFE0BC72);
        g.fill(leftPos, topPos + 231, leftPos + imageWidth, topPos + 233, 0xFFE0BC72);
        // Eclipse ring surrounding the ritual's central slot.
        int cx = leftPos + 128, cy = topPos + 70;
        for (int a = 0; a < 360; a += 8) {
            double rad = Math.toRadians(a);
            int px = cx + (int)(Math.cos(rad) * 52), py = cy + (int)(Math.sin(rad) * 52);
            g.fill(px, py, px + 2, py + 2, 0xFFB895D1);
        }
        if (menu.isInscription()) {
            slotFrame(g, 120, 71, 0xFFE5C17E);
            slotFrame(g, 62, 112, 0xFFAC8FD0);
            slotFrame(g, 120, 112, 0xFFAC8FD0);
            slotFrame(g, 178, 112, 0xFFE5C17E);
            ghost(g, 2, cyclingGhost(DYE_GHOSTS));
            ghost(g, 3, cyclingGhost(STYLE_GHOSTS));
            ghost(g, 4, new ItemStack(Items.FEATHER));
        } else {
            slotFrame(g, 62, 62, 0xFFAC8FD0);
            slotFrame(g, 178, 62, 0xFFAC8FD0);
            slotFrame(g, 120, 62, 0xFFE5C17E);
            slotFrame(g, 120, 105, 0xFF65D7DC);
            ghost(g, 2, new ItemStack(Items.DIAMOND_BLOCK));
        }
        for (int i = 0; i < 36; i++) {
            int x = 47 + (i % 9) * 18;
            int y = i < 27 ? 151 + (i / 9) * 18 : 209;
            slotFrame(g, x, y, 0xFF786398);
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFE9D3A5, false);
        g.drawString(font, "Bank: " + menu.bankXp() + " XP", imageWidth + 5, 174, 0xFFF3D48B, false);
        if (menu.isInscription()) {
            g.drawCenteredString(font, "ITEM", 128, 59, 0xFFE9D3A5);
            g.drawCenteredString(font, "DYE", 70, 102, 0xFFE9D3A5);
            g.drawCenteredString(font, "STYLE", 128, 102, 0xFFE9D3A5);
            g.drawCenteredString(font, "FEATHER", 186, 102, 0xFFE9D3A5);
        } else {
            g.drawCenteredString(font, "ITEM I", 70, 51, 0xFFE9D3A5);
            g.drawCenteredString(font, "OUTPUT", 128, 51, 0xFFE9D3A5);
            g.drawCenteredString(font, "ITEM II", 186, 51, 0xFFE9D3A5);
            g.drawCenteredString(font, "DIAMOND", 128, 95, 0xFF8CE3E5);
            g.drawCenteredString(font, "Ritual: " + menu.displayedCost() + " XP", 127, 27, 0xFFF3D48B);
        }
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE9D3A5, false);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        renderBackground(g, mx, my, tick);
        super.render(g, mx, my, tick);
        renderTooltip(g, mx, my);
    }
}
