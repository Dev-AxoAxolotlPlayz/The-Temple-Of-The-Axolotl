package com.devaxo.templeoftheaxolotl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared fixed-anchor pixel toolbar button. Its label replaces, rather than duplicates, a tooltip. */
public final class CelestialQuickButton extends Button {
    private final int destination;
    private final String label;
    private int anchorX, anchorY;
    private boolean growLeft = true;
    // Each row is exactly eight pixels.  The old icon renderer started at pixel 5,
    // clipping the final three columns against the 18px button's right border.
    private static final String[][] ICONS = {
        {".######.",".#....#.","########","##....##","##.##.##","##....##","########","........"},
        {"...##...","..####..",".######.","########","##.##.##",".######.","..####..","........"},
        {"########","#......#","#.####.#","#.#..#.#","#.####.#","#......#","########","...##..."},
        {"#......#",".#....#.","..#..#..","...##...","...##...","..#..#..",".#....#.","#......#"},
        {"..####..",".######.","##....##","##.##.##","##....##","##.##.##",".######.","..####.."}
    };
    public CelestialQuickButton(int destination, String label, OnPress action) {
        super(0, 0, 18, 18, Component.literal(label), action, DEFAULT_NARRATION);
        this.destination = destination;
        this.label = label;
    }
    /** x is the fixed icon-square left edge, independent of expanded label width. */
    public void anchor(int x, int y, boolean expandLeft) {
        anchorX = x;
        anchorY = y;
        growLeft = expandLeft;
        setY(y);
        setX(expandLeft ? x - (getWidth() - 18) : x);
    }
    public int expandedWidth() { return 25 + Minecraft.getInstance().font.width(label); }
    public boolean iconHovered(double mouseX, double mouseY) {
        return mouseX >= anchorX && mouseX < anchorX + 18
                && mouseY >= anchorY && mouseY < anchorY + 18;
    }
    @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Calculate from the fixed 18x18 icon hitbox and currently visible label.
        // Reposition only this button, never its neighbors.
        boolean hovered = mouseX >= (growLeft ? anchorX - Math.max(0, getWidth() - 18) : anchorX)
                && mouseX < (growLeft ? anchorX + 18 : anchorX + getWidth())
                && mouseY >= anchorY && mouseY < anchorY + 18;
        int width = hovered ? expandedWidth() : 18;
        setWidth(width);
        setX(growLeft ? anchorX - (width - 18) : anchorX);
        int x = getX(), y = anchorY;
        g.fill(x, y, x + width, y + 18, 0xFFB8B8B8);
        g.fill(x + 2, y + 2, x + width - 2, y + 16, 0xFF303647);
        int iconX = growLeft ? anchorX : x;
        int color = switch (destination) {
            case 0 -> 0xFFFFD27A;
            case 1 -> 0xFFBCA4FF;
            case 2 -> 0xFF80E8F8;
            case 3 -> 0xFFFF9696;
            default -> 0xFFB9F2AC;
        };
        String[] icon = ICONS[Math.min(Math.max(destination, 0), ICONS.length - 1)];
        // 8x8 icon centered at +5,+5 in the 18px square: final pixel is +12,
        // safely inside the inner 2px border (unlike oversized scaled sprites).
        for (int iy = 0; iy < 8; iy++) for (int ix = 0; ix < 8; ix++) {
            if (icon[iy].charAt(ix) == '#')
                g.fill(iconX + 5 + ix, y + 5 + iy, iconX + 6 + ix, y + 6 + iy, color);
        }
        if (hovered) g.drawString(Minecraft.getInstance().font, label,
                growLeft ? x + 4 : x + 21, y + 5, 0xFFFFFFFF, false);
    }
}
