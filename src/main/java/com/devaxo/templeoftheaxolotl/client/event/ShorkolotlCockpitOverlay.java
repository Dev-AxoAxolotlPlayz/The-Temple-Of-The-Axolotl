package com.devaxo.templeoftheaxolotl.client.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Lightweight translucent light-blue cockpit glass for the local pilot only. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class ShorkolotlCockpitOverlay {
    private ShorkolotlCockpitOverlay() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof Shorkolotl100Entity sub)) return;
        var gui = event.getGuiGraphics();
        int w = gui.guiWidth(), h = gui.guiHeight();
        if (mc.options.getCameraType().isFirstPerson()) {
            gui.fill(0, 0, w, h, 0x174EB9DB); // subtle cockpit glass
        int rim = Math.max(5, Math.min(w, h) / 65);
        gui.fill(0, 0, w, rim, 0x8064BED4);
        gui.fill(0, h - rim, w, h, 0x8064BED4);
        gui.fill(0, 0, rim, h, 0x8064BED4);
        gui.fill(w - rim, 0, w, h, 0x8064BED4);
        }
        // Always show server-synchronized hull integrity to distinguish missing bite
        // registration from a cracked-glass rendering failure during field tests.
        String hullText = "HULL " + sub.hullIntegrity() + "%";
        int hullColor = sub.hullIntegrity() <= 25 ? 0xFFFF5555
                : sub.hullIntegrity() <= 50 ? 0xFFFFC857 : 0xFF88EEFF;
        gui.drawString(mc.font, hullText, 12, h - 30, hullColor, true);
        // Hull integrity is synchronized from the server, so third-person
        // pilots see the same damage state as first-person pilots.
        if (sub.hullIntegrity() <= 50) {
            int glass = 0xC0CDE8EF;
            crack(gui, 0, 0, 1, 1, glass);
            crack(gui, w - 1, h - 1, -1, -1, glass);
            if (sub.hullIntegrity() <= 25) {
                crack(gui, w - 1, 0, -1, 1, glass);
                crack(gui, 0, h - 1, 1, -1, glass);
                long pulse = (mc.level == null ? 0 : mc.level.getGameTime()) % 30;
                int alpha = pulse < 15 ? 0x25 : 0x12;
                int red = (alpha << 24) | 0xDD3344;
                gui.fill(0, 0, w, 3, red);
                gui.fill(0, h - 3, w, h, red);
                gui.fill(0, 0, 3, h, red);
                gui.fill(w - 3, 0, w, h, red);
            }
        }
    }

    /** Jagged cracks remain within the outer edges of the screen. */
    private static void crack(net.minecraft.client.gui.GuiGraphics g,
                              int x, int y, int dx, int dy, int color) {
        int[] run = {14, 9, 19, 7, 15, 11};
        for (int i = 0; i < run.length; i++) {
            int x2 = x + dx * run[i];
            int y2 = y + dy * (i % 2 == 0 ? 7 : 13);
            g.fill(Math.min(x, x2), Math.min(y, y2),
                    Math.max(x, x2) + 1, Math.min(y, y2) + 2, color);
            g.fill(Math.min(x2, x2 + dx), Math.min(y, y2),
                    Math.max(x2, x2 + dx) + 1, Math.max(y, y2) + 1, color);
            x = x2; y = y2;
        }
    }
}
